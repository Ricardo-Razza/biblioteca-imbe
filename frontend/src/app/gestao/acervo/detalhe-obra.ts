import { DatePipe } from '@angular/common';
import { Component, ElementRef, computed, effect, inject, input, numberAttribute, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';

import { camposComErro, mensagemDeErro } from '../../core/api/erro-api';
import { Exemplar, ExemplarAlteracao, Obra, SituacaoExemplar } from '../../core/api/modelos';
import { CapaLivro } from '../../shared/capa-livro/capa-livro';
import { aplicarErrosDoServidor, hojeIso, mensagemDoCampo, textoOuNulo } from '../../shared/formularios';
import { SeloSituacao } from '../../shared/selo-situacao/selo-situacao';
import { Sessao } from '../../core/auth/sessao';
import { AcervoApi } from './acervo-api';

const TAMANHO_MAXIMO_CAPA = 10 * 1024 * 1024;

type AcaoSituacao = { exemplar: Exemplar; situacao: SituacaoExemplar };

const PERGUNTAS: Partial<Record<SituacaoExemplar, (tombo: string) => string>> = {
  EXTRAVIADO: (t) => `Marcar o exemplar ${t} como extraviado? Ele deixa de aparecer como disponível.`,
  BAIXADO: (t) => `Dar baixa no exemplar ${t}? A baixa é definitiva: o exemplar sai do acervo e não volta.`,
  DISPONIVEL: (t) => `O exemplar ${t} foi encontrado e volta a ficar disponível?`,
};

const CONFIRMACOES: Partial<Record<SituacaoExemplar, string>> = {
  EXTRAVIADO: 'Marcar extraviado',
  BAIXADO: 'Dar baixa',
  DISPONIVEL: 'Voltou ao acervo',
};

/** Ficha da obra na gestão: dados, capa e exemplares (adicionar, editar, extravio e baixa). */
@Component({
  selector: 'app-detalhe-obra',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatMenuModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    CapaLivro,
    SeloSituacao,
  ],
  templateUrl: './detalhe-obra.html',
  styleUrl: './detalhe-obra.scss',
})
export class DetalheObra {
  readonly id = input.required({ transform: numberAttribute });
  /** `?novoExemplar=1`: abre o formulário de exemplar (vindo do cadastro). */
  readonly novoExemplar = input<string>();
  /** `?capa=falhou`: a obra foi salva, mas o envio da foto não. */
  readonly capa = input<string>();

  private readonly api = inject(AcervoApi);
  /** Mudar exemplar (localização, estado, baixa, extravio) é do administrador. */
  protected readonly sessao = inject(Sessao);
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly campoTombo = viewChild<ElementRef<HTMLInputElement>>('campoTombo');

  protected readonly obra = signal<Obra | null>(null);
  protected readonly exemplares = signal<Exemplar[]>([]);
  protected readonly erroCarga = signal<string | null>(null);
  protected readonly versaoCapa = signal<number | null>(null);
  protected readonly enviandoCapa = signal(false);
  protected readonly erroCapa = signal<string | null>(null);

  protected readonly formAberto = signal(false);
  protected readonly salvandoExemplar = signal(false);
  protected readonly erroExemplar = signal<string | null>(null);
  protected readonly adicionado = signal<string | null>(null);

  protected readonly confirmando = signal<AcaoSituacao | null>(null);
  protected readonly editando = signal<Exemplar | null>(null);
  protected readonly salvandoAlteracao = signal(false);
  protected readonly erroAlteracao = signal<string | null>(null);

  protected readonly hoje = hojeIso();
  protected readonly isbn = computed(() => this.obra()?.isbn13 ?? '');
  protected readonly detalhes = computed(() => {
    const o = this.obra();
    if (!o) return '';
    return [o.editora, o.ano, o.paginas ? `${o.paginas} páginas` : null, o.idioma].filter((x) => x).join(' · ');
  });
  protected readonly pergunta = computed(() => {
    const c = this.confirmando();
    return c ? (PERGUNTAS[c.situacao]?.(c.exemplar.tombo) ?? '') : '';
  });
  protected readonly textoConfirmar = computed(() => {
    const c = this.confirmando();
    return c ? (CONFIRMACOES[c.situacao] ?? 'Confirmar') : '';
  });

  protected readonly formExemplar = this.fb.group({
    tombo: ['', [Validators.required, Validators.maxLength(30), Validators.pattern(/^\s*[A-Za-z0-9.-]{1,30}\s*$/)]],
    localizacao: ['', [Validators.maxLength(100)]],
    estado: this.fb.control<'BOM' | 'DANIFICADO'>('BOM'),
    origem: this.fb.control<'COMPRA' | 'DOACAO' | null>(null),
    dataAquisicao: [''],
  });

  protected readonly formAlteracao = this.fb.group({
    localizacao: ['', [Validators.maxLength(100)]],
    estado: this.fb.control<'BOM' | 'DANIFICADO'>('BOM'),
  });

  constructor() {
    effect(() => this.carregar(this.id()));
    effect(() => {
      if (this.novoExemplar()) {
        this.abrirFormulario();
      }
    });
  }

  protected erroTombo(): string {
    const c = this.formExemplar.controls.tombo;
    if (c.errors?.['pattern']) {
      return 'Use até 30 letras, números, ponto ou hífen, sem espaços nem barra.';
    }
    return mensagemDoCampo(c);
  }

  protected erro(campo: string): string {
    return mensagemDoCampo(this.formExemplar.get(campo));
  }

