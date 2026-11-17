package br.gov.imbe.biblioteca.comum.seguranca;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Funcionário da requisição atual, lido do JWT (claim {@code fid}). */
public final class UsuarioAtual {

    /** Claim do JWT com o id do funcionário. */
    public static final String CLAIM_ID = "fid";

    private UsuarioAtual() {
    }

    /** Vazio fora de uma requisição autenticada por JWT (tarefas agendadas, site público). */
    public static Optional<Long> id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken token && token.getToken().getClaim(CLAIM_ID) instanceof Number n) {
            return Optional.of(n.longValue());
        }
        return Optional.empty();
    }

    public static Long idOuNulo() {
        return id().orElse(null);
    }
}
