package br.com.fitpay.pagamento.provedor;

/** Resposta do PSP ao gerar uma cobrança Pix. */
public record QrCodePix(String txid, String copiaECola) { }
