package br.com.fiap.techchallenge.oficina.atendimento.controllers;

import br.com.fiap.techchallenge.oficina.atendimento.dtos.AberturaOrdemServicoResponse;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.AbrirOrdemServicoRequest;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.InserirItemPecaRequest;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.InserirItemServicoRequest;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.OrdemServicoPublicaResponse;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.OrdemServicoResponse;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.StatusOSResponse;
import br.com.fiap.techchallenge.oficina.atendimento.entities.AcessoNegadoAOrdemServicoException;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServico;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ServicoId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.StatusOS;
import br.com.fiap.techchallenge.oficina.atendimento.entities.VeiculoId;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.ClienteGateway;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.MetricasGateway;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.NotificacaoGateway;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.OrdemServicoGateway;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.ServicoGateway;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.VeiculoGateway;
import br.com.fiap.techchallenge.oficina.atendimento.presenters.OrdemServicoPresenter;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.AbrirOrdemServicoUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.AprovarOrcamentoUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.BuscarOrdemServicoPorIdUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.EnviarOrcamentoUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.EntregarVeiculoUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.FinalizarServicoUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.GerarOrcamentoUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.IniciarDiagnosticoUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.InserirPecaNaOSUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.InserirServicoNaOSUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.ListarOrdensServicoUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.NotificarStatusOSUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.RecusarOrcamentoUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.RemoverPecaDaOSUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.RemoverServicoDaOSUseCase;
import br.com.fiap.techchallenge.oficina.estoque.entities.PecaId;
import br.com.fiap.techchallenge.oficina.estoque.gateways.PecaGateway;
import br.com.fiap.techchallenge.oficina.estoque.usecases.BaixarPecaUseCase;
import br.com.fiap.techchallenge.oficina.estoque.usecases.LiberarReservaUseCase;
import br.com.fiap.techchallenge.oficina.estoque.usecases.ReservarPecaUseCase;
import br.com.fiap.techchallenge.oficina.shared.usecases.ExecutorTransacional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Controller do agregado Ordem de Serviço — o orquestrador mais rico. Compõe os use
 * cases de OS e os sub-use-cases do BC Estoque (reservar/baixar/liberar) que entram
 * no fluxo de orçamento. A relação Customer-Supplier entre Atendimento e Estoque é
 * resolvida AQUI (no adaptador), nunca dentro do agregado.
 *
 * <p>Cada operação roda sob a porta {@link ExecutorTransacional}; como ela é
 * {@code REQUIRED}, a aprovação (que chama a baixa), a recusa (que libera reservas)
 * e a geração de orçamento (que chama a reserva) ficam atômicas mesmo cruzando
 * bounded contexts.
 *
 * <p>Transições de status notificam o cliente por e-mail <b>após</b> o commit
 * (best-effort — falha de envio não desfaz a operação).
 */
public class OrdemServicoController {

    private final AbrirOrdemServicoUseCase abrir;
    private final BuscarOrdemServicoPorIdUseCase buscar;
    private final ListarOrdensServicoUseCase listar;
    private final IniciarDiagnosticoUseCase iniciarDiagnostico;
    private final InserirServicoNaOSUseCase inserirServico;
    private final RemoverServicoDaOSUseCase removerServico;
    private final InserirPecaNaOSUseCase inserirPeca;
    private final RemoverPecaDaOSUseCase removerPeca;
    private final GerarOrcamentoUseCase gerarOrcamento;
    private final EnviarOrcamentoUseCase enviarOrcamento;
    private final AprovarOrcamentoUseCase aprovarOrcamento;
    private final RecusarOrcamentoUseCase recusarOrcamento;
    private final FinalizarServicoUseCase finalizar;
    private final EntregarVeiculoUseCase entregar;
    private final NotificarStatusOSUseCase notificarStatus;
    private final ExecutorTransacional tx;
    private final MetricasGateway metricas;
    private final OrdemServicoPresenter presenter = new OrdemServicoPresenter();

