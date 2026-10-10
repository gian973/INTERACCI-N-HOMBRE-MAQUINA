# Diccionario de datos

Esquema final después de V1–V5. TIMESTAMP(6) guarda instantes UTC; DATE representa calendario de Lima. Importes DECIMAL(10,2), moneda PEN. Las PK implican NOT NULL. Las migraciones SQL son la definición ejecutable.

Calculados sin columna: estado activo/vencido, días restantes, reminder.dueOn, payment.appliedUntil y request.extensionDays.

## users

| Columna | Tipo / restricciones | Descripción |
|---|---|---|
| id | BIGINT AUTO_INCREMENT PRIMARY KEY | Identificador técnico de la fila. |
| name | VARCHAR(100) NOT NULL | Nombre del usuario, rol o plan según la relación. |
| dni | VARCHAR(8) NOT NULL UNIQUE | DNI de ocho dígitos, usado como identificador. |
| email | VARCHAR(160) NOT NULL UNIQUE | Correo normalizado a minúsculas. |
| phone | VARCHAR(20) NOT NULL | Teléfono de contacto o número de WhatsApp en preferencias. |
| password_hash | VARCHAR(255) NOT NULL | Hash BCrypt; nunca contraseña legible. |
| demo | BOOLEAN NOT NULL DEFAULT FALSE | Marca de cuenta ficticia afectada por reinicio demo. |
| created_at | TIMESTAMP(6) NOT NULL | Instante de creación/revisión/auditoría en UTC. |
| branch_id | BIGINT | Sede asignada a la membresía; FK a branches.id. NULL en membresías históricas sin sede asignada. |

## roles

| Columna | Tipo / restricciones | Descripción |
|---|---|---|
| id | BIGINT PRIMARY KEY | Identificador técnico de la fila. |
| name | VARCHAR(20) NOT NULL UNIQUE | Nombre del usuario, rol o plan según la relación. |

## user_roles

| Columna | Tipo / restricciones | Descripción |
|---|---|---|
| user_id | BIGINT NOT NULL | Usuario relacionado; FK a users.id. |
| role_id | BIGINT NOT NULL | Rol asignado; FK a roles.id. |

Claves y restricciones:

- PRIMARY KEY(user_id,role_id)
- FOREIGN KEY(user_id) REFERENCES users(id)
- FOREIGN KEY(role_id) REFERENCES roles(id)

## plans

| Columna | Tipo / restricciones | Descripción |
|---|---|---|
| id | BIGINT AUTO_INCREMENT PRIMARY KEY | Identificador técnico de la fila. |
| name | VARCHAR(100) NOT NULL | Nombre del usuario, rol o plan según la relación. |
| months | INT NOT NULL | Meses de catálogo o meses históricos contratados. |
| price | DECIMAL(10,2) NOT NULL | Importe total actual del catálogo en soles. |
| active | BOOLEAN NOT NULL | Disponibilidad para nuevas contrataciones. |

Claves y restricciones:

- CONSTRAINT ck_plan_months CHECK(months BETWEEN 1 AND 36)
- CONSTRAINT ck_plan_price CHECK(price>0)

## benefits

| Columna | Tipo / restricciones | Descripción |
|---|---|---|
| id | BIGINT AUTO_INCREMENT PRIMARY KEY | Identificador técnico de la fila. |
| label | VARCHAR(200) NOT NULL UNIQUE | Texto del beneficio actual o su instantánea contractual. |

## plan_benefits

| Columna | Tipo / restricciones | Descripción |
|---|---|---|
| plan_id | BIGINT NOT NULL | Plan de catálogo; FK a plans.id. |
| benefit_id | BIGINT NOT NULL | Beneficio de catálogo; FK a benefits.id. |

Claves y restricciones:

- PRIMARY KEY(plan_id,benefit_id)
- FOREIGN KEY(plan_id) REFERENCES plans(id)
- FOREIGN KEY(benefit_id) REFERENCES benefits(id)

## payments

