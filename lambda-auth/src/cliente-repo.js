import pg from 'pg';

/**
 * Acesso ao cliente no RDS.
 *
 * O pool é criado FORA do handler, no escopo do módulo: a Lambda reaproveita o
 * container entre invocações, então a conexão sobrevive às chamadas seguintes e
 * só a primeira paga o custo do handshake. Criar o pool dentro do handler abriria
 * uma conexão nova por requisição e esgotaria o limite do Postgres sob carga.
 */
let pool;

function obterPool() {
  if (!pool) {
    pool = new pg.Pool({
      host: exigir('DB_HOST'),
      port: Number(process.env.DB_PORT ?? 5432),
      database: exigir('DB_NAME'),
      user: exigir('DB_USER'),
      password: exigir('DB_PASSWORD'),
      // A Lambda vive na VPC junto do RDS; sem NAT, uma conexão pendurada
      // seguraria o container até o timeout da função.
      connectionTimeoutMillis: 5000,
      idleTimeoutMillis: 30000,
      max: 1,
      ssl: process.env.DB_SSL === 'false' ? false : { rejectUnauthorized: false },
    });
  }
  return pool;
}

/**
 * Busca o cliente pelo documento.
 *
 * @param {string} documento CPF normalizado (só dígitos) — é assim que a coluna
 *   é gravada pela aplicação (`Documento.numero()`).
 * @returns {Promise<{id: string, nome: string, ativo: boolean} | null>}
 */
export async function buscarPorDocumento(documento) {
  const { rows } = await obterPool().query(
    'SELECT id, nome, ativo FROM clientes WHERE documento = $1',
    [documento],
  );
  return rows[0] ?? null;
}

/** Fecha o pool. Usado nos testes; a Lambda não chama isso. */
export async function encerrar() {
  if (pool) {
    await pool.end();
    pool = undefined;
  }
}

function exigir(nome) {
  const valor = process.env[nome];
  if (!valor) {
    throw new Error(
      `Variável de ambiente ${nome} não definida. ` +
        'O Terraform a injeta a partir do SSM/Secrets Manager no apply (ver plano.md, decisão 4).',
    );
  }
  return valor;
}
