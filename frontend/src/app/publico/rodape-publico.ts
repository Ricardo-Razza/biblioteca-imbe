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
        <section class="bloco sobre" aria-labelledby="rodape-sobre">
          <h2 id="rodape-sobre">Sala Literária Castelinho</h2>
          <p>Consulte o acervo, veja o que está disponível agora e reserve para retirar no balcão.</p>
        </section>

        <section class="bloco" aria-labelledby="rodape-visite">
          <h2 id="rodape-visite">Visite a Sala Literária</h2>
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
              cadastrado com os dados da reserva, e a Sala Literária completa na retirada.
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
        <span>{{ biblioteca()?.nome ?? 'Sala Literária Castelinho' }}</span>
      </div>

      <!-- Faixa da Prefeitura, no padrão dos sistemas municipais -->
      <div class="prefeitura">
        <div class="conteudo-prefeitura">
          <img src="marca/imbe-horizontal-branca.png" alt="Prefeitura de Imbé: um lugar cheio de vida" height="56" />
          <ul class="canais">
            <li>
              <a href="https://www.imbe.rs.gov.br" rel="noopener" target="_blank">
                <mat-icon aria-hidden="true">language</mat-icon> imbe.rs.gov.br
                <span class="visualmente-oculto">(abre em nova aba)</span>
              </a>
            </li>
            <li>
              <span class="rede"><mat-icon aria-hidden="true">share</mat-icon> /prefeituraimbe</span>
            </li>
          </ul>
        </div>
        <div class="cores" aria-hidden="true"></div>
      </div>
    </footer>
  `,
  styles: `
    /* Topo do rodapé em degradê: o fundo claro da página se dissolve no marinho, sem corte seco. */
    .rodape {
      background: linear-gradient(
          180deg,
          var(--mat-sys-surface-container-low) 0,
          color-mix(in srgb, var(--marca-marinho) 30%, var(--mat-sys-surface-container-low)) 14px,
          color-mix(in srgb, var(--marca-marinho) 75%, var(--mat-sys-surface-container-low)) 28px,
          var(--marca-marinho) 44px
        )
        var(--marca-marinho);
      color: #d5deea;
      font: var(--mat-sys-body-medium);
    }
    .grade {
      max-width: 1120px;
      margin: 0 auto;
      padding: 72px 16px 32px;
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
      font-family: var(--fonte-titulo);
      font-size: 18px;
      font-weight: 500;
    }
    p {
      margin: 0;
      line-height: 1.55;
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
    .prefeitura {
      background: color-mix(in srgb, var(--marca-marinho) 82%, #000);
    }
    .conteudo-prefeitura {
      max-width: 1120px;
      margin: 0 auto;
      padding: 20px 16px;
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 16px 32px;
    }
    .conteudo-prefeitura img {
      display: block;
      height: 56px;
      width: auto;
    }
    .canais {
      list-style: none;
      display: flex;
      flex-wrap: wrap;
      gap: 8px 28px;
      font: var(--mat-sys-title-small);
      font-family: var(--fonte-marca);
    }
    .canais a,
    .rede {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      color: #fff;
      text-decoration: none;
    }
    .canais a:hover {
      text-decoration: underline;
    }
    .canais mat-icon {
      font-size: 20px;
      width: 20px;
      height: 20px;
    }
    .cores {
      height: 8px;
      background: var(--prefeitura-faixa);
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
        padding-top: 64px;
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
