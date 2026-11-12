package br.com.fitpay.servidor;

import br.com.fitpay.servidor.FitPayApi.Pedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("API do back-end sobre as classes do núcleo")
class FitPayApiTest {

    private FitPayApi api;

    @BeforeEach
    void preparar() {
        api = FitPayApi.comDadosDeExemplo();
    }

    @SuppressWarnings("unchecked")
    private static <T> T campo(Object mapa, String chave) {
        return (T) ((Map<String, Object>) mapa).get(chave);
    }

    private int lancamentos() {
        List<Object> l = campo(api.estado(), "lancamentos");
        return l.size();
    }

    @Test
    @DisplayName("Mensalidade no cartão passa pelos 8 passos e o painel recebe quem implementou cada um")
    void mensalidadeNoCartao() {
        Map<String, Object> r = api.cobrar(new Pedido("2024-0187", "mensalidade", null, 1, "cartao", false));
        Object l = r.get("lancamento");
        assertEquals("APROVADO", campo(l, "status"));
        assertEquals(new BigDecimal("127.42"), campo(l, "valor"));
        assertEquals("ReciboMensalidade", campo(campo(l, "comprovante"), "classe"));
        List<Object> trace = campo(r, "trace");
        assertEquals(8, trace.size());
        List<Object> passo5 = campo(trace.get(4), "quem");
        assertEquals("PagamentoCartao", campo(passo5.get(0), "dono"));
        List<Object> passo3 = campo(trace.get(2), "quem");
        assertEquals("CobrancaMensalidade", campo(passo3.get(0), "dono"));
    }

    @Test
    @DisplayName("Aluna bloqueada para no passo 1 e nada entra no extrato")
    void alunaBloqueada() {
        int antes = lancamentos();
        Map<String, Object> r = api.cobrar(new Pedido("2023-0099", "mensalidade", null, 1, "boleto", false));
        assertEquals(true, campo(r.get("erro"), "bloqueio"));
        List<Object> trace = campo(r, "trace");
        assertEquals(1, trace.size());
        assertEquals(antes, lancamentos());
    }

    @Test
    @DisplayName("Plano anual parcelado no boleto é recusado na montagem")
    void parcelamentoNoBoleto() {
        Map<String, Object> r = api.cobrar(new Pedido("2025-0342", "anual", null, 12, "boleto", false));
        assertEquals(true, campo(r.get("erro"), "montagem"));
        assertEquals("Parcelamento só é aceito no cartão", campo(r.get("erro"), "mensagem"));
    }

    @Test
    @DisplayName("Débito automático entra como meio novo sem mudar nenhuma cobrança")
    void debitoAutomatico() {
        assertTrue(api.definirDebito(true).contains("debito"));
        Map<String, Object> r = api.cobrar(new Pedido("2024-0233", "avulsa", "diaria", 1, "debito", false));
        assertEquals("PENDENTE", campo(r.get("lancamento"), "status"));
        List<Object> trace = campo(r, "trace");
        List<Object> passo5 = campo(trace.get(4), "quem");
        assertEquals("PagamentoDebitoAutomatico", campo(passo5.get(0), "dono"));
    }

    @Test
    @DisplayName("Agendador do dia 10 cobra Ana e Elisa e ignora Carla")
    void agendador() {
        Map<String, Object> r = api.agendar(10);
        assertEquals(2, (int) campo(r, "gerados"));
        assertEquals(List.of("Carla Mendes"), campo(r, "ignorados"));
    }

    @Test
    @DisplayName("Agendador recusa dia que não existe no mês")
    void agendadorDiaInvalido() {
        assertThrows(FitPayApi.RequisicaoInvalida.class, () -> api.agendar(0));
        assertThrows(FitPayApi.RequisicaoInvalida.class, () -> api.agendar(32));
    }

    @Test
    @DisplayName("Plano anual com mais de 12 parcelas é recusado na montagem, sem lançamento")
    void planoAnualParcelasDemais() {
        int antes = lancamentos();
        Map<String, Object> r = api.cobrar(new Pedido("2024-0187", "anual", null, 13, "cartao", false));
        assertEquals(true, campo(r.get("erro"), "montagem"));
        assertEquals(antes, lancamentos());
        Map<String, Object> previa = api.previa(new Pedido("2024-0187", "anual", null, 13, "cartao", false));
        assertTrue(previa.containsKey("erro"));
    }

    @Test
    @DisplayName("Prévia usa o cálculo da própria subclasse")
    void previa() {
        Map<String, Object> r = api.previa(new Pedido("2024-0187", "mensalidade", null, 1, "cartao", false));
        assertEquals(new BigDecimal("127.42"), campo(r, "valor"));
    }

    @Test
    @DisplayName("Aluno inexistente gera erro de requisição")
    void alunoInexistente() {
        assertThrows(FitPayApi.RequisicaoInvalida.class,
                () -> api.cobrar(new Pedido("0000", "mensalidade", null, 1, "pix", false)));
    }
}
