package br.com.fitpay.comprovante;

import br.com.fitpay.pagamento.StatusPagamento;

/**
 * <b>Factory Method: Product.</b>
 * <p>
 * Interface que o fluxo de cobrança e o notificador usam. Cada tipo de
 * cobrança cria o seu ConcreteProduct em {@code criarComprovante(...)}.
 */
public interface Comprovante {
    String numero();
    StatusPagamento status();      // APROVADO, PENDENTE ou RECUSADO
    String renderizar();           // texto enviado ao aluno
}
