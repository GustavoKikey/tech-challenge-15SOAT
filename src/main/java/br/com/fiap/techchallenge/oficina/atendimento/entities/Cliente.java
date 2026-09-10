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
    private boolean ativo;

    private Cliente(ClienteId id, String nome, Documento documento, String email,
                    String telefone, boolean ativo) {
        this.id = Objects.requireNonNull(id, "id");
        this.documento = Objects.requireNonNull(documento, "documento");
        this.nome = exigirNome(nome);
        this.email = email;
        this.telefone = telefone;
        this.ativo = ativo;
    }

    /** Cria um novo cliente (gera id). Nasce ativo: pode autenticar e abrir OS. */
    public static Cliente novo(String nome, Documento documento, String email, String telefone) {
        return new Cliente(ClienteId.novo(), nome, documento, email, telefone, true);
    }

    /**
     * Reidrata um cliente já persistido (não regera id). Uso restrito ao mapper.
     *
     * <p>{@code ativo} é explícito de propósito: um default implícito faria um cliente
     * desativado voltar como ativo se alguém esquecesse de mapear a coluna — falha
     * silenciosa de segurança, já que é esse campo que autoriza a emissão do token.
     */
    public static Cliente reconstituir(ClienteId id, String nome, Documento documento,
                                       String email, String telefone, boolean ativo) {
        return new Cliente(id, nome, documento, email, telefone, ativo);
    }

    public void renomear(String novoNome) {
        this.nome = exigirNome(novoNome);
    }

    /**
     * Desativa o cliente. A Function Serverless de autenticação recusa emitir token
     * para cliente inativo; tokens já emitidos continuam válidos até expirarem
     * (ver ADR 001 — janela de 30 min).
     */
    public void desativar() {
        this.ativo = false;
    }

    /** Reabilita um cliente desativado. */
    public void ativar() {
        this.ativo = true;
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
    public boolean ativo()          { return ativo; }

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
