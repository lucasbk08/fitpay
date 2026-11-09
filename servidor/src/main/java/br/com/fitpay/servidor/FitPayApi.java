package br.com.fitpay.servidor;

import br.com.fitpay.cobranca.Cobranca;
import br.com.fitpay.cobranca.CobrancaAvulsa;
import br.com.fitpay.cobranca.CobrancaMensalidade;
import br.com.fitpay.cobranca.CobrancaNaoPermitidaException;
import br.com.fitpay.cobranca.CobrancaPlanoAnual;
import br.com.fitpay.comprovante.Comprovante;
import br.com.fitpay.comprovante.ExtratoParcelamento;
import br.com.fitpay.comprovante.ReciboAvulso;
import br.com.fitpay.comprovante.ReciboMensalidade;
import br.com.fitpay.dominio.Aluno;
import br.com.fitpay.dominio.FormaPagamento;
import br.com.fitpay.dominio.ItemAvulso;
import br.com.fitpay.dominio.Plano;
import br.com.fitpay.dominio.Transacao;
import br.com.fitpay.financeiro.LivroCaixa;
import br.com.fitpay.notificacao.Notificador;
import br.com.fitpay.pagamento.MeioPagamento;
import br.com.fitpay.pagamento.PagamentoBoleto;
import br.com.fitpay.pagamento.PagamentoCartao;
import br.com.fitpay.pagamento.PagamentoPix;
import br.com.fitpay.pagamento.ResultadoPagamento;
import br.com.fitpay.pagamento.StatusPagamento;
import br.com.fitpay.pagamento.provedor.simulado.AdquirenteSimulada;
import br.com.fitpay.pagamento.provedor.simulado.BancoSimulado;
import br.com.fitpay.pagamento.provedor.simulado.PspPixSimulado;
import br.com.fitpay.servico.Agendador;
import br.com.fitpay.servico.ServicoCobranca;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static br.com.fitpay.servidor.Json.obj;

/**
 * Casos de uso que o front-end chama pela API. Usa as classes do núcleo
 * ({@code src/main/java}) sem alterá-las: monta a cobrança, chama
 * {@code processar()} e devolve o resultado como mapas prontos para JSON.
 * <p>
 * Para o painel "Por dentro", cada cobrança é feita com o meio de pagamento,
 * o livro-caixa e o notificador embrulhados em observadores, que só anotam o
 * que o {@code processar()} real fez e repassam a chamada ao objeto verdadeiro.
 */
