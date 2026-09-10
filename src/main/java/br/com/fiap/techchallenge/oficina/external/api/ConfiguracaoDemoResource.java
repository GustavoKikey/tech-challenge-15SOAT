package br.com.fiap.techchallenge.oficina.external.api;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Map;
import java.util.Optional;

/**
 * Endereços que o painel de demonstração precisa conhecer em tempo de execução.
 *
 * <p>O painel roda no mesmo host da aplicação e chama a API por caminho
 * relativo — menos a autenticação por CPF, que é servida pelo API Gateway, em
 * outro domínio. Esse endereço muda a cada provisionamento, então não pode ser
 * fixado no JavaScript.
 *
 * <p>Ele chega até aqui pelo mesmo caminho de tudo mais nesta fase: o repositório
 * da Function publica a URL no Parameter Store, o deploy lê de lá e injeta como
 * variável de ambiente. Este recurso apenas repassa ao navegador — é o último
 * elo do contrato entre os quatro repositórios.
 *
 * <p>Público de propósito: são endereços de serviços públicos, não segredo. Sem
 * a anotação o recurso responderia 401, porque a aplicação nega por padrão
 * qualquer rota não anotada.
 */
@Path("/config-demo")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Demonstração", description = "Endereços usados pelo painel de demonstração")
public class ConfiguracaoDemoResource {

    private final String apiGatewayUrl;
    private final String ambiente;

    /**
     * A URL do Gateway é {@code Optional} e não {@code String} com valor padrão
     * vazio. O SmallRye Config converte string vazia em {@code null} ao injetar
     * {@code String}, e a injeção falha com SRCFG00040 — no <em>startup</em>,
     * derrubando a aplicação inteira. Ausência é o estado normal aqui (rodar a
     * aplicação sem a infraestrutura da fase 3 provisionada), então precisa ser
     * representável sem quebrar nada.
     */
    public ConfiguracaoDemoResource(
            @ConfigProperty(name = "oficina.demo.api-gateway-url") Optional<String> apiGatewayUrl,
            @ConfigProperty(name = "oficina.ambiente", defaultValue = "local") String ambiente) {
        this.apiGatewayUrl = apiGatewayUrl.orElse("");
        this.ambiente = ambiente;
    }

    @GET
    @PermitAll
    @Operation(summary = "Endereços de runtime do painel de demonstração",
            description = "URL do API Gateway (vazia quando não há Gateway provisionado) e o ambiente em execução.")
    public Map<String, String> configuracao() {
        return Map.of(
                "apiGatewayUrl", apiGatewayUrl,
                "ambiente", ambiente
        );
    }
}
