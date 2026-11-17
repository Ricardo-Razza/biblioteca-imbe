package br.gov.imbe.biblioteca.funcionario;

import java.time.Clock;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Banco sem nenhum funcionário (primeira instalação): cria o administrador LOCAL com a senha de
 * {@code ADMIN_INICIAL_SENHA}, que é provisória (troca obrigatória no primeiro acesso). Depois não faz nada.
 */
@Component
class AdminInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);

    private final FuncionarioRepository funcionarios;
    private final PasswordEncoder senhas;
    private final Clock relogio;
    private final String login;
    private final String senha;

    AdminInicial(FuncionarioRepository funcionarios, PasswordEncoder senhas, Clock relogio,
            @Value("${biblioteca.auth.admin-inicial.login:admin}") String login,
            @Value("${biblioteca.auth.admin-inicial.senha:}") String senha) {
        this.funcionarios = funcionarios;
        this.senhas = senhas;
        this.relogio = relogio;
        this.login = login;
        this.senha = senha;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (funcionarios.count() > 0) {
            return;
        }
        if (senha == null || senha.isBlank()) {
            log.warn("Nenhum funcionário cadastrado. Defina ADMIN_INICIAL_SENHA para criar o primeiro administrador.");
            return;
        }
        Funcionario admin = new Funcionario("Administrador", login, TipoAutenticacao.LOCAL, Perfil.ADMIN,
                LocalDateTime.now(relogio));
        admin.definirSenhaProvisoria(senhas.encode(senha));
        funcionarios.save(admin);
        log.info("Administrador inicial \"{}\" criado. Troque a senha no primeiro acesso.", login);
    }
}
