package br.gov.imbe.biblioteca.publico;

import java.time.LocalDate;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Conta as buscas do site que não acharam nada ({@code busca_sem_resultado}), por termo e dia. Grava numa
 * transação própria (a busca é só leitura) e nunca derruba a busca: se falhar, só fica no log.
 * <p>
 * Para não guardar dado pessoal por engano, termo com 5 dígitos seguidos (CPF, telefone) é descartado.
 */
@Component
public class RegistroDeBuscas {

    static final int TAMANHO_MAXIMO = 100;
    static final int TAMANHO_MINIMO = 3;
    private static final Pattern MUITOS_DIGITOS = Pattern.compile("\\d{5,}");
    private static final Pattern ESPACOS = Pattern.compile("\\s+");
    private static final Logger log = LoggerFactory.getLogger(RegistroDeBuscas.class);

    private final JdbcClient jdbc;

    public RegistroDeBuscas(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Termo como fica guardado, ou {@code null} quando não deve ser guardado. */
    static String normalizar(String termo) {
        if (termo == null) {
            return null;
        }
        String t = ESPACOS.matcher(termo.strip()).replaceAll(" ").toLowerCase();
        if (t.length() < TAMANHO_MINIMO || MUITOS_DIGITOS.matcher(t.replaceAll("[\\s.\\-/()]", "")).find()) {
            return null;
        }
        return t.length() > TAMANHO_MAXIMO ? t.substring(0, TAMANHO_MAXIMO).strip() : t;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void semResultado(String termo, LocalDate dia) {
        String t = normalizar(termo);
        if (t == null) {
            return;
        }
        try {
            jdbc.sql("""
                    insert into busca_sem_resultado (termo, dia, vezes) values (:termo, :dia, 1)
                    on duplicate key update vezes = vezes + 1
                    """).param("termo", t).param("dia", dia).update();
        } catch (RuntimeException e) {
            log.warn("Não deu para registrar a busca sem resultado: {}", e.getMessage());
        }
    }
}
