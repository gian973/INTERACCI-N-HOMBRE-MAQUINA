CREATE TABLE branches (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(100) NOT NULL UNIQUE,
 active BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO branches (id,name,active) VALUES
 (1,'San Miguel',TRUE),
 (2,'Pueblo Libre',TRUE),
 (3,'Magdalena',TRUE);

ALTER TABLE payments ADD COLUMN branch_id BIGINT;
ALTER TABLE payments ADD COLUMN branch_name VARCHAR(100);
ALTER TABLE payments ADD CONSTRAINT fk_payment_branch FOREIGN KEY(branch_id) REFERENCES branches(id);
ALTER TABLE payments ADD CONSTRAINT ck_payment_branch CHECK (
 (branch_id IS NULL AND branch_name IS NULL) OR (branch_id IS NOT NULL AND branch_name IS NOT NULL)
);
ALTER TABLE memberships ADD COLUMN branch_id BIGINT;
ALTER TABLE memberships ADD CONSTRAINT fk_membership_branch FOREIGN KEY(branch_id) REFERENCES branches(id);
CREATE INDEX ix_membership_branch_expiry ON memberships(branch_id,expires_on);
CREATE INDEX ix_payment_branch_status ON payments(branch_id,status);
