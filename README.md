# Naruto Ninja Wars

Addon de guerras entre as cinco grandes vilas ninja para **Minecraft 1.20.1 + Minecraft Forge 47.2.0**. O projeto está escrito em Java 17 e foi organizado para publicação e colaboração pelo GitHub.

> **Estado do projeto:** código-fonte inicial. A compilação e os testes dentro do Minecraft ainda precisam ser executados em um ambiente com Gradle, Forge e as dependências necessárias.

## Recursos

- Cinco vilas: Konoha, Suna, Kiri, Kumo e Iwa.
- Detecção automática das estruturas de Konoha, Suna e Kumo geradas pelo Narutoto; o centro é registrado ao carregar o chunk da estrutura.
- Exércitos compostos pelas entidades reais Genin, Chunin, Jonin e ANBU do Narutoto.
- Guarnições de ninjas com cinco níveis de rank.
- Sistema de guerras com ondas de invasores, vitória, derrota e trégua; guerras começam apenas por comando administrativo.
- Líderes únicos: Minato em Konoha, Gaara em Suna e um Jōnin de Kumo.
- Comando `/ninjawars participar` teleporta o jogador ao campo de batalha.
- Konoha não causa dano a jogadores e jogadores não causam dano aos ninjas de Konoha.
- A geração de reforços é desativada quando todos os blocos de construção dentro dos limites detectados da vila forem removidos.
- Jutsus temáticos para cada vila.
- Missões, recompensas em esmeraldas e progressão de XP.
- Comandos administrativos para gerenciar vilas e guerras.

## Requisitos

- Minecraft **1.20.1**.
- Minecraft Forge **47.2.0** (linha Forge 47).
- JDK **17**.
- Gradle compatível com o plugin ForgeGradle 6.
- Narutoto **0.4.1 ou superior compatível com Forge 47** é dependência obrigatória.

## Como compilar

1. Instale o JDK 17 e o Gradle.
2. Clone este repositório:
   ```bash
   git clone https://github.com/SEU-USUARIO/ninjawars.git
   cd ninjawars
   ```
3. Gere o Gradle Wrapper (uma única vez, caso ainda não esteja versionado):
   ```bash
   gradle wrapper --gradle-version 8.8
   ```
4. Compile:
   - Windows: `gradlew.bat build`
   - Linux/macOS: `./gradlew build`

O arquivo `.jar` será gerado em `build/libs/`.

> O repositório ainda não inclui os arquivos do Gradle Wrapper. Depois de gerá-los, versione `gradlew`, `gradlew.bat` e `gradle/wrapper/` para que qualquer pessoa consiga compilar sem instalar Gradle manualmente.

## Instalação

Copie o `.jar` gerado para a pasta `mods` da instalação Minecraft Forge 1.20.1. Instale também as dependências exigidas pelo seu ambiente/modpack.

## Comandos

| Comando | Permissão | Descrição |
|---|---:|---|
| `/ninjawars list` | Todos | Lista vilas fundadas e guerras ativas |
| `/ninjawars status` | Todos | Mostra vila, rank e XP do jogador |
| `/ninjawars participar` | Todos | Teleporta para o campo de batalha de uma guerra ativa |
| `/ninjawars join <vila>` | Todos | Entra para uma vila |
| `/ninjawars mission` | Todos | Aceita ou consulta uma missão |
| `/ninjawars village set <vila>` | OP (nível 2) | Registra a posição central de uma vila do Narutoto |
| `/ninjawars village remove <vila>` | OP (nível 2) | Remove a vila do registro; os blocos permanecem |
| `/ninjawars war declare <atacante> <defensor>` | OP (nível 2) | Declara guerra entre duas vilas fundadas |
| `/ninjawars war peace <vila1> <vila2>` | OP (nível 2) | Encerra a guerra |
| `/ninjawars spawn <vila> <rank>` | OP (nível 2) | Cria um ninja real do Narutoto para testes; rank de 0 a 4 |

Vilas aceitas: `konoha`, `suna`, `kiri`, `kumo` e `iwa`.

### Vilas automáticas do Narutoto

