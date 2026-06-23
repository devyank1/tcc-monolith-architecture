# BR Tickets I - Sistema de Venda de Ingressos (Monolito)

> Trabalho de Conclusão de Curso - Engenharia de Software
> Fase 1 de 2: Arquitetura Monolítica em Camadas
> Próxima fase: migração para microsserviços ([BRTickets II](#))

## Sobre o projeto

O **EventLocal** é a primeira fase de um estudo de migração de arquitetura monolítica
para microsserviços, utilizando como cenário um sistema de venda de ingressos inspirado
no case real do Ticketmaster.

Esta fase implementa um monolito modular em **Java 17 + Spring Boot 3.x**, organizado em
camadas (`Controller → Service → Repository`) e estruturado **por domínio de negócio**,
preparando o terreno arquitetural para a extração futura de microsserviços (Fase 2).

O objetivo acadêmico desta fase é estabelecer uma baseline funcional e mensurável e
em termos de desempenho, escalabilidade e manutenibilidade, que servirá de comparação
com a arquitetura distribuída implementada posteriormente.

---

## Stack tecnológica

| Categoria | Tecnologia |
|---|---|
| Linguagem | Java 17+ |
| Framework | Spring Boot 3.x |
| Build | Maven |
| Banco de dados | PostgreSQL (instância única) |
| ORM | Spring Data JPA / Hibernate |
| Migração de schema | Flyway |
| Segurança | Spring Security + JWT |
| Containerização | Docker + Docker Compose |
| Observabilidade | Spring Boot Actuator + Prometheus + Grafana |
| Testes de carga | k6 / JMeter |
| Análise estática | SonarQube |

---

## Requisitos do sistema

### Requisitos Funcionais (Fase 1)

| ID | Descrição |
|---|---|
| RF01 | Cadastro e autenticação de usuários |
| RF02 | Visualização de eventos disponíveis |
| RF03 | Pesquisa de eventos por nome/data/local |
| RF04 | Compra de ingressos |
| RF05 | Geração de comprovante da compra |
| RF06 | Gerenciamento de eventos pelo administrador |

### Requisitos Não Funcionais (Fase 1)

| ID | Descrição |
|---|---|
| RNF01 | Resposta em até 2s sob carga normal |
| RNF02 | Suporte a até 100 usuários simultâneos |
| RNF03 | Dados em banco relacional único |

> Os requisitos RF07–RF10 e RNF04–RNF10, relativos à arquitetura de microsserviços,
> estão documentados no repositório da Fase 2 (EventGlobal).

---

## Arquitetura

### Visão em camadas

O sistema segue o padrão de **Arquitetura em Camadas (Layered Architecture)**, organizada
internamente **por domínio de negócio** e não por tipo técnico. Essa escolha é central
para a narrativa do TCC: cada pacote de domínio é, em essência, um microsserviço em
potencial, recortado de forma cirúrgica na Fase 2.

```mermaid
flowchart TB
    subgraph Client["Cliente"]
        WEB["Aplicação Web / Postman / Frontend"]
    end

    subgraph Monolith["EventLocal — Monolito Spring Boot"]
        direction TB
        CTRL["Controller Layer<br/>Recebe HTTP, valida entrada, delega"]
        SVC["Service Layer<br/>Regras de negócio, transações, autorização"]
        REPO["Repository Layer<br/>Spring Data JPA"]
        DOM["Domain Layer<br/>Entidades, enums, value objects"]

        CTRL --> SVC
        SVC --> REPO
        SVC -.-> DOM
        REPO -.-> DOM
    end

    subgraph Data["Persistência"]
        DB[("PostgreSQL<br/>banco único")]
    end

    WEB -->|"HTTP/REST + JWT"| CTRL
    REPO -->|"SQL via Hibernate"| DB

    style Monolith fill:#1a1a2e,stroke:#0f3460,color:#eaeaea
    style Data fill:#16213e,stroke:#0f3460,color:#eaeaea
    style Client fill:#1a1a2e,stroke:#0f3460,color:#eaeaea
```

### Organização por domínio

```mermaid
flowchart LR
    subgraph Pkg["br.com.eventlocal"]
        direction TB
        U["usuario/<br/>RF01"]
        E["evento/<br/>RF02 · RF03 · RF06"]
        V["venue/"]
        I["ingresso/<br/>RF04 · RF05"]
        B["booking/<br/>RF04 · RF05"]
        P["pagamento/<br/>RF04"]
        S["shared/<br/>config · security · exception"]
    end

    U -.->|"createdBy"| E
    V -.->|"venueId"| E
    E -.->|"eventId"| I
    I -.->|"ticketId"| B
    U -.->|"userId"| B
    B -.->|"bookingId"| P

    style Pkg fill:#1a1a2e,stroke:#0f3460,color:#eaeaea
```

> Cada pasta de domínio (`usuario/`, `evento/`, `pagamento/`...) contém sua própria
> estrutura interna `controller / service / repository / model / dto`. Essa coesão é
> proposital: na Fase 2, cada uma dessas pastas é extraída para um microsserviço
> independente, com mínima refatoração estrutural.

---

## Modelo de domínio (Diagrama de Classes)

```mermaid
classDiagram
    class UserModel {
        +UUID userId
        +String firstName
        +String lastName
        +String email
        +String passwordHash
        +String cpf
        +String phone
        +LocalDate birthday
        +UserRole role
        +boolean active
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
    }

    class VenueModel {
        +UUID venueId
        +String name
        +String description
        +VenueEnum type
        +String street
        +String city
        +String state
        +String zipCode
        +String country
        +Double latitude
        +Double longitude
        +Integer capacity
        +SeatMap seatMap
        +boolean active
    }

    class EventModel {
        +UUID eventId
        +String name
        +String description
        +LocalDateTime date
        +LocalDateTime endDate
        +EventType type
        +String artist
        +EventStatus status
        +Integer ageRate
        +String imageUrl
        +LocalDateTime salesStartAt
        +LocalDateTime salesEndAt
        +LocalDateTime createdAt
    }

    class TicketModel {
        +UUID ticketId
        +String sector
        +String row
        +String seat
        +BigDecimal price
        +TicketType type
        +TicketStatus status
        +String qrCode
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
    }

    class BookingModel {
        +UUID bookingId
        +BookingStatus status
        +BigDecimal totalAmount
        +String receiptCode
        +LocalDateTime createdAt
        +LocalDateTime confirmedAt
        +LocalDateTime cancelledAt
    }

    class BookingItemModel {
        +UUID bookingItemId
        +BigDecimal unitPrice
    }

    class PaymentModel {
        +UUID paymentId
        +PaymentMethod method
        +PaymentStatus status
        +BigDecimal amount
        +String cardLastFourDigits
        +String pixKey
        +String boletoCode
        +String gatewayTransactionId
        +LocalDateTime createdAt
        +LocalDateTime paidAt
    }

    UserModel "1" --> "0..*" EventModel : createdBy (admin)
    UserModel "1" --> "0..*" BookingModel : realiza
    VenueModel "1" --> "0..*" EventModel : sedia
    EventModel "1" --> "0..*" TicketModel : possui
    BookingModel "1" --> "1..*" BookingItemModel : contém
    TicketModel "1" --> "0..1" BookingItemModel : referenciado por
    BookingModel "1" --> "1" PaymentModel : gera
```

---

## Fluxo principal — Compra de ingresso (RF04 + RF05)

```mermaid
sequenceDiagram
    actor User as Usuário
    participant Ctrl as BookingController
    participant Svc as BookingService
    participant TRepo as TicketRepository
    participant BRepo as BookingRepository
    participant PaySvc as PaymentService
    participant DB as PostgreSQL

    User->>Ctrl: POST /bookings (ticketIds, paymentMethod)
    Ctrl->>Svc: createBooking(dto, usuarioAutenticado)

    Svc->>TRepo: findByIdForUpdate(ticketId)
    Note over TRepo,DB: Lock pessimista evita<br/>venda duplicada (RNF05)
    TRepo->>DB: SELECT ... FOR UPDATE
    DB-->>TRepo: Ticket (status = AVAILABLE)
    TRepo-->>Svc: TicketModel

    alt Ticket indisponível
        Svc-->>Ctrl: TicketUnavailableException
        Ctrl-->>User: 409 Conflict
    else Ticket disponível
        Svc->>Svc: ticket.setStatus(SOLD)
        Svc->>BRepo: save(Booking + BookingItems)
        BRepo->>DB: INSERT booking, booking_items

        Svc->>PaySvc: processar(booking, paymentMethod)
        PaySvc->>DB: INSERT payment
        PaySvc-->>Svc: PaymentModel

        Svc->>Svc: gerar receiptCode (RF05)
        Svc-->>Ctrl: BookingResponseDTO
        Ctrl-->>User: 201 Created + comprovante
    end
```

---

## Modelo entidade-relacionamento

```mermaid
erDiagram
    USER ||--o{ EVENT : "cria (createdBy)"
    USER ||--o{ BOOKING : "realiza"
    VENUE ||--o{ EVENT : "sedia"
    EVENT ||--o{ TICKET : "possui"
    BOOKING ||--|{ BOOKING_ITEM : "contém"
    TICKET ||--o| BOOKING_ITEM : "referenciado"
    BOOKING ||--|| PAYMENT : "gera"

    USER {
        uuid user_id PK
        string email UK
        string cpf UK
        string password_hash
        enum role
        boolean active
    }

    VENUE {
        uuid venue_id PK
        string name
        string city
        integer capacity
        jsonb seat_map
        boolean active
    }

    EVENT {
        uuid event_id PK
        uuid venue_id FK
        uuid created_by FK
        string name
        enum type
        enum status
        timestamp date
    }

    TICKET {
        uuid ticket_id PK
        uuid event_id FK
        string sector
        string seat
        decimal price
        enum status
        string qr_code UK
    }

    BOOKING {
        uuid booking_id PK
        uuid user_id FK
        enum status
        decimal total_amount
        string receipt_code UK
    }

    BOOKING_ITEM {
        uuid booking_item_id PK
        uuid booking_id FK
        uuid ticket_id FK
        decimal unit_price
    }

    PAYMENT {
        uuid payment_id PK
        uuid booking_id FK
        enum method
        enum status
        decimal amount
    }
```
---

## Decisões arquiteturais relevantes (justificativa acadêmica)

| Decisão | Alternativa descartada | Justificativa |
|---|---|---|
| Camadas organizadas por domínio | Camadas por tipo técnico (`controllers/`, `services/`) | Facilita a extração cirúrgica de microsserviços na Fase 2 |
| `SeatMap` como JSON convertido (Java ↔ JSONB) | Entidades `Sector`/`Row` normalizadas | Evita JOINs custosos para algo lido em bloco; YAGNI no contexto do monolito |
| Lock pessimista (`PESSIMISTIC_WRITE`) | Lock otimista / sem lock | Garante RNF05 (sem venda duplicada) em banco único — será substituído por lock distribuído (Redis) na Fase 2 |
| `Payment` em tabela única com campos por método | Herança `JOINED` (`CreditCardPayment`, `PixPayment`...) | Reduz complexidade de schema no monolito; herança se justifica quando o serviço for extraído |
| Autorização via `@PreAuthorize` | Validação manual de role no Service | Separa autorização (cross-cutting concern) de regra de negócio, alinhado a AOP do Spring |
| Soft delete (`active = false`) | Delete físico | Preserva histórico de eventos/vendas para auditoria e fins acadêmicos de análise |

---

## Como rodar o projeto

```bash
# Clonar o repositório
git clone <repo-url>
cd projeto-1-monolito

# Subir banco PostgreSQL via Docker
docker-compose up -d postgres

# Rodar as migrações Flyway e iniciar a aplicação
./mvnw spring-boot:run

# Acessar a documentação da API
http://localhost:8080/swagger-ui.html

# Acessar métricas (Actuator)
http://localhost:8080/actuator/health
```

---

## Observabilidade

A aplicação expõe métricas via Spring Boot Actuator, consumidas pelo Prometheus e
visualizadas em dashboards Grafana — usadas para estabelecer a baseline de desempenho
(RNF01, RNF02) que será comparada com a Fase 2.

```bash
docker-compose up -d prometheus grafana
```

| Serviço | URL padrão |
|---|---|
| Actuator | `http://localhost:8080/actuator` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3000` |

---

## Roadmap do TCC

- [x] Modelagem de entidades e relacionamentos
- [x] Camada de domínio: `usuario`, `venue`, `evento`
- [ ] Camada de domínio: `ingresso`, `booking`, `pagamento`
- [ ] Autenticação JWT completa + `@PreAuthorize`
- [ ] Testes de carga (baseline RNF01/RNF02) com k6/JMeter
- [ ] Análise estática com SonarQube
- [ ] Migração para arquitetura de microsserviços (EventGlobal — Fase 2)

---

## Autor

Projeto desenvolvido como Trabalho de Conclusão de Curso em Engenharia de Software.
