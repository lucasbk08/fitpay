package br.com.fitpay.app;

import br.com.fitpay.cobranca.CobrancaNaoPermitidaException;
import br.com.fitpay.comprovante.Comprovante;
import br.com.fitpay.dominio.Aluno;
import br.com.fitpay.dominio.FormaPagamento;
import br.com.fitpay.dominio.ItemAvulso;
import br.com.fitpay.dominio.Plano;
import br.com.fitpay.financeiro.Lancamento;
import br.com.fitpay.financeiro.LivroCaixa;
import br.com.fitpay.notificacao.NotificadorConsole;
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

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Demonstração de console com os cenários da apresentação do Kit G.
 * Alunos, preços e regras são fictícios; os provedores são simulados.
 */
public final class FitPayApp {

    private static final PrintStream OUT = new PrintStream(System.out, true, StandardCharsets.UTF_8);
    private static final YearMonth NOVEMBRO = YearMonth.of(2026, 11);

    public static void main(String[] args) {
        // Montagem das dependências (injeção pelo construtor, sem Singleton)
        Map<FormaPagamento, MeioPagamento> meios = Map.of(
                FormaPagamento.PIX, new PagamentoPix(new PspPixSimulado()),
                FormaPagamento.CARTAO, new PagamentoCartao(new AdquirenteSimulada()),
                FormaPagamento.BOLETO, new PagamentoBoleto(new BancoSimulado()));
        LivroCaixa livro = new LivroCaixa();
        ServicoCobranca servico = new ServicoCobranca(meios, livro, new NotificadorConsole(OUT));

        Aluno ana = new Aluno("2024-0187", "Ana Souza", Plano.BLACK, true, 10, false,
                FormaPagamento.CARTAO, "tok_ana_4242");
        Aluno bruno = new Aluno("2025-0342", "Bruno Lima", Plano.BASICO, false, 5, false,
                FormaPagamento.PIX, "tok_bruno_1881");
        Aluno carla = new Aluno("2023-0099", "Carla Mendes", Plano.BLACK, false, 10, true,
                FormaPagamento.BOLETO, null);
        Aluno diego = new Aluno("2025-0410", "Diego Rocha", Plano.BASICO, false, 20, false,
                FormaPagamento.CARTAO, AdquirenteSimulada.PREFIXO_RECUSA + "_0002");

        titulo("FitPay · Kit G · Bridge, Template Method e Factory Method");

        cenario(1, "Mensalidade da Ana no cartão (fluxo do slide 17)",
                "Template Method conduz os 8 passos; convênio aplica 15% no gancho aplicarDescontos().");
        mostrar(servico.cobrarMensalidade(ana, NOVEMBRO));

        cenario(2, "Mesma mensalidade, trocando só o meio para Pix",
                "Bridge: muda só o passo 5. A cobrança e o comprovante continuam os mesmos.");
        mostrar(servico.cobrarMensalidade(ana, NOVEMBRO, FormaPagamento.PIX));

        cenario(3, "Diária avulsa do Bruno, no boleto",
                "CobrancaAvulsa não sobrescreve aplicarDescontos(): herda o padrão, sem desconto.");
        mostrar(servico.cobrarAvulso(bruno, ItemAvulso.DIARIA, FormaPagamento.BOLETO));

        cenario(4, "Plano anual do Bruno: à vista no Pix e em 12x no cartão",
                "Factory Method: CobrancaPlanoAnual cria um ExtratoParcelamento.");
        mostrar(servico.cobrarPlanoAnual(bruno, FormaPagamento.PIX, 1));
        mostrar(servico.cobrarPlanoAnual(bruno, FormaPagamento.CARTAO, 12));

        cenario(5, "Plano anual em 12x no boleto",
                "O construtor pergunta ao meio aceitaParcelamento() sem conhecer a classe concreta.");
        try {
            servico.cobrarPlanoAnual(bruno, FormaPagamento.BOLETO, 12);
        } catch (IllegalArgumentException e) {
            OUT.println("    Recusado ao montar a cobrança: " + e.getMessage());
        }

        cenario(6, "Cartão recusado e nova tentativa com Pix",
                "Bridge: o meio é só uma referência; a mesma cobrança vai para outro meio.");
        Comprovante recusado = servico.cobrarMensalidade(diego, NOVEMBRO);
        mostrar(recusado);
        if (recusado.status() == StatusPagamento.RECUSADO) {
            OUT.println("    Refazendo com Pix…");
            mostrar(servico.cobrarMensalidade(diego, NOVEMBRO, FormaPagamento.PIX));
        }

        cenario(7, "Carla está bloqueada",
                "Template Method: o passo 1 é fixo; nada é pago nem registrado.");
        int antes = livro.lancamentos().size();
        try {
            servico.cobrarMensalidade(carla, NOVEMBRO);
        } catch (CobrancaNaoPermitidaException e) {
            OUT.println("    " + e.getMessage());
        }
        OUT.println("    Lançamentos no livro-caixa antes e depois: " + antes + " e " + livro.lancamentos().size());

        cenario(8, "Agendador no dia 10/11/2026",
                "Ator de sistema: cobra a mensalidade de quem vence no dia, no meio preferido.");
        Agendador agendador = new Agendador(servico, List.of(ana, bruno, carla, diego));
        Agendador.Execucao rodada = agendador.executar(LocalDate.of(2026, 11, 10));
        OUT.println("    Cobranças geradas: " + rodada.comprovantes().size());
        rodada.ignorados().forEach(i -> OUT.println("    Ignorado: " + i));

        titulo("Livro-caixa (passo 6, executado em toda cobrança válida)");
        OUT.printf("    %-9s  %-13s  %10s  %s%n", "Transação", "Aluno", "Valor", "Status");
        for (Lancamento l : livro.lancamentos()) {
            OUT.printf("    %-9s  %-13s  %10s  %s%n", l.transacao().id(), l.transacao().pagador(),
                    "R$ " + l.transacao().valor(), l.resultado().status());
        }
        OUT.printf("    Aprovado: R$ %s · Pendente: R$ %s · Recusado: R$ %s%n",
                livro.totalCom(StatusPagamento.APROVADO),
                livro.totalCom(StatusPagamento.PENDENTE),
                livro.totalCom(StatusPagamento.RECUSADO));
    }

    private static void titulo(String texto) {
        OUT.println();
        OUT.println("=".repeat(texto.length()));
        OUT.println(texto);
        OUT.println("=".repeat(texto.length()));
    }

    private static void cenario(int n, String nome, String padrao) {
        OUT.println();
        OUT.println(n + ". " + nome);
        OUT.println("   " + padrao);
    }

    private static void mostrar(Comprovante c) {
        OUT.printf("    -> %s %s (%s)%n", c.getClass().getSimpleName(), c.numero(), c.status());
    }
}
