package br.com.fitpay.notificacao;

import br.com.fitpay.comprovante.Comprovante;
import br.com.fitpay.dominio.Aluno;

/**
 * Envia o comprovante ao aluno. Trabalha só com a interface {@link Comprovante},
 * sem saber qual comprovante concreto a cobrança criou.
 */
public interface Notificador {

    void enviar(Aluno aluno, Comprovante comprovante);
}
