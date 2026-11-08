package br.com.fitpay.dominio;

import java.math.BigDecimal;

/**
 * Serviços cobrados no balcão, fora da mensalidade. Preços fictícios.
 */
public enum ItemAvulso {
    DIARIA("Diária", new BigDecimal("35.00")),
    PERSONAL("Aula com personal", new BigDecimal("90.00")),
    TAXA_MATRICULA("Taxa de matrícula", new BigDecimal("60.00"));

    private final String descricao;
    private final BigDecimal preco;

    ItemAvulso(String descricao, BigDecimal preco) {
        this.descricao = descricao;
        this.preco = preco;
    }

    public String descricao() { return descricao; }

    public BigDecimal preco() { return preco; }
}
