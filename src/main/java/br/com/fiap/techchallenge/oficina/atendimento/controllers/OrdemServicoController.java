package br.com.fiap.techchallenge.oficina.atendimento.controllers;

import br.com.fiap.techchallenge.oficina.atendimento.dtos.AberturaOrdemServicoResponse;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.AbrirOrdemServicoRequest;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.InserirItemPecaRequest;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.InserirItemServicoRequest;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.OrdemServicoPublicaResponse;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.OrdemServicoResponse;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.StatusOSResponse;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServico;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ServicoId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.StatusOS;
import br.com.fiap.techchallenge.oficina.atendimento.entities.VeiculoId;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.ClienteGateway;
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
    private final OrdemServicoPresenter presenter = new OrdemServicoPresenter();

    public OrdemServicoController(OrdemServicoGateway osGateway,
                                  ClienteGateway clienteGateway,
                                  VeiculoGateway veiculoGateway,
                                  ServicoGateway servicoGateway,
                                  PecaGateway pecaGateway,
                                  NotificacaoGateway notificacaoGateway,
                                  ExecutorTransacional tx) {
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
    }

    public AberturaOrdemServicoResponse abrir(AbrirOrdemServicoRequest req) {
        OrdemServico os = executarENotificar(() -> abrir.executar(new AbrirOrdemServicoUseCase.Input(
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
        return presenter.apresentar(executarENotificar(
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
        return presenter.apresentar(executarENotificar(
                () -> enviarOrcamento.executar(OrdemServicoId.de(id))));
    }

    public OrdemServicoResponse aprovarOrcamento(UUID id) {
        return presenter.apresentar(executarENotificar(
                () -> aprovarOrcamento.executar(OrdemServicoId.de(id))));
    }

    public OrdemServicoResponse recusarOrcamento(UUID id) {
        return presenter.apresentar(executarENotificar(
                () -> recusarOrcamento.executar(OrdemServicoId.de(id))));
    }

    /** Notificação externa (webhook) da decisão do cliente sobre o orçamento. */
    public OrdemServicoResponse decidirOrcamento(UUID id, boolean aprovado) {
        return aprovado ? aprovarOrcamento(id) : recusarOrcamento(id);
    }

    public OrdemServicoResponse finalizar(UUID id) {
        return presenter.apresentar(executarENotificar(
                () -> finalizar.executar(OrdemServicoId.de(id))));
    }

    public OrdemServicoResponse entregar(UUID id) {
        return presenter.apresentar(executarENotificar(
                () -> entregar.executar(OrdemServicoId.de(id))));
    }

    /** Roda a operação em transação e, só depois do commit, notifica o cliente. */
    private OrdemServico executarENotificar(Supplier<OrdemServico> operacao) {
        OrdemServico os = tx.emTransacao(operacao::get);
        notificarStatus.executar(os);
        return os;
    }
}
