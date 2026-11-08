package br.com.fitpay.notificacao;

import br.com.fitpay.comprovante.Comprovante;
import br.com.fitpay.dominio.Aluno;

import java.io.PrintStream;

/** Notificador que escreve no console, no lugar de um e-mail ou push. */
public final class NotificadorConsole implements Notificador {

    private final PrintStream saida;

    public NotificadorConsole(PrintStream saida) { this.saida = saida; }

    @Override
    public void enviar(Aluno aluno, Comprovante c) {
        saida.printf("    [notificação para %s] %s · %s · %s%n",
                aluno.nome(), c.numero(), c.status(), c.renderizar());
    }
}
