## Contratos de API (payload/response)

Base URL: `http://localhost:5000`

### Tipos e enums

`FormaPagamento`:
```json
["DINHEIRO","ONLINE"]
```

`StatusAgendamento`:
```json
["PENDENTE","ACEITO","CONFIRMADO","RECUSADO","REALIZADO","CANCELADO"]
```

`CategoriaAtuacao`:
```json
["ELETRICISTA","ENCANADOR","PINTOR","PEDREIRO","MARCENEIRO","AR_CONDICIONADO","JARDINAGEM","LIMPEZA","DEDETIZACAO","SERRALHERIA","VIDRACEIRO","GESSO","PISO","REFORMAS_GERAIS","OUTROS"]
```

`Endereco` (embutido em cliente/prestador/agendamento):
```json
{
  "logradouro": "Rua A",
  "bairro": "Centro",
  "cep": "12345678",
  "numero": "10",
  "complemento": "Apto 2",
  "cidade": "São Paulo",
  "uf": "SP",
  "latitude": -23.5,
  "longitude": -46.6
}
```

### Autenticação (Keycloak)

`GET /api/perfil`
Resposta:
```json
{
  "id": "user-id",
  "email": "email@dominio.com",
  "token_id": "jwt-id",
  "claims_completas": { "sub": "user-id" },
  "roles": { "roles": ["cliente"] }
}
```

### Clientes

`POST /api/clientes/vincular` (ROLE_cliente)
Payload:
```json
{
  "nome": "João",
  "email": "joao@email.com",
  "telefone": "11999999999",
  "cpf": "12345678909",
  "endereco": { "logradouro": "Rua A", "bairro": "Centro", "cep": "12345678", "cidade": "São Paulo", "uf": "SP", "numero": "10" }
}
```
Resposta:
```json
{
  "id": 1,
  "keycloakId": "kc-1",
  "cpf": "12345678909",
  "nome": "João",
  "email": "joao@email.com",
  "telefone": "11999999999",
  "ativo": true,
  "avatarUrl": "cliente-1/avatar/arquivo.png",
  "endereco": { "logradouro": "Rua A", "bairro": "Centro", "cep": "12345678", "numero": "10", "complemento": null, "cidade": "São Paulo", "uf": "SP", "latitude": null, "longitude": null }
}
```

`GET /api/clientes/me` (ROLE_cliente)
Resposta: mesmo formato de `Cliente` acima.

`PUT /api/clientes/me` (ROLE_cliente)
Payload:
```json
{
  "nome": "João da Silva",
  "telefone": "11999998888",
  "endereco": { "logradouro": "Rua B", "bairro": "Centro", "cep": "12345678", "cidade": "São Paulo", "uf": "SP", "numero": "20" }
}
```
Resposta: `Cliente`.

`GET /api/clientes/me/agendamentos?status=PENDENTE` (ROLE_cliente)
Resposta:
```json
[
  {
    "id": 10,
    "cliente": { "id": 1, "keycloakId": "kc-1", "nome": "João", "email": "joao@email.com", "telefone": "11999999999", "cpf": "12345678909", "ativo": true, "avatarUrl": null, "endereco": { "logradouro": "Rua A", "bairro": "Centro", "cep": "12345678", "numero": "10", "complemento": null, "cidade": "São Paulo", "uf": "SP", "latitude": null, "longitude": null } },
    "prestador": { "id": 2, "keycloakId": "kc-2", "nomeFantasia": "Casa Limpa", "cnpj": "12345678000199", "categoria": "LIMPEZA", "email": "prestador@email.com", "telefone": "11999990000", "ativo": true, "avatarUrl": null, "valorServico": 120.0, "endereco": { "logradouro": "Rua B", "bairro": "Centro", "cep": "12345678", "numero": "20", "complemento": null, "cidade": "São Paulo", "uf": "SP", "latitude": null, "longitude": null } },
    "dataHora": "2026-02-07T14:00:00",
    "status": "PENDENTE",
    "formaPagamento": "ONLINE",
    "valorServico": 120.0,
    "observacao": "Limpeza pesada",
    "endereco": { "logradouro": "Rua A", "bairro": "Centro", "cep": "12345678", "numero": "10", "complemento": null, "cidade": "São Paulo", "uf": "SP", "latitude": null, "longitude": null },
    "criadoEm": "2026-02-07T10:00:00",
    "confirmadoEm": null,
    "checkinEm": null,
    "checkoutEm": null,
    "checkinLatitude": null,
    "checkinLongitude": null,
    "checkoutLatitude": null,
    "checkoutLongitude": null
  }
]
```

