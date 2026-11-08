package br.com.fitpay.financeiro;

import br.com.fitpay.dominio.Transacao;
import br.com.fitpay.pagamento.ResultadoPagamento;
import br.com.fitpay.pagamento.StatusPagamento;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Livro-caixa em memória. O passo 6 do Template Method registra aqui toda
 * cobrança que passou da validação, qualquer que seja o tipo ou o meio.
 * <p>
 * É recebido pelo construtor das cobranças (injeção de dependência), e não
 * um Singleton: assim os testes usam um livro novo e vazio a cada caso.
 */
public class LivroCaixa {

    private final List<Lancamento> lancamentos = new ArrayList<>();

    public void registrar(Transacao t, ResultadoPagamento r) {
        lancamentos.add(new Lancamento(t, r));
    }

    public List<Lancamento> lancamentos() {
        return Collections.unmodifiableList(lancamentos);
    }

    public BigDecimal totalCom(StatusPagamento status) {
        return lancamentos.stream()
                .filter(l -> l.resultado().status() == status)
                .map(l -> l.transacao().valor())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
