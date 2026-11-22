import { caminhoDe, destinoDaRolagem } from './rolagem';

describe('rolagem entre páginas', () => {
  it('caminho ignora busca e âncora', () => {
    expect(caminhoDe('/livro/55?x=1#topo')).toBe('/livro/55');
    expect(caminhoDe('/')).toBe('/');
  });

  it('página nova vai para o topo (ficha do livro para o formulário de reserva)', () => {
    expect(destinoDaRolagem('/livro/55', '/reservar/55', null, null)).toEqual([0, 0]);
    expect(destinoDaRolagem(null, '/', null, null)).toEqual([0, 0]);
  });

  it('só a busca mudou (filtro, ordem do catálogo): fica onde está', () => {
    expect(destinoDaRolagem('/?q=a', '/?q=a&ordem=recentes', null, null)).toBeNull();
  });

  it('voltar do navegador restaura a posição; âncora rola até ela', () => {
    expect(destinoDaRolagem('/reservar/55', '/livro/55', [0, 640], null)).toEqual([0, 640]);
    expect(destinoDaRolagem('/', '/sobre#contato', null, 'contato')).toBe('contato');
  });
});
