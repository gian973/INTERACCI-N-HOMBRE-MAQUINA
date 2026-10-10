-- Keep only the facts needed to reconstruct these deterministic values.
-- appliedUntil = appliedFrom.plusMonths(contracted months).
-- extensionDays = inclusive interval length only when policy is EXTEND_DAYS.
ALTER TABLE payments DROP COLUMN applied_until;
ALTER TABLE freeze_requests DROP CONSTRAINT ck_freeze_days;
ALTER TABLE freeze_requests DROP COLUMN extension_days;
