import { describe, it, expect } from 'vitest';
import { cpfValido, normalizar } from '../src/cpf.js';

describe('validação de CPF', () => {
  it('aceita CPF válido com e sem máscara', () => {
    expect(cpfValido('529.982.247-25')).toBe(true);
    expect(cpfValido('52998224725')).toBe(true);
    expect(cpfValido('111.444.777-35')).toBe(true);
  });

  it('rejeita dígito verificador errado', () => {
    expect(cpfValido('529.982.247-26')).toBe(false);
    expect(cpfValido('11144477734')).toBe(false);
  });

  it('rejeita sequências de dígitos iguais', () => {
    // Passam no cálculo de módulo 11, mas não são CPFs reais.
    expect(cpfValido('111.111.111-11')).toBe(false);
    expect(cpfValido('00000000000')).toBe(false);
  });

  it('rejeita tamanho errado, letras e vazios', () => {
    expect(cpfValido('5299822472')).toBe(false);
    expect(cpfValido('529982247250')).toBe(false);
    expect(cpfValido('5299822472a')).toBe(false);
    expect(cpfValido('')).toBe(false);
    expect(cpfValido(null)).toBe(false);
    expect(cpfValido(undefined)).toBe(false);
  });

  it('normaliza a mesma pontuação que o domínio Java aceita', () => {
    expect(normalizar('529.982.247-25')).toBe('52998224725');
    expect(normalizar(' 529 982 247 25 ')).toBe('52998224725');
  });

  it('concorda com o cálculo do domínio Java em CPFs gerados', () => {
    // Mesma faixa e mesmo algoritmo do TokenDeTeste.cpfValido() do lado Java.
    for (let i = 1; i <= 50; i++) {
      const base = String(900_000_000 + i);
      expect(cpfValido(base + dvJava(base))).toBe(true);
    }
  });
});

/** Réplica de Documento.calcularDv da aplicação, para provar a equivalência. */
function dvJava(base9) {
  const dv = (digitos, tamanho, pesoInicial) => {
    let soma = 0;
    for (let i = 0; i < tamanho; i++) {
      soma += Number(digitos[i]) * (pesoInicial - i);
    }
    const resto = soma % 11;
    return resto < 2 ? 0 : 11 - resto;
  };
  const d1 = dv(base9, 9, 10);
  const d2 = dv(base9 + d1, 10, 11);
  return `${d1}${d2}`;
}
