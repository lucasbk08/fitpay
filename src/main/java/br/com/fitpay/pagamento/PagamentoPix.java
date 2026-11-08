package br.com.fitpay.pagamento;

import br.com.fitpay.dominio.Transacao;
import br.com.fitpay.pagamento.provedor.PspPixClient;
import br.com.fitpay.pagamento.provedor.QrCodePix;

import java.time.Duration;

/**
 * <b>Bridge: ConcreteImplementor.</b> Encapsula o PSP do Pix.
 * O Pix fica pendente até o aluno pagar o QR Code.
 */
public final class PagamentoPix implements MeioPagamento {
    private final PspPixClient psp;

    public PagamentoPix(PspPixClient psp) { this.psp = psp; }

    @Override
    public ResultadoPagamento executar(Transacao t) {
        QrCodePix qr = psp.gerarCobrancaImediata(t.valor(), t.id(), Duration.ofMinutes(30));
        return ResultadoPagamento.pendente(qr.txid(), qr.copiaECola());
    }

    @Override
    public void estornar(String txid) { psp.devolver(txid); }

    @Override
    public boolean aceitaParcelamento() { return false; }
}