  protected acoesDe(e: Exemplar): SituacaoExemplar[] {
    switch (e.situacao) {
      case 'DISPONIVEL':
        return ['EXTRAVIADO', 'BAIXADO'];
      case 'EXTRAVIADO':
        return ['DISPONIVEL', 'BAIXADO'];
      default:
        return [];
    }
  }

  protected textoAcao(s: SituacaoExemplar): string {
    return CONFIRMACOES[s] ?? s;
  }

  protected abrirFormulario(): void {
    this.formAberto.set(true);
    this.adicionado.set(null);
    setTimeout(() => this.campoTombo()?.nativeElement.focus());
  }

  protected fecharFormulario(): void {
    this.formAberto.set(false);
    this.erroExemplar.set(null);
    this.formExemplar.reset();
  }

  protected adicionarExemplar(): void {
    if (this.formExemplar.invalid) {
      this.formExemplar.markAllAsTouched();
      return;
    }
    const v = this.formExemplar.getRawValue();
    if (v.dataAquisicao && v.dataAquisicao > this.hoje) {
      this.formExemplar.controls.dataAquisicao.setErrors({ futura: true });
      this.formExemplar.controls.dataAquisicao.markAsTouched();
      return;
    }
    this.salvandoExemplar.set(true);
    this.erroExemplar.set(null);
    this.api
      .cadastrarExemplar(this.id(), {
        tombo: v.tombo.trim(),
        localizacao: textoOuNulo(v.localizacao),
        estado: v.estado,
        origem: v.origem,
        dataAquisicao: v.dataAquisicao || null,
      })
      .subscribe({
        next: (e) => {
          this.salvandoExemplar.set(false);
          this.adicionado.set(e.tombo);
          // Mantém localização, origem e data para a próxima cópia; só o tombo muda.
          this.formExemplar.controls.tombo.reset('');
          this.campoTombo()?.nativeElement.focus();
          this.recarregar();
        },
        error: (err) => {
          this.salvandoExemplar.set(false);
          this.adicionado.set(null);
          if (err?.status === 409) {
            // Tombo repetido: o aviso vai no próprio campo.
            aplicarErrosDoServidor(this.formExemplar, [{ campo: 'tombo', mensagem: mensagemDeErro(err) }]);
            this.campoTombo()?.nativeElement.select();
            return;
          }
          const campos = camposComErro(err);
          const sobras = aplicarErrosDoServidor(this.formExemplar, campos);
          this.erroExemplar.set(sobras.length || !campos.length ? mensagemDeErro(err) : null);
        },
      });
  }

  protected pedirSituacao(exemplar: Exemplar, situacao: SituacaoExemplar): void {
    this.editando.set(null);
    this.erroAlteracao.set(null);
    this.confirmando.set({ exemplar, situacao });
  }

  protected confirmarSituacao(): void {
    const c = this.confirmando();
    if (c) {
      this.alterar(c.exemplar, { situacao: c.situacao });
    }
  }

  protected editar(e: Exemplar): void {
    this.confirmando.set(null);
    this.erroAlteracao.set(null);
    this.formAlteracao.reset({ localizacao: e.localizacao ?? '', estado: e.estado });
    this.editando.set(e);
  }

  protected salvarEdicao(): void {
    const e = this.editando();
    if (!e || this.formAlteracao.invalid) {
      this.formAlteracao.markAllAsTouched();
      return;
    }
    const v = this.formAlteracao.getRawValue();
    this.alterar(e, { localizacao: v.localizacao.trim(), estado: v.estado });
  }

  protected cancelarAlteracao(): void {
    this.confirmando.set(null);
    this.editando.set(null);
    this.erroAlteracao.set(null);
  }

  protected enviarCapa(entrada: HTMLInputElement): void {
    const arquivo = entrada.files?.[0];
    entrada.value = '';
    if (!arquivo) return;
    if (!arquivo.type.startsWith('image/')) {
      this.erroCapa.set('Escolha um arquivo de imagem (JPG ou PNG).');
      return;
    }
    if (arquivo.size > TAMANHO_MAXIMO_CAPA) {
      this.erroCapa.set('A foto passa de 10 MB. Escolha uma imagem menor.');
      return;
    }
    this.enviandoCapa.set(true);
    this.erroCapa.set(null);
    this.api.enviarCapa(this.id(), arquivo).subscribe({
      next: (o) => {
        this.enviandoCapa.set(false);
        this.obra.set(o);
        this.versaoCapa.set(Date.now());
      },
      error: (err) => {
        this.enviandoCapa.set(false);
        this.erroCapa.set(mensagemDeErro(err));
      },
    });
  }

  private alterar(e: Exemplar, alt: ExemplarAlteracao): void {
    this.salvandoAlteracao.set(true);
    this.erroAlteracao.set(null);
    this.api.alterarExemplar(e.id, alt).subscribe({
      next: () => {
        this.salvandoAlteracao.set(false);
        this.cancelarAlteracao();
        this.recarregar();
      },
      error: (err) => {
        this.salvandoAlteracao.set(false);
        this.erroAlteracao.set(mensagemDeErro(err));
      },
    });
  }

  private carregar(id: number): void {
    this.erroCarga.set(null);
    this.api.obra(id).subscribe({
      next: (o) => this.obra.set(o),
      error: (err) => this.erroCarga.set(mensagemDeErro(err)),
    });
    this.api.exemplares(id).subscribe({
      next: (lista) => this.exemplares.set(lista),
      error: () => this.exemplares.set([]),
    });
  }

  private recarregar(): void {
    this.carregar(this.id());
  }
}
