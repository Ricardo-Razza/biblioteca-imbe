import { Component, computed, inject, input } from '@angular/core';
import { Router } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';

import { AbaAuditoria } from './aba-auditoria';
import { AbaFeriados } from './aba-feriados';
import { AbaFuncionarios } from './aba-funcionarios';
import { AbaParametros } from './aba-parametros';

const ABAS = ['funcionarios', 'parametros', 'feriados', 'auditoria'] as const;

/** Configurações (só administrador), em abas. A aba fica na URL (`?aba=`). */
@Component({
  selector: 'app-configuracoes',
  imports: [MatTabsModule, AbaFuncionarios, AbaParametros, AbaFeriados, AbaAuditoria],
  template: `
    <h1>Configurações</h1>
    <mat-tab-group [selectedIndex]="indice()" (selectedIndexChange)="trocar($event)" mat-stretch-tabs="false" animationDuration="0ms">
      <mat-tab label="Funcionários">
        <ng-template matTabContent><app-aba-funcionarios /></ng-template>
      </mat-tab>
      <mat-tab label="Prazos e limites">
        <ng-template matTabContent><app-aba-parametros /></ng-template>
      </mat-tab>
      <mat-tab label="Feriados">
        <ng-template matTabContent><app-aba-feriados /></ng-template>
      </mat-tab>
      <mat-tab label="Auditoria">
        <ng-template matTabContent><app-aba-auditoria /></ng-template>
      </mat-tab>
    </mat-tab-group>
  `,
  styles: `
    h1 {
      margin-bottom: 8px;
    }
    mat-tab-group {
      --conteudo-aba: 16px;
    }
  `,
})
export class Configuracoes {
  readonly aba = input<string>();
  private readonly router = inject(Router);
  protected readonly indice = computed(() => Math.max(0, ABAS.indexOf(this.aba() as (typeof ABAS)[number])));

  protected trocar(i: number): void {
    this.router.navigate([], { queryParams: { aba: i === 0 ? null : ABAS[i] }, replaceUrl: true });
  }
}
