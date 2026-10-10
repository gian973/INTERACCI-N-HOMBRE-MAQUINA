# Modelo entidad–relación

El SVG exportado está en [ER.svg](ER.svg). Los nombres camelCase del dibujo corresponden a tablas SQL snake_case; no son entidades adicionales.

```mermaid
erDiagram
    users ||--o{ userRoles : assigned
    roles ||--o{ userRoles : grants
    plans ||--o{ planBenefits : includes
    benefits ||--o{ planBenefits : catalog
    users ||--o{ payments : submits
    plans ||--o{ payments : references
    payments ||--|{ contractBenefits : snapshots
    users ||--o| memberships : owns
    payments ||--o| memberships : latestConfirmed
    users ||--o{ freezeRequests : requests
    users ||--o{ privateFiles : owns
    payments |o--o| privateFiles : receipt
    freezeRequests |o--o| privateFiles : attachment
    payments |o--o{ reviews : reviewed
    freezeRequests |o--o{ reviews : reviewed
    users ||--o{ reviews : reviewer
    users ||--o| preferences : chooses
    users ||--o{ reminders : receives
    users |o--o{ auditEvents : actor
    users ||--o{ resetTokens : recovers
    users {
        bigint id PK
        varchar dni UK
        varchar email UK
        varchar name
        varchar phone
        varchar password_hash
        boolean demo
        timestamp created_at
    }
    roles {
        bigint id PK
        varchar name UK
    }
    userRoles {
        bigint user_id PK,FK
        bigint role_id PK,FK
    }
    plans {
        bigint id PK
        varchar name
        int months
        decimal price
        boolean active
    }
    benefits {
        bigint id PK
        varchar label UK
    }
    planBenefits {
        bigint plan_id PK,FK
        bigint benefit_id PK,FK
    }
    payments {
        bigint id PK
        bigint user_id FK
        bigint plan_id FK
        varchar idempotency_key UK
        varchar plan_name
        int months
        decimal amount
        varchar method
        varchar operation_number
        varchar status
        date applied_from
        timestamp created_at
    }
    contractBenefits {
        bigint id PK
        bigint payment_id FK
        varchar label
    }
    memberships {
        bigint id PK
        bigint user_id FK,UK
        bigint payment_id FK,UK
        date started_on
        date expires_on
    }
    freezeRequests {
        bigint id PK
        bigint user_id FK
        varchar kind
        date starts_on
        date ends_on
        varchar comment
        varchar status
        varchar policy
        timestamp created_at
    }
    privateFiles {
        bigint id PK
        bigint owner_id FK
        bigint payment_id FK,UK
        bigint request_id FK,UK
        varchar storage_key UK
        varchar original_name
        varchar media_type
        bigint bytes
    }
    reviews {
        bigint id PK
        bigint payment_id FK
        bigint request_id FK
        bigint reviewer_id FK
        varchar from_status
        varchar to_status
        varchar comment
        timestamp created_at
    }
    preferences {
        bigint id PK
        bigint user_id FK,UK
        varchar phone
        boolean consent
        boolean notice7
        boolean notice1
    }
    reminders {
        bigint id PK
        bigint user_id FK
        date expiry_date
        int days_before
        varchar status
        timestamp processed_at
    }
    auditEvents {
        bigint id PK
        bigint actor_id FK
        varchar action
        varchar target_type
        bigint target_id
        varchar detail
        timestamp created_at
    }
    resetTokens {
        bigint id PK
        bigint user_id FK
        varchar token_hash UK
        timestamp expires_at
        boolean used
    }
```

En private_files y reviews exactamente una referencia (pago o solicitud) debe existir: la restricción CHECK impide ambos padres o ninguno. El ER expresa cardinalidades individuales; esa exclusión se comprueba en SQL. La clave alternativa (method, operation_number) aplica a pagos manuales con referencia no nula. recordatorios tienen UNIQUE(user_id, expiry_date, days_before). contract_benefits tiene UNIQUE(payment_id,label).
