package br.com.fiap.techchallenge.oficina.atendimento.usecases;

import br.com.fiap.techchallenge.oficina.atendimento.entities.Cliente;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.ClienteGateway;
import br.com.fiap.techchallenge.oficina.atendimento.entities.Veiculo;
import br.com.fiap.techchallenge.oficina.atendimento.entities.VeiculoId;
import br.com.fiap.techchallenge.oficina.atendimento.entities.VeiculoJaCadastradoException;
import br.com.fiap.techchallenge.oficina.atendimento.entities.VeiculoNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.atendimento.gateways.VeiculoGateway;
import br.com.fiap.techchallenge.oficina.shared.entities.Documento;
import br.com.fiap.techchallenge.oficina.shared.entities.Placa;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VeiculoUseCasesTest {

    @Mock VeiculoGateway veiculoRepo;
    @Mock ClienteGateway clienteRepo;

    @InjectMocks CadastrarVeiculoUseCase cadastrar;
    @InjectMocks AtualizarVeiculoUseCase atualizar;
    @InjectMocks RemoverVeiculoUseCase remover;
    @InjectMocks BuscarVeiculoPorIdUseCase buscar;
    @InjectMocks ListarVeiculosUseCase listar;

    private final ClienteId clienteId = ClienteId.novo();
    private final Cliente clienteMock = Cliente.reconstituir(
            clienteId, "Ana", Documento.de("52998224725"), null, null);
    private final Placa placa = Placa.de("ABC1234");

    @Test
    void cadastrarNovoVeiculo() {
        when(clienteRepo.buscarPorId(clienteId)).thenReturn(Optional.of(clienteMock));
        when(veiculoRepo.buscarPorPlaca(placa)).thenReturn(Optional.empty());
        when(veiculoRepo.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        Veiculo v = cadastrar.executar(new CadastrarVeiculoUseCase.Input(
                "ABC-1234", "Fiat", "Uno", 2010, clienteId));
        assertEquals(placa, v.placa());
        verify(veiculoRepo).salvar(any());
    }

    @Test
    void cadastrarFalhaSeClienteNaoExiste() {
        when(clienteRepo.buscarPorId(clienteId)).thenReturn(Optional.empty());
        var input = new CadastrarVeiculoUseCase.Input("ABC-1234", "Fiat", "Uno", 2010, clienteId);
        assertThrows(ClienteNaoEncontradoException.class, () -> cadastrar.executar(input));
        verify(veiculoRepo, never()).salvar(any());
    }

    @Test
    void cadastrarFalhaSePlacaJaExiste() {
        Veiculo existente = Veiculo.novo(placa, "Fiat", "Uno", 2010, clienteId);
        when(clienteRepo.buscarPorId(clienteId)).thenReturn(Optional.of(clienteMock));
        when(veiculoRepo.buscarPorPlaca(placa)).thenReturn(Optional.of(existente));

        var input = new CadastrarVeiculoUseCase.Input("ABC-1234", "Fiat", "Uno", 2010, clienteId);
        assertThrows(VeiculoJaCadastradoException.class, () -> cadastrar.executar(input));
    }

    @Test
    void atualizarFichaTecnicaSemTrocaDeDono() {
        Veiculo existente = Veiculo.novo(placa, "Fiat", "Uno", 2010, clienteId);
        when(veiculoRepo.buscarPorId(existente.id())).thenReturn(Optional.of(existente));
        when(veiculoRepo.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        Veiculo res = atualizar.executar(new AtualizarVeiculoUseCase.Input(
                existente.id(), "VW", "Gol", 2018, clienteId));
        assertEquals("VW", res.marca());
        verify(clienteRepo, never()).buscarPorId(any());
    }

    @Test
    void atualizarComTrocaDeDonoValidaCliente() {
        Veiculo existente = Veiculo.novo(placa, "Fiat", "Uno", 2010, clienteId);
        ClienteId novoDono = ClienteId.novo();
        Cliente novoDonoCliente = Cliente.reconstituir(novoDono, "Bia",
                Documento.de("11222333000181"), null, null);

        when(veiculoRepo.buscarPorId(existente.id())).thenReturn(Optional.of(existente));
        when(clienteRepo.buscarPorId(novoDono)).thenReturn(Optional.of(novoDonoCliente));
        when(veiculoRepo.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        Veiculo res = atualizar.executar(new AtualizarVeiculoUseCase.Input(
                existente.id(), "VW", "Gol", 2018, novoDono));
        assertEquals(novoDono, res.clienteId());
    }

    @Test
    void atualizarFalhaSeVeiculoNaoExiste() {
        VeiculoId id = VeiculoId.novo();
        when(veiculoRepo.buscarPorId(id)).thenReturn(Optional.empty());
        var input = new AtualizarVeiculoUseCase.Input(id, "VW", "Gol", 2018, null);
        assertThrows(VeiculoNaoEncontradoException.class, () -> atualizar.executar(input));
    }

    @Test
    void atualizarFalhaSeNovoDonoNaoExiste() {
        Veiculo existente = Veiculo.novo(placa, "Fiat", "Uno", 2010, clienteId);
        ClienteId novoDono = ClienteId.novo();

        when(veiculoRepo.buscarPorId(existente.id())).thenReturn(Optional.of(existente));
        when(clienteRepo.buscarPorId(novoDono)).thenReturn(Optional.empty());

        var input = new AtualizarVeiculoUseCase.Input(existente.id(), "VW", "Gol", 2018, novoDono);
        assertThrows(ClienteNaoEncontradoException.class, () -> atualizar.executar(input));
    }

    @Test
    void removerVeiculoExistente() {
        Veiculo existente = Veiculo.novo(placa, "Fiat", "Uno", 2010, clienteId);
        when(veiculoRepo.buscarPorId(existente.id())).thenReturn(Optional.of(existente));
        remover.executar(existente.id());
        verify(veiculoRepo).remover(existente.id());
    }

    @Test
    void removerFalhaSeNaoExiste() {
        VeiculoId id = VeiculoId.novo();
        when(veiculoRepo.buscarPorId(id)).thenReturn(Optional.empty());
        assertThrows(VeiculoNaoEncontradoException.class, () -> remover.executar(id));
    }

    @Test
    void buscarPorId() {
        Veiculo existente = Veiculo.novo(placa, "Fiat", "Uno", 2010, clienteId);
        when(veiculoRepo.buscarPorId(existente.id())).thenReturn(Optional.of(existente));
        assertSame(existente, buscar.executar(existente.id()));
    }

    @Test
    void buscarPorIdFalha() {
        VeiculoId id = VeiculoId.novo();
        when(veiculoRepo.buscarPorId(id)).thenReturn(Optional.empty());
        assertThrows(VeiculoNaoEncontradoException.class, () -> buscar.executar(id));
    }

    @Test
    void listarRetornaTudo() {
        Veiculo v = Veiculo.novo(placa, "Fiat", "Uno", 2010, clienteId);
        when(veiculoRepo.listar()).thenReturn(List.of(v));
        assertEquals(List.of(v), listar.executar());
    }

    @Test
    void listarPorClienteDelega() {
        Veiculo v = Veiculo.novo(placa, "Fiat", "Uno", 2010, clienteId);
        when(veiculoRepo.listarPorCliente(clienteId)).thenReturn(List.of(v));
        assertEquals(List.of(v), listar.porCliente(clienteId));
    }

    @Test
    void atualizarSemPassarNovoDono() {
        Veiculo existente = Veiculo.novo(placa, "Fiat", "Uno", 2010, clienteId);
        when(veiculoRepo.buscarPorId(existente.id())).thenReturn(Optional.of(existente));
        when(veiculoRepo.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        Veiculo res = atualizar.executar(new AtualizarVeiculoUseCase.Input(
                existente.id(), "VW", "Gol", 2018, null));
        
        assertEquals("VW", res.marca());
        assertEquals(clienteId, res.clienteId()); // Dono foi mantido
        verify(clienteRepo, never()).buscarPorId(any());
    }
}
