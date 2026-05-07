package br.com.fiap.techchallenge.oficina.interfaces.rest;

import br.com.fiap.techchallenge.oficina.application.seguranca.AutenticarUsuarioUseCase;
import br.com.fiap.techchallenge.oficina.application.seguranca.CadastrarUsuarioUseCase;
import br.com.fiap.techchallenge.oficina.domain.seguranca.usuario.Usuario;
import br.com.fiap.techchallenge.oficina.interfaces.rest.dto.seguranca.CadastrarUsuarioRequest;
import br.com.fiap.techchallenge.oficina.interfaces.rest.dto.seguranca.LoginRequest;
import br.com.fiap.techchallenge.oficina.interfaces.rest.dto.seguranca.LoginResponse;
import br.com.fiap.techchallenge.oficina.interfaces.rest.dto.seguranca.UsuarioResponse;
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

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Autenticação", description = "Login JWT e cadastro de usuários administrativos")
public class AuthResource {

    private final AutenticarUsuarioUseCase autenticar;
    private final CadastrarUsuarioUseCase cadastrar;

    public AuthResource(AutenticarUsuarioUseCase autenticar, CadastrarUsuarioUseCase cadastrar) {
        this.autenticar = autenticar;
        this.cadastrar = cadastrar;
    }

    @POST
    @Path("/login")
    @PermitAll
    @Operation(summary = "Autentica e devolve JWT")
    @APIResponse(responseCode = "200", description = "Token emitido")
    @APIResponse(responseCode = "401", description = "Credenciais inválidas")
    public LoginResponse login(@Valid LoginRequest req) {
        AutenticarUsuarioUseCase.Output out = autenticar.executar(
                new AutenticarUsuarioUseCase.Input(req.username(), req.password()));
        return new LoginResponse(out.accessToken(), out.expiresIn(), out.role());
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
        Usuario usuario = cadastrar.executar(new CadastrarUsuarioUseCase.Input(
                req.username(), req.password(), req.role()));
        return Response.status(Response.Status.CREATED)
                .entity(UsuarioResponse.from(usuario))
                .build();
    }
}
