ALTER TABLE payments DROP CONSTRAINT ck_payment_method;
ALTER TABLE payments ADD CONSTRAINT ck_payment_method CHECK(method IN ('CARD_TEST','WALLET','TRANSFER','CULQI'));
ALTER TABLE payments DROP CONSTRAINT ck_manual_operation;
ALTER TABLE payments ADD CONSTRAINT ck_manual_operation CHECK(method IN ('CARD_TEST','CULQI') OR operation_number IS NOT NULL);
