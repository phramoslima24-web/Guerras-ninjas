# Naruto Ninja Wars

Addon de guerras entre as cinco grandes vilas ninja para **Minecraft 1.20.1 + Minecraft Forge 47.2.0**. O projeto está escrito em Java 17 e foi organizado para publicação e colaboração pelo GitHub.

> **Estado do projeto:** código-fonte inicial. A compilação e os testes dentro do Minecraft ainda precisam ser executados em um ambiente com Gradle, Forge e as dependências necessárias.

## Recursos

- Cinco vilas: Konoha, Suna, Kiri, Kumo e Iwa.
- Integração com as vilas geradas pelo Narutoto: registre o centro de cada vila existente, sem construir uma cidade substituta.
- Exércitos compostos pelas entidades reais Genin, Chunin, Jonin e ANBU do Narutoto.
- Guarnições de ninjas com cinco níveis de rank.
- Sistema de guerras com ondas de invasores, vitória, derrota e trégua.
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
| `/ninjawars join <vila>` | Todos | Entra para uma vila |
| `/ninjawars mission` | Todos | Aceita ou consulta uma missão |
| `/ninjawars village set <vila>` | OP (nível 2) | Registra a posição central de uma vila do Narutoto |
| `/ninjawars village remove <vila>` | OP (nível 2) | Remove a vila do registro; os blocos permanecem |
| `/ninjawars war declare <atacante> <defensor>` | OP (nível 2) | Declara guerra entre duas vilas fundadas |
| `/ninjawars war peace <vila1> <vila2>` | OP (nível 2) | Encerra a guerra |
| `/ninjawars spawn <vila> <rank>` | OP (nível 2) | Cria um ninja real do Narutoto para testes; rank de 0 a 4 |

Vilas aceitas: `konoha`, `suna`, `kiri`, `kumo` e `iwa`.

### Usar as vilas do Narutoto

1. Localize uma vila do Narutoto. Nesta versão, os arquivos de geração de estruturas incluem Konoha, Suna e Kumo:
   - `/locate structure narutoto:konoha_village`
   - `/locate structure narutoto:suna_village`
   - `/locate structure narutoto:kumo_village`
2. Vá até o centro da vila encontrada e registre-a:
   - `/ninjawars village set konoha` (ou `suna` / `kumo`)
3. Repita para cada vila que participará da guerra.
4. Para Kiri e Iwa, o Narutoto 0.4.1 tem ninjas e equipamentos, mas não inclui estruturas de vila na geração de mundo; registre manualmente o centro de uma base que você construir ou escolher.
5. Declare a guerra: `/ninjawars war declare konoha suna`.

O comando antigo `/ninjawars village create <vila>` continua aceito como alias, mas agora apenas registra a posição do jogador: não constrói casas ou muralhas.

### Exércitos do Narutoto

O Ninja Wars cria as entidades registradas pelo Narutoto (`*_genin`, `*_chunin`, `*_jonin`; Konoha também usa `konoha_anbu`). Como o Narutoto 0.4.1 não possui uma entidade Kage, o rank Kage usa o modelo/entidade Jonin. O addon direciona os ninjas aliados para atacar soldados da vila inimiga durante uma guerra.

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
