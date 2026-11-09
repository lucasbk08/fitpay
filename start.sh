#!/usr/bin/env bash
# Sobe o FitPay: compila o núcleo e o back-end, inicia a API Java e abre a tela no navegador.
# Uso: ./start.sh [porta]     (padrão 8080)
set -euo pipefail

PORTA="${1:-8080}"
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SAIDA="$RAIZ/out"
URL="http://localhost:$PORTA"

cd "$RAIZ"

# 1. Java instalado e na versão certa?
if ! command -v javac >/dev/null 2>&1 || ! command -v java >/dev/null 2>&1; then
  echo "Não achei o Java (JDK). Instale o JDK 17 ou mais novo e rode de novo."
  exit 1
fi
VERSAO="$(javac -version 2>&1 | grep -E '^javac ' | sed -E 's/^javac ([0-9]+).*/\1/')"
if [ "${VERSAO:-0}" -lt 17 ]; then
  echo "O FitPay precisa do JDK 17 ou mais novo (encontrei o $VERSAO)."
  exit 1
fi

# 2. Porta livre?
if (exec 3<>"/dev/tcp/127.0.0.1/$PORTA") 2>/dev/null; then
  echo "A porta $PORTA já está em uso. Se o FitPay já estiver rodando, abra $URL"
  echo "Ou rode em outra porta: ./start.sh 8081"
  exit 1
fi

# 3. Compila o núcleo (src/main/java) e o back-end (servidor/src/main/java)
echo "Compilando..."
rm -rf "$SAIDA"
find src/main/java servidor/src/main/java -name "*.java" > "$RAIZ/.fontes.txt"
javac --release 17 -encoding UTF-8 -d "$SAIDA" @"$RAIZ/.fontes.txt"
rm -f "$RAIZ/.fontes.txt"

# 4. Inicia o servidor
java -cp "$SAIDA" br.com.fitpay.servidor.ServidorFitPay "$PORTA" &
PID=$!
trap 'echo; echo "Parando o FitPay..."; kill $PID 2>/dev/null; wait $PID 2>/dev/null; exit 0' INT TERM

# 5. Espera a API responder (até ~15 s)
for _ in $(seq 1 50); do
  if (exec 3<>"/dev/tcp/127.0.0.1/$PORTA") 2>/dev/null; then break; fi
  if ! kill -0 $PID 2>/dev/null; then echo "O servidor não subiu. Veja a mensagem acima."; exit 1; fi
  sleep 0.3
done

# 6. Abre a tela no navegador
if command -v xdg-open >/dev/null 2>&1; then xdg-open "$URL" >/dev/null 2>&1 || true
elif command -v open >/dev/null 2>&1; then open "$URL"
elif command -v cmd.exe >/dev/null 2>&1; then cmd.exe /c start "" "$URL" >/dev/null 2>&1 || true
else echo "Abra no navegador: $URL"
fi

echo "Tudo no ar. O selo no topo da tela deve mostrar \"Back-end Java\". Ctrl+C para parar."
wait $PID
