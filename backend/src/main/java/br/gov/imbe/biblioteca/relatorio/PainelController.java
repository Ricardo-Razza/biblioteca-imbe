package br.gov.imbe.biblioteca.relatorio;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tela inicial da gestão. Perfil: Atendente. */
@RestController
public class PainelController {

    private final PainelService servico;

    public PainelController(PainelService servico) {
        this.servico = servico;
    }

    @GetMapping("/api/v1/painel")
    public PainelResposta painel() {
        return servico.painel();
    }
}
