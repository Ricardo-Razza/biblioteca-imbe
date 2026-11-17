package br.gov.imbe.biblioteca.auth;

import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import br.gov.imbe.biblioteca.comum.seguranca.UsuarioAtual;
import br.gov.imbe.biblioteca.funcionario.Funcionario;
import br.gov.imbe.biblioteca.funcionario.FuncionarioRepository;

/**
 * JWT válido → autenticação com o perfil atual do banco. Funcionário desativado perde o acesso na hora.
 * Com senha provisória, só ganha {@link #SENHA_PROVISORIA}: pode trocar a senha e nada mais.
 */
@Component
public class ConversorJwt implements Converter<Jwt, AbstractAuthenticationToken> {

    public static final String SENHA_PROVISORIA = "SENHA_PROVISORIA";

    private final FuncionarioRepository funcionarios;

    public ConversorJwt(FuncionarioRepository funcionarios) {
        this.funcionarios = funcionarios;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        if (!(jwt.getClaim(UsuarioAtual.CLAIM_ID) instanceof Number id)) {
            throw new InvalidBearerTokenException("Token sem funcionário");
        }
        Funcionario f = funcionarios.findById(id.longValue()).filter(Funcionario::isAtivo)
                .orElseThrow(() -> new InvalidBearerTokenException("Funcionário inativo"));
        List<GrantedAuthority> permissoes = f.isTrocarSenha()
                ? List.of(new SimpleGrantedAuthority(SENHA_PROVISORIA))
                : List.of(new SimpleGrantedAuthority("ROLE_" + f.getPerfil().name()));
        return new JwtAuthenticationToken(jwt, permissoes, f.getLogin());
    }
}
