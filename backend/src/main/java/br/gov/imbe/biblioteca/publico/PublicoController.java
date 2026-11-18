package br.gov.imbe.biblioteca.publico;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.gov.imbe.biblioteca.comum.Pagina;
import br.gov.imbe.biblioteca.comum.limite.LimiteDeTentativas;
import br.gov.imbe.biblioteca.comum.limite.LimiteDeTentativas.Regra;
import br.gov.imbe.biblioteca.comum.parametro.ChaveParametro;
import br.gov.imbe.biblioteca.comum.parametro.ParametroService;
import br.gov.imbe.biblioteca.reserva.ReservaPublicaDtos.Identificacao;
import br.gov.imbe.biblioteca.reserva.ReservaPublicaDtos.MinhasReservas;
import br.gov.imbe.biblioteca.reserva.ReservaPublicaDtos.NovaReserva;
import br.gov.imbe.biblioteca.reserva.ReservaPublicaDtos.ReservaPublica;
import br.gov.imbe.biblioteca.reserva.ReservaService;
import jakarta.validation.Valid;

/**
 * Site público, sem login. CPF e data de nascimento só no corpo do POST. Além do limite por IP (no
 * {@link PublicoConfig}), cada CPF tem um limite próprio de tentativas.
 */
@RestController
@RequestMapping("/api/v1/publico")
public class PublicoController {

    /**
     * Dados da biblioteca para o rodapé e a reserva. Endereço, horário, telefone e e-mail vazios quando não
     * configurados.
     *
     * @param prazoRetiradaDiasUteis  dias úteis para retirar uma reserva
     * @param limiteLivros            livros ao mesmo tempo por leitor, somando empréstimos e reservas
     * @param prazoEmprestimoDias     dias úteis de cada empréstimo
     */
    public record Biblioteca(String nome, String endereco, String horario, String telefone, String email,
            int prazoRetiradaDiasUteis, int limiteLivros, int prazoEmprestimoDias) {
    }

    private final CatalogoService catalogo;
    private final ReservaService reservas;
    private final LimiteDeTentativas limites;
    private final Regra porCpf;
    private final PublicoProperties.Unidade unidade;
    private final ParametroService parametros;

    public PublicoController(CatalogoService catalogo, ReservaService reservas, LimiteDeTentativas limites,
            PublicoProperties propriedades, ParametroService parametros) {
        this.catalogo = catalogo;
        this.reservas = reservas;
        this.limites = limites;
        this.porCpf = new Regra("publico-identificacao-cpf", propriedades.limites().identificacoesPorCpf(),
                propriedades.limites().janelaIdentificacao());
        this.unidade = propriedades.unidade();
        this.parametros = parametros;
    }

    @GetMapping("/biblioteca")
    public Biblioteca biblioteca() {
        return new Biblioteca(unidade.nome(), unidade.endereco(), unidade.horario(), unidade.telefone(),
                unidade.email(),
                parametros.inteiro(ChaveParametro.PRAZO_RETIRADA_RESERVA_DIAS_UTEIS),
                parametros.inteiro(ChaveParametro.LIMITE_LIVROS_POR_LEITOR),
                parametros.inteiro(ChaveParametro.PRAZO_EMPRESTIMO_DIAS));
    }

    @GetMapping("/obras")
    public Pagina<ObraPublica> obras(@RequestParam(required = false) String q,
            @RequestParam(required = false) Long assunto,
            @RequestParam(defaultValue = "false") boolean disponiveis,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "24") int size) {
        return catalogo.buscar(q, assunto, disponiveis, page, size);
    }

    @GetMapping("/obras/{id}")
    public ObraPublica.Detalhe obra(@PathVariable Long id) {
        return catalogo.ficha(id);
    }

    @GetMapping("/assuntos")
    public List<AssuntoPublico> assuntos() {
        return catalogo.assuntos();
    }

    @PostMapping("/reservas")
    public ResponseEntity<ReservaPublica> reservar(@Valid @RequestBody NovaReserva req) {
        limitar(req.cpf());
        ReservaPublica r = reservas.reservar(req.obraId(), req.nome(), req.cpf(), req.dataNascimento(), req.telefone(),
                req.email());
        return ResponseEntity.created(URI.create("/api/v1/publico/reservas/" + r.protocolo())).body(r);
    }

    @PostMapping("/reservas/consulta")
    public MinhasReservas consultar(@Valid @RequestBody Identificacao req) {
        limitar(req.cpf());
        return reservas.consultar(req.cpf(), req.dataNascimento());
    }

    @PostMapping("/reservas/{protocolo}/cancelamento")
    public ReservaPublica cancelar(@PathVariable String protocolo, @Valid @RequestBody Identificacao req) {
        limitar(req.cpf());
        return reservas.cancelarPeloLeitor(protocolo, req.cpf(), req.dataNascimento());
    }

    private void limitar(String cpf) {
        limites.consumir(porCpf, cpf.replaceAll("\\D", ""));
    }
}
