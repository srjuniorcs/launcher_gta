# GTA SGNT RJ Launcher 1.0.0 — teste de login

Projeto pronto para subir na raiz do GitHub.

## O que esta versão testa
- Status ONLINE/OFFLINE e jogadores do servidor SGNT.
- STATUS DO JOGO: NÃO PRONTO / PRONTO PARA JOGAR.
- Preparação e extração da DATA bootstrap.
- Nick salvo.
- Botão JOGAR tenta abrir, nesta ordem: ALYN, cliente com.rockstargames.gtasa e Eagle.
- Envia nick e servidor fixo por extras + samp://.

## Importante
Esta build é o teste de ligação do launcher com um cliente já instalado. Ela ainda NÃO incorpora os binários do GTA/SA:MP dentro do mesmo APK. O repositório SAMP-MOBILE fornecido contém a source C++ da libsamp, mas não contém o projeto Java completo do GTASA necessário para recompilar tudo como um único aplicativo sem reaproveitar binários proprietários.

## GitHub Actions
1. Extraia este ZIP.
2. Suba TODO o conteúdo na raiz do repositório.
3. Abra Actions > Gerar APK GTA SGNT > Run workflow.
4. Baixe o artifact e instale app-debug.apk.

Versão do app: 1.0.0
