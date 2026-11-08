package br.com.fitpay.pagamento;

import br.com.fitpay.dominio.Transacao;

/**
 * <b>Bridge: Implementor.</b>
 * <p>
 * Operações primitivas que toda cobrança precisa de um meio de pagamento.
 * A {@code Cobranca} (Abstraction) guarda uma referência para esta interface
 * e delega a ela o pagamento; nunca conhece a classe concreta.
 */
public interface MeioPagamento {

    ResultadoPagamento executar(Transacao transacao);

    void estornar(String idExterno);

    boolean aceitaParcelamento();
}
