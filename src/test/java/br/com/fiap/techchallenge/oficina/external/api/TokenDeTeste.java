package br.com.fiap.techchallenge.oficina.external.api;

import io.smallrye.jwt.build.Jwt;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Emite tokens JWT reais para os testes, assinados com a mesma chave do
 * classpath que a aplicação usa para validar.
 *
 * <p>Existe porque {@code @TestSecurity} fixa a identidade na anotação, e a área
 * do cliente precisa de tokens com {@code sub} <b>diferente por chamada</b> — é
 * justamente a troca de identidade que os testes de isolamento exercitam.
 *
 * <p>Os tokens de cliente seguem o contrato do ADR 001, o mesmo que a Function
 * Serverless terá de honrar. Se o contrato mudar aqui sem mudar lá (ou o
 * contrário), os testes de isolamento quebram — que é o efeito desejado.
 */
final class TokenDeTeste {

    private static final String ISSUER = "oficina-mvp";

    private TokenDeTeste() {}

    /** Token de cliente conforme ADR 001: {@code sub} = UUID e {@code groups} = CLIENTE. */
    static String paraCliente(String clienteId) {
        return Jwt.issuer(ISSUER)
                .subject(clienteId)
                .upn(clienteId)
                .groups(Set.of("CLIENTE"))
                .claim("cpf", "00000000000")
                .expiresIn(Duration.ofMinutes(30))
                .sign();
    }

    /** Token administrativo, como o emitido por {@code POST /auth/login}. */
    static String paraAdministrador() {
        return Jwt.issuer(ISSUER)
                .subject("admin")
                .upn("admin")
                .groups(Set.of("ADMINISTRADOR"))
                .expiresIn(Duration.ofHours(1))
                .sign();
    }

    /**
     * CPF válido e único por chamada.
     *
     * <p>A base compartilhada entre os ITs faz CPF fixo colidir com 409 assim que
     * dois testes criam o mesmo cliente — daí gerar em vez de constante. Os dois
     * dígitos verificadores são calculados de verdade porque o
     * {@code Documento} do domínio os valida.
     *
     * <p>A faixa começa em 9xx para não bater nos CPFs fixos dos ITs mais antigos,
     * que ocupam 1xx a 7xx (o primeiro valor gerado aqui já foi, uma vez,
     * exatamente o {@code 100.000.001-08} do {@code OrdemServicoResourceIT}).
     */
    static String cpfValido() {
        int base = 900_000_000 + SEQUENCIA.getAndIncrement();
        String digitos = String.valueOf(base);
        return digitos + verificadores(digitos);
    }

    private static final AtomicInteger SEQUENCIA = new AtomicInteger(1);

    private static String verificadores(String base9) {
        int d1 = digito(base9, 10);
        int d2 = digito(base9 + d1, 11);
        return "" + d1 + d2;
    }

    private static int digito(String digitos, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < digitos.length(); i++) {
            soma += Character.getNumericValue(digitos.charAt(i)) * (pesoInicial - i);
        }
        int resto = soma * 10 % 11;
        return resto == 10 ? 0 : resto;
    }
}
