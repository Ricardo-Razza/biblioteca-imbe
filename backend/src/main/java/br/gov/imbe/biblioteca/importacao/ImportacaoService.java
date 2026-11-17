package br.gov.imbe.biblioteca.importacao;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import br.gov.imbe.biblioteca.acervo.Obra;
import br.gov.imbe.biblioteca.acervo.ObraRepository;
import br.gov.imbe.biblioteca.comum.Pagina;
import br.gov.imbe.biblioteca.comum.auditoria.AcaoAuditoria;
import br.gov.imbe.biblioteca.comum.auditoria.Auditoria;
import br.gov.imbe.biblioteca.comum.erro.RecursoNaoEncontradoException;
import br.gov.imbe.biblioteca.comum.erro.RegraNegocioException;
import br.gov.imbe.biblioteca.comum.seguranca.UsuarioAtual;
import br.gov.imbe.biblioteca.funcionario.Funcionario;
import br.gov.imbe.biblioteca.funcionario.FuncionarioRepository;
import br.gov.imbe.biblioteca.importacao.ImportacaoDtos.ImportacaoResposta;
import br.gov.imbe.biblioteca.importacao.ImportacaoDtos.ItemResposta;



/** Carga do acervo por planilha de ISBNs. O trabalho pesado fica no {@link ProcessadorDeImportacao}. */
@Service
public class ImportacaoService {

    private final ImportacaoRepository importacoes;
    private final ItemImportacaoRepository itens;
    private final ObraRepository obras;
    private final FuncionarioRepository funcionarios;
    private final ProcessadorDeImportacao processador;
    private final Auditoria auditoria;
    private final Clock relogio;

    ImportacaoService(ImportacaoRepository importacoes, ItemImportacaoRepository itens, ObraRepository obras, FuncionarioRepository funcionarios,
            ProcessadorDeImportacao processador, Auditoria auditoria, Clock relogio) {
        this.importacoes = importacoes;
        this.itens = itens;
        this.obras = obras;
        this.funcionarios = funcionarios;
        this.processador = processador;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    @Transactional
    public ImportacaoResposta importar(String nomeArquivo, byte[] conteudo) {
        List<LinhaPlanilha> linhas = LeitorDePlanilha.ler(conteudo);
        String arquivo = nomeArquivo == null || nomeArquivo.isBlank() ? "planilha.csv"
                : nomeArquivo.length() > 255 ? nomeArquivo.substring(0, 255) : nomeArquivo;
        Importacao importacao = importacoes.save(
                new Importacao(arquivo, UsuarioAtual.idOuNulo(), LocalDateTime.now(relogio)));
        itens.saveAll(linhas.stream().map(l -> new ItemImportacao(importacao.getId(), l)).toList());
        auditoria.registrar(AcaoAuditoria.IMPORTACAO_INICIADA, "importacao", importacao.getId(),
                arquivo + ": " + linhas.size() + (linhas.size() == 1 ? " linha" : " linhas"));
        agendarDepoisDoCommit(importacao.getId());
        return resposta(importacao);
    }

    @Transactional(readOnly = true)
    public Pagina<ImportacaoResposta> listar(int pagina, int tamanho) {
        return Pagina.de(importacoes.findAllByOrderByIdDesc(PageRequest.of(Math.max(0, pagina),
                Math.clamp(tamanho, 1, 50))), this::resposta);
    }

    @Transactional(readOnly = true)
    public ImportacaoResposta buscar(Long id) {
        return resposta(entidade(id));
    }

    /** Linhas da importação; {@code resultados} vazio = todas. */
    @Transactional(readOnly = true)
    public Pagina<ItemResposta> itens(Long id, Collection<ResultadoItem> resultados, int pagina, int tamanho) {
        entidade(id);
        Collection<ResultadoItem> filtro = resultados == null || resultados.isEmpty()
                ? EnumSet.allOf(ResultadoItem.class) : resultados;
        Page<ItemImportacao> page = itens.findByImportacaoIdAndResultadoInOrderByLinha(id, filtro,
                PageRequest.of(Math.max(0, pagina), Math.clamp(tamanho, 1, 100)));
        Map<Long, String> titulos = obras.findAllById(page.getContent().stream().map(ItemImportacao::getObraId)
                .filter(o -> o != null).distinct().toList()).stream()
                .collect(Collectors.toMap(Obra::getId, Obra::getTitulo));
        return Pagina.de(page, i -> new ItemResposta(i.getId(), i.getLinha(), i.getIsbnOriginal(), i.getIsbn13(),
                i.getTombo(), i.getLocalizacao(), i.getResultado(), i.getObraId(), titulos.get(i.getObraId()),
                i.getMensagem()));
    }

    /** Volta para a fila as linhas que falharam por fonte fora do ar. */
    @Transactional
    public ImportacaoResposta tentarDeNovo(Long id) {
        Importacao importacao = entidade(id);
        if (importacao.getStatus() == Importacao.Status.PROCESSANDO) {
            throw new RegraNegocioException("A importação ainda está em andamento.");
        }
        if (itens.voltarIndisponiveisParaFila(id) == 0) {
            throw new RegraNegocioException("Nenhuma linha falhou por fonte fora do ar.");
        }
        importacao.reabrir();
        agendarDepoisDoCommit(id);
        return resposta(importacao);
    }

    private void agendarDepoisDoCommit(Long id) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    processador.agendar(id);
                }
            });
        } else {
            processador.agendar(id);
        }
    }

    private Importacao entidade(Long id) {
        return importacoes.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Importação " + id + " não encontrada."));
    }

    private ImportacaoResposta resposta(Importacao i) {
        Map<ResultadoItem, Long> contagem = new EnumMap<>(ResultadoItem.class);
        for (ResultadoItem r : ResultadoItem.values()) {
            contagem.put(r, 0L);
        }
        for (Object[] linha : itens.contar(i.getId())) {
            contagem.put((ResultadoItem) linha[0], (Long) linha[1]);
        }
        long total = contagem.values().stream().mapToLong(Long::longValue).sum();
        String funcionario = i.getFuncionarioId() == null ? null
                : funcionarios.findById(i.getFuncionarioId()).map(Funcionario::getNome).orElse(null);
        return new ImportacaoResposta(i.getId(), i.getArquivo(), funcionario, i.getCriadaEm(), i.getStatus(),
                i.getFinalizadaEm(), total, contagem.get(ResultadoItem.PENDENTE), contagem);
    }
}
