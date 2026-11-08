package br.com.fitpay.servico;

import br.com.fitpay.cobranca.Cobranca;
import br.com.fitpay.cobranca.CobrancaAvulsa;
import br.com.fitpay.cobranca.CobrancaMensalidade;
import br.com.fitpay.cobranca.CobrancaPlanoAnual;
import br.com.fitpay.comprovante.Comprovante;
import br.com.fitpay.dominio.Aluno;
import br.com.fitpay.dominio.FormaPagamento;
import br.com.fitpay.dominio.ItemAvulso;
import br.com.fitpay.financeiro.LivroCaixa;
import br.com.fitpay.notificacao.Notificador;
import br.com.fitpay.pagamento.MeioPagamento;

import java.time.YearMonth;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Cliente dos padrões. Combina qualquer tipo de cobrança com qualquer meio de
 * pagamento em tempo de execução (Bridge) e chama {@code processar()} (Template Method).
 * <p>
 * Todas as dependências chegam pelo construtor. Nenhuma classe do FitPay é Singleton.
 */
public class ServicoCobranca {

    private final Map<FormaPagamento, MeioPagamento> meios;
    private final LivroCaixa livroCaixa;
    private final Notificador notificador;

    public ServicoCobranca(Map<FormaPagamento, MeioPagamento> meios,
                           LivroCaixa livroCaixa, Notificador notificador) {
        this.meios = new EnumMap<>(meios);
        this.livroCaixa = Objects.requireNonNull(livroCaixa);
        this.notificador = Objects.requireNonNull(notificador);
    }

    /** Usado pelo agendador: cobra no meio preferido do aluno (trecho do relatório, seção 2.4). */
    public Comprovante cobrarMensalidade(Aluno aluno, YearMonth competencia) {
        MeioPagamento meio = meios.get(aluno.formaPreferida());   // Pix, cartão ou boleto
        Cobranca cobranca = new CobrancaMensalidade(aluno, meio, livroCaixa, notificador,
                aluno.plano(), competencia);
        Comprovante comprovante = cobranca.processar();
        return comprovante;
    }

    /** Mesma mensalidade em outro meio, por exemplo depois de um cartão recusado. */
    public Comprovante cobrarMensalidade(Aluno aluno, YearMonth competencia, FormaPagamento forma) {
        MeioPagamento meio = meio(forma);   // Pix, cartão ou boleto
        Cobranca cobranca = new CobrancaMensalidade(aluno, meio, livroCaixa, notificador,
                aluno.plano(), competencia);
        return cobranca.processar();
    }

    /** Usado pela recepcionista no balcão. */
    public Comprovante cobrarAvulso(Aluno aluno, ItemAvulso item, FormaPagamento forma) {
        Cobranca cobranca = new CobrancaAvulsa(aluno, meio(forma), livroCaixa, notificador, item);
        return cobranca.processar();
    }

    public Comprovante cobrarPlanoAnual(Aluno aluno, FormaPagamento forma, int parcelas) {
        Cobranca cobranca = new CobrancaPlanoAnual(aluno, meio(forma), livroCaixa, notificador,
                aluno.plano(), parcelas);
        return cobranca.processar();
    }

    private MeioPagamento meio(FormaPagamento forma) {
        MeioPagamento meio = meios.get(forma);
        if (meio == null) {
            throw new IllegalStateException("Nenhum meio de pagamento configurado para " + forma);
        }
        return meio;
    }
}
