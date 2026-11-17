package br.gov.imbe.biblioteca.importacao;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import br.gov.imbe.biblioteca.acervo.ExemplarService;
import br.gov.imbe.biblioteca.comum.erro.BibliotecaException;
import br.gov.imbe.biblioteca.comum.erro.RequisicaoInvalidaException;
import br.gov.imbe.biblioteca.isbn.Isbn;

/**
 * Lê a planilha da carga inicial: uma linha por exemplar, com {@code isbn;tombo;localização} (tombo e
 * localização opcionais). Aceita ponto e vírgula, vírgula ou tabulação, UTF-8 (com ou sem BOM) ou o
 * Windows-1252 do Excel. Linha de cabeçalho (com "isbn"), linhas vazias e linhas com {@code #} são ignoradas.
 * <p>
 * O que dá para conferir sem consultar nada (ISBN válido, formato do tombo, tombo repetido no próprio arquivo)
 * já sai como erro da linha; o resto fica para o processamento.
 */
final class LeitorDePlanilha {

    static final int MAXIMO_LINHAS = 5000;
    static final String TITULO_ERRO = "Planilha inválida";

    private static final Pattern TOMBO = Pattern.compile("[A-Za-z0-9.-]{1,30}");
    private static final Pattern NOTACAO_CIENTIFICA = Pattern.compile("\\d[.,]\\d+[eE]\\+?\\d+");
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    private LeitorDePlanilha() {
    }

    static List<LinhaPlanilha> ler(byte[] conteudo) {
        String texto = decodificar(conteudo);
        if (!texto.isEmpty() && texto.charAt(0) == '﻿') {
            texto = texto.substring(1);
        }
        String[] linhas = texto.split("\\r\\n|\\r|\\n", -1);
        List<LinhaPlanilha> lidas = new ArrayList<>();
        Map<String, Integer> tombos = new HashMap<>();
        for (int i = 0; i < linhas.length; i++) {
            String linha = linhas[i].strip();
            if (linha.isEmpty() || linha.startsWith("#") || (lidas.isEmpty() && cabecalho(linha))) {
                continue;
            }
            if (lidas.size() == MAXIMO_LINHAS) {
                throw new RequisicaoInvalidaException(TITULO_ERRO,
                        "A planilha pode ter no máximo " + MAXIMO_LINHAS + " linhas. Divida em arquivos menores.");
            }
            lidas.add(linha(i + 1, linha, tombos));
        }
        if (lidas.isEmpty()) {
            throw new RequisicaoInvalidaException(TITULO_ERRO, "A planilha não tem nenhuma linha com ISBN.");
        }
        return lidas;
    }

    private static LinhaPlanilha linha(int numero, String linha, Map<String, Integer> tombos) {
        String[] c = colunas(linha);
        String isbnOriginal = limitar(c[0], 40);
        String tomboOriginal = c.length > 1 ? c[1] : "";
        String localizacao = c.length > 2 && !c[2].isEmpty() ? limitar(c[2], 100) : null;

        String isbn13 = null;
        String erro = null;
        if (NOTACAO_CIENTIFICA.matcher(isbnOriginal).matches()) {
            erro = "O ISBN virou número no Excel (" + isbnOriginal
                    + "). Formate a coluna como texto e exporte de novo.";
        } else {
            try {
                isbn13 = Isbn.de(isbnOriginal).valor();
            } catch (BibliotecaException e) {
                erro = e.getMessage();
            }
        }

        String tombo = null;
        if (!tomboOriginal.isEmpty()) {
            if (!TOMBO.matcher(tomboOriginal).matches()) {
                erro = junta(erro, "Tombo inválido (" + limitar(tomboOriginal, 40)
                        + "): use até 30 letras, números, ponto ou hífen.");
                tombo = limitar(tomboOriginal, 30);
            } else {
                tombo = ExemplarService.normalizarTombo(tomboOriginal);
                Integer anterior = tombos.putIfAbsent(tombo, numero);
                if (anterior != null) {
                    erro = junta(erro, "Tombo " + tombo + " repetido (já está na linha " + anterior + ").");
                }
            }
        }
        return new LinhaPlanilha(numero, isbnOriginal, isbn13, tombo, localizacao, erro);
    }

    /**
     * Separa as colunas respeitando aspas ({@code "Sala 2; fundo"}). O separador é o primeiro ponto e vírgula,
     * tabulação ou vírgula fora de aspas.
     */
    private static String[] colunas(String linha) {
        char separador = ',';
        boolean aspas = false;
        for (char c : linha.toCharArray()) {
            if (c == '"') {
                aspas = !aspas;
            } else if (!aspas && (c == ';' || c == '	')) {
                separador = c;
                break;
            }
        }
        List<String> colunas = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        aspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (c == '"') {
                if (aspas && i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                    atual.append('"');
                    i++;
                } else {
                    aspas = !aspas;
                }
            } else if (c == separador && !aspas) {
                colunas.add(atual.toString().strip());
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        colunas.add(atual.toString().strip());
        return colunas.toArray(String[]::new);
    }

    private static boolean cabecalho(String linha) {
        String primeira = colunas(linha)[0].toLowerCase();
        return primeira.contains("isbn");
    }

    private static String decodificar(byte[] conteudo) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(conteudo)).toString();
        } catch (CharacterCodingException e) {
            return new String(conteudo, WINDOWS_1252);
        }
    }

    private static String junta(String a, String b) {
        return a == null ? b : a + " " + b;
    }

    private static String limitar(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
