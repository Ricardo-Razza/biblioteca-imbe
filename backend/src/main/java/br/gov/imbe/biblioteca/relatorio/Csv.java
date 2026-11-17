package br.gov.imbe.biblioteca.relatorio;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * CSV no formato que o Excel e o LibreOffice em português abrem com dois cliques: UTF-8 com BOM, ponto e vírgula
 * entre colunas, datas dd/MM/aaaa e quebra de linha CRLF.
 * <p>
 * Texto que começa com {@code = + - @}, tabulação ou retorno ganha um apóstrofo na frente: a planilha não o
 * interpreta como fórmula (um título de livro ou nome digitado no cadastro não pode virar comando).
 */
final class Csv {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final StringBuilder texto = new StringBuilder("﻿");

    Csv linha(Object... valores) {
        for (int i = 0; i < valores.length; i++) {
            if (i > 0) {
                texto.append(';');
            }
            texto.append(celula(valores[i]));
        }
        texto.append("\r\n");
        return this;
    }

    byte[] bytes() {
        return texto.toString().getBytes(StandardCharsets.UTF_8);
    }

    static String celula(Object valor) {
        return switch (valor) {
            case null -> "";
            case LocalDate d -> DATA.format(d);
            case LocalDateTime d -> DATA_HORA.format(d);
            case Number n -> n.toString();
            default -> texto(valor.toString());
        };
    }

    private static String texto(String s) {
        if (s.isEmpty()) {
            return s;
        }
        char c = s.charAt(0);
        if (c == '=' || c == '+' || c == '-' || c == '@' || c == '\t' || c == '\r') {
            s = "'" + s;
        }
        if (s.indexOf(';') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }
}
