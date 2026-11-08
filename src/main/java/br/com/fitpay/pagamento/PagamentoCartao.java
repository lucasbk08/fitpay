package br.com.fitpay.pagamento;

import br.com.fitpay.dominio.Transacao;
import br.com.fitpay.pagamento.provedor.AdquirenteClient;
import br.com.fitpay.pagamento.provedor.RespostaAdquirente;

/**
 * <b>Bridge: ConcreteImplementor.</b> Encapsula a adquirente de cartão.
 * O cartão responde na hora e é o único meio que aceita parcelamento.
 */
public final class PagamentoCartao implements MeioPagamento {
    private final AdquirenteClient adquirente;

    public PagamentoCartao(AdquirenteClient adquirente) { this.adquirente = adquirente; }

    @Override
    public ResultadoPagamento executar(Transacao t) {
        RespostaAdquirente r = adquirente.capturar(t.tokenCartao(), t.valor(), t.parcelas());
        return r.aprovada()
                ? ResultadoPagamento.aprovado(r.nsu())
                : ResultadoPagamento.recusado(r.motivo());
    }

    @Override
    public void estornar(String nsu) { adquirente.cancelar(nsu); }

    @Override
    public boolean aceitaParcelamento() { return true; }
}
