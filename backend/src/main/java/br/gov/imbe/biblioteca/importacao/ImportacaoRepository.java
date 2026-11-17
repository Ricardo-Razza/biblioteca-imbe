package br.gov.imbe.biblioteca.importacao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ImportacaoRepository extends JpaRepository<Importacao, Long> {

    Page<Importacao> findAllByOrderByIdDesc(Pageable paginacao);

    List<Importacao> findByStatusOrderById(Importacao.Status status);
}
