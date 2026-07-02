package br.com.fiap.techchallenge.oficina.atendimento.entities;

import br.com.fiap.techchallenge.oficina.shared.entities.Documento;

import java.util.Objects;

/**
 * Agregado raiz <b>Cliente</b>.
 *
 * <p>Identidade: {@link ClienteId}. Documento ({@link Documento}) é imutável após
 * criação — alteração de documento implica criar outro cliente. Nome, email e
 * telefone podem ser alterados via métodos comportamentais.
 */
public class Cliente {

    private final ClienteId id;
    private final Documento documento;
    private String nome;
    private String email;
    private String telefone;

    private Cliente(ClienteId id, String nome, Documento documento, String email, String telefone) {
        this.id = Objects.requireNonNull(id, "id");
        this.documento = Objects.requireNonNull(documento, "documento");
        this.nome = exigirNome(nome);
        this.email = email;
        this.telefone = telefone;
    }

    /** Cria um novo cliente (gera id). */
    public static Cliente novo(String nome, Documento documento, String email, String telefone) {
        return new Cliente(ClienteId.novo(), nome, documento, email, telefone);
    }

    /** Reidrata um cliente já persistido (não regera id). Uso restrito ao mapper. */
    public static Cliente reconstituir(ClienteId id, String nome, Documento documento,
                                       String email, String telefone) {
        return new Cliente(id, nome, documento, email, telefone);
    }

    public void renomear(String novoNome) {
        this.nome = exigirNome(novoNome);
    }

    public void alterarContato(String email, String telefone) {
        this.email = email;
        this.telefone = telefone;
    }

    private static String exigirNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do cliente é obrigatório");
        }
        return nome.trim();
    }

    public ClienteId id()           { return id; }
    public String nome()            { return nome; }
    public Documento documento()    { return documento; }
    public String email()           { return email; }
    public String telefone()        { return telefone; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cliente that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
