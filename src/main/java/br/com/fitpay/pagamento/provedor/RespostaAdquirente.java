package br.com.fitpay.pagamento.provedor;

/** Resposta da adquirente a uma captura. */
public record RespostaAdquirente(boolean aprovada, String nsu, String motivo) {

    public static RespostaAdquirente aprovada(String nsu) {
        return new RespostaAdquirente(true, nsu, null);
    }

    public static RespostaAdquirente negada(String motivo) {
        return new RespostaAdquirente(false, null, motivo);
    }
}
