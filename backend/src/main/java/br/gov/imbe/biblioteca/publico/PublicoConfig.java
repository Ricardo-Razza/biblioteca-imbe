package br.gov.imbe.biblioteca.publico;

import java.time.Duration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import br.gov.imbe.biblioteca.comum.limite.LimiteDeTentativas;
import br.gov.imbe.biblioteca.comum.limite.LimiteDeTentativas.Regra;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Limite por IP em {@code /api/v1/publico/**}: um para navegar (GET) e outro, bem menor, para os POST que
 * identificam o leitor. O IP vem do Nginx ({@code server.forward-headers-strategy: native}).
 * Lançado no interceptor, o 429 passa pelo {@code TratadorGlobalDeErros} como os outros erros.
 * Nos testes de fatia ({@code @WebMvcTest}) não há limitador, e o interceptor não entra.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PublicoProperties.class)
public class PublicoConfig implements WebMvcConfigurer {

    private final ObjectProvider<LimiteDeTentativas> limites;
    private final Regra consultas;
    private final Regra identificacoes;

    public PublicoConfig(ObjectProvider<LimiteDeTentativas> limites, PublicoProperties propriedades) {
        this.limites = limites;
        PublicoProperties.Limites l = propriedades.limites();
        this.consultas = new Regra("publico-consulta-ip", l.consultasPorMinuto(), Duration.ofMinutes(1));
        this.identificacoes = new Regra("publico-identificacao-ip", l.identificacoesPorIp(), l.janelaIdentificacao());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        LimiteDeTentativas limite = limites.getIfAvailable();
        if (limite == null) {
            return;
        }
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
                limite.consumir("POST".equals(req.getMethod()) ? identificacoes : consultas, req.getRemoteAddr());
                return true;
            }
        }).addPathPatterns("/api/v1/publico/**");
    }
}
