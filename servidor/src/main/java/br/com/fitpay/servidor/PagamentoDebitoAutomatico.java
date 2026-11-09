package br.com.fitpay.servidor;

import br.com.fitpay.dominio.Transacao;
import br.com.fitpay.pagamento.MeioPagamento;
import br.com.fitpay.pagamento.ResultadoPagamento;

import java.time.format.DateTimeFormatter;

/**
 * <b>Bridge: um ConcreteImplementor novo.</b>
 * <p>
 * Débito automático, ligado em Ajustes no app. Mostra a extensão prometida pelo
 * Bridge: um meio de pagamento novo é uma classe a mais, e nenhuma classe de
 * cobrança do núcleo ({@code src/}) precisou mudar.
 */
public final class PagamentoDebitoAutomatico implements MeioPagamento {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public ResultadoPagamento executar(Transacao t) {
        return ResultadoPagamento.pendente("DA" + t.id().replaceAll("\\D", ""),
                "Débito agendado para " + t.vencimento().format(DATA));
    }

    @Override
    public void estornar(String idExterno) { }

    @Override
    public boolean aceitaParcelamento() { return false; }
}
