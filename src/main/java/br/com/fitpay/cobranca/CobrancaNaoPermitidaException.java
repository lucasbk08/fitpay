package br.com.fitpay.cobranca;

/** Lançada no passo 1 do fluxo quando o aluno não pode ser cobrado. */
public class CobrancaNaoPermitidaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CobrancaNaoPermitidaException(String matricula) {
        super("Matrícula " + matricula + " bloqueada: cobrança não permitida");
    }
}
