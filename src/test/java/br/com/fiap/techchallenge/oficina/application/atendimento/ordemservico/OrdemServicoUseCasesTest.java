package br.com.fiap.techchallenge.oficina.application.atendimento.ordemservico;

import br.com.fiap.techchallenge.oficina.application.estoque.BaixarPecaUseCase;
import br.com.fiap.techchallenge.oficina.application.estoque.ReservarPecaUseCase;
import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.Cliente;
import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteRepository;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.ItemPeca;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServico;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.OrdemServicoRepository;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.StatusOS;
import br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico.TransicaoStatusInvalidaException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.Servico;
import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.ServicoId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.ServicoNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.ServicoRepository;
import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.Veiculo;
import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.VeiculoNaoEncontradoException;
import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.VeiculoRepository;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.EstoqueInsuficienteException;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.Peca;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaId;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaNaoEncontradaException;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaRepository;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.Reserva;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.ReservaId;
import br.com.fiap.techchallenge.oficina.domain.shared.Dinheiro;
import br.com.fiap.techchallenge.oficina.domain.shared.Documento;
import br.com.fiap.techchallenge.oficina.domain.shared.Placa;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdemServicoUseCasesTest {

    @Mock OrdemServicoRepository osRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock VeiculoRepository veiculoRepository;
    @Mock ServicoRepository servicoRepository;
    @Mock PecaRepository pecaRepository;
    @Mock ReservarPecaUseCase reservarPeca;
    @Mock BaixarPecaUseCase baixarPeca;

    private static final String CPF = "11144477735";
    private static final String PLACA = "ABC1234";

    // ---------- CriarOrdemServicoUseCase ----------

    @Test
    void criarComCpfEPlacaValidos() {
        Cliente cliente = Cliente.novo("Ana", Documento.de(CPF), null, null);
        Veiculo veiculo = Veiculo.novo(Placa.de(PLACA), "VW", "Golf", 2020, cliente.id());
        when(clienteRepository.buscarPorDocumento(any())).thenReturn(Optional.of(cliente));
        when(veiculoRepository.buscarPorPlaca(any())).thenReturn(Optional.of(veiculo));
        when(osRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        CriarOrdemServicoUseCase uc = new CriarOrdemServicoUseCase(
                osRepository, clienteRepository, veiculoRepository);
        OrdemServico os = uc.executar(new CriarOrdemServicoUseCase.Input(CPF, PLACA));

        assertEquals(StatusOS.RECEBIDA, os.status());
        assertEquals(cliente.id(), os.clienteId());
        assertEquals(veiculo.id(), os.veiculoId());
    }

    @Test
    void criarFalhaSeClienteNaoExiste() {
        when(clienteRepository.buscarPorDocumento(any())).thenReturn(Optional.empty());
        CriarOrdemServicoUseCase uc = new CriarOrdemServicoUseCase(
                osRepository, clienteRepository, veiculoRepository);
        assertThrows(ClienteNaoEncontradoException.class, () ->
                uc.executar(new CriarOrdemServicoUseCase.Input(CPF, PLACA)));
    }

    @Test
    void criarFalhaSeVeiculoNaoExiste() {
        Cliente cliente = Cliente.novo("Ana", Documento.de(CPF), null, null);
        when(clienteRepository.buscarPorDocumento(any())).thenReturn(Optional.of(cliente));
        when(veiculoRepository.buscarPorPlaca(any())).thenReturn(Optional.empty());
        CriarOrdemServicoUseCase uc = new CriarOrdemServicoUseCase(
                osRepository, clienteRepository, veiculoRepository);
        assertThrows(VeiculoNaoEncontradoException.class, () ->
                uc.executar(new CriarOrdemServicoUseCase.Input(CPF, PLACA)));
    }

    // ---------- IniciarDiagnostico / Finalizar / Entregar ----------

    @Test
    void iniciarDiagnostico() {
        OrdemServico os = novaOSRecebida();
        when(osRepository.buscarPorId(os.id())).thenReturn(Optional.of(os));
        when(osRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));
        OrdemServico out = new IniciarDiagnosticoUseCase(osRepository).executar(os.id());
        assertEquals(StatusOS.EM_DIAGNOSTICO, out.status());
    }

    @Test
    void iniciarDiagnosticoFalhaSeNaoExiste() {
        OrdemServicoId id = OrdemServicoId.novo();
        when(osRepository.buscarPorId(id)).thenReturn(Optional.empty());
        IniciarDiagnosticoUseCase uc = new IniciarDiagnosticoUseCase(osRepository);
        assertThrows(OrdemServicoNaoEncontradaException.class, () -> uc.executar(id));
    }

    // ---------- InserirServico / InserirPeca ----------

    @Test
    void inserirServicoExigeServicoExistente() {
        ServicoId sid = ServicoId.novo();
        when(servicoRepository.buscarPorId(sid)).thenReturn(Optional.empty());
        InserirServicoNaOSUseCase uc = new InserirServicoNaOSUseCase(osRepository, servicoRepository);
        var input = new InserirServicoNaOSUseCase.Input(
                OrdemServicoId.novo(), sid, BigDecimal.TEN);
        assertThrows(ServicoNaoEncontradoException.class, () -> uc.executar(input));
    }

    @Test
    void inserirServicoNaOS() {
        Servico servico = Servico.novo("Troca de óleo", Dinheiro.de("100.00"));
        OrdemServico os = novaOSEmDiagnostico();
        when(servicoRepository.buscarPorId(servico.id())).thenReturn(Optional.of(servico));
        when(osRepository.buscarPorId(os.id())).thenReturn(Optional.of(os));
        when(osRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        InserirServicoNaOSUseCase uc = new InserirServicoNaOSUseCase(osRepository, servicoRepository);
        var out = uc.executar(new InserirServicoNaOSUseCase.Input(
                os.id(), servico.id(), new BigDecimal("120.00")));
        assertEquals(1, out.ordemServico().itensServico().size());
        assertEquals(servico.id(), out.item().servicoId());
    }

    @Test
    void inserirPecaValidaSaldo() {
        Peca peca = Peca.novo("Filtro", Dinheiro.de("50.00"), 2);
        when(pecaRepository.buscarPorId(peca.id())).thenReturn(Optional.of(peca));

        InserirPecaNaOSUseCase uc = new InserirPecaNaOSUseCase(osRepository, pecaRepository);
        var input = new InserirPecaNaOSUseCase.Input(
                OrdemServicoId.novo(), peca.id(), 5, new BigDecimal("50.00"));
        assertThrows(EstoqueInsuficienteException.class, () -> uc.executar(input));
        verify(osRepository, never()).salvar(any());
    }

    @Test
    void inserirPecaFalhaSePecaNaoExiste() {
        PecaId pid = PecaId.novo();
        when(pecaRepository.buscarPorId(pid)).thenReturn(Optional.empty());
        InserirPecaNaOSUseCase uc = new InserirPecaNaOSUseCase(osRepository, pecaRepository);
        var input = new InserirPecaNaOSUseCase.Input(
                OrdemServicoId.novo(), pid, 1, BigDecimal.ONE);
        assertThrows(PecaNaoEncontradaException.class, () -> uc.executar(input));
    }

    // ---------- GerarOrcamentoUseCase ----------

    @Test
    void gerarOrcamentoReservaCadaPecaERegistraReservaId() {
        OrdemServico os = novaOSEmDiagnostico();
        os.inserirServico(ServicoId.novo(), Dinheiro.de("100.00"));
        ItemPeca item1 = os.inserirPeca(PecaId.novo(), 2, Dinheiro.de("50.00"));
        ItemPeca item2 = os.inserirPeca(PecaId.novo(), 1, Dinheiro.de("30.00"));

        when(osRepository.buscarPorIdComLock(os.id())).thenReturn(Optional.of(os));
        when(osRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        ReservaId r1 = ReservaId.novo();
        ReservaId r2 = ReservaId.novo();
        when(reservarPeca.executar(any())).thenReturn(
                Reserva.reconstituir(r1, os.id(), 2,
                        br.com.fiap.techchallenge.oficina.domain.estoque.peca.StatusReserva.ATIVA,
                        java.time.OffsetDateTime.now()),
                Reserva.reconstituir(r2, os.id(), 1,
                        br.com.fiap.techchallenge.oficina.domain.estoque.peca.StatusReserva.ATIVA,
                        java.time.OffsetDateTime.now())
        );

        OrdemServico salvo = new GerarOrcamentoUseCase(osRepository, reservarPeca).executar(os.id());

        verify(reservarPeca, times(2)).executar(any());
        assertNotNull(salvo.orcamento());
        assertEquals(Dinheiro.de("230.00"), salvo.orcamento().valorTotal());
        assertEquals(r1, salvo.itensPeca().get(0).reservaId());
        assertEquals(r2, salvo.itensPeca().get(1).reservaId());
        // sanity: usamos os refs
        assertNotNull(item1);
        assertNotNull(item2);
    }

    @Test
    void gerarOrcamentoPropagaFalhaDeReservaParaRollback() {
        OrdemServico os = novaOSEmDiagnostico();
        os.inserirPeca(PecaId.novo(), 2, Dinheiro.de("50.00"));
        os.inserirPeca(PecaId.novo(), 1, Dinheiro.de("30.00"));
        when(osRepository.buscarPorIdComLock(os.id())).thenReturn(Optional.of(os));
        when(reservarPeca.executar(any()))
                .thenReturn(Reserva.reconstituir(ReservaId.novo(), os.id(), 2,
                        br.com.fiap.techchallenge.oficina.domain.estoque.peca.StatusReserva.ATIVA,
                        java.time.OffsetDateTime.now()))
                .thenThrow(new EstoqueInsuficienteException(PecaId.novo(), 1, 0));

        GerarOrcamentoUseCase uc = new GerarOrcamentoUseCase(osRepository, reservarPeca);
        assertThrows(EstoqueInsuficienteException.class, () -> uc.executar(os.id()));
        // ao falhar antes do salvar, a transação JTA reverte tudo (a 1ª reserva também).
        verify(osRepository, never()).salvar(any());
    }

    @Test
    void gerarOrcamentoFalhaSeNaoExiste() {
        OrdemServicoId id = OrdemServicoId.novo();
        when(osRepository.buscarPorIdComLock(id)).thenReturn(Optional.empty());
        GerarOrcamentoUseCase uc = new GerarOrcamentoUseCase(osRepository, reservarPeca);
        assertThrows(OrdemServicoNaoEncontradaException.class, () -> uc.executar(id));
    }

    // ---------- AprovarOrcamentoUseCase ----------

    @Test
    void aprovarOrcamentoBaixaCadaPeca() {
        OrdemServico os = osPronta();
        when(osRepository.buscarPorIdComLock(os.id())).thenReturn(Optional.of(os));
        when(osRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        OrdemServico salvo = new AprovarOrcamentoUseCase(osRepository, baixarPeca).executar(os.id());

        verify(baixarPeca, times(2)).executar(any());
        assertEquals(StatusOS.EM_EXECUCAO, salvo.status());
        assertTrue(salvo.orcamento().aprovado());
    }

    @Test
    void aprovarOrcamentoPropagaFalhaDaBaixa() {
        OrdemServico os = osPronta();
        when(osRepository.buscarPorIdComLock(os.id())).thenReturn(Optional.of(os));
        doThrow(new RuntimeException("falha de baixa"))
                .when(baixarPeca).executar(any());
        AprovarOrcamentoUseCase uc = new AprovarOrcamentoUseCase(osRepository, baixarPeca);
        assertThrows(RuntimeException.class, () -> uc.executar(os.id()));
        verify(osRepository, never()).salvar(any());
    }

    @Test
    void aprovarSemAguardandoLanca() {
        OrdemServico os = novaOSRecebida();
        when(osRepository.buscarPorIdComLock(os.id())).thenReturn(Optional.of(os));
        AprovarOrcamentoUseCase uc = new AprovarOrcamentoUseCase(osRepository, baixarPeca);
        assertThrows(TransicaoStatusInvalidaException.class, () -> uc.executar(os.id()));
        verifyNoInteractions(baixarPeca);
    }

    // ---------- Finalizar / Entregar / Buscar / Listar ----------

    @Test
    void finalizar() {
        OrdemServico os = osPronta();
        os.aprovarOrcamento();
        when(osRepository.buscarPorId(os.id())).thenReturn(Optional.of(os));
        when(osRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));
        OrdemServico out = new FinalizarServicoUseCase(osRepository).executar(os.id());
        assertEquals(StatusOS.FINALIZADA, out.status());
    }

    @Test
    void entregar() {
        OrdemServico os = osPronta();
        os.aprovarOrcamento();
        os.finalizar();
        when(osRepository.buscarPorId(os.id())).thenReturn(Optional.of(os));
        when(osRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));
        OrdemServico out = new EntregarVeiculoUseCase(osRepository).executar(os.id());
        assertEquals(StatusOS.ENTREGUE, out.status());
    }

    @Test
    void buscarFalha() {
        OrdemServicoId id = OrdemServicoId.novo();
        when(osRepository.buscarPorId(id)).thenReturn(Optional.empty());
        BuscarOrdemServicoPorIdUseCase uc = new BuscarOrdemServicoPorIdUseCase(osRepository);
        assertThrows(OrdemServicoNaoEncontradaException.class, () -> uc.executar(id));
    }

    // ---------- EnviarOrcamentoUseCase ----------

    @Test
    void enviarOrcamentoMudaStatusParaAguardandoAprovacao() {
        OrdemServico os = novaOSEmDiagnostico();
        os.inserirServico(ServicoId.novo(), Dinheiro.de("100.00"));
        os.gerarOrcamento();
        when(osRepository.buscarPorId(os.id())).thenReturn(Optional.of(os));
        when(osRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        OrdemServico out = new EnviarOrcamentoUseCase(osRepository).executar(os.id());
        assertEquals(StatusOS.AGUARDANDO_APROVACAO, out.status());
    }

    @Test
    void enviarOrcamentoFalhaSeOSNaoExiste() {
        OrdemServicoId id = OrdemServicoId.novo();
        when(osRepository.buscarPorId(id)).thenReturn(Optional.empty());
        EnviarOrcamentoUseCase uc = new EnviarOrcamentoUseCase(osRepository);
        assertThrows(OrdemServicoNaoEncontradaException.class, () -> uc.executar(id));
        verify(osRepository, never()).salvar(any());
    }

    // ---------- RemoverServicoDaOSUseCase ----------

    @Test
    void removerServicoTiraItemDaOS() {
        OrdemServico os = novaOSEmDiagnostico();
        var item = os.inserirServico(ServicoId.novo(), Dinheiro.de("50.00"));
        when(osRepository.buscarPorId(os.id())).thenReturn(Optional.of(os));
        when(osRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        RemoverServicoDaOSUseCase uc = new RemoverServicoDaOSUseCase(osRepository);
        OrdemServico out = uc.executar(new RemoverServicoDaOSUseCase.Input(os.id(), item.id()));

        assertEquals(0, out.itensServico().size());
    }

    @Test
    void removerServicoFalhaSeOSNaoExiste() {
        OrdemServicoId id = OrdemServicoId.novo();
        when(osRepository.buscarPorId(id)).thenReturn(Optional.empty());
        RemoverServicoDaOSUseCase uc = new RemoverServicoDaOSUseCase(osRepository);
        assertThrows(OrdemServicoNaoEncontradaException.class,
                () -> uc.executar(new RemoverServicoDaOSUseCase.Input(id, java.util.UUID.randomUUID())));
    }

    // ---------- RemoverPecaDaOSUseCase ----------

    @Test
    void removerPecaTiraItemDaOS() {
        OrdemServico os = novaOSEmDiagnostico();
        var item = os.inserirPeca(PecaId.novo(), 1, Dinheiro.de("20.00"));
        when(osRepository.buscarPorId(os.id())).thenReturn(Optional.of(os));
        when(osRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        RemoverPecaDaOSUseCase uc = new RemoverPecaDaOSUseCase(osRepository);
        OrdemServico out = uc.executar(new RemoverPecaDaOSUseCase.Input(os.id(), item.id()));

        assertEquals(0, out.itensPeca().size());
    }

    @Test
    void removerPecaFalhaSeOSNaoExiste() {
        OrdemServicoId id = OrdemServicoId.novo();
        when(osRepository.buscarPorId(id)).thenReturn(Optional.empty());
        RemoverPecaDaOSUseCase uc = new RemoverPecaDaOSUseCase(osRepository);
        assertThrows(OrdemServicoNaoEncontradaException.class,
                () -> uc.executar(new RemoverPecaDaOSUseCase.Input(id, java.util.UUID.randomUUID())));
    }

    // ---------- InserirPecaNaOSUseCase — caminho feliz ----------

    @Test
    void inserirPecaSalvaQuandoSaldoEhSuficiente() {
        Peca peca = Peca.novo("Filtro", Dinheiro.de("50.00"), 5);
        OrdemServico os = novaOSEmDiagnostico();
        when(pecaRepository.buscarPorId(peca.id())).thenReturn(Optional.of(peca));
        when(osRepository.buscarPorId(os.id())).thenReturn(Optional.of(os));
        when(osRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        InserirPecaNaOSUseCase uc = new InserirPecaNaOSUseCase(osRepository, pecaRepository);
        InserirPecaNaOSUseCase.Output out = uc.executar(new InserirPecaNaOSUseCase.Input(
                os.id(), peca.id(), 2, new BigDecimal("50.00")));

        assertEquals(1, out.ordemServico().itensPeca().size());
        assertEquals(peca.id(), out.item().pecaId());
        assertEquals(2, out.item().quantidade());
    }

    @Test
    void listarRepassaFiltro() {
        OrdemServicoRepository.Filtro f = new OrdemServicoRepository.Filtro(
                StatusOS.RECEBIDA, ClienteId.novo(), null);
        when(osRepository.listar(f)).thenReturn(java.util.List.of());
        new ListarOrdensServicoUseCase(osRepository).executar(f);
        verify(osRepository).listar(f);
    }

    // ---------- Helpers ----------

    private OrdemServico novaOSRecebida() {
        return OrdemServico.abrir(ClienteId.novo(),
                br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.VeiculoId.novo());
    }

    private OrdemServico novaOSEmDiagnostico() {
        OrdemServico os = novaOSRecebida();
        os.iniciarDiagnostico();
        return os;
    }

    /** OS já com orçamento gerado, peças com reservaId preenchido e status AGUARDANDO_APROVACAO. */
    private OrdemServico osPronta() {
        OrdemServico os = novaOSEmDiagnostico();
        ItemPeca i1 = os.inserirPeca(PecaId.novo(), 2, Dinheiro.de("50.00"));
        ItemPeca i2 = os.inserirPeca(PecaId.novo(), 1, Dinheiro.de("30.00"));
        os.gerarOrcamento();
        os.registrarReserva(i1.id(), ReservaId.novo());
        os.registrarReserva(i2.id(), ReservaId.novo());
        os.enviarOrcamento();
        return os;
    }
}
