package br.com.fiap.techchallenge.oficina.interfaces.rest.exception;

import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteJaCadastradoException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.ItemOSNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrcamentoJaGeradoException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrcamentoVazioException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.TransicaoStatusInvalidaException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.ServicoNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.VeiculoJaCadastradoException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.VeiculoNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.EstoqueInsuficienteException;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaPossuiReservasAtivasException;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.ReservaInvalidaException;
import br.com.fiap.techchallenge.oficina.domain.seguranca.usuario.CredenciaisInvalidasException;
import br.com.fiap.techchallenge.oficina.domain.seguranca.usuario.UsernameJaCadastradoException;
import br.com.fiap.techchallenge.oficina.domain.seguranca.usuario.UsuarioNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.domain.shared.DocumentoInvalidoException;
import br.com.fiap.techchallenge.oficina.domain.shared.DomainException;
import br.com.fiap.techchallenge.oficina.domain.shared.PlacaInvalidaException;
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
