# GTA SGNT RJ Launcher

Projeto Android original para o GTA SGNT RJ.

## Configuração atual
- Servidor: 51.222.193.109:7777
- Redes: @gtasaogoncalo
- Discord: não incluído
- Tema: vermelho/preto
- Logo: SGNT fornecida pelo proprietário do servidor

## V1
A V1 implementa:
- tela própria SGNT;
- nick persistente;
- consulta SA:MP UDP para status e contagem de jogadores;
- servidor único/fixo;
- links sociais;
- botão JOGAR preparado para integração com o cliente-base.

## Importante
Este projeto não inclui arquivos proprietários do GTA San Andreas nem distribui os binários internos do Eagle Launcher.
A integração final com o cliente do jogo deve usar um cliente que o responsável pelo projeto tenha direito de distribuir.

## Build
Abra no Android Studio com JDK 17 e SDK 35 e execute `assembleRelease`/`assembleDebug`.
Também há um workflow do GitHub Actions em `.github/workflows/build-apk.yml`.