| Columna | Tipo / restricciones | Descripción |
|---|---|---|
| id | BIGINT AUTO_INCREMENT PRIMARY KEY | Identificador técnico de la fila. |
| user_id | BIGINT NOT NULL | Usuario relacionado; FK a users.id. |
| plan_id | BIGINT NOT NULL | Plan de catálogo; FK a plans.id. |
| plan_name | VARCHAR(100) NOT NULL | Nombre histórico del plan. |
| months | INT NOT NULL | Meses de catálogo o meses históricos contratados. |
| amount | DECIMAL(10,2) NOT NULL | Importe total histórico contratado en soles. |
| method | VARCHAR(20) NOT NULL | CARD_TEST, WALLET o TRANSFER. |
| status | VARCHAR(20) NOT NULL | Estado del flujo; limitado por CHECK. |
| operation_number | VARCHAR(80) | Referencia de pago manual; NULL en tarjeta simulada. |
| idempotency_key | VARCHAR(36) NOT NULL UNIQUE | UUID del resumen emitido por el servidor. |
| created_at | TIMESTAMP(6) NOT NULL | Instante de creación/revisión/auditoría en UTC. |
| applied_from | DATE | Base calendario al confirmar; NULL antes de confirmar. |
| branch_id | BIGINT | Sede elegida al pagar; FK a branches.id. |
| branch_name | VARCHAR(100) | Nombre de sede guardado como instantánea del pago. |

Claves y restricciones:

- FOREIGN KEY(user_id) REFERENCES users(id)
- FOREIGN KEY(plan_id) REFERENCES plans(id)
- FOREIGN KEY(branch_id) REFERENCES branches(id)
- UNIQUE(method,operation_number)
- CONSTRAINT ck_payment_branch CHECK((branch_id IS NULL AND branch_name IS NULL) OR (branch_id IS NOT NULL AND branch_name IS NOT NULL))
- CONSTRAINT ck_payment_method CHECK(method IN ('CARD_TEST','WALLET','TRANSFER'))
- CONSTRAINT ck_payment_status CHECK(status IN ('PENDING','CONFIRMED','REJECTED'))
- CONSTRAINT ck_payment_amount CHECK(amount>0)
- CONSTRAINT ck_payment_months CHECK(months BETWEEN 1 AND 36)
- CONSTRAINT ck_manual_operation CHECK(method='CARD_TEST' OR operation_number IS NOT NULL)

## contract_benefits

| Columna | Tipo / restricciones | Descripción |
|---|---|---|
| id | BIGINT AUTO_INCREMENT PRIMARY KEY | Identificador técnico de la fila. |
| payment_id | BIGINT NOT NULL | Pago relacionado; FK a payments.id. |
| label | VARCHAR(200) NOT NULL | Texto del beneficio actual o su instantánea contractual. |

Claves y restricciones:

- FOREIGN KEY(payment_id) REFERENCES payments(id)
- UNIQUE(payment_id,label)

## memberships

| Columna | Tipo / restricciones | Descripción |
|---|---|---|
| id | BIGINT AUTO_INCREMENT PRIMARY KEY | Identificador técnico de la fila. |
| user_id | BIGINT NOT NULL UNIQUE | Usuario relacionado; FK a users.id. |
| payment_id | BIGINT NOT NULL UNIQUE | Pago relacionado; FK a payments.id. |
| started_on | DATE NOT NULL | Inicio del período continuo de membresía. |
| expires_on | DATE NOT NULL | Vencimiento exclusivo acumulado. |
| branch_id | BIGINT | Sede actual asignada; FK a branches.id. NULL si una membresía histórica aún no tiene sede. |

Claves y restricciones:

- FOREIGN KEY(user_id) REFERENCES users(id)
- FOREIGN KEY(payment_id) REFERENCES payments(id)
- FOREIGN KEY(branch_id) REFERENCES branches(id)
- CONSTRAINT ck_membership_dates CHECK(expires_on>=started_on)

## branches

| Columna | Tipo / restricciones | Descripción |
|---|---|---|
| id | BIGINT AUTO_INCREMENT PRIMARY KEY | Identificador técnico de la sede. |
| name | VARCHAR(100) NOT NULL UNIQUE | Nombre de la sede. |
| active | BOOLEAN NOT NULL DEFAULT TRUE | Disponibilidad de la sede. |
| capacity | INT NOT NULL DEFAULT 30 | Aforo máximo; debe ser positivo. |

Claves y restricciones:

- CONSTRAINT ck_branch_capacity CHECK(capacity>0)

## attendance

| Columna | Tipo / restricciones | Descripción |
|---|---|---|
| id | BIGINT AUTO_INCREMENT PRIMARY KEY | Identificador técnico de la visita. |
| branch_id | BIGINT NOT NULL | Sede de la entrada; FK a branches.id. |
| user_id | BIGINT NOT NULL | Socio que ingresó; FK a users.id. |
| checked_in_at | TIMESTAMP(6) NOT NULL | Instante UTC de entrada. |
| checked_out_at | TIMESTAMP(6) | Instante UTC de salida; NULL mientras el socio permanece dentro. |

Claves y restricciones:

