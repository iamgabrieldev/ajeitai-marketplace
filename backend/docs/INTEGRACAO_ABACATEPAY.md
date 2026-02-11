# Integração AbacatePay

O backend integra com a [AbacatePay](https://docs.abacatepay.com/) para gerar links de pagamento PIX quando o prestador **aceita** um agendamento com forma de pagamento **ONLINE**. A integração usa o **SDK oficial Java** da AbacatePay ([abacatepay-java-sdk](https://github.com/AbacatePay/abacatepay-java-sdk)), disponível via [JitPack](https://jitpack.io/#AbacatePay/abacatepay-java-sdk).

## Fluxo

1. Cliente cria agendamento com forma de pagamento **ONLINE** (PIX).
2. Prestador aceita o agendamento → o backend chama a API AbacatePay `POST /billing/create` e obtém a URL de pagamento.
3. O link é enviado ao cliente (e exibido em "Meus Agendamentos"); o cliente paga pela página da AbacatePay.
4. Quando o pagamento é confirmado, a AbacatePay envia um **webhook** `billing.paid` para o backend.
5. O backend confirma o pagamento e o agendamento (status → CONFIRMADO), permitindo check-in pelo prestador.

## Configuração

**Guia completo (dashboard, API key, webhook, produto, cliente, cupom):** [CONFIGURACAO_ABACATEPAY.md](./CONFIGURACAO_ABACATEPAY.md)

Resumo em `application.properties` (ou variáveis de ambiente):

| Propriedade | Descrição |
|-------------|-----------|
| `app.abacatepay.api-key` | Chave de API (Bearer). Obrigatória para gerar cobranças reais. |
| `app.abacatepay.base-url` | URL base da API (default: `https://api.abacatepay.com/v1`). |
| `app.abacatepay.webhook-secret` | Secret configurado no webhook no dashboard AbacatePay (validação da URL). |
| `app.abacatepay.frontend-base-url` | Base URL do frontend para `returnUrl` e `completionUrl` (ex: `https://meuapp.com`). |

- **Sem API key**: o sistema gera um link mock (`https://pagamentos.ajeitai.com/checkout/{id}`) e o cliente pode usar "Confirmar pagamento" manualmente.
- **Com API key**: o link é o da AbacatePay; ao pagar, o webhook confirma automaticamente.

## Webhook

- **URL**: `POST /api/webhooks/abacatepay?webhookSecret=SEU_SECRET`
- Configure no [dashboard AbacatePay](https://abacatepay.com): URL do webhook e o mesmo `webhookSecret` em `app.abacatepay.webhook-secret`.
- Evento tratado: `billing.paid` → o backend identifica o agendamento pelo `externalId` (formato `ag-{id}`) e confirma o pagamento.

## Referências

- [Documentação AbacatePay](https://docs.abacatepay.com/)
- [SDK Java (GitHub)](https://github.com/AbacatePay/abacatepay-java-sdk)
- [SDKs disponíveis](https://docs.abacatepay.com/pages/sdks.md)
- [Criar cobrança](https://docs.abacatepay.com/pages/payment/create.md)
- [Webhooks](https://docs.abacatepay.com/pages/webhooks.md)
- [Autenticação](https://docs.abacatepay.com/pages/authentication.md)