    public OrdemServicoController(OrdemServicoGateway osGateway,
                                  ClienteGateway clienteGateway,
                                  VeiculoGateway veiculoGateway,
                                  ServicoGateway servicoGateway,
                                  PecaGateway pecaGateway,
                                  NotificacaoGateway notificacaoGateway,
                                  ExecutorTransacional tx,
                                  MetricasGateway metricas) {
        ReservarPecaUseCase reservarPeca = new ReservarPecaUseCase(pecaGateway);
        BaixarPecaUseCase baixarPeca = new BaixarPecaUseCase(pecaGateway);
        LiberarReservaUseCase liberarReserva = new LiberarReservaUseCase(pecaGateway);
        this.abrir = new AbrirOrdemServicoUseCase(osGateway, clienteGateway, veiculoGateway,
                servicoGateway, pecaGateway);
        this.buscar = new BuscarOrdemServicoPorIdUseCase(osGateway);
        this.listar = new ListarOrdensServicoUseCase(osGateway);
        this.iniciarDiagnostico = new IniciarDiagnosticoUseCase(osGateway);
        this.inserirServico = new InserirServicoNaOSUseCase(osGateway, servicoGateway);
        this.removerServico = new RemoverServicoDaOSUseCase(osGateway);
        this.inserirPeca = new InserirPecaNaOSUseCase(osGateway, pecaGateway);
        this.removerPeca = new RemoverPecaDaOSUseCase(osGateway);
        this.gerarOrcamento = new GerarOrcamentoUseCase(osGateway, reservarPeca);
        this.enviarOrcamento = new EnviarOrcamentoUseCase(osGateway);
        this.aprovarOrcamento = new AprovarOrcamentoUseCase(osGateway, baixarPeca);
        this.recusarOrcamento = new RecusarOrcamentoUseCase(osGateway, liberarReserva);
        this.finalizar = new FinalizarServicoUseCase(osGateway);
        this.entregar = new EntregarVeiculoUseCase(osGateway);
        this.notificarStatus = new NotificarStatusOSUseCase(clienteGateway, notificacaoGateway);
        this.tx = tx;
        this.metricas = metricas;
    }

    public AberturaOrdemServicoResponse abrir(AbrirOrdemServicoRequest req) {
        OrdemServico os = executarENotificar("abrir", () -> abrir.executar(new AbrirOrdemServicoUseCase.Input(
                new AbrirOrdemServicoUseCase.DadosCliente(
                        req.cliente().documento(), req.cliente().nome(),
                        req.cliente().email(), req.cliente().telefone()),
                new AbrirOrdemServicoUseCase.DadosVeiculo(
                        req.veiculo().placa(), req.veiculo().marca(),
                        req.veiculo().modelo(), req.veiculo().ano()),
                req.servicos() == null ? List.of() : req.servicos().stream()
                        .map(s -> new AbrirOrdemServicoUseCase.ItemServicoInput(
                                ServicoId.de(s.servicoId()), s.valorCobrado()))
                        .toList(),
                req.pecas() == null ? List.of() : req.pecas().stream()
                        .map(p -> new AbrirOrdemServicoUseCase.ItemPecaInput(
                                PecaId.de(p.pecaId()), p.quantidade()))
                        .toList())));
        metricas.ordemServicoAberta();
        return presenter.apresentarAbertura(os);
    }

    public List<OrdemServicoResponse> listar(String status, UUID clienteId, UUID veiculoId) {
        OrdemServicoGateway.Filtro filtro = new OrdemServicoGateway.Filtro(
                status == null ? null : StatusOS.valueOf(status),
                clienteId == null ? null : ClienteId.de(clienteId),
                veiculoId == null ? null : VeiculoId.de(veiculoId));
        return presenter.apresentar(tx.emTransacao(() -> listar.executar(filtro)));
    }

