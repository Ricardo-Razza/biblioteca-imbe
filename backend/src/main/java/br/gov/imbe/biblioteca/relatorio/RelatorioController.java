package br.gov.imbe.biblioteca.relatorio;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.gov.imbe.biblioteca.relatorio.RelatorioService.Arquivo;

/**
 * {@code GET /api/v1/relatorios/{tipo}.csv}: emprestimos, atrasados, acervo, mais-emprestados, reservas e buscas-sem-resultado.
 * Os de período aceitam {@code de} e {@code ate} (aaaa-mm-dd); sem eles, os últimos 30 dias. Perfil: Atendente.
 */
@RestController
@RequestMapping("/api/v1/relatorios")
public class RelatorioController {

    static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final RelatorioService servico;

    public RelatorioController(RelatorioService servico) {
        this.servico = servico;
    }

    @GetMapping("/{tipo}.csv")
    public ResponseEntity<byte[]> baixar(@PathVariable String tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        Arquivo arquivo = servico.gerar(tipo, de, ate);
        return ResponseEntity.ok()
                .contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(arquivo.nome()).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(arquivo.conteudo());
    }
}
