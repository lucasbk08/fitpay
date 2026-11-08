package br.com.fitpay.financeiro;

import br.com.fitpay.dominio.Transacao;
import br.com.fitpay.pagamento.ResultadoPagamento;

/** Uma linha do livro-caixa. */
public record Lancamento(Transacao transacao, ResultadoPagamento resultado) { }
