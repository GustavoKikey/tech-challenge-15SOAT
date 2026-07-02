package br.com.fiap.techchallenge.oficina.external.api.exception;

import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteJaCadastradoException;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ItemOSNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrcamentoJaGeradoException;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrcamentoVazioException;
import br.com.fiap.techchallenge.oficina.atendimento.entities.OrdemServicoNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.atendimento.entities.TransicaoStatusInvalidaException;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ServicoNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.atendimento.entities.VeiculoJaCadastradoException;
import br.com.fiap.techchallenge.oficina.atendimento.entities.VeiculoNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.estoque.entities.EstoqueInsuficienteException;
import br.com.fiap.techchallenge.oficina.estoque.entities.PecaNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.estoque.entities.PecaPossuiReservasAtivasException;
import br.com.fiap.techchallenge.oficina.estoque.entities.ReservaInvalidaException;
import br.com.fiap.techchallenge.oficina.seguranca.entities.CredenciaisInvalidasException;
import br.com.fiap.techchallenge.oficina.seguranca.entities.UsernameJaCadastradoException;
import br.com.fiap.techchallenge.oficina.seguranca.entities.UsuarioNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.shared.entities.DocumentoInvalidoException;
import br.com.fiap.techchallenge.oficina.shared.entities.DomainException;
import br.com.fiap.techchallenge.oficina.shared.entities.PlacaInvalidaException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class DomainExceptionMapper implements ExceptionMapper<DomainException> {

    private static final int UNPROCESSABLE_ENTITY = 422;

    @Override
    public Response toResponse(DomainException ex) {
        int status = mapearStatus(ex);
        String reason = reasonPhrase(status);
        ApiError body = ApiError.of(status, reason, ex.getMessage());
        return Response.status(status).entity(body).build();
    }

    private int mapearStatus(DomainException ex) {
        if (ex instanceof DocumentoInvalidoException
                || ex instanceof PlacaInvalidaException) {
            return Response.Status.BAD_REQUEST.getStatusCode();
        }
        if (ex instanceof CredenciaisInvalidasException) {
            return Response.Status.UNAUTHORIZED.getStatusCode();
        }
        if (ex instanceof ClienteJaCadastradoException
                || ex instanceof VeiculoJaCadastradoException
                || ex instanceof PecaPossuiReservasAtivasException
                || ex instanceof ReservaInvalidaException
                || ex instanceof TransicaoStatusInvalidaException
                || ex instanceof OrcamentoJaGeradoException
                || ex instanceof UsernameJaCadastradoException) {
            return Response.Status.CONFLICT.getStatusCode();
        }
        if (ex instanceof ClienteNaoEncontradoException
                || ex instanceof VeiculoNaoEncontradoException
                || ex instanceof ServicoNaoEncontradoException
                || ex instanceof PecaNaoEncontradaException
                || ex instanceof OrdemServicoNaoEncontradaException
                || ex instanceof ItemOSNaoEncontradoException
                || ex instanceof UsuarioNaoEncontradoException) {
            return Response.Status.NOT_FOUND.getStatusCode();
        }
        if (ex instanceof EstoqueInsuficienteException
                || ex instanceof OrcamentoVazioException) {
            return UNPROCESSABLE_ENTITY;
        }
        return Response.Status.BAD_REQUEST.getStatusCode();
    }

    private String reasonPhrase(int status) {
        Response.Status s = Response.Status.fromStatusCode(status);
        if (s != null) return s.getReasonPhrase();
        if (status == UNPROCESSABLE_ENTITY) return "Unprocessable Entity";
        return "Error";
    }
}
