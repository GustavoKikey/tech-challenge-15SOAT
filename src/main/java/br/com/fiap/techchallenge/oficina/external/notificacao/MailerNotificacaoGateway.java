package br.com.fiap.techchallenge.oficina.external.notificacao;

import br.com.fiap.techchallenge.oficina.atendimento.entities.Cliente;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServico;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.MetricasGateway;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.NotificacaoGateway;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/**
 * Adapter da porta {@link NotificacaoGateway} sobre o Quarkus Mailer (SMTP).
 *
 * <p>Em dev/test o mailer roda em modo <i>mock</i> (loga o e-mail em vez de enviar);
 * em produção as credenciais SMTP vêm de variáveis de ambiente. Envio é best-effort:
 * qualquer falha é logada e engolida para não derrubar a operação de negócio.
 */
@ApplicationScoped
public class MailerNotificacaoGateway implements NotificacaoGateway {

    private static final Logger LOG = Logger.getLogger(MailerNotificacaoGateway.class);

    private final Mailer mailer;
    private final MetricasGateway metricas;

    public MailerNotificacaoGateway(Mailer mailer, MetricasGateway metricas) {
        this.mailer = mailer;
        this.metricas = metricas;
    }

    @Override
    public void notificarMudancaStatus(OrdemServico os, Cliente cliente) {
        try {
            String assunto = "Oficina — OS %s: %s".formatted(
                    os.id().valor(), os.status().descricao());
            String corpo = """
                    Olá, %s!

                    Sua Ordem de Serviço %s mudou de situação: %s.

                    Acompanhe pelo link: /publico/ordens-servico/%s/status
                    """.formatted(cliente.nome(), os.id().valor(),
                    os.status().descricao(), os.id().valor());
            mailer.send(Mail.withText(cliente.email(), assunto, corpo));
            LOG.infof("Notificação de status enviada — OS %s → %s (%s)",
                    os.id().valor(), os.status(), cliente.email());
        } catch (Exception e) {
            metricas.falhaIntegracao("email");
            LOG.errorf(e, "Falha ao notificar cliente %s sobre a OS %s (status %s)",
                    cliente.email(), os.id().valor(), os.status());
        }
    }
}
