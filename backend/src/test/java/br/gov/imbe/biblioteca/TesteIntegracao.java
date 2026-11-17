package br.gov.imbe.biblioteca;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

/**
 * Teste de integração: aplicação inteira com MySQL 8.4 do Testcontainers e MockMvc.
 * Todas as classes com esta anotação compartilham o mesmo contexto do Spring e o mesmo container.
 * Combine com {@code @Transactional} para desfazer o que cada teste grava.
 * <p>
 * As fontes de ISBN e a BrasilAPI de feriados apontam para uma porta fechada: teste nunca acessa a internet
 * (as fontes ficam "indisponíveis"). Capas vão para {@code target/capas-teste}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = {
        "biblioteca.isbn.brasilapi.url=http://127.0.0.1:1",
        "biblioteca.isbn.google-books.url=http://127.0.0.1:1",
        "biblioteca.isbn.open-library.url=http://127.0.0.1:1",
        "biblioteca.isbn.open-library.capas-url=http://127.0.0.1:1",
        "biblioteca.feriados.url=http://127.0.0.1:1",
        "biblioteca.capas.diretorio=target/capas-teste",
        "biblioteca.importacao.intervalo=0ms",
        "biblioteca.auth.jwt.segredo=segredo-so-para-os-testes-0123456789abcdef"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public @interface TesteIntegracao {
}
