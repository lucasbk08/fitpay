package br.com.fitpay.pagamento.provedor;

/** Boleto registrado no banco. */
public record Boleto(String nossoNumero, String linhaDigitavel) { }
