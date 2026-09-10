import { describe, it, expect, beforeAll, vi } from 'vitest';
import { generateKeyPairSync } from 'node:crypto';
import jwt from 'jsonwebtoken';

/**
 * Testa o handler ponta a ponta com o banco mockado.
 *
 * O foco é o contrato do ADR 001: se o token sair diferente do que a aplicação
 * Quarkus valida, estes testes quebram antes de o problema virar um 401 mudo em
 * produção.
 */

const CLIENTE_ATIVO = {
  id: '3f1a2b3c-4d5e-6f70-8192-a3b4c5d6e7f8',
  nome: 'Alice',
  ativo: true,
};

const buscarPorDocumento = vi.fn();
vi.mock('../src/cliente-repo.js', () => ({
  buscarPorDocumento: (...args) => buscarPorDocumento(...args),
}));

let handler;
let publicKey;

beforeAll(async () => {
  const par = generateKeyPairSync('rsa', {
    modulusLength: 2048,
    publicKeyEncoding: { type: 'spki', format: 'pem' },
    privateKeyEncoding: { type: 'pkcs8', format: 'pem' },
  });
  publicKey = par.publicKey;
  process.env.JWT_PRIVATE_KEY = par.privateKey;
  process.env.JWT_ISSUER = 'oficina-mvp';

  ({ handler } = await import('../src/index.js'));
});

const corpo = (r) => JSON.parse(r.body);

describe('handler de autenticação', () => {
  it('emite token no contrato do ADR 001 para cliente ativo', async () => {
    buscarPorDocumento.mockResolvedValueOnce(CLIENTE_ATIVO);

    const r = await handler({ body: JSON.stringify({ cpf: '529.982.247-25' }) });
    expect(r.statusCode).toBe(200);

    const { accessToken, tokenType, expiresIn } = corpo(r);
    expect(tokenType).toBe('Bearer');
    expect(expiresIn).toBe(1800); // 30 min

    // Verifica a assinatura com a chave pública — é o que a app Quarkus faz.
    const claims = jwt.verify(accessToken, publicKey, {
      algorithms: ['RS256'],
      issuer: 'oficina-mvp',
    });

    expect(claims.sub).toBe(CLIENTE_ATIVO.id);   // UUID, não o CPF
    expect(claims.upn).toBe(CLIENTE_ATIVO.id);
    expect(claims.groups).toEqual(['CLIENTE']);
    expect(claims.cpf).toBe('52998224725');      // normalizado
    expect(claims.exp - claims.iat).toBe(1800);
  });

  it('busca no banco pelo CPF normalizado, como a coluna é gravada', async () => {
    buscarPorDocumento.mockResolvedValueOnce(CLIENTE_ATIVO);
    await handler({ body: JSON.stringify({ cpf: '529.982.247-25' }) });
    expect(buscarPorDocumento).toHaveBeenCalledWith('52998224725');
  });

  it('recusa cliente inativo com 401', async () => {
    buscarPorDocumento.mockResolvedValueOnce({ ...CLIENTE_ATIVO, ativo: false });
    const r = await handler({ body: JSON.stringify({ cpf: '529.982.247-25' }) });
    expect(r.statusCode).toBe(401);
  });

  it('recusa cliente inexistente com a MESMA resposta do inativo', async () => {
    buscarPorDocumento.mockResolvedValueOnce({ ...CLIENTE_ATIVO, ativo: false });
    const inativo = await handler({ body: JSON.stringify({ cpf: '529.982.247-25' }) });

    buscarPorDocumento.mockResolvedValueOnce(null);
    const inexistente = await handler({ body: JSON.stringify({ cpf: '529.982.247-25' }) });

    // Respostas idênticas: o endpoint não pode virar verificador de cadastro.
    expect(inexistente.statusCode).toBe(inativo.statusCode);
    expect(inexistente.body).toBe(inativo.body);
  });

  it('rejeita CPF inválido com 400, sem tocar no banco', async () => {
    buscarPorDocumento.mockClear();
    const r = await handler({ body: JSON.stringify({ cpf: '111.111.111-11' }) });
    expect(r.statusCode).toBe(400);
    expect(buscarPorDocumento).not.toHaveBeenCalled();
  });

  it('rejeita corpo ausente, vazio ou malformado com 400', async () => {
    expect((await handler({ body: '{}' })).statusCode).toBe(400);
    expect((await handler({ body: 'nao e json' })).statusCode).toBe(400);
    expect((await handler({})).statusCode).toBe(400);
  });

  it('aceita corpo em base64 (isBase64Encoded do API Gateway)', async () => {
    buscarPorDocumento.mockResolvedValueOnce(CLIENTE_ATIVO);
    const r = await handler({
      body: Buffer.from(JSON.stringify({ cpf: '52998224725' })).toString('base64'),
      isBase64Encoded: true,
    });
    expect(r.statusCode).toBe(200);
  });

  it('não vaza detalhe interno quando o banco falha', async () => {
    buscarPorDocumento.mockRejectedValueOnce(new Error('connection refused: 10.0.1.5:5432'));
    const r = await handler({ body: JSON.stringify({ cpf: '529.982.247-25' }) });

    expect(r.statusCode).toBe(500);
    expect(r.body).not.toContain('10.0.1.5');
    expect(r.body).not.toContain('connection refused');
  });
});
