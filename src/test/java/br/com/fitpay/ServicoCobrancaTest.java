package br.com.fitpay;

import br.com.fitpay.Fixtures.NotificadorEmMemoria;
import br.com.fitpay.comprovante.Comprovante;
import br.com.fitpay.comprovante.ReciboAvulso;
import br.com.fitpay.dominio.Aluno;
import br.com.fitpay.dominio.FormaPagamento;
import br.com.fitpay.dominio.ItemAvulso;
import br.com.fitpay.financeiro.LivroCaixa;
import br.com.fitpay.pagamento.MeioPagamento;
import br.com.fitpay.pagamento.PagamentoBoleto;
import br.com.fitpay.pagamento.PagamentoCartao;
import br.com.fitpay.pagamento.PagamentoPix;
import br.com.fitpay.pagamento.StatusPagamento;
import br.com.fitpay.pagamento.provedor.simulado.AdquirenteSimulada;
import br.com.fitpay.pagamento.provedor.simulado.BancoSimulado;
import br.com.fitpay.pagamento.provedor.simulado.PspPixSimulado;
import br.com.fitpay.servico.Agendador;
import br.com.fitpay.servico.ServicoCobranca;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("ServicoCobranca e Agendador")
class ServicoCobrancaTest {

    private LivroCaixa livro;
    private ServicoCobranca servico;

    @BeforeEach
    void preparar() {
        livro = new LivroCaixa();
        Map<FormaPagamento, MeioPagamento> meios = Map.of(
                FormaPagamento.PIX, new PagamentoPix(new PspPixSimulado()),
                FormaPagamento.CARTAO, new PagamentoCartao(new AdquirenteSimulada()),
                FormaPagamento.BOLETO, new PagamentoBoleto(new BancoSimulado()));
        servico = new ServicoCobranca(meios, livro, new NotificadorEmMemoria());
    }

    @Test
    @DisplayName("Mensalidade usa o meio preferido do aluno")
    void usaMeioPreferido() {
        Comprovante ana = servico.cobrarMensalidade(Fixtures.ana(), YearMonth.of(2026, 11));      // cartão
        Comprovante bruno = servico.cobrarMensalidade(Fixtures.bruno(), YearMonth.of(2026, 11));  // Pix
        assertEquals(StatusPagamento.APROVADO, ana.status());
        assertEquals(StatusPagamento.PENDENTE, bruno.status());
    }

    @Test
    @DisplayName("Recepcionista cobra avulso no meio escolhido no balcão")
    void avulsoNoBalcao() {
        Comprovante c = servico.cobrarAvulso(Fixtures.ana(), ItemAvulso.DIARIA, FormaPagamento.BOLETO);
        assertInstanceOf(ReciboAvulso.class, c);
        assertEquals(StatusPagamento.PENDENTE, c.status());
    }

    @Test
    @DisplayName("Meio não configurado gera erro claro")
    void meioNaoConfigurado() {
        ServicoCobranca soPix = new ServicoCobranca(
                Map.of(FormaPagamento.PIX, new PagamentoPix(new PspPixSimulado())), livro, new NotificadorEmMemoria());
        assertThrows(IllegalStateException.class,
                () -> soPix.cobrarAvulso(Fixtures.ana(), ItemAvulso.DIARIA, FormaPagamento.CARTAO));
    }

    @Test
    @DisplayName("Agendador cobra só quem vence no dia e pula alunos bloqueados")
    void agendador() {
        Aluno ana = Fixtures.ana();                 // vence dia 10
        Aluno bruno = Fixtures.bruno();             // vence dia 5
        Aluno carla = Fixtures.carlaBloqueada();    // vence dia 10, bloqueada
        Agendador agendador = new Agendador(servico, List.of(ana, bruno, carla));

        Agendador.Execucao rodada = agendador.executar(LocalDate.of(2026, 11, 10));

        assertEquals(1, rodada.comprovantes().size());
        assertEquals(1, rodada.ignorados().size());
        assertEquals(1, livro.lancamentos().size());
        assertEquals(ana, livro.lancamentos().get(0).transacao().aluno());
    }
}
