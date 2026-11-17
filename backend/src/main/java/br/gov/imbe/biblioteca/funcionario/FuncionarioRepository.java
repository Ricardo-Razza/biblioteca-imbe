package br.gov.imbe.biblioteca.funcionario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {

    Optional<Funcionario> findByLogin(String login);

    boolean existsByLogin(String login);

    List<Funcionario> findAllByOrderByAtivoDescNomeAsc();

    long countByPerfilAndAtivoTrue(Perfil perfil);
}
