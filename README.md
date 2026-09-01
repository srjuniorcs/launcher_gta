# GTA SGNT RJ Launcher V3 — DATA Manager

Projeto Android pronto para subir no GitHub e compilar pelo GitHub Actions.

## O que esta V3 faz
- Consulta ONLINE/OFFLINE e jogadores do servidor, sem exibir IP.
- Mostra `STATUS DO JOGO: NÃO PRONTO / PRONTO PARA JOGAR`.
- Botão `PREPARAR JOGO`.
- Barra de progresso para baixar/carregar, extrair e verificar DATA.
- Salva o nick.
- Nas próximas aberturas reconhece a DATA preparada e mostra `JOGAR`.
- Faz até 3 tentativas na consulta UDP para reduzir falso OFFLINE.

## DATA real
No arquivo:
`app/src/main/java/br/com/gtasgntrj/launcher/MainActivity.java`

Existe:
`private static final String DATA_URL = "";`

Cole ali um **link direto para um arquivo .zip**. Não use página do MediaFire/GitHub; precisa ser o URL que baixa o ZIP diretamente.

Enquanto `DATA_URL` estiver vazio, o APK usa `assets/sgnt_bootstrap_data.zip` para permitir testar TODO o fluxo de `NÃO PRONTO -> PREPARAR -> EXTRAIR -> PRONTO`. Esse pacote bootstrap NÃO é a DATA completa do GTA.

## Importante sobre JOGAR
A V3 implementa o gerenciador de DATA, mas o cliente SA:MP próprio ainda não foi incorporado ao APK. O botão JOGAR mantém a ponte temporária com Eagle quando instalado. A integração nativa do cliente é uma etapa separada.

## GitHub Actions
1. Extraia este ZIP.
2. Envie o conteúdo da pasta para a raiz do repositório.
3. Abra Actions > Build SGNT Launcher APK > Run workflow.
4. Baixe o artifact e instale `app-debug.apk`.
