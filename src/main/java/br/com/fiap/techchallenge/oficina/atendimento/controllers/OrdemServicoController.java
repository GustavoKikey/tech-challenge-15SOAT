package br.com.fiap.techchallenge.oficina.atendimento.controllers;

import br.com.fiap.techchallenge.oficina.atendimento.dtos.CriarOrdemServicoRequest;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.InserirItemPecaRequest;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.InserirItemServicoRequest;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.OrdemServicoPublicaResponse;
import br.com.fiap.techchallenge.oficina.atendimento.dtos.OrdemServicoResponse;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ServicoId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.StatusOS;
import br.com.fiap.techchallenge.oficina.atendimento.entities.VeiculoId;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.ClienteGateway;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.OrdemServicoGateway;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.ServicoGateway;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.VeiculoGateway;
import br.com.fiap.techchallenge.oficina.atendimento.presenters.OrdemServicoPresenter;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.AprovarOrcamentoUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.BuscarOrdemServicoPorIdUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.CriarOrdemServicoUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.EnviarOrcamentoUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.EntregarVeiculoUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.FinalizarServicoUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.GerarOrcamentoUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.IniciarDiagnosticoUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.InserirPecaNaOSUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.InserirServicoNaOSUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.ListarOrdensServicoUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.RemoverPecaDaOSUseCase;
import br.com.fiap.techchallenge.oficina.atendimento.usecases.RemoverServicoDaOSUseCase;
import br.com.fiap.techchallenge.oficina.estoque.entities.PecaId;
import br.com.fiap.techchallenge.oficina.estoque.gateways.PecaGateway;
import br.com.fiap.techchallenge.oficina.estoque.usecases.BaixarPecaUseCase;
import br.com.fiap.techchallenge.oficina.estoque.usecases.ReservarPecaUseCase;
import br.com.fiap.techchallenge.oficina.shared.usecases.ExecutorTransacional;

import java.util.List;
import java.util.UUID;

/**
 * Controller do agregado Ordem de Serviço — o orquestrador mais rico. Compõe os use
 * cases de OS e os sub-use-cases do BC Estoque (reservar/baixar) que entram no fluxo
 * de orçamento. A relação Customer-Supplier entre Atendimento e Estoque é resolvida
 * AQUI (no adaptador), nunca dentro do agregado.
 *
 * <p>Cada operação roda sob a porta {@link ExecutorTransacional}; como ela é
 * {@code REQUIRED}, a aprovação (que chama a baixa) e a geração de orçamento (que
 * chama a reserva) ficam atômicas mesmo cruzando bounded contexts.
 */
public class OrdemServicoController {

    private final CriarOrdemServicoUseCase criar;
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
    private final FinalizarServicoUseCase finalizar;
    private final EntregarVeiculoUseCase entregar;
    private final ExecutorTransacional tx;
    private final OrdemServicoPresenter presenter = new OrdemServicoPresenter();

    public OrdemServicoController(OrdemServicoGateway osGateway,
                                  ClienteGateway clienteGateway,
                                  VeiculoGateway veiculoGateway,
                                  ServicoGateway servicoGateway,
                                  PecaGateway pecaGateway,
                                  ExecutorTransacional tx) {
        ReservarPecaUseCase reservarPeca = new ReservarPecaUseCase(pecaGateway);
        BaixarPecaUseCase baixarPeca = new BaixarPecaUseCase(pecaGateway);
        this.criar = new CriarOrdemServicoUseCase(osGateway, clienteGateway, veiculoGateway);
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
        this.finalizar = new FinalizarServicoUseCase(osGateway);
        this.entregar = new EntregarVeiculoUseCase(osGateway);
        this.tx = tx;
    }

    public OrdemServicoResponse criar(CriarOrdemServicoRequest req) {
        return presenter.apresentar(tx.emTransacao(() -> criar.executar(
                new CriarOrdemServicoUseCase.Input(req.documentoCliente(), req.placaVeiculo()))));
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

    public OrdemServicoResponse iniciarDiagnostico(UUID id) {
        return presenter.apresentar(tx.emTransacao(() -> iniciarDiagnostico.executar(OrdemServicoId.de(id))));
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
        return presenter.apresentar(tx.emTransacao(() -> enviarOrcamento.executar(OrdemServicoId.de(id))));
    }

    public OrdemServicoResponse aprovarOrcamento(UUID id) {
        return presenter.apresentar(tx.emTransacao(() -> aprovarOrcamento.executar(OrdemServicoId.de(id))));
    }

    public OrdemServicoResponse finalizar(UUID id) {
        return presenter.apresentar(tx.emTransacao(() -> finalizar.executar(OrdemServicoId.de(id))));
    }

    public OrdemServicoResponse entregar(UUID id) {
        return presenter.apresentar(tx.emTransacao(() -> entregar.executar(OrdemServicoId.de(id))));
    }
}
