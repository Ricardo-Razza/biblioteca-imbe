package br.gov.imbe.biblioteca.acervo;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CapaProperties.class)
public class AcervoConfig {
}
