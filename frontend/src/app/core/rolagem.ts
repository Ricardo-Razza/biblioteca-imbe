import { ViewportScroller } from '@angular/common';
import { EnvironmentProviders, inject, provideAppInitializer } from '@angular/core';
import { Router, Scroll } from '@angular/router';
import { filter } from 'rxjs';

/** Só o caminho, sem `?` e sem `#`: "/livro/55?x=1#a" vira "/livro/55". */
export function caminhoDe(url: string): string {
  return url.split(/[?#]/)[0];
}

/**
 * Para onde rolar depois de uma navegação. Voltar/avançar do navegador: onde o leitor estava. Âncora: até ela.
 * Página nova: o topo. Só mudou a busca da mesma página (filtro, ordem): fica onde está.
 */
export function destinoDaRolagem(
  anterior: string | null,
  atual: string,
  posicao: [number, number] | null,
  ancora: string | null,
): [number, number] | string | null {
  if (posicao) return posicao;
  if (ancora) return ancora;
  return anterior === null || caminhoDe(anterior) !== caminhoDe(atual) ? [0, 0] : null;
}

/**
 * Sem isso o Angular mantém a rolagem da página anterior: quem rolava a ficha do livro e tocava em Reservar caía
 * no rodapé do formulário. Usa os eventos `Scroll` do `withInMemoryScrolling` (com a restauração própria dele
 * desligada) para não subir a página a cada filtro do catálogo.
 */
export function provideRolagem(): EnvironmentProviders {
  return provideAppInitializer(() => {
    const router = inject(Router);
    const rolagem = inject(ViewportScroller);
    let anterior: string | null = null;
    router.events.pipe(filter((e): e is Scroll => e instanceof Scroll)).subscribe((e) => {
      const atual = 'urlAfterRedirects' in e.routerEvent ? e.routerEvent.urlAfterRedirects : e.routerEvent.url;
      const destino = destinoDaRolagem(anterior, atual, e.position, e.anchor);
      anterior = atual;
      if (typeof destino === 'string') {
        rolagem.scrollToAnchor(destino);
      } else if (destino) {
        rolagem.scrollToPosition(destino);
      }
    });
  });
}
