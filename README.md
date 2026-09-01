# GTA SGNT RJ 1.0.0 — Instalador único

Projeto de teste integrado para GitHub Actions.

## O que este projeto faz

- Gera **um único APK para o jogador baixar**: `GTA_SGNT_RJ_1.0.0.apk`.
- Mantém o launcher SGNT com servidor ONLINE/OFFLINE e jogadores online.
- O botão **PREPARAR JOGO** solicita acesso aos arquivos quando necessário.
- O próprio instalador prepara o componente GTA/SA:MP interno, sem depender de ALYN ou Eagle.
- Baixa a DATA completa de 623 MB pelo link MediaFire configurado no launcher.
- Mostra progresso de download e extração.
- Grava o nick, servidor `51.222.193.109` e porta `7777` em `settings.ini`.
- A biblioteca `libsamp.so` é recompilada no GitHub Actions a partir da source SAMP-MOBILE enviada para o projeto.
- O `libsamp.so` foi alterado para usar `host` e `port` do `settings.ini`, em vez do servidor antigo hardcoded.
- Versão do launcher: **1.0.0**.

## Como compilar

1. Extraia este ZIP.
2. Envie **o conteúdo da pasta** para a raiz do seu repositório GitHub.
3. Abra **Actions**.
4. Entre em **Build GTA SGNT RJ 1.0.0**.
5. Clique em **Run workflow**.
6. Quando ficar verde, baixe o artifact **GTA-SGNT-RJ-1.0.0**.
7. Extraia o artifact e instale `GTA_SGNT_RJ_1.0.0.apk` no Android.

## Primeiro teste

Na primeira abertura:

1. coloque um nick;
2. aperte **PREPARAR JOGO**;
3. se o Android pedir acesso a todos os arquivos, permita e volte;
4. confirme a instalação do componente do jogo quando o Android solicitar;
5. volte ao launcher;
6. o launcher deve baixar a DATA, extrair e chegar em **PRONTO PARA JOGAR**;
7. aperte **JOGAR**.

## Observação de teste

O `client-test.keystore` incluído serve somente para manter a mesma assinatura do componente interno durante os testes. Para distribuição definitiva, a assinatura deve ser movida para GitHub Secrets/keystore privado.
