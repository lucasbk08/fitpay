package br.com.fitpay.cobranca;

import br.com.fitpay.comprovante.Comprovante;
import br.com.fitpay.comprovante.ReciboAvulso;
import br.com.fitpay.dominio.Aluno;
import br.com.fitpay.dominio.ItemAvulso;
import br.com.fitpay.dominio.Transacao;
import br.com.fitpay.financeiro.LivroCaixa;
import br.com.fitpay.notificacao.Notificador;
import br.com.fitpay.pagamento.MeioPagamento;
import br.com.fitpay.pagamento.ResultadoPagamento;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Serviço avulso lançado no balcão (diária, personal, matrícula).
 * Papéis: <b>RefinedAbstraction</b>, <b>ConcreteClass</b> e <b>ConcreteCreator</b>.
 * <p>
 * Não sobrescreve {@code aplicarDescontos(...)}: herda o gancho padrão, sem desconto.
 */
public class CobrancaAvulsa extends Cobranca {
    private final ItemAvulso item;   // DIARIA, PERSONAL, TAXA_MATRICULA

    public CobrancaAvulsa(Aluno aluno, MeioPagamento meio, LivroCaixa livro,
                          Notificador notificador, ItemAvulso item) {
        super(aluno, meio, livro, notificador);
        this.item = item;
    }

    @Override
    protected BigDecimal calcularValor() { return item.preco(); }

    @Override
    protected LocalDate vencimento() { return LocalDate.now(); }

    @Override
    protected Comprovante criarComprovante(Transacao t, ResultadoPagamento r) {
        return new ReciboAvulso(t, r, item);
    }
}
