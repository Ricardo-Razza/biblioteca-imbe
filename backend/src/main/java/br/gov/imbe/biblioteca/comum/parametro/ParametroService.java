package br.gov.imbe.biblioteca.comum.parametro;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.gov.imbe.biblioteca.comum.auditoria.AcaoAuditoria;
import br.gov.imbe.biblioteca.comum.auditoria.Auditoria;
import br.gov.imbe.biblioteca.comum.erro.RecursoNaoEncontradoException;
import br.gov.imbe.biblioteca.comum.erro.RequisicaoInvalidaException;

/** Leitura e edição dos parâmetros. Lidos a cada uso (são poucos e mudam raramente). */
@Service
public class ParametroService {

    public record ParametroResposta(String chave, int valor, String descricao, int minimo, int maximo) {
    }

    private final ParametroRepository repositorio;
    private final Auditoria auditoria;

    public ParametroService(ParametroRepository repositorio, Auditoria auditoria) {
        this.repositorio = repositorio;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public int inteiro(ChaveParametro chave) {
        return Integer.parseInt(buscar(chave).getValor().strip());
    }

    @Transactional(readOnly = true)
    public List<ParametroResposta> listar() {
        return Arrays.stream(ChaveParametro.values()).map(c -> resposta(c, buscar(c))).toList();
    }

    @Transactional
    public ParametroResposta alterar(String chave, int valor) {
        ChaveParametro c = Arrays.stream(ChaveParametro.values()).filter(x -> x.name().equals(chave)).findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Parâmetro " + chave + " não existe."));
        if (valor < c.minimo() || valor > c.maximo()) {
            throw new RequisicaoInvalidaException("Valor inválido",
                    "Use um valor entre " + c.minimo() + " e " + c.maximo() + ".");
        }
        Parametro p = buscar(c);
        String antes = p.getValor().strip();
        if (!antes.equals(String.valueOf(valor))) {
            auditoria.registrar(AcaoAuditoria.PARAMETRO_ALTERADO, "PARAMETRO", null, c.name() + ": " + antes + " → " + valor);
        }
        p.alterar(String.valueOf(valor));
        return resposta(c, p);
    }

    private Parametro buscar(ChaveParametro chave) {
        return repositorio.findById(chave.name())
                .orElseThrow(() -> new IllegalStateException("Parâmetro " + chave + " ausente no banco"));
    }

    private static ParametroResposta resposta(ChaveParametro c, Parametro p) {
        return new ParametroResposta(c.name(), Integer.parseInt(p.getValor().strip()), p.getDescricao(), c.minimo(),
                c.maximo());
    }
}
