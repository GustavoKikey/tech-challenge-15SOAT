package br.com.fiap.techchallenge.oficina.external.config;

import br.com.fiap.techchallenge.oficina.seguranca.controllers.SegurancaController;
import br.com.fiap.techchallenge.oficina.seguranca.gateways.PasswordHasher;
import br.com.fiap.techchallenge.oficina.seguranca.gateways.TokenService;
import br.com.fiap.techchallenge.oficina.seguranca.gateways.UsuarioGateway;
import br.com.fiap.techchallenge.oficina.shared.usecases.ExecutorTransacional;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * Composition root do BC Segurança. Único lugar que conhece CDI <i>e</i> os
 * controllers/use cases ao mesmo tempo: monta o grafo de objetos do núcleo a
 * partir dos gateways (beans da infra). Mantém Controllers e Use Cases como POJOs
 * livres de anotação de framework.
 */
@ApplicationScoped
public class SegurancaBeans {

    @Produces
    @ApplicationScoped
    SegurancaController segurancaController(UsuarioGateway usuarioGateway,
                                            PasswordHasher hasher,
                                            TokenService tokenService,
                                            ExecutorTransacional tx) {
        return new SegurancaController(usuarioGateway, hasher, tokenService, tx);
    }
}
