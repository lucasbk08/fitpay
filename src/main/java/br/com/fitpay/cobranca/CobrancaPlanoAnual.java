package br.com.fitpay.cobranca;

import br.com.fitpay.comprovante.Comprovante;
import br.com.fitpay.comprovante.ExtratoParcelamento;
import br.com.fitpay.dominio.Aluno;
import br.com.fitpay.dominio.Plano;
import br.com.fitpay.dominio.Transacao;
import br.com.fitpay.financeiro.LivroCaixa;
import br.com.fitpay.notificacao.Notificador;
import br.com.fitpay.pagamento.MeioPagamento;
import br.com.fitpay.pagamento.ResultadoPagamento;

import java.math.BigDecimal;

/**
 * Plano anual, à vista ou parcelado. Papéis: <b>RefinedAbstraction</b>,
 * <b>ConcreteClass</b> e <b>ConcreteCreator</b>.
 * <p>
 * Regras fictícias: à vista dá 10% de desconto; parcelamento só no cartão.
 * Não sobrescreve {@code vencimento()}: herda o gancho padrão de 3 dias.
 */
public class CobrancaPlanoAnual extends Cobranca {
    private final Plano plano;
    private final int parcelas;

    public CobrancaPlanoAnual(Aluno aluno, MeioPagamento meio, LivroCaixa livro,
                              Notificador notificador, Plano plano, int parcelas) {
        super(aluno, meio, livro, notificador);
        if (parcelas > 1 && !meio.aceitaParcelamento()) {
            throw new IllegalArgumentException("Parcelamento só é aceito no cartão");
        }
        this.plano = plano;
        this.parcelas = parcelas;
    }

    @Override
    protected BigDecimal calcularValor() { return plano.valorAnual(); }

    @Override
    protected BigDecimal aplicarDescontos(BigDecimal valor) {
        return parcelas == 1 ? valor.multiply(new BigDecimal("0.90")) : valor;
    }

    @Override
    protected int parcelas() { return parcelas; }

    @Override
    protected Comprovante criarComprovante(Transacao t, ResultadoPagamento r) {
        return new ExtratoParcelamento(t, r, parcelas);
    }
}
