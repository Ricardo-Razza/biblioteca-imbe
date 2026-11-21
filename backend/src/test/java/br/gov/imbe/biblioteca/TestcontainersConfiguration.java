package br.gov.imbe.biblioteca;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Banco dos testes: MySQL 8.4. Para rodar tudo num MariaDB (o banco da Prefeitura), passe a imagem:
 * {@code ./mvnw verify -Dbanco.teste=mariadb:10.11}.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	JdbcDatabaseContainer<?> bancoContainer() {
		String imagem = System.getProperty("banco.teste", "mysql:8.4");
		return imagem.startsWith("mariadb")
				? new MariaDBContainer(DockerImageName.parse(imagem))
				: new MySQLContainer(DockerImageName.parse(imagem));
	}

}