    public OrdemServicoResponse buscar(UUID id) {
        return presenter.apresentar(tx.emTransacao(() -> buscar.executar(OrdemServicoId.de(id))));
    }

    public OrdemServicoPublicaResponse consultarPublico(UUID id) {
        return presenter.apresentarPublico(tx.emTransacao(() -> buscar.executar(OrdemServicoId.de(id))));
    }

    public StatusOSResponse consultarStatus(UUID id) {
        return presenter.apresentarStatus(tx.emTransacao(() -> buscar.executar(OrdemServicoId.de(id))));
    }

    public OrdemServicoResponse iniciarDiagnostico(UUID id) {
        return presenter.apresentar(executarENotificar("iniciarDiagnostico",
                () -> iniciarDiagnostico.executar(OrdemServicoId.de(id))));
    }

    public OrdemServicoResponse inserirServico(UUID id, InserirItemServicoRequest req) {
        InserirServicoNaOSUseCase.Output out = tx.emTransacao(() -> inserirServico.executar(
                new InserirServicoNaOSUseCase.Input(
                        OrdemServicoId.de(id), ServicoId.de(req.servicoId()), req.valorCobrado())));
        return presenter.apresentar(out.ordemServico());
    }

    public OrdemServicoResponse removerServico(UUID id, UUID itemId) {
        return presenter.apresentar(tx.emTransacao(() -> removerServico.executar(
                new RemoverServicoDaOSUseCase.Input(OrdemServicoId.de(id), itemId))));
    }

    public OrdemServicoResponse inserirPeca(UUID id, InserirItemPecaRequest req) {
        InserirPecaNaOSUseCase.Output out = tx.emTransacao(() -> inserirPeca.executar(
                new InserirPecaNaOSUseCase.Input(
                        OrdemServicoId.de(id), PecaId.de(req.pecaId()), req.quantidade(), req.valorUnitario())));
        return presenter.apresentar(out.ordemServico());
    }

    public OrdemServicoResponse removerPeca(UUID id, UUID itemId) {
        return presenter.apresentar(tx.emTransacao(() -> removerPeca.executar(
                new RemoverPecaDaOSUseCase.Input(OrdemServicoId.de(id), itemId))));
    }

    public OrdemServicoResponse gerarOrcamento(UUID id) {
        return presenter.apresentar(tx.emTransacao(() -> gerarOrcamento.executar(OrdemServicoId.de(id))));
    }

    public OrdemServicoResponse enviarOrcamento(UUID id) {
        return presenter.apresentar(executarENotificar("enviarOrcamento",
                () -> enviarOrcamento.executar(OrdemServicoId.de(id))));
    }

    public OrdemServicoResponse aprovarOrcamento(UUID id) {
        return presenter.apresentar(executarENotificar("aprovarOrcamento",
                () -> aprovarOrcamento.executar(OrdemServicoId.de(id))));
    }

    public OrdemServicoResponse recusarOrcamento(UUID id) {
        return presenter.apresentar(executarENotificar("recusarOrcamento",
                () -> recusarOrcamento.executar(OrdemServicoId.de(id))));
    }

    /** Notificação externa (webhook) da decisão do cliente sobre o orçamento. */
    public OrdemServicoResponse decidirOrcamento(UUID id, boolean aprovado) {
        return aprovado ? aprovarOrcamento(id) : recusarOrcamento(id);
    }

    // ------------------------------------------------------------------
    // Área do cliente autenticado por CPF (fase 3)
    //
    // Toda operação aqui recebe o id do cliente extraído do JWT e confere a
    // propriedade da OS antes de agir. Sem essa conferência, autenticar por CPF
    // apenas trocaria "qualquer um com o UUID" por "qualquer cliente logado".
    // ------------------------------------------------------------------

    /** OS do próprio cliente, na mesma ordenação de prioridade da listagem interna. */
    public List<OrdemServicoResponse> listarDoCliente(UUID clienteId) {
        return listar(null, clienteId, null);
    }

