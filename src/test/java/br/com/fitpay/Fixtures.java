package br.com.fitpay;

import br.com.fitpay.comprovante.Comprovante;
import br.com.fitpay.dominio.Aluno;
import br.com.fitpay.dominio.FormaPagamento;
import br.com.fitpay.dominio.Plano;
import br.com.fitpay.dominio.Transacao;
import br.com.fitpay.notificacao.Notificador;
import br.com.fitpay.pagamento.MeioPagamento;
import br.com.fitpay.pagamento.ResultadoPagamento;

import java.util.ArrayList;
import java.util.List;

/** Dados e objetos falsos compartilhados pelos testes. */
public final class Fixtures {

    private Fixtures() { }

    public static Aluno ana() {
        return new Aluno("2024-0187", "Ana Souza", Plano.BLACK, true, 10, false,
                FormaPagamento.CARTAO, "tok_ana");
    }

    public static Aluno bruno() {
        return new Aluno("2025-0342", "Bruno Lima", Plano.BASICO, false, 5, false,
                FormaPagamento.PIX, "tok_bruno");
    }

    public static Aluno carlaBloqueada() {
        return new Aluno("2023-0099", "Carla Mendes", Plano.BLACK, false, 10, true,
                FormaPagamento.BOLETO, null);
    }

    /** Meio de pagamento falso: devolve um resultado fixo e guarda o que recebeu. */
    public static final class MeioFalso implements MeioPagamento {
        private final ResultadoPagamento resposta;
        private final boolean parcela;
        public final List<Transacao> executadas = new ArrayList<>();

        public MeioFalso(ResultadoPagamento resposta, boolean parcela) {
            this.resposta = resposta;
            this.parcela = parcela;
        }

        public static MeioFalso aprovando() { return new MeioFalso(ResultadoPagamento.aprovado("OK-1"), true); }
        public static MeioFalso semParcelamento() { return new MeioFalso(ResultadoPagamento.pendente("P-1", "aguardando"), false); }

        @Override public ResultadoPagamento executar(Transacao t) { executadas.add(t); return resposta; }
        @Override public void estornar(String idExterno) { }
        @Override public boolean aceitaParcelamento() { return parcela; }
    }

    /** Notificador que só guarda o que seria enviado. */
    public static final class NotificadorEmMemoria implements Notificador {
        public final List<Comprovante> enviados = new ArrayList<>();

        @Override public void enviar(Aluno aluno, Comprovante c) { enviados.add(c); }
    }
}
