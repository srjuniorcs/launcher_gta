# GTA SGNT RJ Launcher V2

Versão visual V2 do launcher oficial GTA SGNT RJ.

## O que mudou
- Visual em tela cheia usando a arte oficial SGNT como fundo.
- IP/porta continuam fixos no código e **não aparecem na interface**.
- Status automático `ONLINE/OFFLINE`.
- Contador de jogadores `online/máximo`, no estilo de launcher de servidor.
- Sem botão gigante de atualizar: toque no próprio status para consultar novamente.
- Campo de nick salvo automaticamente.
- Botão único **JOGAR**.
- Nome instalado: **GTA SGNT RJ**.
- Ícone do aplicativo usando a logo SGNT.
- Instagram/TikTok/YouTube discretos no rodapé.

## Servidor interno
`51.222.193.109:7777`

Esse endereço não é exibido para o jogador.

## Compilar no GitHub
O workflow `.github/workflows/build-apk.yml` já está incluído. Envie/substitua os arquivos no repositório e abra **Actions > Gerar/Build SGNT Launcher APK > Run workflow**.

O APK sai no artefato `GTA-SGNT-RJ-Launcher`.

## Cliente do jogo
Nesta V2 o botão JOGAR ainda usa a ponte temporária para o cliente Eagle quando instalado. O próximo passo é substituir essa ponte pela Activity do cliente próprio SGNT/SA-MP dentro do mesmo projeto.
