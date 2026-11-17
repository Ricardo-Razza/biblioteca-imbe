package br.gov.imbe.biblioteca.auth;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import br.gov.imbe.biblioteca.comum.seguranca.UsuarioAtual;
import br.gov.imbe.biblioteca.funcionario.Funcionario;

/**
 * Emite o JWT. Leva só a identidade: perfil, situação e troca de senha são lidos do banco a cada requisição
 * ({@link ConversorJwt}), então desativar ou mudar o perfil vale na hora.
 */
@Component
public class Tokens {

    public record TokenEmitido(String token, Instant expiraEm) {
    }

    private final JwtEncoder encoder;
    private final AuthProperties propriedades;
    private final Clock relogio;

    public Tokens(JwtEncoder encoder, AuthProperties propriedades, Clock relogio) {
        this.encoder = encoder;
        this.propriedades = propriedades;
        this.relogio = relogio;
    }

    public TokenEmitido emitir(Funcionario f) {
        Instant agora = relogio.instant();
        Instant expira = agora.plus(propriedades.jwt().validade());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("biblioteca-imbe")
                .subject(f.getLogin())
                .issuedAt(agora)
                .expiresAt(expira)
                .claim(UsuarioAtual.CLAIM_ID, f.getId())
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new TokenEmitido(token, expira);
    }
}
