# 🤖 Gehirn Discord Bot

Sistema completo de moderação para Discord desenvolvido em Java, com foco em gerenciamento de warns, mensagens automáticas e controle de servidor.

## 📋 Índice

- [Características](#-características)
- [Tecnologias](#-tecnologias)
- [Pré-requisitos](#-pré-requisitos)
- [Instalação](#-instalação)
- [Configuração](#-configuração)
- [Comandos Disponíveis](#-comandos-disponíveis)
- [Sistema de Warns](#-sistema-de-warns)
- [Estrutura do Projeto](#-estrutura-do-projeto)
- [Banco de Dados](#-banco-de-dados)
- [Logs](#-logs)
- [Contribuindo](#-contribuindo)

## ✨ Características

### Sistema de Moderação
- **Sistema de Warns Progressivo**: 6 níveis de advertência com punições automáticas
- **Expiração Inteligente**: Warns expiram automaticamente com o tempo
- **Timeouts Automáticos**: Aplicados progressivamente conforme warns acumulam
- **Ban Automático**: Após 6 warns, usuário é banido permanentemente

### Mensagens Automáticas
- **Boas-vindas**: Mensagem personalizada para novos membros
- **Despedidas**: Detecta saídas, kicks e bans com mensagens específicas
- **Auto-role**: Atribuição automática de cargo para novos membros
- **Mensagens de Setup**: Informações e regras do servidor em embeds

### Gerenciamento
- **Limpeza de Mensagens**: Comando para deletar até 1000 mensagens
- **Sistema de Debug**: Verificação completa do estado do bot e banco de dados
- **Logs Detalhados**: Sistema de logging em arquivo e console
- **Health Check**: Monitoramento de conexão com banco de dados

## 🛠 Tecnologias

- **Java 20**
- **JDA 5.2.1** (Java Discord API)
- **PostgreSQL 42.7.1**
- **HikariCP 5.1.0** (Connection Pooling)
- **SLF4J 2.0.9** (Logging)
- **Maven** (Build Tool)

## 📦 Pré-requisitos

- Java 20 ou superior
- PostgreSQL 12 ou superior
- Maven 3.6 ou superior
- Servidor Discord com permissões administrativas
- Token de bot do Discord Developer Portal

## 🚀 Instalação

### 1. Clone o Repositório

```bash
git clone https://github.com/seu-usuario/gehirn-discord-bot.git
cd gehirn-discord-bot
```

### 2. Configure o Banco de Dados

Execute o script SQL de setup como superusuário do PostgreSQL:

```bash
psql -U postgres -f setup_database.sql
```

O script irá:
- Criar o banco de dados `discord_bot`
- Criar o usuário `bot_user`
- Criar as tabelas `warns` e `logs`
- Configurar índices e permissões

**⚠️ IMPORTANTE**: Altere a senha padrão no arquivo `setup_database.sql` antes de executar!

### 3. Configure as Variáveis de Ambiente

Crie um arquivo `.env` ou configure as variáveis de ambiente:

```bash
# Discord
DISCORD_BOT_TOKEN=seu_token_aqui
DISCORD_GUILD_ID=id_do_seu_servidor

# Canais
WELCOME_CHANNEL_ID=id_canal_boas_vindas
EXIT_CHANNEL_ID=id_canal_saidas
LOG_CHANNEL_ID=id_canal_logs

# Cargo Automático
AUTO_ROLE_ID=id_cargo_automatico

# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=discord_bot
DB_USER=bot_user
DB_PASSWORD=sua_senha_aqui

# Debug (opcional)
DEBUG=false
```

**Alternativa**: Crie um arquivo `config.properties` na raiz do projeto com as mesmas configurações.

### 4. Compile o Projeto

```bash
mvn clean package
```

### 5. Execute o Bot

```bash
java -jar target/discord-bot-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## ⚙️ Configuração

### Permissões Necessárias do Bot

Ao adicionar o bot ao servidor, certifique-se de conceder as seguintes permissões:

- `VIEW_CHANNELS` - Ver canais
- `SEND_MESSAGES` - Enviar mensagens
- `EMBED_LINKS` - Inserir links
- `MANAGE_MESSAGES` - Gerenciar mensagens
- `MANAGE_ROLES` - Gerenciar cargos
- `MODERATE_MEMBERS` - Aplicar timeout
- `KICK_MEMBERS` - Expulsar membros
- `BAN_MEMBERS` - Banir membros
- `VIEW_AUDIT_LOG` - Ver log de auditoria

### Gateway Intents

O bot requer os seguintes intents:
- `GUILD_MEMBERS`
- `GUILD_MESSAGES`
- `GUILD_MODERATION`
- `GUILD_MESSAGE_REACTIONS`
- `MESSAGE_CONTENT`

Habilite-os no Discord Developer Portal → Bot → Privileged Gateway Intents.

## 📝 Comandos Disponíveis

### Comandos Públicos

| Comando | Descrição | Uso |
|---------|-----------|-----|
| `/ping` | Testa se o bot está online | `/ping` |
| `/warnstatus` | Mostra seus warns ativos | `/warnstatus [user:@usuario]` |

### Comandos de Moderação

| Comando | Descrição | Permissão | Uso |
|---------|-----------|-----------|-----|
| `/warn` | Aplica um warn a um usuário | BAN_MEMBERS | `/warn user:@usuario motivo:"texto"` |
| `/warnclear` | Remove todos os warns de um usuário | BAN_MEMBERS | `/warnclear user:@usuario` |

### Comandos Administrativos

| Comando | Descrição | Permissão | Uso |
|---------|-----------|-----------|-----|
| `/setup` | Envia mensagens de info/regras | ADMINISTRATOR | `/setup tipo:info` ou `/setup tipo:regras` |
| `/nukar` | Apaga até 1000 mensagens | ADMINISTRATOR | `/nukar` |
| `/debug` | Mostra informações do sistema | ADMINISTRATOR | `/debug [action:status/reset/verify]` |

## ⚠️ Sistema de Warns

### Progressão de Punições

| Warns | Punição | Expiração |
|-------|---------|-----------|
| **1º warn** | ⚠️ Apenas aviso | 24 horas |
| **2º warn** | 🕐 Timeout de 10 minutos | 48 horas |
| **3º warn** | 🕐 Timeout de 1 hora | 7 dias |
| **4º warn** | 🕐 Timeout de 24 horas | 14 dias |
| **5º warn** | ⚠️ Timeout de 3 dias (último aviso) | 30 dias |
| **6º warn** | 🔨 **BAN PERMANENTE** | N/A |

### Características do Sistema

- **Expiração Automática**: Warns expiram após o período definido
- **Purge Automático**: Sistema limpa warns expirados do banco
- **Imunidade**: Bots e membros da staff não recebem warns
- **Histórico Completo**: Todos os warns são registrados mesmo após expirar
- **Validações**: Sistema verifica permissões antes de aplicar punições

### Exemplo de Uso

```
Moderador: /warn user:@Usuario motivo:"Spam no chat geral"

Bot: ⚠️ Warn aplicado com sucesso!
     👤 Usuário: @Usuario
     📝 Motivo: Spam no chat geral
     📊 Total de warns: 2/6
     ⚡ Punição: 🕐 Timeout de 10 minutos
```

## 📁 Estrutura do Projeto

```
src/main/java/com/bot/discordbot/
├── Main.java                          # Classe principal
├── commands/                          # Comandos do bot
│   ├── PingCommand.java
│   ├── NukarCommand.java
│   ├── DebugCommand.java
│   └── SetupCommand.java
├── config/                            # Configurações
│   ├── BotConfig.java
│   └── ServerMessages.java
├── database/                          # Banco de dados
│   ├── Database.java
│   └── DatabaseSetup.java
├── listeners/                         # Event listeners
│   └── WelcomeAndGoodbye.java
├── moderation/                        # Sistema de moderação
│   ├── ModerationConfig.java
│   └── warn/
│       ├── commands/
│       │   ├── WarnCommand.java
│       │   ├── WarnStatusCommand.java
│       │   └── WarnClearCommand.java
│       ├── dao/
│       │   └── WarnDAO.java
│       ├── model/
│       │   └── Warn.java
│       └── service/
│           └── WarnService.java
└── util/                              # Utilitários
    └── BotLogger.java
```

## 🗄️ Banco de Dados

### Tabelas

#### `warns`
```sql
CREATE TABLE warns (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(20) NOT NULL,
    moderator_id VARCHAR(20),
    reason TEXT,
    timestamp BIGINT NOT NULL,
    expires_at BIGINT NOT NULL
);
```

#### `logs`
```sql
CREATE TABLE logs (
    id SERIAL PRIMARY KEY,
    action_type VARCHAR(50) NOT NULL,
    user_id VARCHAR(20),
    moderator_id VARCHAR(20),
    details TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Pool de Conexões (HikariCP)

Configurações do pool:
- **Maximum Pool Size**: 10 conexões
- **Minimum Idle**: 2 conexões
- **Connection Timeout**: 30 segundos
- **Idle Timeout**: 10 minutos
- **Max Lifetime**: 30 minutos

## 📊 Logs

O sistema de logging registra todas as ações em:

### Console
Logs coloridos com emojis para fácil identificação:
- ℹ️ INFO - Informações gerais
- ✅ SUCCESS - Operações bem-sucedidas
- ⚠️ WARN - Avisos
- ❌ ERROR - Erros
- 🔍 DEBUG - Informações de debug (quando ativado)

### Arquivos
Logs salvos em `logs/bot-YYYY-MM-DD.log`

### Debug Mode
Ative o modo debug para logs detalhados:
```bash
DEBUG=true java -jar bot.jar
```

## 🔧 Manutenção

### Verificar Saúde do Sistema
```
/debug action:status
```

### Resetar Banco de Dados
```
/debug action:reset
```
**⚠️ ATENÇÃO**: Esta ação apaga TODOS os dados!

### Verificar Integridade
```
/debug action:verify
```

### Purgar Warns Expirados
Executado automaticamente, mas pode ser feito manualmente via código:
```java
WarnService.purgeExpiredWarns();
```

## 🐛 Troubleshooting

### Bot não conecta ao Discord
- Verifique se o token está correto
- Confirme que o bot está ativado no Developer Portal
- Verifique se os Gateway Intents estão habilitados

### Erro de conexão com PostgreSQL
- Confirme que o PostgreSQL está rodando
- Verifique as credenciais no `.env` ou `config.properties`
- Teste a conexão: `psql -U bot_user -d discord_bot`

### Warns não aplicam punições
- Verifique as permissões do bot no servidor
- Confirme que o cargo do bot está acima dos cargos dos membros
- Veja os logs para mensagens de erro

### Comandos não aparecem
- Aguarde até 1 hora para sincronização global
- Use comandos de guild para atualização instantânea
- Reinicie o bot após mudanças

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo `LICENSE` para mais detalhes.

## 👥 Contribuindo

Contribuições são bem-vindas! Por favor:

1. Fork o projeto
2. Crie uma branch para sua feature (`git checkout -b feature/NovaFeature`)
3. Commit suas mudanças (`git commit -m 'Adiciona nova feature'`)
4. Push para a branch (`git push origin feature/NovaFeature`)
5. Abra um Pull Request

## 📞 Suporte

Para suporte, abra uma issue no GitHub ou entre em contato através do servidor Discord.

---

**Desenvolvido com ☕ e Java**

*Gehirn, o fodão. 🤖*
