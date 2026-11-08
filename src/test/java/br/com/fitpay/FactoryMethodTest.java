package br.com.fitpay;

import br.com.fitpay.Fixtures.MeioFalso;
import br.com.fitpay.Fixtures.NotificadorEmMemoria;
import br.com.fitpay.cobranca.CobrancaAvulsa;
import br.com.fitpay.cobranca.CobrancaMensalidade;
import br.com.fitpay.cobranca.CobrancaPlanoAnual;
import br.com.fitpay.comprovante.Comprovante;
import br.com.fitpay.comprovante.ExtratoParcelamento;
import br.com.fitpay.comprovante.ReciboAvulso;
import br.com.fitpay.comprovante.ReciboMensalidade;
import br.com.fitpay.dominio.ItemAvulso;
import br.com.fitpay.dominio.Plano;
import br.com.fitpay.financeiro.LivroCaixa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@DisplayName("Factory Method: cada cobrança cria o seu comprovante")
class FactoryMethodTest {

    private static final YearMonth NOV = YearMonth.of(2026, 11);
    private LivroCaixa livro;
    private NotificadorEmMemoria notificador;

    @BeforeEach
    void preparar() {
        livro = new LivroCaixa();
        notificador = new NotificadorEmMemoria();
    }

    @Test
    @DisplayName("Mensalidade cria ReciboMensalidade com competência e próximo vencimento")
    void mensalidade() {
        Comprovante c = new CobrancaMensalidade(Fixtures.ana(), MeioFalso.aprovando(), livro, notificador,
                Plano.BLACK, NOV).processar();
        ReciboMensalidade recibo = assertInstanceOf(ReciboMensalidade.class, c);
        assertEquals(NOV, recibo.competencia());
        assertEquals(LocalDate.of(2026, 12, 10), recibo.proximoVencimento());
    }

    @Test
    @DisplayName("Avulsa cria ReciboAvulso com o item")
    void avulsa() {
        Comprovante c = new CobrancaAvulsa(Fixtures.bruno(), MeioFalso.aprovando(), livro, notificador,
                ItemAvulso.TAXA_MATRICULA).processar();
        ReciboAvulso recibo = assertInstanceOf(ReciboAvulso.class, c);
        assertEquals(ItemAvulso.TAXA_MATRICULA, recibo.item());
        assertEquals("Taxa de matrícula de R$ 60.00", recibo.renderizar());
    }

    @Test
    @DisplayName("Plano anual cria ExtratoParcelamento com o valor de cada parcela")
    void planoAnual() {
        Comprovante c = new CobrancaPlanoAnual(Fixtures.ana(), MeioFalso.aprovando(), livro, notificador,
                Plano.BLACK, 12).processar();
        ExtratoParcelamento extrato = assertInstanceOf(ExtratoParcelamento.class, c);
        assertEquals(12, extrato.parcelas());
        assertEquals("Plano anual em 12x de R$ 124.92", extrato.renderizar());   // 1.499,00 / 12
    }

    @Test
    @DisplayName("O comprovante enviado é o mesmo produto criado pela subclasse")
    void notificadorRecebeOProduto() {
        Comprovante c = new CobrancaAvulsa(Fixtures.bruno(), MeioFalso.aprovando(), livro, notificador,
                ItemAvulso.DIARIA).processar();
        assertEquals(1, notificador.enviados.size());
        assertEquals(c.numero(), notificador.enviados.get(0).numero());
        assertEquals(livro.lancamentos().get(0).transacao().id(), c.numero());
    }
}
