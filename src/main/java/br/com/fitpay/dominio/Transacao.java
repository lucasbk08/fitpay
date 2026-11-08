package br.com.fitpay.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Dados de uma cobrança prontos para o meio de pagamento: valor final,
 * parcelas e vencimento. É montada no passo 4 do Template Method.
 */
public record Transacao(String id, Aluno aluno, BigDecimal valor, int parcelas, LocalDate vencimento) {

    private static final AtomicLong SEQUENCIA = new AtomicLong(1040);

    public Transacao {
        if (valor.signum() <= 0) throw new IllegalArgumentException("Valor deve ser positivo");
        if (parcelas < 1 || parcelas > 12) throw new IllegalArgumentException("Parcelas devem estar entre 1 e 12");
    }

    public static Transacao nova(Aluno aluno, BigDecimal valor, int parcelas, LocalDate vencimento) {
        String id = "TX-" + SEQUENCIA.incrementAndGet();
        return new Transacao(id, aluno, valor.setScale(2, RoundingMode.HALF_UP), parcelas, vencimento);
    }

    public String tokenCartao() { return aluno.tokenCartao(); }

    public String pagador() { return aluno.nome(); }
}
