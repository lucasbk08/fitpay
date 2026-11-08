package br.com.fitpay.pagamento.provedor;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * API do PSP (provedor de serviços de pagamento) do Pix.
 * Em produção seria um cliente HTTP; aqui há uma versão simulada.
 */
public interface PspPixClient {

    QrCodePix gerarCobrancaImediata(BigDecimal valor, String referencia, Duration expiracao);

    void devolver(String txid);
}
