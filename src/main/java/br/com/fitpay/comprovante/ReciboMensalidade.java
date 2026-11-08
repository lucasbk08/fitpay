package br.com.fitpay.comprovante;

import br.com.fitpay.dominio.Transacao;
import br.com.fitpay.pagamento.ResultadoPagamento;
import br.com.fitpay.pagamento.StatusPagamento;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * <b>Factory Method: ConcreteProduct</b>, criado por {@code CobrancaMensalidade}.
 * Traz a competência e o próximo vencimento.
 */
public final class ReciboMensalidade implements Comprovante {
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MM/yyyy");

    private final Transacao transacao;
    private final ResultadoPagamento resultado;
    private final YearMonth competencia;
    private final LocalDate proximoVencimento;

    public ReciboMensalidade(Transacao t, ResultadoPagamento r, YearMonth competencia, LocalDate proximoVencimento) {
        this.transacao = t;
        this.resultado = r;
        this.competencia = competencia;
        this.proximoVencimento = proximoVencimento;
    }

    @Override public String numero() { return transacao.id(); }
    @Override public StatusPagamento status() { return resultado.status(); }

    @Override
    public String renderizar() {
        return "Mensalidade %s de R$ %s; próximo vencimento em %s".formatted(
                competencia.format(MES), transacao.valor(), proximoVencimento.format(DATA));
    }

    public YearMonth competencia() { return competencia; }
    public LocalDate proximoVencimento() { return proximoVencimento; }
}
