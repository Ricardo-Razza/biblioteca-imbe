package br.gov.imbe.biblioteca.comum.calendario;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.gov.imbe.biblioteca.comum.auditoria.AcaoAuditoria;
import br.gov.imbe.biblioteca.comum.auditoria.Auditoria;
import br.gov.imbe.biblioteca.comum.erro.ConflitoException;
import br.gov.imbe.biblioteca.comum.erro.RecursoNaoEncontradoException;

/** Feriados cadastrados (os municipais pelo administrador). Fim de semana não entra aqui. */
@Service
public class FeriadoService {

    public record FeriadoResposta(LocalDate data, String descricao, TipoFeriado tipo) {

        static FeriadoResposta de(Feriado f) {
            return new FeriadoResposta(f.getData(), f.getDescricao(), f.getTipo());
        }
    }

    /** Resultado da importação dos feriados nacionais de um ano. */
    public record Importacao(int ano, int novos, int atualizados) {
    }

    private static final String ENTIDADE = "FERIADO";

    private final FeriadoRepository feriados;
    private final FeriadosBrasilApi brasilApi;
    private final Auditoria auditoria;

    public FeriadoService(FeriadoRepository feriados, FeriadosBrasilApi brasilApi, Auditoria auditoria) {
        this.feriados = feriados;
        this.brasilApi = brasilApi;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public boolean temNacionais(int ano) {
        return feriados.existsByTipoAndDataBetween(TipoFeriado.NACIONAL, LocalDate.of(ano, 1, 1),
                LocalDate.of(ano, 12, 31));
    }

    /** Busca na BrasilAPI e grava. Fora do ar: 503, nada muda. */
    @Transactional
    public Importacao importarNacionais(int ano) {
        Importacao r = gravarNacionais(ano, brasilApi.doAno(ano));
        auditoria.registrar(AcaoAuditoria.FERIADOS_IMPORTADOS, ENTIDADE, null,
                ano + ": " + r.novos() + " novos, " + r.atualizados() + " atualizados");
        return r;
    }

    /**
     * Inclui os nacionais que faltam e atualiza o nome dos que mudaram. Uma data já cadastrada como municipal fica
     * como está: o cadastro do administrador vale mais.
     */
    @Transactional
    public Importacao gravarNacionais(int ano, List<FeriadosBrasilApi.FeriadoExterno> lista) {
        int novos = 0;
        int atualizados = 0;
        for (FeriadosBrasilApi.FeriadoExterno f : lista) {
            if (f.date().getYear() != ano) {
                continue;
            }
            String nome = f.name().strip();
            String descricao = nome.length() > 150 ? nome.substring(0, 150) : nome;
            Feriado existente = feriados.findById(f.date()).orElse(null);
            if (existente == null) {
                feriados.save(new Feriado(f.date(), descricao, TipoFeriado.NACIONAL));
                novos++;
            } else if (existente.getTipo() == TipoFeriado.NACIONAL && !existente.getDescricao().equals(descricao)) {
                existente.alterar(descricao, TipoFeriado.NACIONAL);
                atualizados++;
            }
        }
        return new Importacao(ano, novos, atualizados);
    }

    @Transactional(readOnly = true)
    public List<FeriadoResposta> doAno(int ano) {
        return feriados.findByDataBetweenOrderByData(LocalDate.of(ano, 1, 1), LocalDate.of(ano, 12, 31)).stream()
                .map(FeriadoResposta::de).toList();
    }

    @Transactional
    public FeriadoResposta incluir(LocalDate data, String descricao, TipoFeriado tipo) {
        if (feriados.existsById(data)) {
            throw new ConflitoException("Já existe feriado em " + data.getDayOfMonth() + "/"
                    + data.getMonthValue() + "/" + data.getYear() + ".");
        }
        Feriado f = feriados.save(new Feriado(data, descricao.strip(), tipo));
        auditoria.registrar(AcaoAuditoria.FERIADO_INCLUIDO, ENTIDADE, null, data + " " + f.getDescricao());
        return FeriadoResposta.de(f);
    }

    /** Prazos já calculados não mudam; só os próximos. */
    @Transactional
    public void remover(LocalDate data) {
        Feriado f = feriados.findById(data)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Não há feriado nessa data."));
        feriados.delete(f);
        auditoria.registrar(AcaoAuditoria.FERIADO_REMOVIDO, ENTIDADE, null, data + " " + f.getDescricao());
    }
}
