package br.com.fitpay.pagamento.provedor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * API do banco que registra os boletos.
 * Em produção seria um cliente HTTP; aqui há uma versão simulada.
 */
public interface BancoClient {

    Boleto registrar(BigDecimal valor, LocalDate vencimento, String pagador);

    void baixar(String nossoNumero);
}
