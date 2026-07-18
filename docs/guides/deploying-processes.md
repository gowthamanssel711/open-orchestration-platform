# Deploying Processes

How to deploy BPMN process definitions and forms to the platform.

## Prerequisites

- Runtime Bundle running on port 8081
- Valid Keycloak token

## Get a Token

```bash
TOKEN=$(curl -s -X POST http://localhost:8180/realms/oop/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=tasklist-ui" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password" | jq -r '.access_token')
```

## Deploy BPMN + Forms Together

```bash
curl -X POST http://localhost:8081/api/v1/deploy \
  -H "Authorization: Bearer $TOKEN" \
  -F "files=@my-process.bpmn" \
  -F "files=@my-form.form"
```

## Deploy BPMN Only

```bash
curl -X POST http://localhost:8081/api/v1/deploy \
  -H "Authorization: Bearer $TOKEN" \
  -F "files=@my-process.bpmn"
```

## Deploy Forms Only

```bash
curl -X POST http://localhost:8081/api/v1/deploy \
  -H "Authorization: Bearer $TOKEN" \
  -F "files=@review-order.form" \
  -F "files=@confirm-delivery.form"
```

## File Extensions

| Extension | Type | Action |
|---|---|---|
| `.bpmn` | Process definition | Deployed to Activiti engine |
| `.bpmn20.xml` | Process definition | Deployed to Activiti engine |
| `.form` | Form definition | Stored in database |
| `.form.json` | Form definition | Stored in database |
| Other | Unknown | Skipped |

## Start a Process Instance

After deployment:

```bash
curl -X POST http://localhost:8080/api/v1/process-instances \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionKey": "order-process",
    "variables": {
      "orderId": "ORD-001",
      "customerName": "John Doe"
    }
  }'
```

## Verify Deployment

List deployed definitions:

```bash
curl http://localhost:8081/api/v1/processes \
  -H "Authorization: Bearer $TOKEN"
```

List deployed forms:

```bash
curl http://localhost:8081/api/v1/forms \
  -H "Authorization: Bearer $TOKEN"
```

## Re-deployment

- **BPMN:** Re-deploying creates a new version. Running instances continue on the old version.
- **Forms:** Re-deploying auto-increments the version. All tasks immediately get the new form.

## Common Issues

| Issue | Cause | Fix |
|---|---|---|
| 401 Unauthorized | Token expired or missing | Get a fresh token |
| Form has no 'id' field | Invalid form JSON | Ensure `"id"` field exists in form file |
| Process definition not found | Deployment failed silently | Check response for errors |
| Form not showing on task | `formKey` mismatch | Ensure BPMN `activiti:formKey` matches form `"id"` |
