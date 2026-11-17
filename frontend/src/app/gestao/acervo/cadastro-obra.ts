import { Component, DestroyRef, computed, inject, signal, viewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { catchError, map, of, switchMap } from 'rxjs';

import { CampoInvalido, camposComErro, mensagemDeErro } from '../../core/api/erro-api';
import { IsbnResposta, NOME_FONTE, ObraRequisicao } from '../../core/api/modelos';
import { CampoIsbn } from '../../shared/campo-isbn/campo-isbn';
import { CapaLivro } from '../../shared/capa-livro/capa-livro';
import { AcervoApi } from './acervo-api';
import { DadosFormularioObra, FormularioObra } from './formulario-obra';

const TAMANHO_MAXIMO_CAPA = 10 * 1024 * 1024;

/**
 * Cadastro pelo ISBN: bipa, o backend busca nas fontes e o formulário volta preenchido para conferência.
 * Nada é salvo sem o funcionário confirmar. Obra já cadastrada: só oferece adicionar exemplar.
 */
@Component({
  selector: 'app-cadastro-obra',
  imports: [RouterLink, MatButtonModule, MatIconModule, CampoIsbn, CapaLivro, FormularioObra],
  template: `
    <div class="cabecalho">
      <a mat-button routerLink="/painel/acervo"><mat-icon>arrow_back</mat-icon> Acervo</a>
      <h1>Cadastrar livro</h1>
    </div>

    @if (!resposta()) {
      <p class="texto-secundario">O sistema busca título, autores e capa na BrasilAPI, no Google Books e na Open Library.</p>
      <app-campo-isbn [carregando]="buscando()" (buscar)="buscar($event)" />
      @if (erro()) {
        <p class="aviso erro" role="alert"><mat-icon>error</mat-icon> {{ erro() }}</p>
      }
    } @else {
      @let r = resposta()!;
      @if (r.obraId) {
        <div class="aviso ok" role="status">
          <mat-icon>check_circle</mat-icon>
          <span>O ISBN {{ isbnFormatado() }} já está no acervo. Para mais uma cópia, adicione um exemplar.</span>
        </div>
        <div class="acoes">
          <a mat-flat-button [routerLink]="['/painel/acervo', r.obraId]" [queryParams]="{ novoExemplar: 1 }">
            <mat-icon>add</mat-icon> Adicionar exemplar
          </a>
          <button mat-button type="button" (click)="outroIsbn()">Outro ISBN</button>
        </div>
      } @else {
        <div class="acoes topo">
          <span>ISBN <strong>{{ isbnFormatado() }}</strong></span>
          <button mat-button type="button" (click)="outroIsbn()"><mat-icon>barcode_scanner</mat-icon> Outro ISBN</button>
        </div>

        @if (r.encontrado) {
          <p class="aviso" role="status">
            <mat-icon>info</mat-icon>
            <span>Dados de {{ nomesFontes() }}. Confira antes de salvar.</span>
          </p>
        } @else {
          <p class="aviso alerta" role="status">
            <mat-icon>edit_note</mat-icon>
            <span>Nenhuma fonte conhece este ISBN. Preencha os dados e envie uma foto da capa.</span>
          </p>
        }
        @if (r.fontesIndisponiveis.length) {
          <p class="aviso alerta" role="status">
            <mat-icon>cloud_off</mat-icon>
            <span>Sem resposta de {{ nomesIndisponiveis() }}. Se faltar algum dado, tente de novo em alguns minutos.</span>
            <button mat-button type="button" (click)="buscar(r.isbn13)">Tentar de novo</button>
          </p>
        }
        @if (erro()) {
          <p class="aviso erro" role="alert"><mat-icon>error</mat-icon> {{ erro() }}</p>
        }

        <div class="conferencia">
          <div class="capa">
            <app-capa-livro [src]="previaCapa()" [titulo]="inicial()?.titulo ?? ''" />
            <input #arquivo type="file" accept="image/*" hidden (change)="escolherCapa(arquivo)" />
            <button mat-stroked-button type="button" (click)="arquivo.click()">
              <mat-icon>photo_camera</mat-icon> {{ previaCapa() ? 'Trocar foto' : 'Enviar foto' }}
            </button>
            @if (arquivoCapa()) {
              <button mat-button type="button" (click)="removerFoto()">Usar a capa da fonte</button>
            } @else if (r.dados?.capaDisponivel) {
              <span class="texto-secundario legenda">Capa de {{ nomeFonte(r.dados!.fonte) }}</span>
            }
          </div>
          <app-formulario-obra
            [inicial]="inicial()!"
            [salvando]="salvando()"
            [errosServidor]="errosCampos()"
            textoSalvar="Salvar obra"
            (salvar)="salvar($event)"
            (cancelar)="outroIsbn()"
            (errosSemCampo)="mostrarSobras($event)"
          />
        </div>
      }
    }
  `,
  styles: `
    .cabecalho {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 4px;
      margin-bottom: 16px;
    }
    .cabecalho a {
      margin-left: -12px;
    }
    .aviso {
      margin: 12px 0;
      max-width: 760px;
    }
    .topo {
      margin-bottom: 4px;
    }
    .conferencia {
      display: flex;
      gap: 24px;
      align-items: flex-start;
      margin-top: 16px;
    }
    .capa {
      display: flex;
      flex-direction: column;
      gap: 8px;
      align-items: stretch;
      --largura-capa: 160px;
    }
    .legenda {
      font: var(--mat-sys-body-small);
      text-align: center;
    }
    app-formulario-obra {
      flex: 1;
    }
    @media (max-width: 760px) {
      .conferencia {
        flex-direction: column;
      }
    }
  `,
})
export class CadastroObra {
  private readonly api = inject(AcervoApi);
  private readonly router = inject(Router);
  private readonly campo = viewChild(CampoIsbn);

  protected readonly buscando = signal(false);
  protected readonly salvando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly resposta = signal<IsbnResposta | null>(null);
  protected readonly errosCampos = signal<CampoInvalido[]>([]);
  protected readonly arquivoCapa = signal<File | null>(null);
  private readonly urlArquivo = signal<string | null>(null);

  protected readonly isbnFormatado = computed(() => this.resposta()?.isbn13 ?? '');
  protected readonly nomesFontes = computed(() => juntar((this.resposta()?.fontes ?? []).map((f) => NOME_FONTE[f])));
  protected readonly nomesIndisponiveis = computed(() =>
    juntar((this.resposta()?.fontesIndisponiveis ?? []).map((f) => NOME_FONTE[f])),
  );

  protected readonly inicial = computed<DadosFormularioObra | null>(() => {
    const r = this.resposta();
    if (!r) {
      return null;
    }
    const d = r.dados;
    return {
      isbn: r.isbn13,
      titulo: d?.titulo,
      subtitulo: d?.subtitulo,
      autores: d?.autores,
      editora: d?.editora,
      ano: d?.ano,
      paginas: d?.paginas,
      idioma: d?.idioma,
      sinopse: d?.sinopse,
      assuntos: d?.assuntos,
      fonteDados: d?.fonte ?? 'MANUAL',
    };
  });

  protected readonly previaCapa = computed(() => {
    if (this.urlArquivo()) {
      return this.urlArquivo();
    }
    const r = this.resposta();
    return r?.dados?.capaDisponivel ? this.api.previaCapaIsbn(r.isbn13) : null;
  });

  constructor() {
    inject(DestroyRef).onDestroy(() => this.liberarArquivo());
  }

  protected nomeFonte = (f: keyof typeof NOME_FONTE) => NOME_FONTE[f];

  protected buscar(isbn13: string): void {
    this.buscando.set(true);
    this.erro.set(null);
    this.api.buscarIsbn(isbn13).subscribe({
      next: (r) => {
        this.buscando.set(false);
        this.errosCampos.set([]);
        this.resposta.set(r);
      },
      error: (e) => {
        this.buscando.set(false);
        this.erro.set(mensagemDeErro(e));
        this.campo()?.focar();
      },
    });
  }

  protected outroIsbn(): void {
    this.resposta.set(null);
    this.erro.set(null);
    this.errosCampos.set([]);
    this.removerFoto();
  }

  protected escolherCapa(entrada: HTMLInputElement): void {
    const arquivo = entrada.files?.[0];
    entrada.value = '';
    if (!arquivo) {
      return;
    }
    if (!arquivo.type.startsWith('image/')) {
      this.erro.set('Escolha um arquivo de imagem (JPG ou PNG).');
      return;
    }
    if (arquivo.size > TAMANHO_MAXIMO_CAPA) {
      this.erro.set('A foto passa de 10 MB. Escolha uma imagem menor.');
      return;
    }
    this.erro.set(null);
    this.liberarArquivo();
    this.arquivoCapa.set(arquivo);
    this.urlArquivo.set(URL.createObjectURL(arquivo));
  }

  protected removerFoto(): void {
    this.liberarArquivo();
    this.arquivoCapa.set(null);
  }

  protected mostrarSobras(mensagens: string[]): void {
    if (mensagens.length) {
      this.erro.set(mensagens.join(' '));
    }
  }

  protected salvar(req: ObraRequisicao): void {
    this.salvando.set(true);
    this.erro.set(null);
    this.errosCampos.set([]);
    const foto = this.arquivoCapa();
    this.api
      .cadastrarObra(req)
      .pipe(
        switchMap((obra) =>
          foto
            ? this.api.enviarCapa(obra.id, foto).pipe(
                map((o) => ({ obra: o, capaFalhou: false })),
                catchError(() => of({ obra, capaFalhou: true })),
              )
            : of({ obra, capaFalhou: false }),
        ),
      )
      .subscribe({
        next: ({ obra, capaFalhou }) => {
          this.salvando.set(false);
          const queryParams: Record<string, string> = { novoExemplar: '1' };
          if (capaFalhou) {
            queryParams['capa'] = 'falhou';
          }
          this.router.navigate(['/painel/acervo', obra.id], { queryParams });
        },
        error: (e) => {
          this.salvando.set(false);
          this.erro.set(mensagemDeErro(e));
          this.errosCampos.set(camposComErro(e));
        },
      });
  }

  private liberarArquivo(): void {
    const url = this.urlArquivo();
    if (url) {
      URL.revokeObjectURL(url);
      this.urlArquivo.set(null);
    }
  }
}

function juntar(nomes: string[]): string {
  if (nomes.length <= 1) {
    return nomes.join('');
  }
  return nomes.slice(0, -1).join(', ') + ' e ' + nomes[nomes.length - 1];
}
