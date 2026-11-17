package br.gov.imbe.biblioteca.comum.auditoria;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.gov.imbe.biblioteca.comum.Pagina;
import br.gov.imbe.biblioteca.comum.seguranca.UsuarioAtual;

/**
 * Registra ações sensíveis. Entra na transação de quem chama: se a ação falhar, o registro também some.
 * Fora de transação (login), grava na hora.
 */
@Service
public class Auditoria {

    private final AuditoriaRepository registros;
    private final Clock relogio;

    public Auditoria(AuditoriaRepository registros, Clock relogio) {
        this.registros = registros;
        this.relogio = relogio;
    }

    /** Feito pelo funcionário da requisição atual. */
    @Transactional
    public void registrar(AcaoAuditoria acao, String entidade, Long entidadeId, String detalhe) {
        registrarPor(UsuarioAtual.idOuNulo(), acao, entidade, entidadeId, detalhe);
    }

    @Transactional
    public void registrarPor(Long funcionarioId, AcaoAuditoria acao, String entidade, Long entidadeId,
            String detalhe) {
        registros.save(new RegistroAuditoria(funcionarioId, acao, entidade, entidadeId, detalhe,
                LocalDateTime.now(relogio)));
    }

    @Transactional(readOnly = true)
    public Pagina<AuditoriaResposta> listar(AcaoAuditoria acao, int pagina, int tamanho) {
        return Pagina.de(registros.listar(acao, PageRequest.of(Math.max(pagina, 0), Math.clamp(tamanho, 1, 100))),
                r -> r);
    }
}
