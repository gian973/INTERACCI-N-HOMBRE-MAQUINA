CREATE TABLE users (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL, dni VARCHAR(8) NOT NULL UNIQUE,
 email VARCHAR(160) NOT NULL UNIQUE, phone VARCHAR(20) NOT NULL, password_hash VARCHAR(255) NOT NULL,
 demo BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMP(6) NOT NULL
);
CREATE TABLE roles (id BIGINT PRIMARY KEY, name VARCHAR(20) NOT NULL UNIQUE);
CREATE TABLE user_roles (user_id BIGINT NOT NULL, role_id BIGINT NOT NULL, PRIMARY KEY(user_id,role_id),
 FOREIGN KEY(user_id) REFERENCES users(id), FOREIGN KEY(role_id) REFERENCES roles(id));
CREATE TABLE plans (id BIGINT AUTO_INCREMENT PRIMARY KEY,name VARCHAR(100) NOT NULL,months INT NOT NULL,price DECIMAL(10,2) NOT NULL,
 active BOOLEAN NOT NULL, CONSTRAINT ck_plan_months CHECK(months BETWEEN 1 AND 36),CONSTRAINT ck_plan_price CHECK(price>0));
CREATE TABLE benefits (id BIGINT AUTO_INCREMENT PRIMARY KEY,label VARCHAR(200) NOT NULL UNIQUE);
CREATE TABLE plan_benefits (plan_id BIGINT NOT NULL,benefit_id BIGINT NOT NULL,PRIMARY KEY(plan_id,benefit_id),
 FOREIGN KEY(plan_id) REFERENCES plans(id),FOREIGN KEY(benefit_id) REFERENCES benefits(id));
CREATE TABLE payments (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT NOT NULL,plan_id BIGINT NOT NULL,plan_name VARCHAR(100) NOT NULL,
 months INT NOT NULL,amount DECIMAL(10,2) NOT NULL,method VARCHAR(20) NOT NULL,status VARCHAR(20) NOT NULL,
 operation_number VARCHAR(80),idempotency_key VARCHAR(36) NOT NULL UNIQUE,created_at TIMESTAMP(6) NOT NULL,
 applied_from DATE,applied_until DATE,
 FOREIGN KEY(user_id) REFERENCES users(id), FOREIGN KEY(plan_id) REFERENCES plans(id),
 UNIQUE(method,operation_number),
 CONSTRAINT ck_payment_method CHECK(method IN ('CARD_TEST','WALLET','TRANSFER')),
 CONSTRAINT ck_payment_status CHECK(status IN ('PENDING','CONFIRMED','REJECTED')),
 CONSTRAINT ck_payment_amount CHECK(amount>0), CONSTRAINT ck_payment_months CHECK(months BETWEEN 1 AND 36),
 CONSTRAINT ck_manual_operation CHECK(method='CARD_TEST' OR operation_number IS NOT NULL)
);
CREATE INDEX ix_payments_status_created ON payments(status,created_at);
CREATE INDEX ix_payments_user_created ON payments(user_id,created_at);
CREATE TABLE contract_benefits (id BIGINT AUTO_INCREMENT PRIMARY KEY,payment_id BIGINT NOT NULL,label VARCHAR(200) NOT NULL,
 FOREIGN KEY(payment_id) REFERENCES payments(id),UNIQUE(payment_id,label));
CREATE TABLE memberships (id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT NOT NULL UNIQUE,payment_id BIGINT NOT NULL UNIQUE,
 started_on DATE NOT NULL,expires_on DATE NOT NULL,FOREIGN KEY(user_id) REFERENCES users(id),FOREIGN KEY(payment_id) REFERENCES payments(id),
 CONSTRAINT ck_membership_dates CHECK(expires_on>=started_on));
CREATE INDEX ix_membership_expiry ON memberships(expires_on);
CREATE TABLE freeze_requests (id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT NOT NULL,kind VARCHAR(20) NOT NULL,
 starts_on DATE NOT NULL,ends_on DATE NOT NULL,comment VARCHAR(1000) NOT NULL,status VARCHAR(20) NOT NULL,created_at TIMESTAMP(6) NOT NULL,
 policy VARCHAR(30),extension_days INT NOT NULL DEFAULT 0,FOREIGN KEY(user_id) REFERENCES users(id),
 CONSTRAINT ck_freeze_dates CHECK(ends_on>=starts_on),CONSTRAINT ck_freeze_kind CHECK(kind IN ('HEALTH','TRAVEL')),
 CONSTRAINT ck_freeze_status CHECK(status IN ('SENT','IN_REVIEW','APPROVED','REJECTED')),
 CONSTRAINT ck_freeze_days CHECK(extension_days>=0));
