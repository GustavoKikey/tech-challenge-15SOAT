package br.com.fiap.techchallenge.oficina.external.api;

import br.com.fiap.techchallenge.oficina.seguranca.controllers.SegurancaController;
import br.com.fiap.techchallenge.oficina.seguranca.dtos.CadastrarUsuarioRequest;
import br.com.fiap.techchallenge.oficina.seguranca.dtos.LoginRequest;
import br.com.fiap.techchallenge.oficina.seguranca.dtos.LoginResponse;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Driver HTTP (Frameworks &amp; Drivers) do BC Segurança. Só cuida de protocolo —
 * rotas, content-type, status codes e segurança declarativa — e delega toda a
 * orquestração ao {@link SegurancaController}.
 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Autenticação", description = "Login JWT e cadastro de usuários administrativos")
public class AuthResource {

    private final SegurancaController controller;

    public AuthResource(SegurancaController controller) {
        this.controller = controller;
    }

    @POST
    @Path("/login")
    @PermitAll
    @Operation(summary = "Autentica e devolve JWT")
    @APIResponse(responseCode = "200", description = "Token emitido")
    @APIResponse(responseCode = "401", description = "Credenciais inválidas")
    public LoginResponse login(@Valid LoginRequest req) {
        return controller.login(req);
    }

    @POST
    @Path("/usuarios")
    @RolesAllowed("ADMINISTRADOR")
    @Operation(summary = "Cadastra um novo usuário administrativo (somente ADMINISTRADOR)")
    @APIResponse(responseCode = "201", description = "Usuário criado")
    @APIResponse(responseCode = "401", description = "Não autenticado")
    @APIResponse(responseCode = "403", description = "Sem permissão (role insuficiente)")
    @APIResponse(responseCode = "409", description = "Username já cadastrado")
    public Response cadastrar(@Valid CadastrarUsuarioRequest req) {
        return Response.status(Response.Status.CREATED)
                .entity(controller.cadastrar(req))
                .build();
    }
}