    public StatusOSResponse consultarStatusDoCliente(UUID osId, UUID clienteId) {
        OrdemServico os = tx.emTransacao(() -> buscarDoCliente(osId, clienteId));
        return presenter.apresentarStatus(os);
    }

    public OrdemServicoResponse consultarDoCliente(UUID osId, UUID clienteId) {
        OrdemServico os = tx.emTransacao(() -> buscarDoCliente(osId, clienteId));
        return presenter.apresentar(os);
    }

    /**
     * Aprova ou recusa o orçamento em nome do próprio cliente. A propriedade é
     * verificada <b>dentro</b> da mesma transação da decisão, para que a checagem
     * e a mudança de estado não possam divergir.
     */
    public OrdemServicoResponse decidirOrcamentoDoCliente(UUID osId, UUID clienteId, boolean aprovado) {
        String nome = aprovado ? "aprovarOrcamento" : "recusarOrcamento";
        OrdemServico os = executarENotificar(nome, () -> {
            buscarDoCliente(osId, clienteId);
            OrdemServicoId id = OrdemServicoId.de(osId);
            return aprovado ? aprovarOrcamento.executar(id) : recusarOrcamento.executar(id);
        });
        return presenter.apresentar(os);
    }

    /**
     * Carrega a OS garantindo que ela pertence ao cliente informado.
     *
     * @throws AcessoNegadoAOrdemServicoException se a OS for de outro cliente
     */
    private OrdemServico buscarDoCliente(UUID osId, UUID clienteId) {
        OrdemServico os = buscar.executar(OrdemServicoId.de(osId));
        if (!os.clienteId().equals(ClienteId.de(clienteId))) {
            throw new AcessoNegadoAOrdemServicoException(OrdemServicoId.de(osId));
        }
        return os;
    }

    public OrdemServicoResponse finalizar(UUID id) {
        return presenter.apresentar(executarENotificar("finalizar",
                () -> finalizar.executar(OrdemServicoId.de(id))));
    }

    public OrdemServicoResponse entregar(UUID id) {
        return presenter.apresentar(executarENotificar("entregar",
                () -> entregar.executar(OrdemServicoId.de(id))));
    }

    /**
     * Roda a operação em transação e, só depois do commit, notifica o cliente e
     * registra a telemetria de negócio.
     *
     * @param nome rótulo curto da operação, usado como tag da métrica de falha.
     */
    private OrdemServico executarENotificar(String nome, Supplier<OrdemServico> operacao) {
        OrdemServico os;
        try {
            os = tx.emTransacao(operacao::get);
        } catch (RuntimeException e) {
            metricas.falhaProcessamento(nome);
            throw e;
        }
        notificarStatus.executar(os);
        registrarFaseConcluida(os);
        return os;
    }

    /**
     * Mede quanto tempo a OS passou na fase que <i>acabou</i> de terminar, a partir
     * dos marcos temporais da própria entidade. Cada transição fecha exatamente uma
     * fase — por isso não há risco de contar a mesma duração duas vezes.
     */
    private void registrarFaseConcluida(OrdemServico os) {
        switch (os.status()) {
            case EM_EXECUCAO -> registrar(StatusOS.EM_DIAGNOSTICO,
                    os.diagnosticoIniciadoEm(), os.execucaoIniciadaEm());
            case FINALIZADA -> registrar(StatusOS.EM_EXECUCAO,
                    os.execucaoIniciadaEm(), os.finalizadaEm());
            case ENTREGUE -> registrar(StatusOS.FINALIZADA,
                    os.finalizadaEm(), os.entregueEm());
            default -> { /* demais status não encerram uma fase medida */ }
        }
    }

    private void registrar(StatusOS fase, OffsetDateTime inicio, OffsetDateTime fim) {
        if (inicio == null || fim == null) {
            return;
        }
        metricas.faseConcluida(fase.descricao(), Duration.between(inicio, fim));
    }
}
