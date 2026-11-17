package br.gov.imbe.biblioteca.comum.auditoria;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.gov.imbe.biblioteca.comum.Pagina;

/** Perfil: Admin. */
@RestController
@RequestMapping("/api/v1/auditoria")
public class AuditoriaController {

    private final Auditoria auditoria;

    public AuditoriaController(Auditoria auditoria) {
        this.auditoria = auditoria;
    }

    @GetMapping
    public Pagina<AuditoriaResposta> listar(@RequestParam(required = false) AcaoAuditoria acao,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "30") int size) {
        return auditoria.listar(acao, page, size);
    }
}