CREATE INDEX ix_freeze_status_date ON freeze_requests(status,starts_on);
CREATE INDEX ix_freeze_user_created ON freeze_requests(user_id,created_at);
CREATE TABLE private_files (id BIGINT AUTO_INCREMENT PRIMARY KEY,owner_id BIGINT NOT NULL,payment_id BIGINT UNIQUE,request_id BIGINT UNIQUE,
 storage_key VARCHAR(100) NOT NULL UNIQUE,original_name VARCHAR(160) NOT NULL,media_type VARCHAR(50) NOT NULL,bytes BIGINT NOT NULL,
 FOREIGN KEY(owner_id) REFERENCES users(id),FOREIGN KEY(payment_id) REFERENCES payments(id),FOREIGN KEY(request_id) REFERENCES freeze_requests(id),
 CONSTRAINT ck_file_parent CHECK((payment_id IS NOT NULL AND request_id IS NULL) OR (payment_id IS NULL AND request_id IS NOT NULL)),
 CONSTRAINT ck_file_size CHECK(bytes BETWEEN 1 AND 5242880));
CREATE TABLE reviews (id BIGINT AUTO_INCREMENT PRIMARY KEY,payment_id BIGINT,request_id BIGINT,reviewer_id BIGINT NOT NULL,
 from_status VARCHAR(20) NOT NULL,to_status VARCHAR(20) NOT NULL,comment VARCHAR(1000) NOT NULL,created_at TIMESTAMP(6) NOT NULL,
 FOREIGN KEY(payment_id) REFERENCES payments(id),FOREIGN KEY(request_id) REFERENCES freeze_requests(id),FOREIGN KEY(reviewer_id) REFERENCES users(id),
 CONSTRAINT ck_review_parent CHECK((payment_id IS NOT NULL AND request_id IS NULL) OR (payment_id IS NULL AND request_id IS NOT NULL)));
CREATE INDEX ix_review_payment ON reviews(payment_id,created_at);
CREATE INDEX ix_review_request ON reviews(request_id,created_at);
CREATE TABLE preferences (id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT NOT NULL UNIQUE,phone VARCHAR(20) NOT NULL,
 consent BOOLEAN NOT NULL DEFAULT FALSE,notice7 BOOLEAN NOT NULL DEFAULT TRUE,notice1 BOOLEAN NOT NULL DEFAULT TRUE,
 FOREIGN KEY(user_id) REFERENCES users(id));
CREATE TABLE reminders (id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT NOT NULL,expiry_date DATE NOT NULL,days_before INT NOT NULL,
 status VARCHAR(20) NOT NULL,processed_at TIMESTAMP(6),FOREIGN KEY(user_id) REFERENCES users(id),
 UNIQUE(user_id,expiry_date,days_before),CONSTRAINT ck_reminder_days CHECK(days_before IN(1,7)),
 CONSTRAINT ck_reminder_status CHECK(status IN('SCHEDULED','CANCELLED','SIMULATED','SENT','FAILED')));
CREATE INDEX ix_reminder_due ON reminders(status,expiry_date,days_before);
CREATE TABLE audit_events (id BIGINT AUTO_INCREMENT PRIMARY KEY,actor_id BIGINT,action VARCHAR(50) NOT NULL,
 target_type VARCHAR(40) NOT NULL,target_id BIGINT NOT NULL,detail VARCHAR(1000) NOT NULL,created_at TIMESTAMP(6) NOT NULL,
 FOREIGN KEY(actor_id) REFERENCES users(id));
CREATE INDEX ix_audit_created ON audit_events(created_at);
CREATE TABLE reset_tokens (id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT NOT NULL,token_hash VARCHAR(64) NOT NULL UNIQUE,
 expires_at TIMESTAMP(6) NOT NULL,used BOOLEAN NOT NULL,FOREIGN KEY(user_id) REFERENCES users(id));
CREATE INDEX ix_reset_user ON reset_tokens(user_id);
