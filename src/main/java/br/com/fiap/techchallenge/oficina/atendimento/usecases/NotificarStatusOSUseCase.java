package br.com.fiap.techchallenge.oficina.atendimento.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.Cliente;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServico;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.ClienteGateway;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.NotificacaoGateway;

/**
 * Notifica o cliente sobre a mudança de status da OS.
 *
 * <p>Chamado pelo controller <b>após</b> a transação da transição de status ser
 * confirmada — assim nenhum e-mail é disparado para uma mudança que sofreu rollback.
 * Cliente sem e-mail cadastrado simplesmente não é notificado.
 */
public class NotificarStatusOSUseCase {

    private final ClienteGateway clienteRepository;
    private final NotificacaoGateway notificacaoGateway;

    public NotificarStatusOSUseCase(ClienteGateway clienteRepository,
                                    NotificacaoGateway notificacaoGateway) {
        this.clienteRepository = clienteRepository;
        this.notificacaoGateway = notificacaoGateway;
    }

    public void executar(OrdemServico os) {
        Cliente cliente = clienteRepository.buscarPorId(os.clienteId()).orElse(null);
        if (cliente == null || cliente.email() == null || cliente.email().isBlank()) {
            return;
        }
        notificacaoGateway.notificarMudancaStatus(os, cliente);
    }
}
