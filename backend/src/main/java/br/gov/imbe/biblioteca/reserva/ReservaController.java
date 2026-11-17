package br.gov.imbe.biblioteca.reserva;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.gov.imbe.biblioteca.comum.Pagina;
import br.gov.imbe.biblioteca.comum.seguranca.UsuarioAtual;

/** Reservas na gestão: separar, entregar e cancelar. Perfil: Atendente. */
@RestController
@RequestMapping("/api/v1/reservas")
public class ReservaController {

    private final ReservaService servico;

    public ReservaController(ReservaService servico) {
        this.servico = servico;
    }

    /**
     * {@code ?status=} um ou mais (padrão {@code AGUARDANDO_RETIRADA}); {@code leitorId} e {@code q} (título, nome do
     * leitor, protocolo ou tombo) opcionais.
     */
    @GetMapping
    public Pagina<ReservaResposta> listar(@RequestParam(required = false) List<StatusReserva> status,
            @RequestParam(required = false) Long leitorId, @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return servico.listar(status, leitorId, q, page, size);
    }

    @GetMapping("/{id}")
    public ReservaResposta buscar(@PathVariable Long id) {
        return servico.buscar(id);
    }

    /** O leitor veio buscar: vira empréstimo com o mesmo exemplar. */
    @PostMapping("/{id}/retirada")
    public ReservaResposta retirar(@PathVariable Long id) {
        return servico.retirar(id, UsuarioAtual.idOuNulo());
    }

    @PostMapping("/{id}/cancelamento")
    public ReservaResposta cancelar(@PathVariable Long id) {
        return servico.cancelar(id, UsuarioAtual.idOuNulo());
    }
}
