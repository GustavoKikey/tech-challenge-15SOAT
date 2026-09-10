/**
 * Validação de CPF.
 *
 * Espelha `shared.entities.Documento` da aplicação Java: mesma normalização e
 * mesmo cálculo de dígitos verificadores. Se as duas divergirem, um CPF aceito
 * aqui pode não existir lá (ou o contrário), e o cliente recebe um token para um
 * documento que a aplicação recusa.
 */

/** Remove pontuação aceita pelo domínio: ponto, hífen, barra e espaço. */
export function normalizar(entrada) {
  if (entrada === null || entrada === undefined) return '';
  return String(entrada).replace(/[.\-/\s]/g, '');
}

/**
 * @param {string} entrada CPF com ou sem máscara
 * @returns {boolean} true se for um CPF de 11 dígitos com DVs corretos
 */
export function cpfValido(entrada) {
  const d = normalizar(entrada);

  if (d.length !== 11) return false;
  if (!/^\d{11}$/.test(d)) return false;
  // Sequências como 111.111.111-11 passam no cálculo de DV mas não são CPFs
  // reais — o domínio Java também as rejeita explicitamente.
  if (/^(\d)\1{10}$/.test(d)) return false;

  return (
    digitoVerificador(d, 9, 10) === Number(d[9]) &&
    digitoVerificador(d, 10, 11) === Number(d[10])
  );
}

/**
 * Dígito verificador pelo módulo 11.
 *
 * @param {string} digitos  CPF normalizado
 * @param {number} tamanho  quantos dígitos entram na soma
 * @param {number} pesoInicial peso do primeiro dígito (decresce a cada posição)
 */
function digitoVerificador(digitos, tamanho, pesoInicial) {
  let soma = 0;
  for (let i = 0; i < tamanho; i++) {
    soma += Number(digitos[i]) * (pesoInicial - i);
  }
  const resto = soma % 11;
  return resto < 2 ? 0 : 11 - resto;
}
