package br.com.fitpay.cobranca;

import br.com.fitpay.comprovante.Comprovante;
import br.com.fitpay.comprovante.ReciboMensalidade;
import br.com.fitpay.dominio.Aluno;
import br.com.fitpay.dominio.Plano;
import br.com.fitpay.dominio.Transacao;
import br.com.fitpay.financeiro.LivroCaixa;
import br.com.fitpay.notificacao.Notificador;
import br.com.fitpay.pagamento.MeioPagamento;
import br.com.fitpay.pagamento.ResultadoPagamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Mensalidade recorrente. Papéis: <b>RefinedAbstraction</b> (Bridge),
 * <b>ConcreteClass</b> (Template Method) e <b>ConcreteCreator</b> (Factory Method).
 * <p>
 * Regra fictícia: convênio empresarial dá 15% de desconto.
 */
public class CobrancaMensalidade extends Cobranca {
    private final Plano plano;
    private final YearMonth competencia;

    public CobrancaMensalidade(Aluno aluno, MeioPagamento meio, LivroCaixa livro,
                               Notificador notificador, Plano plano, YearMonth competencia) {
        super(aluno, meio, livro, notificador);
        this.plano = plano;
        this.competencia = competencia;
    }

    @Override
    protected BigDecimal calcularValor() { return plano.valorMensal(); }

    @Override
    protected BigDecimal aplicarDescontos(BigDecimal valor) {
        return aluno.temConvenioEmpresa() ? valor.multiply(new BigDecimal("0.85")) : valor;
    }

    @Override
    protected LocalDate vencimento() { return competencia.atDay(aluno.diaVencimento()); }

    @Override
    protected Comprovante criarComprovante(Transacao t, ResultadoPagamento r) {
        return new ReciboMensalidade(t, r, competencia, vencimento().plusMonths(1));
    }
}
