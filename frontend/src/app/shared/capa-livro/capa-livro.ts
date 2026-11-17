import { Component, computed, effect, input, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

/**
 * Capa do livro, sempre servida pelo backend (nunca link externo). Sem imagem, ou se ela falhar,
 * mostra um quadro neutro com ícone. Proporção fixa de 2:3.
 *
 * Use `obraId` para a capa salva, ou `src` para uma prévia (fonte do ISBN, arquivo escolhido).
 * `versao` muda a URL depois de trocar a capa, para o navegador não mostrar a antiga.
 */
@Component({
  selector: 'app-capa-livro',
  imports: [MatIconModule],
  template: `
    @if (url() && !falhou()) {
      <img [src]="url()" [alt]="'Capa de ' + titulo()" loading="lazy" (error)="falhou.set(true)" />
    } @else {
      <div class="vazia" role="img" [attr.aria-label]="'Sem capa: ' + titulo()">
        <mat-icon>menu_book</mat-icon>
      </div>
    }
  `,
  styles: `
    :host {
      display: block;
      aspect-ratio: 2 / 3;
      width: var(--largura-capa, 120px);
      flex: none;
      border-radius: 4px;
      overflow: hidden;
      background: var(--mat-sys-surface-container-high);
    }
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }
    .vazia {
      width: 100%;
      height: 100%;
      display: grid;
      place-items: center;
      color: var(--mat-sys-on-surface-variant);
    }
  `,
})
export class CapaLivro {
  readonly obraId = input<number | null>(null);
  readonly temCapa = input(true);
  readonly src = input<string | null>(null);
  readonly mini = input(false);
  readonly versao = input<number | string | null>(null);
  readonly titulo = input('');

  protected readonly falhou = signal(false);

  protected readonly url = computed(() => {
    if (this.src()) {
      return this.src();
    }
    const id = this.obraId();
    if (id == null || !this.temCapa()) {
      return null;
    }
    const params = new URLSearchParams();
    if (this.mini()) {
      params.set('tamanho', 'mini');
    }
    if (this.versao() != null) {
      params.set('v', String(this.versao()));
    }
    const qs = params.toString();
    return `/api/v1/obras/${id}/capa${qs ? '?' + qs : ''}`;
  });

  constructor() {
    // Nova URL: tenta carregar de novo.
    effect(() => {
      this.url();
      this.falhou.set(false);
    });
  }
}
