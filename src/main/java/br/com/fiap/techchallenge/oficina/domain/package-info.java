/**
 * Camada de domínio (DDD).
 *
 * <p><b>Regra de ouro:</b> nenhum tipo neste pacote (ou subpacotes)
 * pode importar Quarkus, Jakarta EE/CDI, JPA/Hibernate, Jackson,
 * Panache ou qualquer outro framework. Apenas Java SE puro e
 * dependências de outros sub-pacotes de {@code domain}.
 *
 * <p>Frameworks pertencem às camadas {@code interfaces},
 * {@code application} (parcial) e {@code infrastructure}.
 */
package br.com.fiap.techchallenge.oficina.domain;
