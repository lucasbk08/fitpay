package br.com.fitpay;

import br.com.fitpay.Fixtures.MeioFalso;
import br.com.fitpay.Fixtures.NotificadorEmMemoria;
import br.com.fitpay.cobranca.Cobranca;
import br.com.fitpay.cobranca.CobrancaMensalidade;
import br.com.fitpay.cobranca.CobrancaPlanoAnual;
import br.com.fitpay.comprovante.Comprovante;
import br.com.fitpay.comprovante.ReciboMensalidade;
import br.com.fitpay.dominio.Aluno;
import br.com.fitpay.dominio.Plano;
import br.com.fitpay.financeiro.LivroCaixa;
import br.com.fitpay.pagamento.MeioPagamento;
import br.com.fitpay.pagamento.PagamentoBoleto;
import br.com.fitpay.pagamento.PagamentoCartao;
import br.com.fitpay.pagamento.PagamentoPix;
import br.com.fitpay.pagamento.StatusPagamento;
import br.com.fitpay.pagamento.provedor.simulado.AdquirenteSimulada;
import br.com.fitpay.pagamento.provedor.simulado.BancoSimulado;
import br.com.fitpay.pagamento.provedor.simulado.PspPixSimulado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.YearMonth;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Bridge: tipo de cobrança × meio de pagamento")
class BridgeTest {

    private static final YearMonth NOV = YearMonth.of(2026, 11);
    private LivroCaixa livro;
    private NotificadorEmMemoria notificador;
    private final Aluno ana = Fixtures.ana();

    @BeforeEach
    void preparar() {
        livro = new LivroCaixa();
        notificador = new NotificadorEmMemoria();
    }

    private Comprovante mensalidadeCom(MeioPagamento meio) {
        return new CobrancaMensalidade(ana, meio, livro, notificador, Plano.BLACK, NOV).processar();
    }

    @Test
    @DisplayName("A mesma cobrança funciona com qualquer um dos três meios")
    void mesmaCobrancaComQualquerMeio() {
        Comprovante pix = mensalidadeCom(new PagamentoPix(new PspPixSimulado()));
        Comprovante cartao = mensalidadeCom(new PagamentoCartao(new AdquirenteSimulada()));
        Comprovante boleto = mensalidadeCom(new PagamentoBoleto(new BancoSimulado()));

        assertEquals(StatusPagamento.PENDENTE, pix.status());
        assertEquals(StatusPagamento.APROVADO, cartao.status());
        assertEquals(StatusPagamento.PENDENTE, boleto.status());
        // trocar o meio não muda o tipo de cobrança nem o comprovante
        assertInstanceOf(ReciboMensalidade.class, pix);
        assertInstanceOf(ReciboMensalidade.class, cartao);
        assertInstanceOf(ReciboMensalidade.class, boleto);
        assertEquals(3, livro.lancamentos().size());
    }

    @Test
    @DisplayName("A cobrança delega o pagamento ao meio recebido (a ponte)")
    void delegaAoImplementor() {
        MeioFalso meio = MeioFalso.aprovando();
        Cobranca cobranca = new CobrancaMensalidade(ana, meio, livro, notificador, Plano.BLACK, NOV);

        cobranca.processar();

        assertSame(meio, cobranca.meio());
        assertEquals(1, meio.executadas.size());
        assertEquals(ana, meio.executadas.get(0).aluno());
    }

    @Test
    @DisplayName("Cobranca depende só da interface MeioPagamento")
    void abstractionConheceSoAInterface() {
        boolean conheceClasseConcreta = Arrays.stream(Cobranca.class.getDeclaredFields())
                .map(Field::getType)
                .anyMatch(tipo -> tipo == PagamentoPix.class || tipo == PagamentoCartao.class
                        || tipo == PagamentoBoleto.class);
        assertTrue(!conheceClasseConcreta, "Cobranca não deve ter campo de meio concreto");
    }

    @Test
    @DisplayName("Plano anual parcelado é recusado em meio que não parcela")
    void parcelamentoSoNoCartao() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new CobrancaPlanoAnual(ana, MeioFalso.semParcelamento(), livro, notificador, Plano.BLACK, 12));
        assertEquals("Parcelamento só é aceito no cartão", erro.getMessage());
    }

    @Test
    @DisplayName("Plano anual parcelado é aceito no cartão")
    void parcelamentoNoCartao() {
        Comprovante c = new CobrancaPlanoAnual(ana, new PagamentoCartao(new AdquirenteSimulada()),
                livro, notificador, Plano.BLACK, 12).processar();
        assertEquals(StatusPagamento.APROVADO, c.status());
        assertEquals(12, livro.lancamentos().get(0).transacao().parcelas());
    }

    @Test
    @DisplayName("Cartão recusado: a mesma cobrança pode ser refeita com outro meio")
    void trocaDeMeioAposRecusa() {
        Aluno semSaldo = new Aluno("2025-0410", "Diego Rocha", Plano.BASICO, false, 20, false,
                ana.formaPreferida(), AdquirenteSimulada.PREFIXO_RECUSA + "_1");
        Comprovante recusado = new CobrancaMensalidade(semSaldo, new PagamentoCartao(new AdquirenteSimulada()),
                livro, notificador, Plano.BASICO, NOV).processar();
        Comprovante comPix = new CobrancaMensalidade(semSaldo, new PagamentoPix(new PspPixSimulado()),
                livro, notificador, Plano.BASICO, NOV).processar();

        assertEquals(StatusPagamento.RECUSADO, recusado.status());
        assertEquals(StatusPagamento.PENDENTE, comPix.status());
    }

    @Test
    @DisplayName("Os meios concretos são final e implementam MeioPagamento")
    void implementadoresConcretos() {
        for (Class<?> c : new Class<?>[] {PagamentoPix.class, PagamentoCartao.class, PagamentoBoleto.class}) {
            assertTrue(MeioPagamento.class.isAssignableFrom(c), c.getSimpleName());
            assertTrue(Modifier.isFinal(c.getModifiers()), c.getSimpleName());
        }
    }
}
