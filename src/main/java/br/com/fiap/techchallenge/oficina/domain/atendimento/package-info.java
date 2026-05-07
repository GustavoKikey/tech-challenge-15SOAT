/**
 * Bounded Context <b>Atendimento</b> (Customer/Downstream).
 *
 * <p>Agregados: Cliente, Veículo, Serviço, Ordem de Serviço.
 * Consome o BC Estoque (Customer-Supplier) para reservar e baixar peças.
 *
 * <p>Framework-free. Veja {@link br.com.fiap.techchallenge.oficina.domain}.
 */
package br.com.fiap.techchallenge.oficina.domain.atendimento;