`POST /api/clientes/me/avaliacoes/{agendamentoId}` (ROLE_cliente)
Payload:
```json
{ "nota": 5, "comentario": "Ótimo serviço" }
```
Resposta:
```json
{
  "id": 100,
  "agendamento": { "id": 10 },
  "cliente": { "id": 1 },
  "prestador": { "id": 2 },
  "nota": 5,
  "comentario": "Ótimo serviço",
  "criadoEm": "2026-02-07T18:00:00"
}
```

`POST /api/clientes/me/avatar` (ROLE_cliente, multipart/form-data)
Campos:
```
arquivo: <arquivo>
```
Resposta: `Cliente` com `avatarUrl` preenchido.

### Prestadores

`POST /api/prestadores/vincular` (ROLE_prestador)
Payload:
```json
{
  "nomeFantasia": "Casa Limpa",
  "email": "prestador@email.com",
  "telefone": "11999990000",
  "cnpj": "12345678000199",
  "categoria": "LIMPEZA",
  "valorServico": 120.00,
  "endereco": { "logradouro": "Rua B", "bairro": "Centro", "cep": "12345678", "cidade": "São Paulo", "uf": "SP", "numero": "20" }
}
```
Resposta:
```json
{
  "id": 2,
  "keycloakId": "kc-2",
  "nomeFantasia": "Casa Limpa",
  "cnpj": "12345678000199",
  "categoria": "LIMPEZA",
  "email": "prestador@email.com",
  "telefone": "11999990000",
  "ativo": true,
  "avatarUrl": "prestador-2/avatar/arquivo.png",
  "valorServico": 120.0,
  "endereco": { "logradouro": "Rua B", "bairro": "Centro", "cep": "12345678", "numero": "20", "complemento": null, "cidade": "São Paulo", "uf": "SP", "latitude": null, "longitude": null }
}
```

`GET /api/prestadores/me` (ROLE_prestador)  
Resposta: `Prestador`.

`PUT /api/prestadores/me` (ROLE_prestador)  
Payload:
```json
{
  "nomeFantasia": "Casa Limpa Serviços",
  "telefone": "11999990001",
  "categoria": "LIMPEZA",
  "valorServico": 130.0,
  "endereco": { "logradouro": "Rua C", "bairro": "Centro", "cep": "12345678", "cidade": "São Paulo", "uf": "SP", "numero": "30" }
}
```
Resposta: `Prestador`.

`POST /api/prestadores/me/avatar` (ROLE_prestador, multipart/form-data)
Campos:
```
arquivo: <arquivo>
```
Resposta: `Prestador`.

`GET /api/prestadores/me/solicitacoes?status=PENDENTE` (ROLE_prestador)
Resposta: lista de `Agendamento`.

`GET /api/prestadores/me/dashboard` (ROLE_prestador)
Resposta:
```json
{
  "quantidadeTrabalhosMes": 12,
  "ganhoBrutoMes": 1440.0,
  "lucroLiquidoMes": 1440.0
}
```

`GET /api/prestadores/me/disponibilidade` (ROLE_prestador)  
Resposta:
```json
[
  { "id": 1, "diaSemana": 1, "horaInicio": "08:00:00", "horaFim": "18:00:00" }
]
```

`PUT /api/prestadores/me/disponibilidade` (ROLE_prestador)
Payload:
```json
[
  { "diaSemana": 1, "horaInicio": "08:00:00", "horaFim": "18:00:00" }
]
```
Resposta: lista de `Disponibilidade`.

`GET /api/prestadores/me/documentos` (ROLE_prestador)
Resposta:
```json
[
  { "id": 10, "nomeArquivo": "cnpj.pdf", "contentType": "application/pdf", "caminho": "prestador-2/abc.pdf", "descricao": "CNPJ" }
]
```

`POST /api/prestadores/me/documentos` (ROLE_prestador, multipart/form-data)
Campos:
```
arquivo: <arquivo>
descricao: "CNPJ"
```
Resposta: `DocumentoPrestador`.

