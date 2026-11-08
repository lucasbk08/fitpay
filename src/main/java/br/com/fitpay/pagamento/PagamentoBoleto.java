package br.com.fitpay.pagamento;

import br.com.fitpay.dominio.Transacao;
import br.com.fitpay.pagamento.provedor.BancoClient;
import br.com.fitpay.pagamento.provedor.Boleto;

/**
 * <b>Bridge: ConcreteImplementor.</b> Encapsula o banco emissor de boletos.
 * O boleto fica pendente até a compensação.
 */
public final class PagamentoBoleto implements MeioPagamento {
    private final BancoClient banco;

    public PagamentoBoleto(BancoClient banco) { this.banco = banco; }

    @Override
    public ResultadoPagamento executar(Transacao t) {
        Boleto b = banco.registrar(t.valor(), t.vencimento(), t.pagador());
        return ResultadoPagamento.pendente(b.nossoNumero(), b.linhaDigitavel());
    }

    @Override
    public void estornar(String nossoNumero) { banco.baixar(nossoNumero); }

    @Override
    public boolean aceitaParcelamento() { return false; }
}
