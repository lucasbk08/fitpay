package br.com.fitpay.dominio;

import java.math.BigDecimal;

/**
 * Plano contratado pelo aluno. Valores fictícios, usados só nos exemplos.
 */
public record Plano(String nome, BigDecimal valorMensal, BigDecimal valorAnual) {

    public static final Plano BLACK = new Plano("Black", new BigDecimal("149.90"), new BigDecimal("1499.00"));
    public static final Plano BASICO = new Plano("Básico", new BigDecimal("99.90"), new BigDecimal("999.00"));
}
