package br.com.fitpay.servidor;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Back-end do FitPay: uma API REST (JSON) sobre as classes do núcleo, usando o
 * servidor HTTP que já vem no JDK. Também entrega a tela em {@code /}.
 *
 * <pre>
 * GET  /api/estado                         alunos, meios, itens e extrato
 * GET  /api/previa?aluno=&amp;tipo=&amp;...        valor calculado pela subclasse
 * POST /api/cobrancas                      monta a cobrança e chama processar()
 * POST /api/lancamentos/{id}/confirmacao   simula a confirmação de Pix ou boleto
 * POST /api/agendador                      cobra as mensalidades do dia
 * POST /api/meios/debito                   liga ou desliga o débito automático
 * </pre>
 *
 * Uso: {@code java -cp out br.com.fitpay.servidor.ServidorFitPay [porta] [endereço]},
 * porta padrão 8080 e endereço padrão localhost (no WSL, use 0.0.0.0).
 */
public final class ServidorFitPay {

    private final FitPayApi api;

    ServidorFitPay(FitPayApi api) { this.api = api; }

    public static void main(String[] args) throws IOException, InterruptedException {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        // No WSL o navegador do Windows não alcança o 127.0.0.1 do Linux; o start.sh passa 0.0.0.0.
        String endereco = args.length > 1 ? args[1] : "localhost";
        ServidorFitPay servidor = new ServidorFitPay(FitPayApi.comDadosDeExemplo());
        HttpServer http;
        try {
            http = HttpServer.create(new InetSocketAddress(endereco, porta), 0);
        } catch (BindException e) {
            System.err.println("A porta " + porta + " já está em uso. Rode com outra porta, por exemplo: 8081");
            return;
        }
        http.createContext("/api/", servidor::api).getFilters().add(new Log());
        http.createContext("/", servidor::pagina).getFilters().add(new Log());
        http.start();
        System.out.println("FitPay rodando em http://localhost:" + porta + "  (Ctrl+C para parar)");
        Thread.currentThread().join();
    }

    // ------------------------------------------------------------------ log

    /** Imprime uma linha por requisição: hora, método, caminho, status e tempo. */
    static final class Log extends Filter {
        @Override
        public void doFilter(HttpExchange ex, Chain chain) throws IOException {
            long inicio = System.nanoTime();
            try {
                chain.doFilter(ex);
            } finally {
                long ms = (System.nanoTime() - inicio) / 1_000_000;
                String uri = ex.getRequestURI().toString();
                System.out.printf("%s  %-4s %-45s %d  %d ms%n",
                        LocalTime.now().truncatedTo(ChronoUnit.SECONDS),
                        ex.getRequestMethod(), uri, ex.getResponseCode(), ms);
            }
        }

        @Override
        public String description() { return "log de requisições"; }
    }

    // ------------------------------------------------------------------ API

    void api(HttpExchange ex) throws IOException {
        try {
            String metodo = ex.getRequestMethod();
            String caminho = ex.getRequestURI().getPath();
            if (metodo.equals("OPTIONS")) {
                responder(ex, 204, null);
                return;
            }
            Map<String, String> q = parametros(ex.getRequestURI().getRawQuery());
            if (metodo.equals("POST")) q.putAll(parametros(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));

            Object resposta;
            if (metodo.equals("GET") && caminho.equals("/api/estado")) {
                resposta = api.estado();
            } else if (metodo.equals("GET") && caminho.equals("/api/previa")) {
                resposta = api.previa(pedido(q));
            } else if (metodo.equals("POST") && caminho.equals("/api/cobrancas")) {
                resposta = api.cobrar(pedido(q));
            } else if (metodo.equals("POST") && caminho.matches("/api/lancamentos/[^/]+/confirmacao")) {
                resposta = api.confirmar(caminho.split("/")[3]);
            } else if (metodo.equals("POST") && caminho.equals("/api/agendador")) {
                resposta = api.agendar(Integer.parseInt(q.getOrDefault("dia", "10")));
            } else if (metodo.equals("POST") && caminho.equals("/api/meios/debito")) {
                List<String> meios = api.definirDebito(Boolean.parseBoolean(q.get("ativo")));
                resposta = Json.obj("meios", meios);
            } else {
                responder(ex, 404, Json.obj("erro", "Rota não encontrada: " + metodo + " " + caminho));
                return;
            }
            responder(ex, 200, resposta);
        } catch (FitPayApi.RequisicaoInvalida | NumberFormatException e) {
            responder(ex, 400, Json.obj("erro", e.getMessage()));
        } catch (RuntimeException e) {
            e.printStackTrace();
            responder(ex, 500, Json.obj("erro", "Erro interno: " + e));
        }
    }

    private static FitPayApi.Pedido pedido(Map<String, String> q) {
        return new FitPayApi.Pedido(q.get("aluno"), q.get("tipo"), q.get("item"),
                Integer.parseInt(q.getOrDefault("parcelas", "1")), q.get("meio"),
                Boolean.parseBoolean(q.get("simularRecusa")));
    }

    private static Map<String, String> parametros(String bruto) {
        Map<String, String> m = new HashMap<>();
        if (bruto == null || bruto.isBlank()) return m;
        for (String par : bruto.split("&")) {
            int i = par.indexOf('=');
            String k = i < 0 ? par : par.substring(0, i);
            String v = i < 0 ? "" : par.substring(i + 1);
            m.put(URLDecoder.decode(k, StandardCharsets.UTF_8), URLDecoder.decode(v, StandardCharsets.UTF_8));
        }
        return m;
    }

    private static void responder(HttpExchange ex, int status, Object corpo) throws IOException {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        if (corpo == null) {
            ex.sendResponseHeaders(status, -1);
            ex.close();
            return;
        }
        byte[] bytes = Json.escrever(corpo).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ------------------------------------------------------------------ tela

    void pagina(HttpExchange ex) throws IOException {
        String caminho = ex.getRequestURI().getPath();
        if (!caminho.equals("/") && !caminho.equals("/index.html")) {
            responder(ex, 404, Json.obj("erro", "Não encontrado: " + caminho));
            return;
        }
        Path arquivo = null;
        for (Path p : List.of(Path.of("docs", "prototipo-web.html"), Path.of("..", "docs", "prototipo-web.html"))) {
            if (Files.isRegularFile(p)) {
                arquivo = p;
                break;
            }
        }
        if (arquivo == null) {
            responder(ex, 404, Json.obj("erro", "docs/prototipo-web.html não encontrado; rode o servidor na pasta do projeto"));
            return;
        }
        byte[] bytes = Files.readAllBytes(arquivo);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
