package br.com.fitpay.dominio;

import java.util.Objects;

/**
 * Aluno da rede de academias.
 *
 * @param matricula       identificador do aluno
 * @param nome            nome completo
 * @param plano           plano contratado (define mensalidade e valor anual)
 * @param convenioEmpresa se o aluno tem convênio empresarial (15% na mensalidade)
 * @param diaVencimento   dia do mês em que a mensalidade vence (1 a 28)
 * @param bloqueado       se a matrícula está bloqueada para novas cobranças
 * @param formaPreferida  meio de pagamento escolhido pelo aluno
 * @param tokenCartao     token do cartão salvo na adquirente (pode ser nulo)
 */
public record Aluno(String matricula, String nome, Plano plano, boolean convenioEmpresa,
                    int diaVencimento, boolean bloqueado, FormaPagamento formaPreferida,
                    String tokenCartao) {

    public Aluno {
        Objects.requireNonNull(matricula, "matricula");
        Objects.requireNonNull(nome, "nome");
        Objects.requireNonNull(plano, "plano");
        Objects.requireNonNull(formaPreferida, "formaPreferida");
        if (diaVencimento < 1 || diaVencimento > 28) {
            throw new IllegalArgumentException("Dia de vencimento deve estar entre 1 e 28");
        }
    }

    public boolean temConvenioEmpresa() { return convenioEmpresa; }

    public boolean estaBloqueado() { return bloqueado; }

    @Override
    public String toString() {
        return nome + " (" + matricula + ")";
    }
}
