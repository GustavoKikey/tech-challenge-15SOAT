package br.com.fiap.techchallenge.oficina.atendimento.entities;

import br.com.fiap.techchallenge.oficina.atendimento.entities.ClienteId;
import br.com.fiap.techchallenge.oficina.shared.entities.Placa;

import java.time.Year;
import java.util.Objects;

/**
 * Agregado raiz <b>Veículo</b>.
 *
 * <p>Identidade: {@link VeiculoId}. Placa é imutável após criação. Marca, modelo,
 * ano e dono (clienteId) podem ser alterados via métodos comportamentais.
 */
public class Veiculo {

    private static final int ANO_MINIMO = 1900;

    private final VeiculoId id;
    private final Placa placa;
    private String marca;
    private String modelo;
    private int ano;
    private ClienteId clienteId;

    private Veiculo(VeiculoId id, Placa placa, String marca, String modelo,
                    int ano, ClienteId clienteId) {
        this.id = Objects.requireNonNull(id, "id");
        this.placa = Objects.requireNonNull(placa, "placa");
        this.marca = exigirTexto(marca, "marca");
        this.modelo = exigirTexto(modelo, "modelo");
        this.ano = exigirAnoValido(ano);
        this.clienteId = Objects.requireNonNull(clienteId, "clienteId");
    }

    public static Veiculo novo(Placa placa, String marca, String modelo,
                               int ano, ClienteId clienteId) {
        return new Veiculo(VeiculoId.novo(), placa, marca, modelo, ano, clienteId);
    }

    public static Veiculo reconstituir(VeiculoId id, Placa placa, String marca, String modelo,
                                       int ano, ClienteId clienteId) {
        return new Veiculo(id, placa, marca, modelo, ano, clienteId);
    }

    public void atualizarFichaTecnica(String marca, String modelo, int ano) {
        this.marca = exigirTexto(marca, "marca");
        this.modelo = exigirTexto(modelo, "modelo");
        this.ano = exigirAnoValido(ano);
    }

    public void transferirPara(ClienteId novoDono) {
        this.clienteId = Objects.requireNonNull(novoDono, "novoDono");
    }

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " é obrigatório");
        }
        return valor.trim();
    }

    private static int exigirAnoValido(int ano) {
        int anoLimite = Year.now().getValue() + 1;
        if (ano < ANO_MINIMO || ano > anoLimite) {
            throw new IllegalArgumentException(
                    "Ano fora do intervalo permitido [" + ANO_MINIMO + ".." + anoLimite + "]: " + ano);
        }
        return ano;
    }

    public VeiculoId id()           { return id; }
    public Placa placa()            { return placa; }
    public String marca()           { return marca; }
    public String modelo()          { return modelo; }
    public int ano()                { return ano; }
    public ClienteId clienteId()    { return clienteId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Veiculo that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
