package br.gov.imbe.biblioteca.acervo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
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

import br.gov.imbe.biblioteca.comum.erro.RequisicaoInvalidaException;

/**
 * Capa de uma obra. O GET é público (site e gestão) a partir da fase 3; o envio é do Atendente.
 */
@RestController
@RequestMapping("/api/v1/obras/{id}/capa")
public class CapaController {

    private final CapaService capas;

    public CapaController(CapaService capas) {
        this.capas = capas;
    }

    /**
     * {@code ?tamanho=mini} para listas (cerca de 200 px de largura); sem parâmetro, a capa grande.
     * O ETag é o nome do arquivo, que muda a cada troca: o navegador revalida e recebe 304 enquanto não mudar.
     */
    @GetMapping
    public ResponseEntity<Resource> obter(@PathVariable Long id, @RequestParam(required = false) String tamanho) {
        if (tamanho != null && !tamanho.equals("mini") && !tamanho.equals("grande")) {
            throw new RequisicaoInvalidaException("Parâmetro inválido", "Use tamanho=mini ou tamanho=grande.");
        }
        return capas.arquivo(id, "mini".equals(tamanho))
                .map(CapaController::imagem)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Campo {@code arquivo} (multipart). JPG, PNG, GIF ou BMP; vira JPEG sem metadados. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ObraResposta enviar(@PathVariable Long id, @RequestPart("arquivo") MultipartFile arquivo) {
        if (arquivo.isEmpty()) {
            throw new RequisicaoInvalidaException("Imagem inválida", "Escolha uma imagem para enviar.");
        }
        try {
            return capas.enviar(id, arquivo.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ResponseEntity<Resource> imagem(Path caminho) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .eTag(caminho.getFileName().toString())
                .cacheControl(CacheControl.noCache().cachePublic())
                .body(new FileSystemResource(caminho));
    }
}
