import { HttpErrorResponse } from '@angular/common/http';

/** Campo inválido no ProblemDetail do backend ("Dados inválidos"). */
export interface CampoInvalido {
  campo: string;
  mensagem: string;
}

interface Problema {
  title?: string;
  detail?: string;
  campos?: CampoInvalido[];
}

/** Mensagem em português para mostrar na tela. O backend já manda o texto pronto no `detail`. */
export function mensagemDeErro(erro: unknown): string {
  if (erro instanceof HttpErrorResponse) {
    if (erro.status === 0) {
      return 'Sem conexão com o servidor. Confira a rede e tente de novo.';
    }
    const problema = erro.error as Problema | null;
    if (problema && typeof problema === 'object' && problema.detail) {
      return problema.detail;
    }
    if (erro.status >= 500) {
      return 'O servidor teve um problema. Tente de novo em instantes.';
    }
  }
  return 'Não foi possível concluir a operação.';
}

/** Erros por campo (400 "Dados inválidos"), para marcar no formulário. */
export function camposComErro(erro: unknown): CampoInvalido[] {
  if (erro instanceof HttpErrorResponse && erro.status === 400) {
    const problema = erro.error as Problema | null;
    if (problema && Array.isArray(problema.campos)) {
      return problema.campos;
    }
  }
  return [];
}
