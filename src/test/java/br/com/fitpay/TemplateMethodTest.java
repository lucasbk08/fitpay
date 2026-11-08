package br.com.fitpay;

import br.com.fitpay.Fixtures.MeioFalso;
import br.com.fitpay.Fixtures.NotificadorEmMemoria;
import br.com.fitpay.cobranca.Cobranca;
import br.com.fitpay.cobranca.CobrancaAvulsa;
import br.com.fitpay.cobranca.CobrancaMensalidade;
import br.com.fitpay.cobranca.CobrancaNaoPermitidaException;
import br.com.fitpay.cobranca.CobrancaPlanoAnual;
import br.com.fitpay.comprovante.Comprovante;
import br.com.fitpay.dominio.Aluno;
import br.com.fitpay.dominio.ItemAvulso;
import br.com.fitpay.dominio.Plano;
import br.com.fitpay.dominio.Transacao;
import br.com.fitpay.financeiro.LivroCaixa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Template Method: o fluxo de 8 passos de processar()")
class TemplateMethodTest {

    private static final YearMonth NOV = YearMonth.of(2026, 11);
    private LivroCaixa livro;
    private NotificadorEmMemoria notificador;
    private MeioFalso meio;

    @BeforeEach
    void preparar() {
        livro = new LivroCaixa();
        notificador = new NotificadorEmMemoria();
        meio = MeioFalso.aprovando();
    }

    private Transacao unicaTransacao() {
        assertEquals(1, livro.lancamentos().size());
        return livro.lancamentos().get(0).transacao();
    }

    @Test
    @DisplayName("processar() é final: nenhuma subclasse muda a ordem")
    void metodoTemplateEhFinal() throws NoSuchMethodException {
        Method processar = Cobranca.class.getMethod("processar");
        assertTrue(Modifier.isFinal(processar.getModifiers()));
    }

    @Test
    @DisplayName("calcularValor() e criarComprovante() são passos abstratos")
    void passosAbstratos() throws NoSuchMethodException {
        assertTrue(Modifier.isAbstract(Cobranca.class.getDeclaredMethod("calcularValor").getModifiers()));
        assertTrue(Modifier.isAbstract(Cobranca.class.getDeclaredMethod("criarComprovante",
                Transacao.class, br.com.fitpay.pagamento.ResultadoPagamento.class).getModifiers()));
    }

    @Test
    @DisplayName("Fluxo completo: paga, registra no livro-caixa e notifica")
    void fluxoCompleto() {
        Comprovante c = new CobrancaAvulsa(Fixtures.bruno(), meio, livro, notificador, ItemAvulso.PERSONAL).processar();

        assertEquals(1, meio.executadas.size());          // passo 5
        assertEquals(1, livro.lancamentos().size());      // passo 6
        assertEquals(1, notificador.enviados.size());     // passo 8
        assertSame(c, notificador.enviados.get(0));
    }

    @Test
    @DisplayName("Aluno bloqueado para no passo 1: nada é pago nem registrado")
    void alunoBloqueado() {
        Aluno carla = Fixtures.carlaBloqueada();
        assertThrows(CobrancaNaoPermitidaException.class,
                () -> new CobrancaMensalidade(carla, meio, livro, notificador, Plano.BLACK, NOV).processar());

        assertEquals(0, meio.executadas.size());
        assertEquals(0, livro.lancamentos().size());
        assertEquals(0, notificador.enviados.size());
    }

    @Test
    @DisplayName("Mensalidade: convênio aplica 15% e vence no dia do aluno")
    void mensalidadeComConvenio() {
        new CobrancaMensalidade(Fixtures.ana(), meio, livro, notificador, Plano.BLACK, NOV).processar();
        Transacao t = unicaTransacao();
        assertEquals(new BigDecimal("127.42"), t.valor());    // 149,90 × 0,85
        assertEquals(LocalDate.of(2026, 11, 10), t.vencimento());
        assertEquals(1, t.parcelas());
    }

    @Test
    @DisplayName("Mensalidade sem convênio: valor cheio do plano")
    void mensalidadeSemConvenio() {
        new CobrancaMensalidade(Fixtures.bruno(), meio, livro, notificador, Plano.BASICO, NOV).processar();
        assertEquals(new BigDecimal("99.90"), unicaTransacao().valor());
    }

    @Test
    @DisplayName("Avulsa herda o gancho padrão: sem desconto, vence hoje")
    void avulsaUsaGanchoPadrao() {
        new CobrancaAvulsa(Fixtures.ana(), meio, livro, notificador, ItemAvulso.DIARIA).processar();
        Transacao t = unicaTransacao();
        assertEquals(new BigDecimal("35.00"), t.valor());   // Ana tem convênio, mas avulso não tem desconto
        assertEquals(LocalDate.now(), t.vencimento());
    }

    @Test
    @DisplayName("Plano anual à vista: 10% de desconto e vencimento padrão de 3 dias")
    void planoAnualAVista() {
        new CobrancaPlanoAnual(Fixtures.bruno(), meio, livro, notificador, Plano.BASICO, 1).processar();
        Transacao t = unicaTransacao();
        assertEquals(new BigDecimal("899.10"), t.valor());  // 999,00 × 0,90
        assertEquals(LocalDate.now().plusDays(3), t.vencimento());
    }

    @Test
    @DisplayName("Plano anual parcelado: sem desconto, com as parcelas na transação")
    void planoAnualParcelado() {
        new CobrancaPlanoAnual(Fixtures.bruno(), meio, livro, notificador, Plano.BASICO, 12).processar();
        Transacao t = unicaTransacao();
        assertEquals(new BigDecimal("999.00"), t.valor());
        assertEquals(12, t.parcelas());
    }
}
