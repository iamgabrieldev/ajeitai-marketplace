-- Banco e usuário para o Keycloak persistir dados
CREATE USER keycloak WITH PASSWORD 'keycloak_secret';
CREATE DATABASE keycloak OWNER keycloak;
GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak;
