ALTER TABLE branches ADD COLUMN capacity INT NOT NULL DEFAULT 30;
ALTER TABLE branches ADD CONSTRAINT ck_branch_capacity CHECK (capacity > 0);

CREATE TABLE attendance (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 branch_id BIGINT NOT NULL,
 user_id BIGINT NOT NULL,
 checked_in_at TIMESTAMP(6) NOT NULL,
 checked_out_at TIMESTAMP(6) NULL DEFAULT NULL,
 FOREIGN KEY(branch_id) REFERENCES branches(id),
 FOREIGN KEY(user_id) REFERENCES users(id),
 CONSTRAINT ck_attendance_times CHECK (checked_out_at IS NULL OR checked_out_at >= checked_in_at)
);
CREATE INDEX ix_attendance_branch_current ON attendance(branch_id,checked_out_at,checked_in_at);
CREATE INDEX ix_attendance_user_current ON attendance(user_id,checked_out_at);
