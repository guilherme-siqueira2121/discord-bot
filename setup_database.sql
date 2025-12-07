-- ================================================
-- Setup do Banco de Dados Discord Bot
-- ================================================
-- Execute este script como superusuário (postgres)
-- psql -U postgres -f setup_database.sql

-- 1. Cria banco de dados
CREATE DATABASE discord_bot
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'English_United States.1252'
    LC_CTYPE = 'English_United States.1252'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

COMMENT ON DATABASE discord_bot IS 'Banco de dados do bot de moderação do Discord';

-- 2. Conecta ao banco
\c discord_bot

-- 3. Cria usuário do bot (ALTERE A SENHA!)
CREATE USER bot_user WITH PASSWORD 'pType-u:PQL20.378';

-- 4. Da permissões ao usuário
GRANT ALL PRIVILEGES ON DATABASE discord_bot TO bot_user;
GRANT ALL ON SCHEMA public TO bot_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO bot_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO bot_user;

-- 5. Cria tabela de warns
CREATE TABLE IF NOT EXISTS warns (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(20) NOT NULL,
    moderator_id VARCHAR(20),
    reason TEXT,
    timestamp BIGINT NOT NULL,
    expires_at BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE warns IS 'Tabela de advertências (warns) dos usuários';
COMMENT ON COLUMN warns.user_id IS 'ID do Discord do usuário que recebeu o warn';
COMMENT ON COLUMN warns.moderator_id IS 'ID do Discord do moderador que aplicou o warn';
COMMENT ON COLUMN warns.timestamp IS 'Timestamp em milissegundos de quando o warn foi aplicado';
COMMENT ON COLUMN warns.expires_at IS 'Timestamp em milissegundos de quando o warn expira';

-- 6. Cria tabela de logs
CREATE TABLE IF NOT EXISTS logs (
    id SERIAL PRIMARY KEY,
    action_type VARCHAR(50) NOT NULL,
    user_id VARCHAR(20),
    moderator_id VARCHAR(20),
    details TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE logs IS 'Tabela de logs de ações de moderação';
COMMENT ON COLUMN logs.action_type IS 'Tipo da ação (WARN, BAN, KICK, etc)';

-- 7. Cria índices para otimização
CREATE INDEX IF NOT EXISTS idx_warns_user_id ON warns(user_id);
CREATE INDEX IF NOT EXISTS idx_warns_expires_at ON warns(expires_at);
CREATE INDEX IF NOT EXISTS idx_warns_user_expires ON warns(user_id, expires_at);
CREATE INDEX IF NOT EXISTS idx_logs_user_id ON logs(user_id);
CREATE INDEX IF NOT EXISTS idx_logs_timestamp ON logs(timestamp);

-- 8. Cria função para limpeza automática de warns expirados (opcional)
CREATE OR REPLACE FUNCTION cleanup_expired_warns()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM warns
    WHERE expires_at < EXTRACT(EPOCH FROM NOW()) * 1000;

    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_expired_warns IS 'Remove warns expirados e retorna quantidade removida';

-- 9. Da permissões finais
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO bot_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO bot_user;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO bot_user;

-- 10. Mostra resumo
\echo '================================================'
\echo 'Setup concluído com sucesso!'
\echo '================================================'
\echo 'Banco de dados: discord_bot'
\echo 'Usuário: bot_user'
\echo 'Tabelas criadas: warns, logs'
\echo 'Índices criados: 5'
\echo ''
\echo 'Configure o bot com:'
\echo '  DB_HOST=localhost'
\echo '  DB_PORT=5432'
\echo '  DB_NAME=discord_bot'
\echo '  DB_USER=bot_user'
\echo '  DB_PASSWORD=Senha123!'
\echo '================================================'

-- Lista tabelas
\dt

-- Mostra estrutura da tabela warns
\d warns