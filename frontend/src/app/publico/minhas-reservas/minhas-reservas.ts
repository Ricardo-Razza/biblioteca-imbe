import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { mensagemDeErro } from '../../core/api/erro-api';
import { MinhasReservas as Resultado, ReservaPublica } from '../../core/api/modelos';
import { CapaLivro } from '../../shared/capa-livro/capa-livro';
import { Selo } from '../../shared/selo/selo';
import { DadosIdentificacao, Identificacao } from '../identificacao';
import { PublicoApi } from '../publico-api';
import { protocoloLegivel, seloReservaPublica } from '../textos';

/**
 * Consulta e cancelamento pelo próprio leitor. CPF e nascimento ficam só na memória desta tela,
 * para recarregar a lista depois de cancelar; "Sair" esquece tudo.
 */
@Component({
  selector: 'app-minhas-reservas',
  imports: [RouterLink, MatButtonModule, MatIconModule, CapaLivro, Selo, Identificacao],
  template: `
    <h1>Minhas reservas</h1>

    @if (erro()) {
      <p class="aviso erro" role="alert"><mat-icon>error</mat-icon> {{ erro() }}</p>
    }
    @if (mensagem()) {
      <p class="aviso ok" role="status"><mat-icon>check_circle</mat-icon> {{ mensagem() }}</p>
    }

    @if (resultado(); as r) {
      <div class="ola">
        <span>Olá, {{ r.primeiroNome }}.</span>
        <button mat-button type="button" (click)="sair()"><mat-icon>logout</mat-icon> Sair</button>
      </div>
      @if (r.reservas.length === 0) {
        <p class="texto-secundario">Você ainda não tem reservas. <a routerLink="/">Procure um livro no catálogo.</a></p>
      } @else {
        <ul class="lista">
          @for (x of r.reservas; track x.protocolo) {
            <li>
              <app-capa-livro [obraId]="x.obraId" [temCapa]="x.temCapa" [mini]="true" [titulo]="x.titulo" />
              <div class="info">
                <a class="titulo" [routerLink]="['/livro', x.obraId]">{{ x.titulo }}</a>
                <span class="texto-secundario">Protocolo {{ legivel(x.protocolo) }}</span>
                <app-selo [tom]="selo(x).tom">{{ selo(x).texto }}</app-selo>
                @if (cancelando() === x.protocolo) {
                  <div class="aviso alerta confirmar" role="alertdialog" aria-label="Confirmar cancelamento">
                    <span>Cancelar esta reserva? O livro volta a ficar disponível para outras pessoas.</span>
                    <div class="acoes">
                      <button mat-button type="button" (click)="cancelando.set(null)">Manter</button>
                      <button mat-flat-button type="button" [disabled]="ocupado()" (click)="cancelar(x)">Cancelar reserva</button>
                    </div>
                  </div>
                } @else if (x.status === 'AGUARDANDO_RETIRADA') {
                  <div>
                    <button mat-stroked-button type="button" (click)="cancelando.set(x.protocolo)">Cancelar</button>
                  </div>
                }
              </div>
            </li>
          }
        </ul>
      }
    } @else {
      <p class="texto-secundario intro">Informe o seu CPF e a data de nascimento para ver as suas reservas.</p>
      <app-identificacao textoBotao="Ver reservas" [ocupado]="ocupado()" (enviado)="consultar($event)" />
    }
  `,
  styles: `
    h1 {
      margin-bottom: 16px;
    }
    .aviso {
      max-width: 640px;
      margin: 12px 0;
    }
    .ola {
      display: flex;
      align-items: center;
      justify-content: space-between;
      max-width: 640px;
      font: var(--mat-sys-title-medium);
    }
    .lista {
      list-style: none;
      padding: 0;
      margin: 8px 0 0;
      max-width: 640px;
    }
    .lista li {
      display: flex;
      gap: 16px;
      padding: 12px 0;
      border-bottom: 1px solid var(--mat-sys-outline-variant);
      --largura-capa: 56px;
    }
    .info {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
      gap: 6px;
      align-items: flex-start;
    }
    .titulo {
      font-weight: 500;
      color: inherit;
      text-decoration: none;
    }
    .confirmar {
      flex-direction: column;
      margin: 4px 0 0;
    }
    .intro {
      max-width: 560px;
    }
  `,
})
export class MinhasReservas {
  private readonly api = inject(PublicoApi);
  private identificacao: DadosIdentificacao | null = null;

  protected readonly resultado = signal<Resultado | null>(null);
  protected readonly cancelando = signal<string | null>(null);
  protected readonly ocupado = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly mensagem = signal<string | null>(null);
  protected readonly legivel = protocoloLegivel;
  protected readonly selo = seloReservaPublica;

  protected consultar(d: DadosIdentificacao): void {
    this.ocupado.set(true);
    this.erro.set(null);
    this.api.minhasReservas(d.cpf, d.dataNascimento).subscribe({
      next: (r) => {
        this.ocupado.set(false);
        this.identificacao = d;
        this.resultado.set(r);
      },
      error: (e) => {
        this.ocupado.set(false);
        this.erro.set(mensagemDeErro(e));
      },
    });
  }

  protected cancelar(x: ReservaPublica): void {
    const d = this.identificacao;
    if (!d) return;
    this.ocupado.set(true);
    this.erro.set(null);
    this.mensagem.set(null);
    this.api.cancelar(x.protocolo, d.cpf, d.dataNascimento).subscribe({
      next: () => {
        this.ocupado.set(false);
        this.cancelando.set(null);
        this.mensagem.set(`Reserva de "${x.titulo}" cancelada.`);
        this.recarregar(d);
      },
      error: (e) => {
        this.ocupado.set(false);
        this.erro.set(mensagemDeErro(e));
      },
    });
  }

  protected sair(): void {
    this.identificacao = null;
    this.resultado.set(null);
    this.mensagem.set(null);
    this.erro.set(null);
  }

  private recarregar(d: DadosIdentificacao): void {
    this.api.minhasReservas(d.cpf, d.dataNascimento).subscribe({
      next: (r) => this.resultado.set(r),
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }
}