As estruturas de Konoha, Suna e Kumo são detectadas automaticamente quando o chunk da estrutura é carregado. Não é necessário usar `village set` nessas três vilas.

1. Para encontrar uma delas, use `/locate structure narutoto:konoha_village`, `/locate structure narutoto:suna_village` ou `/locate structure narutoto:kumo_village`.
2. Ao carregar o chunk da estrutura, o Ninja Wars registra automaticamente o centro e cria o líder único da vila.
3. Kiri e Iwa têm ninjas no Narutoto 0.4.1, mas não têm estruturas de vila na geração de mundo; elas ainda podem ser registradas manualmente com `/ninjawars village set kiri` ou `/ninjawars village set iwa`.
4. Declare a guerra: `/ninjawars war declare konoha suna`.
5. Para entrar no campo de batalha, use `/ninjawars participar`.

O comando antigo `/ninjawars village create <vila>` continua aceito como alias, mas agora apenas registra a posição do jogador: não constrói casas ou muralhas.

### Líderes e destruição das vilas

- Konoha recebe Minato (`narutoto:minato_npc`), Suna recebe Gaara (`narutoto:gaara_npc`) e Kumo recebe um único `narutoto:kumo_jonin` como líder.
- O estado de líder criado é salvo no mundo para evitar duplicatas após reiniciar.
- A vila só é marcada como destruída quando todos os chunks dentro dos limites da estrutura estão carregados e não resta bloco de construção reconhecido dentro desses limites. Nesse estado, os reforços e novas ondas são bloqueados.
- Ninjas de Konoha e jogadores não causam dano uns aos outros.

### Exércitos do Narutoto

O Ninja Wars cria as entidades registradas pelo Narutoto (`*_genin`, `*_chunin`, `*_jonin`; Konoha também usa `konoha_anbu`). O líder de Konoha e o de Suna usam as entidades de personagem Minato e Gaara. Os ranks de combate continuam usando as entidades Genin, Chunin, Jonin e ANBU do Narutoto, que são direcionadas contra os soldados da vila inimiga durante uma guerra.

## Estrutura

```text
.
├── src/main/java/com/ninjawars/   # Código Java
├── src/main/resources/            # Metadados Forge e pack.mcmeta
├── build.gradle
├── gradle.properties
├── settings.gradle
├── LICENSE
└── README.md
```

## Desenvolvimento

- Use Java 17 para manter compatibilidade com Minecraft 1.20.1.
- Faça alterações em branches e abra Pull Requests.
- Ao reportar um bug, inclua a versão do Minecraft, Forge, Java, os mods instalados e o log relevante (`latest.log` ou crash report).
- Não envie `build/`, `.gradle/`, pastas `run/` ou arquivos de IDE.

## Limitações conhecidas

- O sistema de guerra agora usa entidades reais do Narutoto 0.4.1, não apenas skins aplicadas ao NPC genérico do Ninja Wars.
- Kiri e Iwa não possuem estruturas de vila na geração de mundo desta versão do Narutoto; seus ninjas estão disponíveis normalmente.
- A verificação de destruição depende de todos os chunks da estrutura estarem carregados ao mesmo tempo; a detecção é feita periodicamente para evitar forçar carregamento de chunks.
- O projeto não foi compilado nem testado em jogo nesta preparação.
- O equilíbrio das guerras, a navegação dos invasores e a compatibilidade com outros mods precisam de testes em servidor e cliente.

## Licença

Este projeto está sob a licença MIT. Consulte [`LICENSE`](LICENSE).

## Compilação automática pelo GitHub Actions

O workflow em `.github/workflows/build.yml` compila o mod automaticamente em pushes para `main`/`master`, em Pull Requests e quando iniciado manualmente.

Para baixar o JAR:
1. Abra a aba **Actions** do repositório.
2. Selecione a execução mais recente de **Build Ninja Wars**.
3. Aguarde o job terminar com sucesso.
4. Baixe o artefato `ninjawars-minecraft-1.20.1` na seção **Artifacts**.

O workflow usa Temurin Java 17 e Gradle 8.8. O JAR é produzido em `build/libs/` e disponibilizado como artefato por 14 dias.