- FOREIGN KEY(branch_id) REFERENCES branches(id)
- FOREIGN KEY(user_id) REFERENCES users(id)
- CONSTRAINT ck_attendance_times CHECK(checked_out_at IS NULL OR checked_out_at>=checked_in_at)

La aplicación valida membresía vigente y asignada a la sede, impide más de una entrada abierta por socio y serializa los ingresos para respetar el aforo.

## freeze_requests

| Columna | Tipo / restricciones | Descripción |
|---|---|---|
| id | BIGINT AUTO_INCREMENT PRIMARY KEY | Identificador técnico de la fila. |
| user_id | BIGINT NOT NULL | Usuario relacionado; FK a users.id. |
| kind | VARCHAR(20) NOT NULL | HEALTH (salud) o TRAVEL (viaje). |
| starts_on | DATE NOT NULL | Primer día de congelamiento (inclusivo). |
| ends_on | DATE NOT NULL | Último día solicitado (inclusivo). |
| comment | VARCHAR(1000) NOT NULL | Motivo o respuesta; máximo 1000 caracteres. |
| status | VARCHAR(20) NOT NULL | Estado del flujo; limitado por CHECK. |
| created_at | TIMESTAMP(6) NOT NULL | Instante de creación/revisión/auditoría en UTC. |
| policy | VARCHAR(30) | Política aplicada; NULL antes de aprobar. |

Claves y restricciones:

- FOREIGN KEY(user_id) REFERENCES users(id)
- CONSTRAINT ck_freeze_dates CHECK(ends_on>=starts_on)
- CONSTRAINT ck_freeze_kind CHECK(kind IN ('HEALTH','TRAVEL'))
- CONSTRAINT ck_freeze_status CHECK(status IN ('SENT','IN_REVIEW','APPROVED','REJECTED'))

## private_files

| Columna | Tipo / restricciones | Descripción |
|---|---|---|
| id | BIGINT AUTO_INCREMENT PRIMARY KEY | Identificador técnico de la fila. |
| owner_id | BIGINT NOT NULL | Propietario; debe coincidir con el usuario del padre. |
| payment_id | BIGINT UNIQUE | Pago relacionado; FK a payments.id. |
| request_id | BIGINT UNIQUE | Solicitud relacionada; FK a freeze_requests.id. |
| storage_key | VARCHAR(100) NOT NULL UNIQUE | UUID de almacenamiento; no una ruta del cliente. |
| original_name | VARCHAR(160) NOT NULL | Nombre seguro según tipo, p. ej. documento.pdf; evita conservar nombres sensibles del equipo. |
| media_type | VARCHAR(50) NOT NULL | application/pdf, image/png o image/jpeg, verificado. |
| bytes | BIGINT NOT NULL | Tamaño de 1 a 5242880 bytes. |

Claves y restricciones:

- FOREIGN KEY(owner_id) REFERENCES users(id)
- FOREIGN KEY(payment_id) REFERENCES payments(id)
- FOREIGN KEY(request_id) REFERENCES freeze_requests(id)
- CONSTRAINT ck_file_parent CHECK((payment_id IS NOT NULL AND request_id IS NULL) OR (payment_id IS NULL AND request_id IS NOT NULL))
- CONSTRAINT ck_file_size CHECK(bytes BETWEEN 1 AND 5242880)

## reviews

| Columna | Tipo / restricciones | Descripción |
|---|---|---|
| id | BIGINT AUTO_INCREMENT PRIMARY KEY | Identificador técnico de la fila. |
| payment_id | BIGINT | Pago relacionado; FK a payments.id. |
| request_id | BIGINT | Solicitud relacionada; FK a freeze_requests.id. |
| reviewer_id | BIGINT NOT NULL | Administrador revisor; FK a users.id. |
| from_status | VARCHAR(20) NOT NULL | Estado previo. |
| to_status | VARCHAR(20) NOT NULL | Estado posterior. |
| comment | VARCHAR(1000) NOT NULL | Motivo o respuesta; máximo 1000 caracteres. |
| created_at | TIMESTAMP(6) NOT NULL | Instante de creación/revisión/auditoría en UTC. |

Claves y restricciones:

- FOREIGN KEY(payment_id) REFERENCES payments(id)
- FOREIGN KEY(request_id) REFERENCES freeze_requests(id)
- FOREIGN KEY(reviewer_id) REFERENCES users(id)
- CONSTRAINT ck_review_parent CHECK((payment_id IS NOT NULL AND request_id IS NULL) OR (payment_id IS NULL AND request_id IS NOT NULL))

## preferences

