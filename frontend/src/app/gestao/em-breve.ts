import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

/** Tela do menu que ainda não foi feita. Recebe título e fase pelos dados da rota. */
@Component({
  selector: 'app-em-breve',
  imports: [RouterLink, MatIconModule],
  template: `
    <h1>{{ titulo() }}</h1>
    <p class="aviso">
      <mat-icon>construction</mat-icon>
      <span>Esta tela chega na fase {{ fase() }} do roadmap. Por enquanto, use o <a routerLink="/painel/acervo">Acervo</a>.</span>
    </p>
  `,
  styles: `
    h1 {
      margin-bottom: 16px;
    }
  `,
})
export class EmBreve {
  readonly titulo = input('');
  readonly fase = input(0);
}
