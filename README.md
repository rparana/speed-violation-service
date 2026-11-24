# Speed Violation Service

Microsserviço REST para apuração de infrações de excesso de velocidade conforme as regras de tolerância e enquadramento do Código de Trânsito Brasileiro (CTB Art. 218), com armazenamento em memória e consulta por placa.

## Stack Utilizada

- **Java 21** (Records, Enums, `java.time.Clock` e `Instant`)
- **Spring Boot 3.4.3** (`spring-boot-starter-web`, `spring-boot-starter-validation`)
- **Maven** com Maven Wrapper (`./mvnw`)
- **JUnit 5**, **AssertJ**, **Mockito** e **Spring MockMvc**
- **JaCoCo 0.8.12** (relatório de cobertura e gate mínimo de 80% no pacote `domain`)
- **Springdoc OpenAPI 2.8.5** (Swagger UI)
- **Docker** (multi-stage build) e **GitHub Actions** (CI)

---

## Decisões Arquiteturais

1. **Clean Architecture Pragmatizada**:
   - **`domain`**: concentra modelos imutáveis (`record` / `enum`), regras de negócio (`SpeedViolationEvaluator`) e contrato de persistência (`ViolationRepository`).
   - **`application`**: orquestra os casos de uso (`EvaluateSpeedUseCase`, `GetViolationsUseCase`) e expõe configurações/beans (`SpeedToleranceProperties`, `DomainBeanConfig`).
   - **`infrastructure`**: implementa adaptadores de entrada REST (`SpeedViolationController`, DTOs, validador, `GlobalExceptionHandler`, `OpenApiConfig`) e persistência em memória (`InMemoryViolationRepository`).
2. **Isolamento de Domínio sem Framework**:
   - As classes e records em `domain.*` não possuem dependências de Spring ou Jackson, permitindo testes unitários rápidos, determinísticos e desacoplados de infraestrutura.
3. **Thread-Safety em Memória**:
   - `InMemoryViolationRepository` utiliza `ConcurrentHashMap<String, CopyOnWriteArrayList<ViolationRecord>>` com `computeIfAbsent` e retorno defensivo via `List.copyOf(...)` / `List.of()`, garantindo segurança sob concorrência sem bloqueios globais.
4. **Validação de Regex com `Pattern` Estático**:
   - `SpeedReadingRequestValidator` compila os padrões de placa antiga (`^[A-Z]{3}[0-9]{4}$`) e Mercosul (`^[A-Z]{3}[0-9][A-Z][0-9]{2}$`) em constantes `private static final Pattern`, evitando recompilação por requisição e normalizando com `trim().toUpperCase(Locale.ROOT)`.
5. **Externalização de Propriedades via `application.yml`**:
   - As regras de tolerância (`fixed-margin-kmh`, `percentage-margin`, `speed-limit-threshold`) e a porta (`server.port`) são configuráveis via variáveis de ambiente (`TOLERANCE_FIXED_KMH`, `TOLERANCE_PERCENTAGE`, `TOLERANCE_SPEED_THRESHOLD`, `SERVER_PORT`), validadas na inicialização com `@ConfigurationProperties` + `@Validated`.
6. **Injeção de `Clock`**:
   - `java.time.Clock` é injetado no domínio (`SpeedViolationEvaluator`), no validador (`SpeedReadingRequestValidator`) e no `GlobalExceptionHandler`, garantindo testabilidade determinística para `processedAt`, timestamps futuros e respostas de erro.

---

## Pré-requisitos e Execução Local

### Pré-requisitos
- JDK 21 (para execução via Maven) ou Docker.

### Executando com Maven Wrapper
```bash
./mvnw spring-boot:run
```
A aplicação iniciará na porta padrão `8080` (customizável via `SERVER_PORT`).

### Executando com Docker
```bash
docker build -t speed-violation-service .
docker run -p 8080:8080 -e SERVER_PORT=8080 speed-violation-service
```

### Documentação Interativa (Swagger UI) e Postman
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **Postman Collection v2.1**: disponível em `docs/speed-violation-service.postman_collection.json`.

---

## Exemplos de Uso (`curl`)

### 1. Avaliação com Infração (persiste em memória)
```bash
curl -s -X POST http://localhost:8080/api/v1/violations/evaluate \
  -H "Content-Type: application/json" \
  -H "x-origin: FIXED" \
  -d '{
    "licensePlate": "ABC1D23",
    "measuredSpeed": 127,
    "speedLimit": 100,
    "equipmentId": "EQ-001",
    "captureTimestamp": "2026-11-24T12:00:00Z"
  }'
```
Resposta `200 OK`:
```json
{
  "licensePlate": "ABC1D23",
  "equipmentId": "EQ-001",
  "measuredSpeed": 127,
  "consideredSpeed": 120,
  "speedLimit": 100,
  "excessPercentage": 20.0,
  "hasViolation": true,
  "violation": {
    "severity": "MEDIUM",
    "ctbCode": "218-I"
  },
  "processedAt": "2026-11-24T14:30:00Z"
}
```

### 2. Avaliação sem Infração (não persiste)
```bash
curl -s -X POST http://localhost:8080/api/v1/violations/evaluate \
  -H "Content-Type: application/json" \
  -H "x-origin: MOBILE" \
  -d '{
    "licensePlate": "DEF5678",
    "measuredSpeed": 67,
    "speedLimit": 60,
    "equipmentId": "EQ-002",
    "captureTimestamp": "2026-11-24T12:05:00Z"
  }'
```
Resposta `200 OK`:
```json
{
  "licensePlate": "DEF5678",
  "equipmentId": "EQ-002",
  "measuredSpeed": 67,
  "consideredSpeed": 60,
  "speedLimit": 60,
  "excessPercentage": 0.0,
  "hasViolation": false,
  "violation": null,
  "processedAt": "2026-11-24T14:31:00Z"
}
```

### 3. Consulta de Infrações por Placa (`GET`)
```bash
curl -s "http://localhost:8080/api/v1/violations?licensePlate=ABC1D23"
```

### 4. Resposta Padronizada de Erro (`400 Bad Request`)
```bash
curl -s -X POST http://localhost:8080/api/v1/violations/evaluate \
  -H "Content-Type: application/json" \
  -H "x-origin: fixed" \
  -d '{
    "licensePlate": "ABC1D23",
    "measuredSpeed": 120,
    "speedLimit": 100,
    "equipmentId": "EQ-001",
    "captureTimestamp": "2026-11-24T12:00:00Z"
  }'
```
Resposta `400 Bad Request`:
```json
{
  "error": "INVALID_ORIGIN",
  "message": "Header x-origin must be one of: FIXED, MOBILE, HANDHELD (case-sensitive)",
  "timestamp": "2026-11-24T14:32:00Z"
}
```

---

## Testes e Cobertura de Código (JaCoCo)

Para executar todos os testes unitários, testes de integração e validar o mínimo de 80% de cobertura de instruções no pacote `domain`:
```bash
./mvnw clean verify
```

Após a execução, o relatório HTML do JaCoCo fica disponível em:
- `target/site/jacoco/index.html`
