package br.com.fitpay.pagamento.provedor.simulado;

import br.com.fitpay.pagamento.provedor.PspPixClient;
import br.com.fitpay.pagamento.provedor.QrCodePix;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PSP de Pix simulado: gera um txid e um código copia-e-cola fictícios.
 * Nenhum dinheiro é movimentado.
 */
public final class PspPixSimulado implements PspPixClient {

    private final List<String> devolvidos = new ArrayList<>();

    @Override
    public QrCodePix gerarCobrancaImediata(BigDecimal valor, String referencia, Duration expiracao) {
        String txid = "fitpay" + referencia.replaceAll("[^0-9]", "") + "pix";
        String copiaECola = "00020126580014br.gov.bcb.pix0136" + txid
                + "520400005303986540" + valor.toPlainString().length() + valor.toPlainString()
                + "5802BR5906FITPAY6009SAO PAULO";
        return new QrCodePix(txid, copiaECola);
    }

    @Override
    public void devolver(String txid) { devolvidos.add(txid); }

    public List<String> devolvidos() { return Collections.unmodifiableList(devolvidos); }
}
