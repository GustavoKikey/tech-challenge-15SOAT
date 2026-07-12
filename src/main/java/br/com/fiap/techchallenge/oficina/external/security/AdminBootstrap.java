package br.com.fiap.techchallenge.oficina.external.security;

import br.com.fiap.techchallenge.oficina.seguranca.gateways.PasswordHasher;
import br.com.fiap.techchallenge.oficina.seguranca.entities.Role;
import br.com.fiap.techchallenge.oficina.seguranca.entities.Usuario;
import br.com.fiap.techchallenge.oficina.seguranca.gateways.UsuarioGateway;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Garante que existe um usuário {@code admin} ao iniciar a app. Substitui o seed
 * SQL — BCrypt exige cálculo do hash em runtime, então não dá pra hardcodar na
 * migration de forma estável.
 *
 * <p>A senha vem da variável de ambiente {@code ADMIN_PASSWORD} (default
 * {@code admin123} para dev). Em produção, definir via secret manager e rotacionar
 * após o primeiro login.
 */
@Startup
@ApplicationScoped
public class AdminBootstrap {

    private static final Logger LOG = Logger.getLogger(AdminBootstrap.class);

    private final UsuarioGateway repository;
    private final PasswordHasher hasher;
    private final String adminUsername;
    private final String adminPassword;

    public AdminBootstrap(UsuarioGateway repository,
                          PasswordHasher hasher,
                          @ConfigProperty(name = "oficina.admin.username", defaultValue = "admin") String adminUsername,
                          @ConfigProperty(name = "oficina.admin.password", defaultValue = "admin123") String adminPassword) {
        this.repository = repository;
        this.hasher = hasher;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Transactional
    void bootstrap(@Observes StartupEvent event) {
        if (repository.buscarPorUsername(adminUsername).isPresent()) {
            LOG.debugf("Usuário '%s' já existe — bootstrap ignorado", adminUsername);
            return;
        }
        Usuario admin = Usuario.novo(adminUsername, hasher.hash(adminPassword), Role.ADMINISTRADOR);
        repository.salvar(admin);
        LOG.infof("Usuário administrador inicial '%s' provisionado pelo AdminBootstrap", adminUsername);
    }
}