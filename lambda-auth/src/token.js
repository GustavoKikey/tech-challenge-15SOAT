import jwt from 'jsonwebtoken';

/**
 * Emissão do JWT de cliente.
 *
 * Implementa o contrato do ADR 001 — a aplicação Quarkus valida exatamente estes
 * campos. Qualquer mudança aqui exige mudar o ADR e o `TokenDeTeste` do lado Java,
 * senão a app passa a devolver 401 sem explicação.
 */

const ISSUER = process.env.JWT_ISSUER ?? 'oficina-mvp';
const EXPIRACAO = process.env.JWT_EXPIRACAO ?? '30m';
const ROLE_CLIENTE = 'CLIENTE';

/**
 * @param {{id: string, nome: string}} cliente
 * @param {string} cpf CPF normalizado (11 dígitos)
 * @returns {{accessToken: string, expiresIn: number, tokenType: 'Bearer'}}
 */
export function emitirToken(cliente, cpf) {
  const chave = chavePrivada();

  const accessToken = jwt.sign(
    {
      // `upn` é o que o SmallRye usa como principal; `groups` é o formato de
      // roles exigido pelo MP-JWT (array, não string).
      upn: cliente.id,
      groups: [ROLE_CLIENTE],
      cpf,
    },
    chave,
    {
      algorithm: 'RS256',
      issuer: ISSUER,
      // `sub` é o UUID do cliente, nunca o CPF: o subject aparece em logs e
      // traces, e CPF é dado pessoal (ADR 001).
      subject: cliente.id,
      expiresIn: EXPIRACAO,
    },
  );

  return {
    accessToken,
    expiresIn: segundosDe(EXPIRACAO),
    tokenType: 'Bearer',
  };
}

/**
 * Chave privada RSA em PEM.
 *
 * Injetada como variável de ambiente pelo Terraform no momento do apply, a partir
 * do Secrets Manager — a Lambda não sai para a internet em runtime, então não
 * pode consultar o Secrets Manager sozinha (plano.md, decisão 4).
 *
 * Aceita tanto o PEM cru quanto base64: variável de ambiente com quebras de linha
 * é frágil em alguns pipelines, e base64 evita o problema.
 */
function chavePrivada() {
  const bruta = process.env.JWT_PRIVATE_KEY;
  if (!bruta) {
    throw new Error('Variável de ambiente JWT_PRIVATE_KEY não definida.');
  }
  return bruta.includes('BEGIN')
    ? bruta.replace(/\\n/g, '\n')
    : Buffer.from(bruta, 'base64').toString('utf8');
}

/** Converte "30m" / "8h" / "3600" em segundos, para devolver no `expiresIn`. */
function segundosDe(expiracao) {
  const m = /^(\d+)([smhd])?$/.exec(String(expiracao));
  if (!m) return 0;
  const valor = Number(m[1]);
  const fator = { s: 1, m: 60, h: 3600, d: 86400 }[m[2] ?? 's'];
  return valor * fator;
}