`DELETE /api/prestadores/me/documentos/{id}` (ROLE_prestador)
Resposta: `204 No Content`.

`GET /api/prestadores/me/documentos/{id}/download` (ROLE_prestador)
Resposta: arquivo binário.

`GET /api/prestadores/me/portfolio` (ROLE_prestador)
Resposta:
```json
[
  { "id": 1, "titulo": "Antes e depois", "descricao": "Sala", "imagemUrl": "prestador-2/portfolio/img1.jpg", "criadoEm": "2026-02-07T12:00:00" }
]
```

`POST /api/prestadores/me/portfolio` (ROLE_prestador, multipart/form-data)
Campos:
```
arquivo: <imagem>
titulo: "Antes e depois"
descricao: "Sala"
```
Resposta: `PortfolioItem`.

`DELETE /api/prestadores/me/portfolio/{id}` (ROLE_prestador)
Resposta: `204 No Content`.

`POST /api/prestadores/me/notificacoes/token` (ROLE_prestador)
Payload:
```json
{ "token": "push-token", "plataforma": "web" }
```
Resposta: `200 OK`.

### Agendamentos

`POST /api/agendamentos` (ROLE_cliente)
Payload:
```json
{
  "prestadorId": 2,
  "dataHora": "2026-02-07T14:00:00",
  "formaPagamento": "ONLINE",
  "observacao": "Limpeza pesada"
}
```
Resposta: `Agendamento`.

`GET /api/agendamentos` (ROLE_cliente)
Resposta: lista de `Agendamento`.

`PUT /api/agendamentos/{id}/cancelar` (ROLE_cliente)
Resposta: `Agendamento` com `status=CANCELADO`.

`PUT /api/agendamentos/{id}/aceitar` (ROLE_prestador)
Resposta: `Agendamento` com `status=ACEITO` ou `CONFIRMADO` (pagamento em dinheiro).

`PUT /api/agendamentos/{id}/recusar` (ROLE_prestador)
Resposta: `Agendamento` com `status=RECUSADO`.

`PUT /api/agendamentos/{id}/checkin` (ROLE_prestador)
Payload:
```json
{ "latitude": -23.5, "longitude": -46.6 }
```
Resposta: `Agendamento` com `checkinEm` e coordenadas preenchidas.

`PUT /api/agendamentos/{id}/checkout` (ROLE_prestador)
Payload:
```json
{ "latitude": -23.5, "longitude": -46.6 }
```
Resposta: `Agendamento` com `checkoutEm` e `status=REALIZADO`.

`PUT /api/agendamentos/{id}/confirmar-pagamento` (ROLE_cliente)
Resposta: `Agendamento` com `status=CONFIRMADO`.

`GET /api/agendamentos/{id}/pagamento` (ROLE_cliente)
Resposta:
```json
{
  "id": 1,
  "agendamento": { "id": 10 },
  "status": "PENDENTE",
  "linkPagamento": "https://pagamentos.ajeitai.com/checkout/10",
  "criadoEm": "2026-02-07T10:00:00",
  "confirmadoEm": null
}
```

### Catálogo público

`GET /api/catalogo/prestadores?categoria=LIMPEZA&cidade=São Paulo&uf=SP&minAvaliacao=4&latitude=-23.5&longitude=-46.6`
Resposta:
```json
[
  {
    "id": 2,
    "nomeFantasia": "Casa Limpa",
    "categoria": "LIMPEZA",
    "cidade": "São Paulo",
    "uf": "SP",
    "valorServico": 120.0,
    "mediaAvaliacao": 4.8,
    "distanciaKm": 2.1,
    "avatarUrl": "prestador-2/avatar/logo.png"
  }
]
```

`GET /api/catalogo/prestadores/{id}`
Resposta:
```json
{
  "id": 2,
  "nomeFantasia": "Casa Limpa",
  "categoria": "LIMPEZA",
  "cidade": "São Paulo",
  "uf": "SP",
  "valorServico": 120.0,
  "mediaAvaliacao": 4.8,
  "avatarUrl": "prestador-2/avatar/logo.png",
  "portfolio": [
    { "id": 1, "titulo": "Antes e depois", "descricao": "Sala", "imagemUrl": "prestador-2/portfolio/img1.jpg" }
  ]
}
```
