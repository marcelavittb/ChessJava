ChessJava

Uma aplicação em Java de jogo de xadrez com interface gráfica.

Índice

Visão Geral

Funcionalidades

Requisitos

Como executar

Estrutura do Projeto

Contribuições

Licença

Visão Geral

Esse projeto tem como objetivo implementar um jogo de xadrez completo em Java, com interface visual (GUI), permitindo ao usuário jogar em modo local. Ele inclui todas as peças, mecanismos de movimento, regras básicas, e uma interface gráfica para interagir.

Funcionalidades

Movimentação correta de todas as peças do xadrez

Interface gráfica para visualização do tabuleiro e das peças

Interface de usuário para iniciar o jogo

Respeito às regras básicas (xeque, captura de peças, turnos, etc.)

Recursos de carregamento de recursos externos (por exemplo, imagens das peças)

Requisitos

Para compilar e executar o projeto, é necessário:

Java JDK 8 ou superior

Sistema operacional com suporte a GUI (Windows, Linux, macOS)

Ambiente que suporte execução de arquivos .java via terminal ou IDE

Como executar
Via terminal

Clone o repositório ou baixe os arquivos para sua máquina.

git clone https://github.com/marcelavittb/ChessJava.git
cd ChessJava


Compile os arquivos fonte em Java.
No Windows PowerShell, por exemplo:

Remove-Item -Recurse -Force .\out -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force .\out | Out-Null
$files = Get-ChildItem -Recurse -Path .\src -Filter *.java | ForEach-Object FullName
javac -Xlint:all -encoding UTF-8 -d out $files


Execute a aplicação apontando para a classe principal.

java -cp "out;resources" view.ChessGUI


Obs.: o classpath inclui out (onde ficou a junção dos .class) e resources (caso tenha imagens ou outros arquivos externos requeridos).

Via IDE

Abra o projeto como um projeto Java

Certifique-se de que as pastas src e resources estejam marcadas corretamente

Configure a classe principal: view.ChessGUI

Execute a partir da IDE

Estrutura do Projeto
ChessJava/
├── src/                # Código-fonte em Java
│   ├── view/           # Interface gráfica / GUI
│   ├── model/          # Lógica do jogo, regras, movimentação etc.
│   └── controller/     # Separação de responsabilidades, se aplicável
├── resources/          # Imagens, arquivos auxiliares
├── out/                # Diretório de saída para arquivos compilados (não versionado)
├── README.md           # Este documento
└── …                   # Outros arquivos de configuração

Contribuições

Contribuições são bem-vindas! Se quiser ajudar:

Crie um fork do projeto

Crie uma nova branch com sua feature ou correção (git checkout -b minha-feature)

Faça commits com mensagens claras

Abra um pull request descrevendo o que você fez

Sugestões de melhorias podem incluir:

Implementar detecção de xeque-mate

Animações ou visual mais elaborado para peças

Modo de rede (jogar online)

IA (jogador de computador)

Melhor tratamento de eventos inválidos

Licença

Este projeto está sob a licença MIT (ou especificar outra, se houver). Se ainda não tiver definido uma, sugestiono MIT para facilitar reutilização.