package br.gov.imbe.biblioteca.comum.calendario;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.gov.imbe.biblioteca.comum.calendario.FeriadoService.FeriadoResposta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Consulta: Atendente. Inclusão, remoção e importação: Admin. */
@RestController
@RequestMapping("/api/v1/feriados")
public class FeriadoController {

    public record FeriadoRequisicao(
            @NotNull(message = "Informe a data.") LocalDate data,
            @NotBlank(message = "Informe o nome do feriado.") @Size(max = 150, message = "Nome muito longo.")
            String descricao) {
    }

    private final FeriadoService servico;
    private final Clock relogio;

    public FeriadoController(FeriadoService servico, Clock relogio) {
        this.servico = servico;
        this.relogio = relogio;
    }

    /** {@code ?ano=} (padrão: o ano atual). */
    @GetMapping
    public List<FeriadoResposta> listar(@RequestParam(required = false) Integer ano) {
        return servico.doAno(ano != null ? ano : LocalDate.now(relogio).getYear());
    }

    /** Feriado municipal ou ponto facultativo sem atendimento. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeriadoResposta incluir(@Valid @RequestBody FeriadoRequisicao req) {
        return servico.incluir(req.data(), req.descricao(), TipoFeriado.MUNICIPAL);
    }

    /** Busca os nacionais do ano na BrasilAPI ({@code ?ano=}, padrão: o atual). Os municipais não mudam. */
    @PostMapping("/importacao")
    public FeriadoService.Importacao importar(@RequestParam(required = false) Integer ano) {
        return servico.importarNacionais(ano != null ? ano : LocalDate.now(relogio).getYear());
    }

    @DeleteMapping("/{data}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        servico.remover(data);
    }
}
