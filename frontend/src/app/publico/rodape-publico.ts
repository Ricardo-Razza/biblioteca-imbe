import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

import { DadosBiblioteca } from '../core/api/modelos';

/**
 * Rodapé do site público: marca da Prefeitura, como visitar, como reservar (com os prazos configurados)
 * e acesso rápido. Fundo marinho com a marca branca, como pede o manual para fundo escuro.
 */
@Component({
  selector: 'app-rodape-publico',
  imports: [RouterLink, MatIconModule],
  template: `
    <footer class="rodape">
      <div class="grade">
        <section class="bloco sobre" aria-label="Prefeitura de Imbé">
          <img src="marca/imbe-horizontal-branca.png" alt="Prefeitura de Imbé: um lugar cheio de vida" height="64" />
          <p>Consulte o acervo da Biblioteca Municipal, veja o que está disponível agora e reserve para retirar no balcão.</p>
        </section>

        <section class="bloco" aria-labelledby="rodape-visite">
          <h2 id="rodape-visite">Visite a biblioteca</h2>
          @if (temContato()) {
            <ul class="contato">
              @if (biblioteca()?.endereco; as endereco) {
                <li>
                  <mat-icon aria-hidden="true">location_on</mat-icon>
                  <span>
                    {{ endereco }}
                    <a class="mapa" [href]="linkMapa(endereco)" rel="noopener" target="_blank">
                      Ver no mapa <mat-icon class="externo" aria-label="abre em nova aba">open_in_new</mat-icon>
                    </a>
                  </span>
                </li>
              }
              @if (biblioteca()?.horario; as horario) {
                <li><mat-icon aria-hidden="true">schedule</mat-icon><span>{{ horario }}</span></li>
              }
              @if (biblioteca()?.telefone; as telefone) {
                <li>
                  <mat-icon aria-hidden="true">call</mat-icon>
                  <a [href]="'tel:' + soDigitos(telefone)">{{ telefone }}</a>
                </li>
              }
              @if (biblioteca()?.email; as email) {
                <li>
                  <mat-icon aria-hidden="true">mail</mat-icon>
                  <a [href]="'mailto:' + email">{{ email }}</a>
                </li>
              }
            </ul>
          } @else {
            <p class="suave">Endereço e horário de atendimento em breve.</p>
          }
        </section>

        <section class="bloco" aria-labelledby="rodape-reservar">
          <h2 id="rodape-reservar">Como reservar</h2>
          <ol class="passos">
            <li><span>Encontre o livro no <a routerLink="/">catálogo</a> e toque em Reservar.</span></li>
            <li><span>Informe nome, CPF, data de nascimento e telefone.</span></li>
            <li><span>Retire no balcão em até {{ diasUteis(biblioteca()?.prazoRetiradaDiasUteis) }}, com um documento com foto.</span></li>
          </ol>
          @if (biblioteca(); as b) {
            <p class="suave">
              Cada leitor fica com até {{ b.limiteLivros }} {{ b.limiteLivros === 1 ? 'livro' : 'livros' }} por vez, por
              {{ diasUteis(b.prazoEmprestimoDias) }}. Não precisa ter cadastro para reservar: quem ainda não tem é
              cadastrado com os dados da reserva, e a biblioteca completa na retirada.
            </p>
          }
        </section>

        <nav class="bloco" aria-labelledby="rodape-acesso">
          <h2 id="rodape-acesso">Acesso rápido</h2>
          <ul class="links">
            <li><a routerLink="/">Catálogo</a></li>
            <li><a routerLink="/minhas-reservas">Minhas reservas</a></li>
            <li>
              <a href="https://www.imbe.rs.gov.br" rel="noopener" target="_blank">
                Site da Prefeitura <mat-icon class="externo" aria-label="abre em nova aba">open_in_new</mat-icon>
              </a>
            </li>
            <li><a routerLink="/login">Área da equipe</a></li>
          </ul>
        </nav>
      </div>

      <div class="base">
        <span>© {{ ano }} Prefeitura Municipal de Imbé</span>
        <span>{{ biblioteca()?.nome ?? 'Biblioteca Municipal de Imbé' }}</span>
      </div>
    </footer>
  `,
  styles: `
    .rodape {
      background: var(--marca-marinho);
      color: #d5deea;
      font: var(--mat-sys-body-medium);
      border-top: 6px solid transparent;
      border-image: var(--marca-faixa) 1;
    }
    .grade {
      max-width: 1120px;
      margin: 0 auto;
      padding: 40px 16px 32px;
      display: grid;
      grid-template-columns: 1.3fr 1fr 1.2fr 0.8fr;
      gap: 32px;
    }
    .bloco {
      min-width: 0;
    }
    h2 {
      margin: 0 0 14px;
      color: #fff;
      font-family: var(--fonte-marca);
      font-size: 13px;
      font-weight: 600;
      letter-spacing: 0.06em;
      text-transform: uppercase;
    }
    p {
      margin: 0;
      line-height: 1.55;
    }
    .sobre img {
      display: block;
      height: 64px;
      width: auto;
      margin-bottom: 16px;
    }
    .suave {
      color: #aebbd0;
      margin-top: 12px;
    }
    h2 + .suave {
      margin-top: 0;
    }
    ul,
    ol {
      margin: 0;
      padding: 0;
    }
    .contato,
    .links {
      list-style: none;
      display: flex;
      flex-direction: column;
      gap: 10px;
    }
    .contato li {
      display: flex;
      gap: 8px;
      align-items: flex-start;
      line-height: 1.45;
    }
    .contato li > span {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
    }
    .contato mat-icon {
      flex: none;
      font-size: 20px;
      width: 20px;
      height: 20px;
      color: var(--marca-azul-claro);
    }
    .passos {
      counter-reset: passo;
      list-style: none;
      display: flex;
      flex-direction: column;
      gap: 10px;
    }
    .passos li {
      counter-increment: passo;
      display: flex;
      gap: 10px;
      align-items: flex-start;
      line-height: 1.45;
    }
    .passos li::before {
      content: counter(passo);
      flex: none;
      width: 22px;
      height: 22px;
      border-radius: 50%;
      background: var(--marca-azul-claro);
      color: var(--marca-marinho);
      font: 600 12px/22px var(--fonte-marca);
      text-align: center;
    }
    a {
      color: #fff;
      text-underline-offset: 3px;
    }
    .links a {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      text-decoration: none;
    }
    .links a:hover {
      text-decoration: underline;
    }
    .externo {
      font-size: 16px;
      width: 16px;
      height: 16px;
    }
    .mapa {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      margin-top: 4px;
      font-weight: 500;
    }
    .contato .mapa .externo {
      color: inherit;
    }
    .base {
      max-width: 1120px;
      margin: 0 auto;
      padding: 16px;
      display: flex;
      flex-wrap: wrap;
      justify-content: space-between;
      gap: 8px 24px;
      border-top: 1px solid rgb(255 255 255 / 0.14);
      color: #aebbd0;
      font: var(--mat-sys-body-small);
    }
    @media (max-width: 900px) {
      .grade {
        grid-template-columns: 1fr 1fr;
      }
    }
    @media (max-width: 600px) {
      .grade {
        grid-template-columns: 1fr;
        gap: 28px;
        padding-top: 32px;
      }
    }
  `,
})
export class RodapePublico {
  readonly biblioteca = input<DadosBiblioteca | null>(null);
  protected readonly ano = new Date().getFullYear();
  protected readonly temContato = computed(() => {
    const b = this.biblioteca();
    return !!b && !!(b.endereco || b.horario || b.telefone || b.email);
  });

  protected diasUteis(n: number | undefined): string {
    if (!n) return 'alguns dias úteis';
    return n === 1 ? '1 dia útil' : `${n} dias úteis`;
  }

  /** Busca o endereço no Google Maps, sempre em Imbé (endereço da biblioteca, nunca dado de leitor). */
  protected linkMapa(endereco: string): string {
    return 'https://www.google.com/maps/search/?api=1&query=' + encodeURIComponent(`${endereco}, Imbé - RS`);
  }

  protected soDigitos(telefone: string): string {
    return telefone.replace(/\D/g, '');
  }
}
