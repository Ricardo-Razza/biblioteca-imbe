package br.gov.imbe.biblioteca.acervo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class BuscaDeCapasFaltandoTest {

    ObraRepository obras = mock(ObraRepository.class);
    CapaService capas = mock(CapaService.class);

    private static ObraRepository.SemCapa semCapa(long id, String isbn) {
        return new ObraRepository.SemCapa() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getIsbn13() {
                return isbn;
            }
        };
    }

    @Test
    void tentaCadaObraSemCapaRespeitandoOLimiteEContaAsQueGanharam() {
        given(obras.semCapa(any())).willReturn(List.of(semCapa(1, "9788535902778"), semCapa(2, "9780306406157")));
        given(capas.tentarCapaPeloIsbn(1L, "9788535902778")).willReturn(true);
        given(capas.tentarCapaPeloIsbn(2L, "9780306406157")).willReturn(false);

        int achadas = new BuscaDeCapasFaltando(obras, capas, 50, Duration.ZERO).executar();

        assertThat(achadas).isEqualTo(1);
        verify(obras).semCapa(PageRequest.of(0, 50));
        verify(capas).tentarCapaPeloIsbn(2L, "9780306406157");
    }
}
