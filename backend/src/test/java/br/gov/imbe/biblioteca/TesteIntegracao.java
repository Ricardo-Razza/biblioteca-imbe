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
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public @interface TesteIntegracao {
}
