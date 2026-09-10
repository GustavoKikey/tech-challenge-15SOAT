import { cpfValido, normalizar } from './cpf.js';
import { buscarPorDocumento } from './cliente-repo.js';
import { emitirToken } from './token.js';

/**
 * Handler da Function Serverless de autenticação (fase 3).
 *
 * Fluxo exigido pelo enunciado:
 *   1. valida o CPF do cliente;
 *   2. consulta a existência e o status do cliente na base;
 *   3. gera e devolve um JWT válido para as APIs protegidas.
 *
 * Integração: AWS API Gateway, proxy integration (payload v1 ou v2).
 *
 * POST /auth/cliente   { "cpf": "123.456.789-09" }
 *   200 { accessToken, expiresIn, tokenType, cliente: { id, nome } }
 *   400 CPF ausente ou inválido
 *   401 cliente inexistente ou inativo
 *   500 falha interna
 */
export async function handler(event) {
  try {
    const cpfBruto = extrairCpf(event);

    if (!cpfBruto) {
      return resposta(400, { erro: 'Informe o campo "cpf" no corpo da requisição.' });
    }
    if (!cpfValido(cpfBruto)) {
      return resposta(400, { erro: 'CPF inválido.' });
    }

    const cpf = normalizar(cpfBruto);
    const cliente = await buscarPorDocumento(cpf);

    // Inexistente e inativo devolvem a MESMA resposta de propósito: distinguir
    // os dois transformaria o endpoint em um verificador de cadastro, permitindo
    // descobrir quais CPFs são clientes da oficina.
    if (!cliente || !cliente.ativo) {
      console.warn(
        JSON.stringify({
          nivel: 'WARN',
          evento: 'autenticacao_recusada',
          motivo: cliente ? 'cliente_inativo' : 'cliente_inexistente',
          // CPF nunca vai inteiro para o log (dado pessoal).
          cpfParcial: `***${cpf.slice(-4)}`,
        }),
      );
      return resposta(401, { erro: 'Cliente não encontrado ou inativo.' });
    }

    const token = emitirToken(cliente, cpf);

    console.info(
      JSON.stringify({
        nivel: 'INFO',
        evento: 'autenticacao_concedida',
        clienteId: cliente.id,
        expiresIn: token.expiresIn,
      }),
    );

    return resposta(200, {
      ...token,
      cliente: { id: cliente.id, nome: cliente.nome },
    });
  } catch (e) {
    console.error(
      JSON.stringify({ nivel: 'ERROR', evento: 'autenticacao_falhou', erro: e.message }),
    );
    // A mensagem interna fica no log, não na resposta.
    return resposta(500, { erro: 'Erro ao processar a autenticação.' });
  }
}

/**
 * Lê o CPF do corpo, tolerando as duas formas em que o API Gateway entrega:
 * corpo já desserializado (integração direta/teste) ou string, com ou sem
 * base64 (`isBase64Encoded`).
 */
function extrairCpf(event) {
  if (!event) return null;

  let corpo = event.body ?? event;
  if (typeof corpo === 'string') {
    const texto = event.isBase64Encoded
      ? Buffer.from(corpo, 'base64').toString('utf8')
      : corpo;
    try {
      corpo = JSON.parse(texto);
    } catch {
      return null;
    }
  }
  const cpf = corpo?.cpf;
  return typeof cpf === 'string' && cpf.trim() !== '' ? cpf : null;
}

function resposta(statusCode, body) {
  return {
    statusCode,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  };
}
