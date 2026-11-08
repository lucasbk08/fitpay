package br.com.fitpay.cobranca;

import br.com.fitpay.comprovante.Comprovante;
import br.com.fitpay.dominio.Aluno;
import br.com.fitpay.dominio.Transacao;
import br.com.fitpay.financeiro.LivroCaixa;
import br.com.fitpay.notificacao.Notificador;
import br.com.fitpay.pagamento.MeioPagamento;
import br.com.fitpay.pagamento.ResultadoPagamento;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Classe central do FitPay. Acumula três papéis:
 * <ul>
 *   <li><b>Bridge: Abstraction</b>. Guarda a referência {@link #meio} (a ponte)
 *       e delega o pagamento a ela.</li>
 *   <li><b>Template Method: AbstractClass</b>. {@link #processar()} é o
 *       método-template, {@code final}, com os 8 passos em ordem fixa.</li>
 *   <li><b>Factory Method: Creator</b>. Declara {@link #criarComprovante}
 *       e o usa no passo 7, sem saber qual comprovante concreto será criado.</li>
 * </ul>
 */
public abstract class Cobranca {
    protected final Aluno aluno;
    protected final MeioPagamento meio;   // a ponte para o Implementor
    private final LivroCaixa livroCaixa;
    private final Notificador notificador;

    protected Cobranca(Aluno aluno, MeioPagamento meio,
                       LivroCaixa livroCaixa, Notificador notificador) {
        this.aluno = aluno;
        this.meio = meio;
        this.livroCaixa = livroCaixa;
        this.notificador = notificador;
    }

    // Template Method: o esqueleto do algoritmo
    public final Comprovante processar() {
        validarAluno();                                          // 1. fixo
        BigDecimal valor = aplicarDescontos(calcularValor());    // 2 e 3. variam
        Transacao t = Transacao.nova(aluno, valor, parcelas(), vencimento()); // 4
        ResultadoPagamento r = meio.executar(t);                 // 5. Bridge
        livroCaixa.registrar(t, r);                              // 6. fixo
        Comprovante c = criarComprovante(t, r);                  // 7. Factory Method
        if (deveNotificar(r)) {                                  // 8. gancho
            notificador.enviar(aluno, c);
        }
        return c;
    }

    private void validarAluno() {
        if (aluno.estaBloqueado()) {
            throw new CobrancaNaoPermitidaException(aluno.matricula());
        }
    }

    // Operações primitivas: obrigatórias
    protected abstract BigDecimal calcularValor();
    protected abstract Comprovante criarComprovante(Transacao t, ResultadoPagamento r);

    // Ganchos: comportamento padrão, sobrescrita opcional
    protected BigDecimal aplicarDescontos(BigDecimal valor) { return valor; }
    protected int parcelas() { return 1; }
    protected LocalDate vencimento() { return LocalDate.now().plusDays(3); }
    protected boolean deveNotificar(ResultadoPagamento r) { return true; }

    public Aluno aluno() { return aluno; }
    public MeioPagamento meio() { return meio; }
}
