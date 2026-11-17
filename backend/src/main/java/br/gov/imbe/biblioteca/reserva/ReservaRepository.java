package br.gov.imbe.biblioteca.reserva;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * Ordem das travas em todos os fluxos: primeiro o exemplar, depois a reserva. Por isso as consultas que só
 * descobrem o exemplar devolvem o id, sem carregar a reserva (carregada antes da trava, ela ficaria desatualizada).
 */
interface ReservaRepository extends JpaRepository<Reserva, Long> {

    long countByLeitorIdAndStatus(Long leitorId, StatusReserva status);

    Optional<Reserva> findFirstByLeitorIdAndObraIdAndStatus(Long leitorId, Long obraId, StatusReserva status);

    boolean existsByProtocolo(String protocolo);

    List<Reserva> findTop20ByLeitorIdOrderByIdDesc(Long leitorId);

    @Query("select r.exemplar.id from Reserva r where r.id = :id")
    Optional<Long> exemplarDa(@Param("id") Long id);

    @Query("select r.exemplar.id from Reserva r where r.protocolo = :protocolo")
    Optional<Long> exemplarDoProtocolo(@Param("protocolo") String protocolo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.id = :id")
    Optional<Reserva> travar(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.protocolo = :protocolo")
    Optional<Reserva> travarPorProtocolo(@Param("protocolo") String protocolo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r from Reserva r
            where r.exemplar.id = :exemplarId
              and r.status = br.gov.imbe.biblioteca.reserva.StatusReserva.AGUARDANDO_RETIRADA
            """)
    Optional<Reserva> travarAguardandoDoExemplar(@Param("exemplarId") Long exemplarId);

    /** Aguardando com o prazo de retirada já passado. */
    @Query("""
            select r.id from Reserva r
            where r.status = br.gov.imbe.biblioteca.reserva.StatusReserva.AGUARDANDO_RETIRADA
              and r.prazoRetirada < :hoje
            """)
    List<Long> vencidas(@Param("hoje") LocalDate hoje);

    /**
     * Filtros opcionais (nulo = sem filtro). {@code padrao} procura no título da obra e no nome do leitor;
     * {@code protocolo} (sem hífen) e {@code tombo} comparam por igualdade.
     */
    @Query(value = """
            select r from Reserva r join r.obra o join r.leitor l join r.exemplar x
            where r.status in :status
              and (:leitorId is null or l.id = :leitorId)
              and (:padrao is null or o.titulo like :padrao escape '!' or l.nome like :padrao escape '!'
                   or r.protocolo = :protocolo or x.tombo = :tombo)
            """, countQuery = """
            select count(r) from Reserva r join r.obra o join r.leitor l join r.exemplar x
            where r.status in :status
              and (:leitorId is null or l.id = :leitorId)
              and (:padrao is null or o.titulo like :padrao escape '!' or l.nome like :padrao escape '!'
                   or r.protocolo = :protocolo or x.tombo = :tombo)
            """)
    Page<Reserva> buscar(@Param("status") Collection<StatusReserva> status, @Param("leitorId") Long leitorId,
            @Param("padrao") String padrao, @Param("protocolo") String protocolo, @Param("tombo") String tombo,
            Pageable paginacao);
}
