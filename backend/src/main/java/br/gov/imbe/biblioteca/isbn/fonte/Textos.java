package br.gov.imbe.biblioteca.isbn.fonte;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Conversões comuns às respostas das fontes. */
final class Textos {

    private static final Pattern ANO = Pattern.compile("(?<!\\d)(1[4-9]\\d{2}|20\\d{2})(?!\\d)");
    private static final Pattern PAGINAS = Pattern.compile("\\b(\\d{1,5})\\s*p\\b", Pattern.CASE_INSENSITIVE);

    /** Códigos MARC (Open Library) para ISO 639-1. Os demais ficam como vieram. */
    private static final Map<String, String> IDIOMAS_MARC = Map.of(
            "por", "pt", "eng", "en", "spa", "es", "fre", "fr", "ger", "de", "ita", "it", "jpn", "ja", "lat", "la");

    private Textos() {
    }

    /** Primeiro ano plausível no texto: "2017-05-01", "May 1, 2017", "c1999". */
    static Integer ano(String data) {
        if (data == null) {
            return null;
        }
        Matcher m = ANO.matcher(data);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    /** "pt-BR" vira "pt"; "por" (MARC) vira "pt". */
    static String idioma(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        String c = codigo.strip().toLowerCase(Locale.ROOT);
        if (c.length() == 3) {
            return IDIOMAS_MARC.getOrDefault(c, c);
        }
        int hifen = c.indexOf('-');
        return hifen > 0 ? c.substring(0, hifen) : c;
    }

    /** Paginação de catálogo: "417 p. :" → 417; "xii, 300 p." → 300. Sem "p" explícito, desiste. */
    static Integer paginas(String paginacao) {
        if (paginacao == null) {
            return null;
        }
        Matcher m = PAGINAS.matcher(paginacao);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    static String https(String url) {
        return url != null && url.startsWith("http://") ? "https://" + url.substring("http://".length()) : url;
    }
}
