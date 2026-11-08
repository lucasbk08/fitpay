package br.com.fitpay.pagamento.provedor.simulado;

import br.com.fitpay.pagamento.provedor.AdquirenteClient;
import br.com.fitpay.pagamento.provedor.RespostaAdquirente;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Adquirente simulada. Aprova qualquer captura, exceto:
 * <ul>
 *   <li>sem token de cartão salvo: "Aluno sem cartão cadastrado";</li>
 *   <li>token que começa com {@value #PREFIXO_RECUSA}: "Saldo insuficiente".</li>
 * </ul>
 */
public final class AdquirenteSimulada implements AdquirenteClient {

    public static final String PREFIXO_RECUSA = "tok_recusa";

    private final AtomicLong nsu = new AtomicLong(480_000);
    private final List<String> cancelados = new ArrayList<>();

    @Override
    public RespostaAdquirente capturar(String tokenCartao, BigDecimal valor, int parcelas) {
        if (tokenCartao == null || tokenCartao.isBlank()) {
            return RespostaAdquirente.negada("Aluno sem cartão cadastrado");
        }
        if (tokenCartao.startsWith(PREFIXO_RECUSA)) {
            return RespostaAdquirente.negada("Saldo insuficiente");
        }
        return RespostaAdquirente.aprovada(String.valueOf(nsu.incrementAndGet()));
    }

    @Override
    public void cancelar(String nsu) { cancelados.add(nsu); }

    public List<String> cancelados() { return Collections.unmodifiableList(cancelados); }
}
