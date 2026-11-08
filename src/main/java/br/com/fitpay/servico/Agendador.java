package br.com.fitpay.servico;

import br.com.fitpay.cobranca.CobrancaNaoPermitidaException;
import br.com.fitpay.comprovante.Comprovante;
import br.com.fitpay.dominio.Aluno;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Ator de sistema: no dia de vencimento de cada aluno, dispara a cobrança
 * da mensalidade no meio preferido dele.
 */
public class Agendador {

    private final ServicoCobranca servico;
    private final List<Aluno> alunos;

    public Agendador(ServicoCobranca servico, List<Aluno> alunos) {
        this.servico = servico;
        this.alunos = List.copyOf(alunos);
    }

    /** Resultado de uma rodada do agendador. */
    public record Execucao(List<Comprovante> comprovantes, List<String> ignorados) { }

    public Execucao executar(LocalDate hoje) {
        YearMonth competencia = YearMonth.from(hoje);
        List<Comprovante> comprovantes = new ArrayList<>();
        List<String> ignorados = new ArrayList<>();
        for (Aluno aluno : alunos) {
            if (aluno.diaVencimento() != hoje.getDayOfMonth()) {
                continue;
            }
            try {
                comprovantes.add(servico.cobrarMensalidade(aluno, competencia));
            } catch (CobrancaNaoPermitidaException e) {
                ignorados.add(aluno.nome() + ": " + e.getMessage());
            }
        }
        return new Execucao(comprovantes, ignorados);
    }
}
