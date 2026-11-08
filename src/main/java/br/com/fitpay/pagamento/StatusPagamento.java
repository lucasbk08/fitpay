package br.com.fitpay.pagamento;

/**
 * Situação de uma cobrança depois de passar pelo meio de pagamento.
 * O cartão responde na hora (APROVADO ou RECUSADO); Pix e boleto ficam
 * PENDENTE até o provedor confirmar.
 */
public enum StatusPagamento {
    APROVADO,
    PENDENTE,
    RECUSADO
}
