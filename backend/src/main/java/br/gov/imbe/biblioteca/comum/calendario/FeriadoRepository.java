package br.gov.imbe.biblioteca.comum.calendario;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface FeriadoRepository extends JpaRepository<Feriado, LocalDate> {

    List<Feriado> findByDataBetweenOrderByData(LocalDate inicio, LocalDate fim);

    boolean existsByTipoAndDataBetween(TipoFeriado tipo, LocalDate inicio, LocalDate fim);
}
