-- Conectar como postgres ao banco discord_bot
-- psql -U postgres -d discord_bot -f fix_ownership.sql

-- Transferir propriedade
ALTER TABLE warns OWNER TO bot_user;
ALTER TABLE logs OWNER TO bot_user;
ALTER SEQUENCE warns_id_seq OWNER TO bot_user;
ALTER SEQUENCE logs_id_seq OWNER TO bot_user;
ALTER FUNCTION cleanup_expired_warns() OWNER TO bot_user;

-- Dar todas as permissões (garantia extra)
GRANT ALL ON TABLE warns TO bot_user;
GRANT ALL ON TABLE logs TO bot_user;
GRANT ALL ON SEQUENCE warns_id_seq TO bot_user;
GRANT ALL ON SEQUENCE logs_id_seq TO bot_user;

-- Verificar
SELECT
    tablename as tabela,
    tableowner as dono
FROM pg_tables
WHERE schemaname = 'public';