package br.gov.imbe.biblioteca.comum.calendario;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dias de atendimento da biblioteca: segunda a sexta, exceto feriados cadastrados.
 * Prazos que caem em dia sem atendimento passam para o próximo dia útil.
 */
@Service
public class Calendario {

    /** Folga para o maior prazo somado (90 dias + feriados emendados). */
    private static final int JANELA_DIAS = 200;

    private final FeriadoRepository feriados;

    public Calendario(FeriadoRepository feriados) {
        this.feriados = feriados;
    }

    @Transactional(readOnly = true)
    public boolean diaUtil(LocalDate data) {
        return !fimDeSemana(data) && !feriados.existsById(data);
    }

    /** A própria data, se for dia útil; senão o próximo. */
    @Transactional(readOnly = true)
    public LocalDate proximoDiaUtil(LocalDate data) {
        Set<LocalDate> datas = feriadosEntre(data, data.plusDays(JANELA_DIAS));
        LocalDate d = data;
        while (fimDeSemana(d) || datas.contains(d)) {
            d = d.plusDays(1);
        }
        return d;
    }

    /** {@code inicio} + n dias úteis (o próprio início não conta). */
    @Transactional(readOnly = true)
    public LocalDate somarDiasUteis(LocalDate inicio, int n) {
        Set<LocalDate> datas = feriadosEntre(inicio, inicio.plusDays(JANELA_DIAS));
        LocalDate d = inicio;
        int contados = 0;
        while (contados < n) {
            d = d.plusDays(1);
            if (!fimDeSemana(d) && !datas.contains(d)) {
                contados++;
            }
        }
        return d;
    }

    private Set<LocalDate> feriadosEntre(LocalDate inicio, LocalDate fim) {
        Set<LocalDate> datas = new HashSet<>();
        feriados.findByDataBetweenOrderByData(inicio, fim).forEach(f -> datas.add(f.getData()));
        return datas;
    }

    private static boolean fimDeSemana(LocalDate d) {
        return d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
