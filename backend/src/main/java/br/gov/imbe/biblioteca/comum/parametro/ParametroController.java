package br.gov.imbe.biblioteca.comum.parametro;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.gov.imbe.biblioteca.comum.parametro.ParametroService.ParametroResposta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Prazos e limites. Leitura: Atendente; alteração: Admin. */
@RestController
@RequestMapping("/api/v1/parametros")
public class ParametroController {

    public record AlteracaoParametro(@NotNull(message = "Informe o valor.") Integer valor) {
    }

    private final ParametroService servico;

    public ParametroController(ParametroService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<ParametroResposta> listar() {
        return servico.listar();
    }

    @PutMapping("/{chave}")
    public ParametroResposta alterar(@PathVariable String chave, @Valid @RequestBody AlteracaoParametro alteracao) {
        return servico.alterar(chave, alteracao.valor());
    }
}
