package br.gov.imbe.biblioteca.leitor;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeitorRepository extends JpaRepository<Leitor, Long> {

    Optional<Leitor> findByCpf(String cpf);

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, Long id);

    /** Trecho do nome (a collation ignora maiúsculas e acentos) ou começo do CPF. */
    @Query("""
            select l from Leitor l
            where l.nome like :padrao escape '!' or l.cpf like :cpf
            """)
    Page<Leitor> buscar(@Param("padrao") String padrao, @Param("cpf") String cpf, Pageable paginacao);

    /** Sem depender do módulo de empréstimo: só para impedir desativar quem está com livro. */
    @Query(value = "select count(*) from emprestimo where leitor_id = :id and data_devolucao is null",
            nativeQuery = true)
    long contarEmprestimosEmAberto(@Param("id") Long id);
}
