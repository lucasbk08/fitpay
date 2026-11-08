package br.com.fitpay.comprovante;

import br.com.fitpay.dominio.ItemAvulso;
import br.com.fitpay.dominio.Transacao;
import br.com.fitpay.pagamento.ResultadoPagamento;
import br.com.fitpay.pagamento.StatusPagamento;

/**
 * <b>Factory Method: ConcreteProduct</b>, criado por {@code CobrancaAvulsa}.
 * Traz o item cobrado no balcão.
 */
public final class ReciboAvulso implements Comprovante {
    private final Transacao transacao;
    private final ResultadoPagamento resultado;
    private final ItemAvulso item;

    public ReciboAvulso(Transacao t, ResultadoPagamento r, ItemAvulso item) {
        this.transacao = t;
        this.resultado = r;
        this.item = item;
    }

    @Override public String numero() { return transacao.id(); }
    @Override public StatusPagamento status() { return resultado.status(); }

    @Override
    public String renderizar() {
        return "%s de R$ %s".formatted(item.descricao(), transacao.valor());
    }

    public ItemAvulso item() { return item; }
}
