package br.com.fitpay.pagamento.provedor;

import java.math.BigDecimal;

/**
 * API da adquirente de cartão de crédito.
 * Em produção seria um cliente HTTP; aqui há uma versão simulada.
 */
public interface AdquirenteClient {

    RespostaAdquirente capturar(String tokenCartao, BigDecimal valor, int parcelas);

    void cancelar(String nsu);
}