| Columna | Tipo / restricciones | Descripción |
|---|---|---|
| id | BIGINT AUTO_INCREMENT PRIMARY KEY | Identificador técnico de la fila. |
| user_id | BIGINT NOT NULL UNIQUE | Usuario relacionado; FK a users.id. |
| phone | VARCHAR(20) NOT NULL | Teléfono de contacto o número de WhatsApp en preferencias. |
| consent | BOOLEAN NOT NULL DEFAULT FALSE | Autorización opcional; inicialmente false. |
| notice7 | BOOLEAN NOT NULL DEFAULT TRUE | Aviso 7 días antes; no constituye consentimiento. |
| notice1 | BOOLEAN NOT NULL DEFAULT TRUE | Aviso 1 día antes; no constituye consentimiento. |

Claves y restricciones:

- FOREIGN KEY(user_id) REFERENCES users(id)

## reminders

| Columna | Tipo / restricciones | Descripción |
|---|---|---|
| id | BIGINT AUTO_INCREMENT PRIMARY KEY | Identificador técnico de la fila. |
| user_id | BIGINT NOT NULL | Usuario relacionado; FK a users.id. |
| expiry_date | DATE NOT NULL | Vencimiento al que corresponde el aviso. |
| days_before | INT NOT NULL | Anticipación: 7 o 1 días. |
| status | VARCHAR(20) NOT NULL | Estado del flujo; limitado por CHECK. |
| processed_at | TIMESTAMP(6) | Instante de intento/cancelación; NULL sin procesar. |

Claves y restricciones:

- FOREIGN KEY(user_id) REFERENCES users(id)
- UNIQUE(user_id,expiry_date,days_before)
- CONSTRAINT ck_reminder_days CHECK(days_before IN(1,7))
- CONSTRAINT ck_reminder_status CHECK(status IN('SCHEDULED','CANCELLED','SIMULATED','SENT','FAILED'))

## audit_events

| Columna | Tipo / restricciones | Descripción |
|---|---|---|
| id | BIGINT AUTO_INCREMENT PRIMARY KEY | Identificador técnico de la fila. |
| actor_id | BIGINT | Autor de la acción o NULL para sistema; FK a users.id. |
| action | VARCHAR(50) NOT NULL | Tipo de acción auditada. |
| target_type | VARCHAR(40) NOT NULL | Tipo lógico del destino auditado. |
| target_id | BIGINT NOT NULL | ID histórico sin FK: puede sobrevivir al reinicio demo. |
| detail | VARCHAR(1000) NOT NULL | Descripción histórica, sin secretos. |
| created_at | TIMESTAMP(6) NOT NULL | Instante de creación/revisión/auditoría en UTC. |

Claves y restricciones:

- FOREIGN KEY(actor_id) REFERENCES users(id)

## reset_tokens

| Columna | Tipo / restricciones | Descripción |
|---|---|---|
| id | BIGINT AUTO_INCREMENT PRIMARY KEY | Identificador técnico de la fila. |
| user_id | BIGINT NOT NULL | Usuario relacionado; FK a users.id. |
| token_hash | VARCHAR(64) NOT NULL UNIQUE | SHA-256 del token; no se almacena el original. |
| expires_at | TIMESTAMP(6) NOT NULL | Límite de validez del token. |
| used | BOOLEAN NOT NULL | Token usado o invalidado. |

Claves y restricciones:

- FOREIGN KEY(user_id) REFERENCES users(id)

## Índices y justificación

PK y UNIQUE crean índices para identidad, acceso por correo/DNI, idempotencia y unicidad. InnoDB añade índices para FK cuando no hay un prefijo compatible.

| Índice | Uso |
|---|---|
| ix_payments_status_created | Cola por estado y antigüedad. |
| ix_payments_user_created | Historial de pagos del socio. |
| ix_membership_expiry | Conteos de activas/vencidas. |
| ix_membership_branch_expiry | Membresías por sede y vencimiento. |
| ix_payment_branch_status | Pagos por sede y estado. |
| ix_attendance_branch_current | Personas presentes por sede y hora de entrada. |
| ix_attendance_user_current | Entradas abiertas por socio. |
| ix_freeze_status_date | Solicitudes por estado y período. |
| ix_freeze_user_created | Historial del cliente. |
| ix_review_payment / ix_review_request | Revisiones cronológicas por operación. |
| ix_reminder_due (status,expiry_date,days_before) | Avisos pendientes por vencimiento y anticipación sin persistir fecha derivada. |
| ix_audit_created | Auditoría cronológica. |
| ix_reset_user | Invalidación de tokens previos. |

Las búsquedas con comodín inicial priorizan flexibilidad para este portal. Con gran volumen, medir planes de consulta antes de incorporar índices de texto específicos.