public final class FitPayApi {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] MES = {"jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"};

    /** Pedido de cobrança vindo do front-end. */
    public record Pedido(String matricula, String tipo, String item, int parcelas, String meio, boolean simularRecusa) { }

    /** Erro na requisição (aluno, meio ou tipo inexistente). Vira HTTP 400. */
    public static final class RequisicaoInvalida extends RuntimeException {
        private static final long serialVersionUID = 1L;
        RequisicaoInvalida(String mensagem) { super(mensagem); }
    }

    /** O que o extrato precisa e o núcleo não guarda: rótulo, meio, horário e comprovante. */
    private static final class Registro {
        Transacao transacao;
        ResultadoPagamento resultado;
        String rotulo;
        String tipo;
        String meio;
        String cobrancaClasse;
        LocalDateTime quando;
        Comprovante comprovante;
        boolean confirmado;
    }

    private final List<Aluno> alunos;
    private final YearMonth competencia;
    private final Map<String, MeioPagamento> meios = new LinkedHashMap<>();
    private final LivroCaixa livro = new LivroCaixa();
    private final List<String> notificacoes = new ArrayList<>();
    private final Notificador notificador = (aluno, c) -> notificacoes.add(aluno.nome() + ": " + c.renderizar());
    private final Map<String, Registro> registros = new LinkedHashMap<>();

    public FitPayApi(List<Aluno> alunos, YearMonth competencia) {
        this.alunos = List.copyOf(alunos);
        this.competencia = competencia;
        meios.put("pix", new PagamentoPix(new PspPixSimulado()));
        meios.put("cartao", new PagamentoCartao(new AdquirenteSimulada()));
        meios.put("boleto", new PagamentoBoleto(new BancoSimulado()));
    }

    /** Os mesmos alunos e movimentos de exemplo do protótipo. Dados fictícios. */
    public static FitPayApi comDadosDeExemplo() {
        List<Aluno> alunos = List.of(
                new Aluno("2024-0187", "Ana Souza", Plano.BLACK, true, 10, false, FormaPagamento.CARTAO, "tok_ana_4242"),
                new Aluno("2025-0342", "Bruno Lima", Plano.BASICO, false, 5, false, FormaPagamento.PIX, "tok_bruno_1881"),
                new Aluno("2023-0099", "Carla Mendes", Plano.BLACK, false, 10, true, FormaPagamento.BOLETO, null),
                new Aluno("2025-0410", "Diego Rocha", Plano.BASICO, false, 20, false, FormaPagamento.CARTAO, "tok_recusa_0002"),
                new Aluno("2024-0233", "Elisa Prado", Plano.BLACK, false, 10, false, FormaPagamento.BOLETO, null));
        FitPayApi api = new FitPayApi(alunos, YearMonth.now());
        api.exemplo(new Pedido("2024-0233", "avulsa", "taxa_matricula", 1, "boleto", false), LocalTime.of(8, 12), false);
        api.exemplo(new Pedido("2025-0342", "avulsa", "diaria", 1, "pix", false), LocalTime.of(9, 29), true);
        api.exemplo(new Pedido("2024-0187", "avulsa", "personal", 1, "cartao", false), LocalTime.of(11, 46), false);
        return api;
    }

    private void exemplo(Pedido p, LocalTime hora, boolean confirmar) {
        cobrar(p);
        Registro r = ultimoRegistro();
        r.quando = LocalDateTime.now().with(hora);
        r.confirmado = confirmar;
    }

    // ------------------------------------------------------------------ consultas

    public synchronized Map<String, Object> estado() {
        List<Object> lancamentos = new ArrayList<>();
        List<Registro> lista = new ArrayList<>(registros.values());
        for (int i = lista.size() - 1; i >= 0; i--) lancamentos.add(dto(lista.get(i)));
        List<Object> itens = new ArrayList<>();
        for (ItemAvulso i : ItemAvulso.values()) {
            itens.add(obj("id", i.name().toLowerCase(Locale.ROOT), "nome", i.descricao(), "preco", i.preco()));
        }
        List<Object> listaAlunos = new ArrayList<>();
        for (Aluno a : alunos) {
            Map<String, Object> d = dto(a);
            d.put("valorMensalidade", previa(new Pedido(a.matricula(), "mensalidade", null, 1, "cartao", false)).get("valor"));
            listaAlunos.add(d);
        }
        List<Object> listaMeios = new ArrayList<>();
        for (Map.Entry<String, MeioPagamento> m : meios.entrySet()) {
            listaMeios.add(obj("id", m.getKey(), "classe", m.getValue().getClass().getSimpleName(),
                    "aceitaParcelamento", m.getValue().aceitaParcelamento()));
        }
        return obj("backend", "java",
                "competencia", obj("ano", competencia.getYear(), "mes", competencia.getMonthValue(), "rotulo", rotulo(competencia)),
                "alunos", listaAlunos,
                "meios", listaMeios,
                "itens", itens,
                "lancamentos", lancamentos);
    }

    /** Valor que a cobrança teria, calculado pela própria subclasse, sem processar nada. */
    public synchronized Map<String, Object> previa(Pedido p) {
        Aluno aluno = aluno(p.matricula());
        MeioPagamento meio = meio(p.meio() == null || p.meio().isBlank() ? "cartao" : p.meio());
        try {
            Cobranca c = montar(aluno, meio, new LivroCaixa(), (a, comp) -> { }, p);
            BigDecimal bruto = invocar(c, "calcularValor");
            BigDecimal valor = invocar(c, "aplicarDescontos", bruto);
            return obj("bruto", bruto, "valor", valor.setScale(2, RoundingMode.HALF_UP));
        } catch (IllegalArgumentException e) {
            return obj("erro", e.getMessage());
        }
    }

    // ------------------------------------------------------------------ comandos

    /** Monta a cobrança (Bridge), chama processar() (Template Method) e devolve o resultado. */
    public synchronized Map<String, Object> cobrar(Pedido p) {
        Aluno aluno = aluno(p.matricula());
        if (p.simularRecusa() && "cartao".equals(p.meio())) {
            aluno = new Aluno(aluno.matricula(), aluno.nome(), aluno.plano(), aluno.convenioEmpresa(),
                    aluno.diaVencimento(), aluno.estaBloqueado(), aluno.formaPreferida(),
                    AdquirenteSimulada.PREFIXO_RECUSA + "_teste");
        }
        MeioPagamento real = meio(p.meio());
        Observacao obs = new Observacao();
        Map<String, Object> montagem = obj("tipoClasse", classeDoTipo(p.tipo()), "meioClasse", real.getClass().getSimpleName());

        Cobranca cobranca;
        try {
            cobranca = montar(aluno, new MeioObservado(real, obs), new LivroObservado(livro, obs), notificadorObservado(obs), p);
        } catch (IllegalArgumentException e) {
            return obj("lancamento", null, "trace", List.of(), "montagem", montagem,
                    "erro", obj("classe", e.getClass().getSimpleName(), "mensagem", e.getMessage(), "bloqueio", false, "montagem", true));
        }

        BigDecimal bruto = invocar(cobranca, "calcularValor");
        Comprovante comprovante;
        try {
            comprovante = cobranca.processar();
        } catch (CobrancaNaoPermitidaException e) {
            List<Object> trace = List.of(passo(1, false, e.getMessage(), e.getClass().getSimpleName(), null));
            return obj("lancamento", null, "trace", trace, "montagem", montagem,
                    "erro", obj("classe", e.getClass().getSimpleName(), "mensagem", e.getMessage(), "bloqueio", true, "montagem", false));
        }

        Registro reg = new Registro();
        reg.transacao = obs.transacao;
        reg.resultado = obs.resultado;
        reg.rotulo = rotulo(p, aluno);
        reg.tipo = p.tipo();
        reg.meio = p.meio();
        reg.cobrancaClasse = cobranca.getClass().getSimpleName();
        reg.quando = LocalDateTime.now();
        reg.comprovante = comprovante;
        registros.put(reg.transacao.id(), reg);

        return obj("lancamento", dto(reg), "erro", null, "montagem", montagem,
                "trace", trace(cobranca, real, bruto, reg, obs, p.tipo()));
    }

    /** Simula a confirmação do provedor para um Pix ou boleto pendente. */
    public synchronized Map<String, Object> confirmar(String id) {
        Registro r = registros.get(id);
        if (r == null) throw new RequisicaoInvalida("Lançamento não encontrado: " + id);
        if (r.resultado.status() == StatusPagamento.PENDENTE) r.confirmado = true;
        return dto(r);
    }

    /** Roda o Agendador do núcleo para as mensalidades que vencem no dia informado. */
    public synchronized Map<String, Object> agendar(int dia) {
        Map<FormaPagamento, MeioPagamento> porForma = new EnumMap<>(FormaPagamento.class);
        porForma.put(FormaPagamento.PIX, meios.get("pix"));
        porForma.put(FormaPagamento.CARTAO, meios.get("cartao"));
        porForma.put(FormaPagamento.BOLETO, meios.get("boleto"));
        Observacao obs = new Observacao();
        LivroObservado coletor = new LivroObservado(livro, obs);
        Agendador agendador = new Agendador(new ServicoCobranca(porForma, coletor, notificador), alunos);

        Agendador.Execucao execucao = agendador.executar(competencia.atDay(dia));

        Map<String, Comprovante> porNumero = new HashMap<>();
        for (Comprovante c : execucao.comprovantes()) porNumero.put(c.numero(), c);
        for (int i = 0; i < obs.todas.size(); i++) {
            Transacao t = obs.todas.get(i);
            Registro reg = new Registro();
            reg.transacao = t;
            reg.resultado = obs.resultados.get(i);
            reg.rotulo = "Mensalidade " + rotulo(competencia);
            reg.tipo = "mensalidade";
            reg.meio = t.aluno().formaPreferida().name().toLowerCase(Locale.ROOT);
            reg.cobrancaClasse = CobrancaMensalidade.class.getSimpleName();
            reg.quando = LocalDateTime.now();
            reg.comprovante = porNumero.get(t.id());
            registros.put(t.id(), reg);
        }
        List<String> ignorados = new ArrayList<>();
        for (String s : execucao.ignorados()) ignorados.add(s.contains(":") ? s.substring(0, s.indexOf(':')) : s);
        return obj("gerados", execucao.comprovantes().size(), "ignorados", ignorados);
    }

    /** Liga ou desliga o débito automático: um MeioPagamento novo, sem mudar nenhuma cobrança. */
    public synchronized List<String> definirDebito(boolean ativo) {
        if (ativo) meios.putIfAbsent("debito", new PagamentoDebitoAutomatico());
        else meios.remove("debito");
        return new ArrayList<>(meios.keySet());
    }

    // ------------------------------------------------------------------ montagem

    private Cobranca montar(Aluno aluno, MeioPagamento meio, LivroCaixa livroCaixa, Notificador notif, Pedido p) {
        return switch (p.tipo() == null ? "" : p.tipo()) {
            case "mensalidade" -> new CobrancaMensalidade(aluno, meio, livroCaixa, notif, aluno.plano(), competencia);
            case "avulsa" -> new CobrancaAvulsa(aluno, meio, livroCaixa, notif, item(p.item()));
            case "anual" -> new CobrancaPlanoAnual(aluno, meio, livroCaixa, notif, aluno.plano(), Math.max(1, p.parcelas()));
            default -> throw new RequisicaoInvalida("Tipo de cobrança inválido: " + p.tipo());
        };
    }

    private Aluno aluno(String matricula) {
        for (Aluno a : alunos) if (a.matricula().equals(matricula)) return a;
        throw new RequisicaoInvalida("Aluno não encontrado: " + matricula);
    }

    private MeioPagamento meio(String id) {
        MeioPagamento m = meios.get(id);
        if (m == null) throw new RequisicaoInvalida("Meio de pagamento indisponível: " + id);
        return m;
    }

    private static ItemAvulso item(String id) {
        try {
            return ItemAvulso.valueOf((id == null ? "diaria" : id).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new RequisicaoInvalida("Item avulso inválido: " + id);
        }
    }

    private static String classeDoTipo(String tipo) {
        return switch (tipo == null ? "" : tipo) {
            case "mensalidade" -> CobrancaMensalidade.class.getSimpleName();
            case "avulsa" -> CobrancaAvulsa.class.getSimpleName();
            case "anual" -> CobrancaPlanoAnual.class.getSimpleName();
            default -> throw new RequisicaoInvalida("Tipo de cobrança inválido: " + tipo);
        };
    }

    // ------------------------------------------------------------------ painel "Por dentro"

    private List<Object> trace(Cobranca c, MeioPagamento real, BigDecimal bruto, Registro reg, Observacao obs, String tipo) {
        Transacao t = reg.transacao;
        ResultadoPagamento r = reg.resultado;
        BigDecimal brutoArred = bruto.setScale(2, RoundingMode.HALF_UP);
        String motivo = "mensalidade".equals(tipo) ? "convênio empresarial, 15%" : "à vista, 10%";
        String passo3 = brutoArred.compareTo(t.valor()) == 0
                ? "Sem desconto: " + reais(t.valor())
                : reais(brutoArred) + " → " + reais(t.valor()) + " (" + motivo + ")";
        String passo4 = t.id() + " · " + (t.parcelas() > 1 ? t.parcelas() + " parcelas" : "parcela única")
                + " · vence " + t.vencimento().format(DATA);
        String passo5 = r.status() + (r.status() == StatusPagamento.RECUSADO ? " · " + r.detalhe()
                : r.idExterno() != null ? " · " + r.idExterno() : "");
        String passo8 = obs.notificado ? "Comprovante enviado para " + t.aluno().nome().split(" ")[0] : "Notificação dispensada";
        List<Object> passos = new ArrayList<>();
        passos.add(passo(1, true, "Aluno liberado para cobrança", null, null));
        passos.add(passo(2, true, "Valor bruto: " + reais(brutoArred), null, quem(c, "calcularValor")));
        passos.add(passo(3, true, passo3, null, quem(c, "aplicarDescontos")));
        passos.add(passo(4, true, passo4, null, quem(c, "parcelas", "vencimento")));
        passos.add(passo(5, true, passo5, null, List.of(obj("m", null, "dono", real.getClass().getSimpleName(), "padrao", false))));
        passos.add(passo(6, true, "Lançamento " + t.id() + " registrado", null, null));
        passos.add(passo(7, true, "new " + reg.comprovante.getClass().getSimpleName() + "(…)", null, quem(c, "criarComprovante")));
        passos.add(passo(8, true, passo8, null, quem(c, "deveNotificar")));
        return passos;
    }

    private static Map<String, Object> passo(int n, boolean ok, String saida, String erro, List<Object> quem) {
        return obj("n", n, "ok", ok, "out", saida, "erro", erro, "quem", quem);
    }

    /** Descobre por reflexão quem implementou cada passo: a subclasse ou o padrão de Cobranca. */
    private static List<Object> quem(Cobranca c, String... metodos) {
        List<Object> lista = new ArrayList<>();
        for (String nome : metodos) {
            String dono = Cobranca.class.getSimpleName();
            for (Class<?> k = c.getClass(); k != null && k != Object.class; k = k.getSuperclass()) {
                if (declara(k, nome)) {
                    dono = k.getSimpleName();
                    break;
                }
            }
            lista.add(obj("m", nome, "dono", dono, "padrao", dono.equals(Cobranca.class.getSimpleName())));
        }
        return lista;
    }

    private static boolean declara(Class<?> k, String nome) {
        for (Method m : k.getDeclaredMethods()) if (m.getName().equals(nome) && !m.isSynthetic()) return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T> T invocar(Object alvo, String nome, Object... args) {
        for (Class<?> k = alvo.getClass(); k != null; k = k.getSuperclass()) {
            for (Method m : k.getDeclaredMethods()) {
                if (m.getName().equals(nome) && m.getParameterCount() == args.length) {
                    try {
                        m.setAccessible(true);
                        return (T) m.invoke(alvo, args);
                    } catch (InvocationTargetException e) {
                        if (e.getCause() instanceof RuntimeException re) throw re;
                        throw new IllegalStateException(e.getCause());
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }
        throw new IllegalStateException("Método não encontrado: " + nome);
    }

    // ------------------------------------------------------------------ observadores

    private static final class Observacao {
        Transacao transacao;
        ResultadoPagamento resultado;
        boolean notificado;
        final List<Transacao> todas = new ArrayList<>();
        final List<ResultadoPagamento> resultados = new ArrayList<>();
    }

    /** Repassa ao meio verdadeiro; só existe para o painel saber o que aconteceu. */
    private static final class MeioObservado implements MeioPagamento {
        private final MeioPagamento real;
        private final Observacao obs;
        MeioObservado(MeioPagamento real, Observacao obs) { this.real = real; this.obs = obs; }
        @Override public ResultadoPagamento executar(Transacao t) { return real.executar(t); }
        @Override public void estornar(String idExterno) { real.estornar(idExterno); }
        @Override public boolean aceitaParcelamento() { return real.aceitaParcelamento(); }
    }

    /** Registra no livro-caixa verdadeiro e anota a transação para a resposta. */
    private static final class LivroObservado extends LivroCaixa {
        private final LivroCaixa destino;
        private final Observacao obs;
        LivroObservado(LivroCaixa destino, Observacao obs) { this.destino = destino; this.obs = obs; }
        @Override
        public void registrar(Transacao t, ResultadoPagamento r) {
            obs.transacao = t;
            obs.resultado = r;
            obs.todas.add(t);
            obs.resultados.add(r);
            destino.registrar(t, r);
        }
    }

    private Notificador notificadorObservado(Observacao obs) {
        return (aluno, c) -> {
            obs.notificado = true;
            notificador.enviar(aluno, c);
        };
    }

    private Registro ultimoRegistro() {
        Registro ultimo = null;
        for (Registro r : registros.values()) ultimo = r;
        return ultimo;
    }

    // ------------------------------------------------------------------ JSON

    private static Map<String, Object> dto(Aluno a) {
        Plano p = a.plano();
        return obj("matricula", a.matricula(), "nome", a.nome(),
                "plano", obj("nome", p.nome(), "valorMensal", p.valorMensal(), "valorAnual", p.valorAnual()),
                "convenio", a.temConvenioEmpresa(), "diaVencimento", a.diaVencimento(), "bloqueado", a.estaBloqueado(),
                "formaPreferida", a.formaPreferida().name().toLowerCase(Locale.ROOT),
                "cartaoFinal", a.tokenCartao() == null ? null : a.tokenCartao().substring(a.tokenCartao().length() - 4));
    }

    private Map<String, Object> dto(Registro r) {
        Transacao t = r.transacao;
        StatusPagamento original = r.resultado.status();
        String status = r.confirmado ? StatusPagamento.APROVADO.name() : original.name();
        return obj("id", t.id(),
                "aluno", obj("matricula", t.aluno().matricula(), "nome", t.aluno().nome()),
                "rotulo", r.rotulo, "tipo", r.tipo, "meio", r.meio, "cobrancaClasse", r.cobrancaClasse,
                "valor", t.valor(), "parcelas", t.parcelas(), "vencimento", t.vencimento().toString(),
                "status", status, "statusOriginal", original.name(), "confirmado", r.confirmado,
                "idExterno", r.resultado.idExterno(), "detalhe", r.resultado.detalhe(),
                "quando", r.quando.withNano(0).toString(),
                "comprovante", r.comprovante == null ? null : dto(r.comprovante, t));
    }

    private Map<String, Object> dto(Comprovante c, Transacao t) {
        String titulo;
        List<Object> linhas = new ArrayList<>();
        if (c instanceof ReciboMensalidade rm) {
            titulo = "Recibo de mensalidade";
            linhas.add(List.of("Competência", rotulo(rm.competencia())));
            linhas.add(List.of("Vencimento", t.vencimento().format(DATA)));
            linhas.add(List.of("Próximo vencimento", rm.proximoVencimento().format(DATA)));
        } else if (c instanceof ReciboAvulso ra) {
            titulo = "Recibo avulso";
            linhas.add(List.of("Item", ra.item().descricao()));
            linhas.add(List.of("Vencimento", t.vencimento().format(DATA)));
        } else if (c instanceof ExtratoParcelamento ep) {
            titulo = "Extrato do plano anual";
            int n = ep.parcelas();
            linhas.add(List.of("Forma", n == 1 ? "À vista, com 10% de desconto"
                    : n + "x de " + reais(t.valor().divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP))));
            linhas.add(List.of("1º vencimento", t.vencimento().format(DATA)));
        } else {
            titulo = "Comprovante";
        }
        return obj("classe", c.getClass().getSimpleName(), "titulo", titulo, "linhas", linhas, "texto", c.renderizar());
    }

    private String rotulo(Pedido p, Aluno aluno) {
        return switch (p.tipo()) {
            case "mensalidade" -> "Mensalidade " + rotulo(competencia);
            case "avulsa" -> item(p.item()).descricao();
            default -> "Plano anual " + aluno.plano().nome() + (p.parcelas() > 1 ? " em " + p.parcelas() + "x" : " à vista");
        };
    }

    private static String rotulo(YearMonth ym) {
        return MES[ym.getMonthValue() - 1] + "/" + ym.getYear();
    }

    static String reais(BigDecimal v) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(v).replace(' ', ' ');
    }
}
