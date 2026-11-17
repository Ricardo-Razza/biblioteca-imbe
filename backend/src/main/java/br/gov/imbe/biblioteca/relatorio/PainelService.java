package br.gov.imbe.biblioteca.relatorio;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.gov.imbe.biblioteca.relatorio.PainelResposta.Dia;

@Service
public class PainelService {

    static final int ITENS_POR_LISTA = 10;
    static final int DIAS_DO_GRAFICO = 7;

    private final RelatorioRepository consultas;
    private final Clock relogio;

    public PainelService(RelatorioRepository consultas, Clock relogio) {
        this.consultas = consultas;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public PainelResposta painel() {
        LocalDate hoje = LocalDate.now(relogio);
        return new PainelResposta(hoje, consultas.contadores(hoje), consultas.venceHoje(hoje, ITENS_POR_LISTA),
                consultas.atrasados(hoje, ITENS_POR_LISTA), consultas.paraSeparar(ITENS_POR_LISTA), movimento(hoje));
    }

    /** Últimos sete dias, hoje por último, com os dias sem movimento zerados. */
    private List<Dia> movimento(LocalDate hoje) {
        LocalDate inicio = hoje.minusDays(DIAS_DO_GRAFICO - 1);
        Map<LocalDate, Long> saidas = consultas.saidasPorDia(inicio, hoje);
        Map<LocalDate, Long> devolucoes = consultas.devolucoesPorDia(inicio, hoje);
        List<Dia> dias = new ArrayList<>(DIAS_DO_GRAFICO);
        for (LocalDate d = inicio; !d.isAfter(hoje); d = d.plusDays(1)) {
            dias.add(new Dia(d, saidas.getOrDefault(d, 0L), devolucoes.getOrDefault(d, 0L)));
        }
        return dias;
    }
}
