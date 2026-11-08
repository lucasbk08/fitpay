package br.com.fitpay.comprovante;

import br.com.fitpay.dominio.Transacao;
import br.com.fitpay.pagamento.ResultadoPagamento;
import br.com.fitpay.pagamento.StatusPagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * <b>Factory Method: ConcreteProduct</b>, criado por {@code CobrancaPlanoAnual}.
 * Traz o número de parcelas e o valor de cada uma.
 */
public final class ExtratoParcelamento implements Comprovante {
    private final Transacao transacao;
    private final ResultadoPagamento resultado;
    private final int parcelas;

    public ExtratoParcelamento(Transacao t, ResultadoPagamento r, int parcelas) {
        this.transacao = t;
        this.resultado = r;
        this.parcelas = parcelas;
    }

    @Override public String numero() { return transacao.id(); }
    @Override public StatusPagamento status() { return resultado.status(); }

    @Override
    public String renderizar() {
        BigDecimal valorParcela = transacao.valor()
                .divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_UP);
        return "Plano anual em %dx de R$ %s".formatted(parcelas, valorParcela);
    }

    public int parcelas() { return parcelas; }
}
