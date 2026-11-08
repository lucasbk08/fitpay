package br.com.fitpay.pagamento;

import java.util.Objects;

/**
 * Resposta comum a todos os meios de pagamento. Cada provedor externo
 * devolve um formato diferente; os ConcreteImplementors traduzem para este.
 *
 * @param status     situação do pagamento
 * @param idExterno  identificador no provedor (txid, NSU ou nosso número); nulo se recusado
 * @param detalhe    instrução ao aluno (código Pix, linha digitável) ou motivo da recusa
 */
public record ResultadoPagamento(StatusPagamento status, String idExterno, String detalhe) {

    public ResultadoPagamento {
        Objects.requireNonNull(status, "status");
    }

    public static ResultadoPagamento aprovado(String idExterno) {
        return new ResultadoPagamento(StatusPagamento.APROVADO, idExterno, "Pagamento aprovado");
    }

    public static ResultadoPagamento pendente(String idExterno, String instrucao) {
        return new ResultadoPagamento(StatusPagamento.PENDENTE, idExterno, instrucao);
    }

    public static ResultadoPagamento recusado(String motivo) {
        return new ResultadoPagamento(StatusPagamento.RECUSADO, null, motivo);
    }
}
