package br.com.fiap.techchallenge.oficina.domain.atendimento.ordemservico;

import br.com.fiap.techchallenge.oficina.domain.atendimento.cliente.ClienteId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.servico.ServicoId;
import br.com.fiap.techchallenge.oficina.domain.atendimento.veiculo.VeiculoId;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.PecaId;
import br.com.fiap.techchallenge.oficina.domain.estoque.peca.ReservaId;
import br.com.fiap.techchallenge.oficina.domain.shared.Dinheiro;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrdemServicoTest {

    private OrdemServico novaOS() {
        return OrdemServico.abrir(ClienteId.novo(), VeiculoId.novo());
    }

    @Test
    void aberturaCriaEmRecebida() {
        OrdemServico os = novaOS();
        assertEquals(StatusOS.RECEBIDA, os.status());
        assertNotNull(os.id());
        assertNotNull(os.criadaEm());
        assertTrue(os.itensServico().isEmpty());
        assertTrue(os.itensPeca().isEmpty());
        assertNull(os.orcamento());
    }

    @Test
    void iniciarDiagnosticoTransicionaERegistraData() {
        OrdemServico os = novaOS();
        os.iniciarDiagnostico();
        assertEquals(StatusOS.EM_DIAGNOSTICO, os.status());
        assertNotNull(os.diagnosticoIniciadoEm());
    }

    @Test
    void iniciarDiagnosticoForaDeRecebidaLanca() {
        OrdemServico os = novaOS();
        os.iniciarDiagnostico();
        assertThrows(TransicaoStatusInvalidaException.class, os::iniciarDiagnostico);
    }

    @Test
    void inserirServicoExigeEmDiagnostico() {
        OrdemServico os = novaOS();
        assertThrows(TransicaoStatusInvalidaException.class,
                () -> os.inserirServico(ServicoId.novo(), Dinheiro.de("100.00")));
    }

    @Test
    void inserirItensEmDiagnostico() {
        OrdemServico os = novaOS();
        os.iniciarDiagnostico();
        ItemServico s = os.inserirServico(ServicoId.novo(), Dinheiro.de("100.00"));
        ItemPeca p = os.inserirPeca(PecaId.novo(), 2, Dinheiro.de("50.00"));
        assertEquals(1, os.itensServico().size());
        assertEquals(1, os.itensPeca().size());
        assertEquals(s, os.itensServico().get(0));
        assertEquals(p, os.itensPeca().get(0));
    }

    @Test
    void naoDevePermitirInserirItensAposGerarOrcamento() {
        OrdemServico os = novaOS();
        os.iniciarDiagnostico();
        os.inserirServico(ServicoId.novo(), Dinheiro.de("10.00"));
        os.gerarOrcamento();
        assertThrows(OrcamentoJaGeradoException.class,
                () -> os.inserirServico(ServicoId.novo(), Dinheiro.de("5.00")));
        assertThrows(OrcamentoJaGeradoException.class,
                () -> os.inserirPeca(PecaId.novo(), 1, Dinheiro.de("5.00")));
    }

    @Test
    void removerItemServicoEPecaAntesDoOrcamento() {
        OrdemServico os = novaOS();
        os.iniciarDiagnostico();
        ItemServico s = os.inserirServico(ServicoId.novo(), Dinheiro.de("10.00"));
        ItemPeca p = os.inserirPeca(PecaId.novo(), 1, Dinheiro.de("5.00"));
        os.removerItemServico(s.id());
        os.removerItemPeca(p.id());
        assertTrue(os.itensServico().isEmpty());
        assertTrue(os.itensPeca().isEmpty());
    }

    @Test
    void removerItemInexistenteLanca() {
        OrdemServico os = novaOS();
        os.iniciarDiagnostico();
        assertThrows(ItemOSNaoEncontradoException.class,
                () -> os.removerItemServico(UUID.randomUUID()));
        assertThrows(ItemOSNaoEncontradoException.class,
                () -> os.removerItemPeca(UUID.randomUUID()));
    }

    @Test
    void gerarOrcamentoSomaServicosEPecas() {
        OrdemServico os = novaOS();
        os.iniciarDiagnostico();
        os.inserirServico(ServicoId.novo(), Dinheiro.de("100.00"));
        os.inserirServico(ServicoId.novo(), Dinheiro.de("250.00"));
        os.inserirPeca(PecaId.novo(), 2, Dinheiro.de("50.00"));   // 100
        os.inserirPeca(PecaId.novo(), 3, Dinheiro.de("10.50"));   // 31.50
        Orcamento orc = os.gerarOrcamento();
        assertEquals(Dinheiro.de("481.50"), orc.valorTotal());
        assertNotNull(orc.geradoEm());
        assertFalse(orc.aprovado());
    }

    @Test
    void gerarOrcamentoSemItensLanca() {
        OrdemServico os = novaOS();
        os.iniciarDiagnostico();
        assertThrows(OrcamentoVazioException.class, os::gerarOrcamento);
    }

    @Test
    void gerarOrcamentoForaDeDiagnosticoLanca() {
        OrdemServico os = novaOS();
        assertThrows(TransicaoStatusInvalidaException.class, os::gerarOrcamento);
    }

    @Test
    void gerarOrcamentoDuasVezesLanca() {
        OrdemServico os = novaOS();
        os.iniciarDiagnostico();
        os.inserirServico(ServicoId.novo(), Dinheiro.de("10.00"));
        os.gerarOrcamento();
        assertThrows(OrcamentoJaGeradoException.class, os::gerarOrcamento);
    }

    @Test
    void enviarSemOrcamentoLanca() {
        OrdemServico os = novaOS();
        os.iniciarDiagnostico();
        assertThrows(TransicaoStatusInvalidaException.class, os::enviarOrcamento);
    }

    @Test
    void fluxoCompletoDeStatus() {
        OrdemServico os = novaOS();
        os.iniciarDiagnostico();
        os.inserirServico(ServicoId.novo(), Dinheiro.de("100.00"));
        os.gerarOrcamento();
        os.enviarOrcamento();
        assertEquals(StatusOS.AGUARDANDO_APROVACAO, os.status());

        os.aprovarOrcamento();
        assertEquals(StatusOS.EM_EXECUCAO, os.status());
        assertNotNull(os.execucaoIniciadaEm());
        assertTrue(os.orcamento().aprovado());

        os.finalizar();
        assertEquals(StatusOS.FINALIZADA, os.status());
        assertNotNull(os.finalizadaEm());

        os.entregar();
        assertEquals(StatusOS.ENTREGUE, os.status());
        assertNotNull(os.entregueEm());
    }

    @Test
    void aprovarSemEstarAguardandoLanca() {
        OrdemServico os = novaOS();
        assertThrows(TransicaoStatusInvalidaException.class, os::aprovarOrcamento);
    }

    @Test
    void finalizarForaDeExecucaoLanca() {
        OrdemServico os = novaOS();
        assertThrows(TransicaoStatusInvalidaException.class, os::finalizar);
    }

    @Test
    void entregarForaDeFinalizadaLanca() {
        OrdemServico os = novaOS();
        assertThrows(TransicaoStatusInvalidaException.class, os::entregar);
    }

    @Test
    void registrarReservaPreencheReservaIdNoItem() {
        OrdemServico os = novaOS();
        os.iniciarDiagnostico();
        ItemPeca item = os.inserirPeca(PecaId.novo(), 1, Dinheiro.de("5.00"));
        ReservaId reservaId = ReservaId.novo();

        os.registrarReserva(item.id(), reservaId);

        ItemPeca depois = os.itensPeca().get(0);
        assertEquals(reservaId, depois.reservaId());
        assertTrue(depois.possuiReserva());
    }

    @Test
    void registrarReservaEmItemInexistenteLanca() {
        OrdemServico os = novaOS();
        assertThrows(ItemOSNaoEncontradoException.class,
                () -> os.registrarReserva(UUID.randomUUID(), ReservaId.novo()));
    }

    @Test
    void itensListSaoImutaveis() {
        OrdemServico os = novaOS();
        assertThrows(UnsupportedOperationException.class,
                () -> os.itensServico().add(ItemServico.novo(ServicoId.novo(), Dinheiro.ZERO)));
        assertThrows(UnsupportedOperationException.class,
                () -> os.itensPeca().add(ItemPeca.novo(PecaId.novo(), 1, Dinheiro.ZERO)));
    }

    @Test
    void equalsEHashCodePorId() {
        OrdemServicoId id = OrdemServicoId.novo();
        ClienteId c = ClienteId.novo();
        VeiculoId v = VeiculoId.novo();
        OrdemServico a = OrdemServico.reconstituir(id, c, v, StatusOS.RECEBIDA,
                java.util.List.of(), java.util.List.of(), null,
                java.time.OffsetDateTime.now(), null, null, null, null);
        OrdemServico b = OrdemServico.reconstituir(id, c, v, StatusOS.ENTREGUE,
                java.util.List.of(), java.util.List.of(), null,
                java.time.OffsetDateTime.now(), null, null, null, null);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, novaOS());
        assertNotEquals(a, "x");
        assertEquals(a, a);
    }

    @Test
    void aprovarOrcamentoSemOrcamentoLanca() {
        OrdemServico os = OrdemServico.reconstituir(OrdemServicoId.novo(), ClienteId.novo(), VeiculoId.novo(),
                StatusOS.AGUARDANDO_APROVACAO, java.util.List.of(), java.util.List.of(), null,
                java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now(), null, null, null);
        assertThrows(TransicaoStatusInvalidaException.class, os::aprovarOrcamento);
    }

    @Test
    void removerItemAposOrcamentoLanca() {
        OrdemServico os = novaOS();
        os.iniciarDiagnostico();
        ItemServico s = os.inserirServico(ServicoId.novo(), Dinheiro.de("10.00"));
        ItemPeca p = os.inserirPeca(PecaId.novo(), 1, Dinheiro.de("5.00"));
        os.gerarOrcamento();
        assertThrows(OrcamentoJaGeradoException.class, () -> os.removerItemServico(s.id()));
        assertThrows(OrcamentoJaGeradoException.class, () -> os.removerItemPeca(p.id()));
    }

    @Test
    void gerarOrcamentoSoComServicoOuSoComPeca() {
        OrdemServico os1 = novaOS();
        os1.iniciarDiagnostico();
        os1.inserirServico(ServicoId.novo(), Dinheiro.de("10.00"));
        assertNotNull(os1.gerarOrcamento());

        OrdemServico os2 = novaOS();
        os2.iniciarDiagnostico();
        os2.inserirPeca(PecaId.novo(), 1, Dinheiro.de("5.00"));
        assertNotNull(os2.gerarOrcamento());
    }

    @Test
    void registrarReservaEmItemJaReservadoLanca() {
        OrdemServico os = novaOS();
        os.iniciarDiagnostico();
        ItemPeca item = os.inserirPeca(PecaId.novo(), 1, Dinheiro.de("5.00"));
        os.registrarReserva(item.id(), ReservaId.novo());
        assertThrows(IllegalStateException.class, () -> os.registrarReserva(item.id(), ReservaId.novo()));
    }
}
