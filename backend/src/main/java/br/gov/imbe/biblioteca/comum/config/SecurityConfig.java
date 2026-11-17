package br.gov.imbe.biblioteca.comum.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * API sem sessão, com o JWT do próprio sistema. Livres: login, site público e capas. Perfis: ATENDENTE para o
 * dia a dia; ADMIN (que inclui ATENDENTE) para baixa de exemplar, desbloqueio, parâmetros, feriados,
 * funcionários e auditoria. Com senha provisória, só a troca de senha.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    /** Autoridade de quem ainda precisa trocar a senha provisória (ver {@code ConversorJwt}). */
    static final String SENHA_PROVISORIA = "SENHA_PROVISORIA";

    @Bean
    static RoleHierarchy hierarquiaDePerfis() {
        return RoleHierarchyImpl.withDefaultRolePrefix().role("ADMIN").implies("ATENDENTE").build();
    }

    @Bean
    SecurityFilterChain filtroDeSeguranca(HttpSecurity http,
            Converter<Jwt, AbstractAuthenticationToken> conversorJwt) throws Exception {
        AuthenticationEntryPoint semLogin = (req, resp, ex) -> problema(req, resp, HttpStatus.UNAUTHORIZED,
                "Não autenticado", "Entre no sistema para continuar.");
        AccessDeniedHandler semPermissao = (req, resp, ex) -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean provisoria = auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> SENHA_PROVISORIA.equals(a.getAuthority()));
            problema(req, resp, HttpStatus.FORBIDDEN, "Sem permissão", provisoria
                    ? "Troque a senha provisória antes de continuar."
                    : "Seu perfil não permite esta operação.");
        };

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(rs -> rs
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorJwt))
                        .authenticationEntryPoint(semLogin)
                        .accessDeniedHandler(semPermissao))
                .exceptionHandling(e -> e.authenticationEntryPoint(semLogin).accessDeniedHandler(semPermissao))
                .authorizeHttpRequests(regras -> regras
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/publico/**").permitAll()
                        // <img> não manda o token: capas e a prévia da capa da fonte ficam livres
                        .requestMatchers(HttpMethod.GET, "/api/v1/obras/*/capa", "/api/v1/isbn/*/capa").permitAll()
                        .requestMatchers("/actuator/health", "/error").permitAll()
                        .requestMatchers("/api/v1/auth/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/exemplares/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/leitores/*/desbloqueio").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/parametros/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/feriados/**").hasRole("ATENDENTE")
                        .requestMatchers("/api/v1/feriados/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/funcionarios/**", "/api/v1/auditoria/**", "/api/v1/importacoes/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/**").hasRole("ATENDENTE")
                        .anyRequest().denyAll())
                .build();
    }

    /** ProblemDetail (RFC 9457) escrito à mão: estes erros acontecem antes do Spring MVC. */
    private static void problema(HttpServletRequest req, HttpServletResponse resp, HttpStatus status, String titulo,
            String detalhe) throws IOException {
        resp.setStatus(status.value());
        resp.setContentType("application/problem+json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + titulo + "\",\"status\":" + status.value()
                + ",\"detail\":\"" + detalhe + "\",\"instance\":\"" + json(req.getRequestURI()) + "\"}");
    }

    private static String json(String texto) {
        StringBuilder s = new StringBuilder();
        for (char c : texto.toCharArray()) {
            if (c == '"' || c == '\\') {
                s.append('\\').append(c);
            } else if (c < 0x20) {
                s.append(String.format("\\u%04x", (int) c));
            } else {
                s.append(c);
            }
        }
        return s.toString();
    }
}
