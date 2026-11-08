package br.com.fitpay.pagamento.provedor.simulado;

import br.com.fitpay.pagamento.provedor.BancoClient;
import br.com.fitpay.pagamento.provedor.Boleto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Banco simulado: registra boletos com nosso número e linha digitável fictícios.
 */
public final class BancoSimulado implements BancoClient {

    private final AtomicLong sequencia = new AtomicLong(20_260_000);
    private final List<String> baixados = new ArrayList<>();

    @Override
    public Boleto registrar(BigDecimal valor, LocalDate vencimento, String pagador) {
        String nossoNumero = String.valueOf(sequencia.incrementAndGet());
        String centavos = String.format("%010d", valor.movePointRight(2).longValueExact());
        String linha = "34191.09008 " + nossoNumero.substring(0, 5) + "." + nossoNumero.substring(5)
                + " 00000.000000 1 " + centavos;
        return new Boleto(nossoNumero, linha);
    }

    @Override
    public void baixar(String nossoNumero) { baixados.add(nossoNumero); }

    public List<String> baixados() { return Collections.unmodifiableList(baixados); }
}
