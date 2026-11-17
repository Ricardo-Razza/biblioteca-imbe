package br.gov.imbe.biblioteca.importacao;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.gov.imbe.biblioteca.comum.Pagina;
import br.gov.imbe.biblioteca.comum.erro.RequisicaoInvalidaException;
import br.gov.imbe.biblioteca.importacao.ImportacaoDtos.ImportacaoResposta;
import br.gov.imbe.biblioteca.importacao.ImportacaoDtos.ItemResposta;

/** Carga inicial do acervo por planilha (CSV ou texto) de ISBNs. Perfil: Admin. */
@RestController
@RequestMapping("/api/v1/importacoes")
public class ImportacaoController {

    /** Planilha de 5.000 linhas com localização cabe com folga. */
    static final long TAMANHO_MAXIMO = 1024 * 1024;

    private final ImportacaoService servico;

    public ImportacaoController(ImportacaoService servico) {
        this.servico = servico;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportacaoResposta> importar(@RequestPart("arquivo") MultipartFile arquivo)
            throws IOException {
        if (arquivo.isEmpty()) {
            throw new RequisicaoInvalidaException(LeitorDePlanilha.TITULO_ERRO, "Escolha a planilha para enviar.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new RequisicaoInvalidaException(LeitorDePlanilha.TITULO_ERRO,
                    "A planilha pode ter no máximo 1 MB. Divida em arquivos menores.");
        }
        ImportacaoResposta r = servico.importar(arquivo.getOriginalFilename(), arquivo.getBytes());
        return ResponseEntity.created(URI.create("/api/v1/importacoes/" + r.id())).body(r);
    }

    @GetMapping
    public Pagina<ImportacaoResposta> listar(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return servico.listar(page, size);
    }

    @GetMapping("/{id}")
    public ImportacaoResposta buscar(@PathVariable Long id) {
        return servico.buscar(id);
    }

    @GetMapping("/{id}/itens")
    public Pagina<ItemResposta> itens(@PathVariable Long id,
            @RequestParam(required = false) List<ResultadoItem> resultado,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return servico.itens(id, resultado, page, size);
    }

    /** As linhas que falharam por fonte fora do ar voltam para a fila. */
    @PostMapping("/{id}/nova-tentativa")
    public ImportacaoResposta tentarDeNovo(@PathVariable Long id) {
        return servico.tentarDeNovo(id);
    }
}
