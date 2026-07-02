package br.com.fiap.techchallenge.oficina.atendimento.gateways;

import br.com.fiap.techchallenge.oficina.atendimento.entities.Cliente;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServico;

/**
 * Porta de saída para avisar o cliente sobre mudanças de status da OS
 * (fase 2: "atualização de status via alguma ferramenta como e-mail").
 *
 * <p>Implementação em {@code external/notificacao} (Quarkus Mailer). O envio é
 * <i>best-effort</i>: falha de notificação não pode derrubar a operação de negócio,
 * então implementações devem capturar e logar erros de entrega.
 */
public interface NotificacaoGateway {

    void notificarMudancaStatus(OrdemServico os, Cliente cliente);
}
