@echo off
rem Sobe o FitPay: compila o núcleo e o back-end, inicia a API Java e abre a tela no navegador.
rem Uso: start.bat [porta]     (padrão 8080)
setlocal EnableDelayedExpansion
chcp 65001 >nul

set "PORTA=%~1"
if "%PORTA%"=="" set "PORTA=8080"
set "RAIZ=%~dp0"
set "RAIZ=%RAIZ:~0,-1%"
set "SAIDA=%RAIZ%\out"
set "URL=http://localhost:%PORTA%"

cd /d "%RAIZ%"

rem 1. Java instalado e na versão certa?
where javac >nul 2>&1 || goto :semjava
where java >nul 2>&1 || goto :semjava
set "VERSAO=0"
for /f "tokens=2 delims=. " %%v in ('javac -version 2^>^&1') do set "VERSAO=%%v"
if %VERSAO% LSS 17 (
  echo O FitPay precisa do JDK 17 ou mais novo ^(encontrei o %VERSAO%^).
  exit /b 1
)

rem 2. Porta livre?
powershell -NoProfile -Command "if (Get-NetTCPConnection -LocalPort %PORTA% -State Listen -ErrorAction SilentlyContinue) { exit 1 }"
if errorlevel 1 (
  echo A porta %PORTA% já está em uso. Se o FitPay já estiver rodando, abra %URL%
  echo Ou rode em outra porta: start.bat 8081
  exit /b 1
)

rem 3. Compila o núcleo (src\main\java) e o back-end (servidor\src\main\java)
echo Compilando...
if exist "%SAIDA%" rmdir /s /q "%SAIDA%"
set "FONTES=%RAIZ%\.fontes.txt"
if exist "%FONTES%" del "%FONTES%"
for /r "src\main\java" %%f in (*.java) do (
  set "F=%%f"
  >>"%FONTES%" echo "!F:\=/!"
)
for /r "servidor\src\main\java" %%f in (*.java) do (
  set "F=%%f"
  >>"%FONTES%" echo "!F:\=/!"
)
javac --release 17 -encoding UTF-8 -d "%SAIDA%" @"%FONTES%"
if errorlevel 1 (
  del "%FONTES%" 2>nul
  echo A compilação falhou. Veja a mensagem acima.
  exit /b 1
)
del "%FONTES%" 2>nul

rem 4. Abre a tela no navegador assim que a API responder (até ~15 s)
start "" /b powershell -NoProfile -Command "for ($i = 0; $i -lt 50; $i++) { try { (New-Object Net.Sockets.TcpClient('127.0.0.1', %PORTA%)).Close(); Start-Process '%URL%'; exit } catch { Start-Sleep -Milliseconds 300 } }"

rem 5. Inicia o servidor (Ctrl+C para parar)
echo Tudo no ar. O selo no topo da tela deve mostrar "Back-end Java". Ctrl+C para parar.
java -cp "%SAIDA%" br.com.fitpay.servidor.ServidorFitPay %PORTA% localhost
exit /b %errorlevel%

:semjava
echo Não achei o Java (JDK). Instale o JDK 17 ou mais novo e rode de novo.
exit /b 1
