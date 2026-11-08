package br.com.fitpay.dominio;

/**
 * Forma de pagamento que o aluno escolhe. O {@code ServicoCobranca} usa este valor
 * para encontrar o {@code MeioPagamento} (o Implementor do Bridge) correspondente.
 */
public enum FormaPagamento {
    PIX,
    CARTAO,
    BOLETO
}
