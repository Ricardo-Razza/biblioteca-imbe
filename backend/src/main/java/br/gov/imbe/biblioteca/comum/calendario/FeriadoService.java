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

    private static final String ENTIDADE = "FERIADO";

    private final FeriadoRepository feriados;
    private final Auditoria auditoria;

    public FeriadoService(FeriadoRepository feriados, Auditoria auditoria) {
        this.feriados = feriados;
        this.auditoria = auditoria;
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
