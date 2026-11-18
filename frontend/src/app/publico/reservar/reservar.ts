import { Component, effect, inject, input, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { catchError, of } from 'rxjs';

import { mensagemDeErro } from '../../core/api/erro-api';
import { ObraPublicaDetalhe, ReservaPublica } from '../../core/api/modelos';
import { CapaLivro } from '../../shared/capa-livro/capa-livro';
import { DadosIdentificacao, Identificacao } from '../identificacao';
import { PublicoApi } from '../publico-api';
import { dataComDia, protocoloLegivel } from '../textos';

/** Reserva: CPF + data de nascimento → protocolo, prazo e onde retirar. */
@Component({
  selector: 'app-reservar',
  imports: [RouterLink, MatButtonModule, MatIconModule, CapaLivro, Identificacao],
  template: `
    @if (reserva(); as r) {
      <section class="feita" aria-labelledby="titulo-feita">
        <mat-icon class="icone-ok" aria-hidden="true">check_circle</mat-icon>
        <h1 id="titulo-feita">Reserva feita, {{ r.primeiroNome }}</h1>
        <p>O livro <strong>{{ r.titulo }}</strong> está separado para você.</p>

        <div class="protocolo">
          <span class="texto-secundario">Protocolo</span>
          <strong>{{ legivel(r.protocolo) }}</strong>
        </div>
        <p class="prazo">Retire até <strong>{{ comDia(r.prazoRetirada) }}</strong>. Depois disso, a reserva é cancelada e o livro volta para o acervo.</p>

        @if (biblioteca(); as b) {
          @if (b.endereco || b.horario) {
            <div class="onde">
              <strong>{{ b.nome }}</strong>
              @if (b.endereco) {
                <span><mat-icon aria-hidden="true">location_on</mat-icon> {{ b.endereco }}</span>
              }
              @if (b.horario) {
                <span><mat-icon aria-hidden="true">schedule</mat-icon> {{ b.horario }}</span>
              }
            </div>
          }
        }
        <p class="texto-secundario">Anote o protocolo. Você pode consultar ou cancelar a reserva em "Minhas reservas".</p>
        <div class="acoes">
          <a mat-flat-button routerLink="/minhas-reservas">Minhas reservas</a>
          <a mat-button routerLink="/">Voltar ao catálogo</a>
        </div>
      </section>
    } @else {
      <a class="voltar" [routerLink]="['/livro', id()]"><mat-icon>arrow_back</mat-icon> Voltar ao livro</a>
      <h1>Reservar</h1>

      @if (obra(); as o) {
        <div class="livro">
          <app-capa-livro [obraId]="o.id" [temCapa]="o.temCapa" [mini]="true" [titulo]="o.titulo" />
          <div>
            <strong>{{ o.titulo }}</strong>
            @if (o.autores.length) {
              <span class="texto-secundario">{{ o.autores.join(', ') }}</span>
            }
          </div>
        </div>
        @if (o.disponiveis === 0) {
          <p class="aviso alerta" role="status">
            <mat-icon>info</mat-icon>
            <span>Todos os exemplares deste livro estão emprestados ou reservados agora.</span>
          </p>
        }
      }

      <p class="regras">
        Para reservar, você precisa ter cadastro na biblioteca. Informe o seu CPF, a data de nascimento e um telefone
        para a biblioteca falar com você sobre a reserva.
        @if (biblioteca(); as b) {
          O livro fica separado por {{ b.prazoRetiradaDiasUteis === 1 ? '1 dia útil' : b.prazoRetiradaDiasUteis + ' dias úteis' }},
          e cada pessoa pode ter até {{ b.limiteLivros }} livros, somando empréstimos e reservas.
        }
      </p>
      <p class="texto-secundario">Ainda não tem cadastro? Faça na biblioteca, com um documento com foto.</p>

      @if (erro()) {
        <p class="aviso erro" role="alert"><mat-icon>error</mat-icon> {{ erro() }}</p>
      }
      <app-identificacao textoBotao="Reservar" [pedirContato]="true" [ocupado]="ocupado()" (enviado)="reservar($event)" />
    }
  `,
  styles: `
    .voltar {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      margin-bottom: 16px;
      text-decoration: none;
    }
    h1 {
      margin-bottom: 16px;
    }
    .livro {
      display: flex;
      gap: 12px;
      align-items: center;
      margin-bottom: 16px;
      --largura-capa: 56px;
    }
    .livro div {
      display: flex;
      flex-direction: column;
    }
    .regras {
      max-width: 560px;
      line-height: 1.5;
    }
    .aviso {
      max-width: 560px;
      margin: 12px 0;
    }
    .feita {
      max-width: 560px;
    }
    .icone-ok {
      color: var(--situacao-ok);
      font-size: 48px;
      width: 48px;
      height: 48px;
    }
    .protocolo {
      display: flex;
      flex-direction: column;
      gap: 4px;
      margin: 20px 0;
      padding: 16px 20px;
      border-radius: 12px;
      background: var(--mat-sys-surface-container);
    }
    .protocolo strong {
      font: var(--mat-sys-headline-medium);
      font-family: ui-monospace, 'Roboto Mono', monospace;
      letter-spacing: 2px;
    }
    .prazo {
      line-height: 1.5;
    }
    .onde {
      display: flex;
      flex-direction: column;
      gap: 4px;
      margin: 16px 0;
    }
    .onde span {
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .acoes {
      margin-top: 16px;
    }
  `,
})
export class Reservar {
  readonly id = input.required<string>();

  private readonly api = inject(PublicoApi);
  protected readonly biblioteca = toSignal(this.api.biblioteca().pipe(catchError(() => of(null))), { initialValue: null });
  protected readonly obra = signal<ObraPublicaDetalhe | null>(null);
  protected readonly reserva = signal<ReservaPublica | null>(null);
  protected readonly ocupado = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly legivel = protocoloLegivel;
  protected readonly comDia = dataComDia;

  constructor() {
    effect((aoLimpar) => {
      const s = this.api.obra(Number(this.id())).subscribe({
        next: (o) => this.obra.set(o),
        error: (e) => this.erro.set(mensagemDeErro(e)),
      });
      aoLimpar(() => s.unsubscribe());
    });
  }

  protected reservar(d: DadosIdentificacao): void {
    this.ocupado.set(true);
    this.erro.set(null);
    this.api.reservar(Number(this.id()), d.cpf, d.dataNascimento, d.telefone ?? '', d.email ?? null).subscribe({
      next: (r) => {
        this.ocupado.set(false);
        this.reserva.set(r);
        window.scrollTo({ top: 0 });
      },
      error: (e) => {
        this.ocupado.set(false);
        this.erro.set(mensagemDeErro(e));
      },
    });
  }
}
