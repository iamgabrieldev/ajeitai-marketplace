# Configuração do Keycloak

Para o backend e frontend funcionarem com autenticação, configure o Keycloak assim:

## 1. Acessar o Keycloak

- URL: http://localhost:8180
- Usuário admin: `admin`
- Senha admin: `admin`

## 2. Criar o Realm `ajeitai`

1. No menu lateral, clique em **Create realm**
2. Nome: `ajeitai`
3. Clique em **Create**

## 3. Criar as Roles

1. Vá em **Realm roles** → **Create role**
2. Crie a role `cliente`
3. Crie a role `prestador`

## 4. Criar o Client para o Frontend

1. Vá em **Clients** → **Create client**
2. **Client ID**: `ajeitai-frontend`
3. **Client type**: `OpenID Connect`
4. Clique em **Next**
5. **Client authentication**: OFF (é um client público/SPA)
6. **Authorization**: OFF
7. **Authentication flow**:
   - ✅ Standard flow
   - ✅ Direct access grants (para o fluxo Resource Owner Password usado no login)
8. Clique em **Save**
9. Em **Valid redirect URIs**: adicione `http://localhost:3000/*` (ou a URL do seu frontend)
10. Em **Web origins**: adicione `http://localhost:3000` (ou use `+` para herdar de Valid redirect URIs)
11. Clique em **Save**

> **Nota sobre CORS**: O frontend usa um proxy em `/api/auth/token` — o browser chama o Next.js (mesma origem) e o servidor Next.js chama o Keycloak. Assim evita-se erros de CORS.

## 5. Criar usuário de teste

1. Vá em **Users** → **Add user**
2. **Username**: email do usuário (ex: `usuario@email.com`)
3. **Email**: o mesmo email
4. Marque **Email verified**
5. Clique em **Create**
6. Vá na aba **Credentials** e defina uma senha
7. Desmarque **Temporary** (ou mantenha se quiser trocar no primeiro login)
8. Na aba **Role mapping**, clique em **Assign role** e adicione `cliente` ou `prestador`

## 6. Testar token via cURL

```bash
curl -X POST "http://localhost:8180/realms/ajeitai/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=ajeitai-frontend" \
  -d "username=usuario@email.com" \
  -d "password=SUA_SENHA"
```

A resposta inclui `access_token` (JWT). Use no header:

```
Authorization: Bearer <access_token>
```

## 7. Testar com Postman

1. **Auth type**: Bearer Token
2. Cole o `access_token` obtido do Keycloak
3. Ou use **OAuth 2.0** com:
   - Grant Type: Password Credentials
   - Access Token URL: `http://localhost:8180/realms/ajeitai/protocol/openid-connect/token`
   - Client ID: `ajeitai-frontend`
   - Username/Password: credenciais do usuário
