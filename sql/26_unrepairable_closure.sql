-- Upgrade an existing database before deploying code that uses UNREPAIRABLE.
ALTER TABLE mnt_work_order DROP CHECK chk_work_order_status;
ALTER TABLE mnt_work_order DROP CHECK chk_work_order_complete;
ALTER TABLE mnt_work_order ADD CONSTRAINT chk_work_order_status CHECK (status IN ('PENDING_ASSIGN','ASSIGNED','PROCESSING','SUSPENDED','PENDING_ACCEPT','COMPLETED','UNREPAIRABLE','CANCELLED'));
ALTER TABLE mnt_work_order ADD CONSTRAINT chk_work_order_complete CHECK ((status IN ('COMPLETED','UNREPAIRABLE') AND completed_at IS NOT NULL) OR status NOT IN ('COMPLETED','UNREPAIRABLE'));

ALTER TABLE mnt_work_order_flow DROP CHECK chk_work_order_flow_from;
ALTER TABLE mnt_work_order_flow DROP CHECK chk_work_order_flow_to;
ALTER TABLE mnt_work_order_flow DROP CHECK chk_work_order_flow_action;
ALTER TABLE mnt_work_order_flow ADD CONSTRAINT chk_work_order_flow_from CHECK (from_status IS NULL OR from_status IN ('PENDING_ASSIGN','ASSIGNED','PROCESSING','SUSPENDED','PENDING_ACCEPT','COMPLETED','UNREPAIRABLE','CANCELLED'));
ALTER TABLE mnt_work_order_flow ADD CONSTRAINT chk_work_order_flow_to CHECK (to_status IN ('PENDING_ASSIGN','ASSIGNED','PROCESSING','SUSPENDED','PENDING_ACCEPT','COMPLETED','UNREPAIRABLE','CANCELLED'));
ALTER TABLE mnt_work_order_flow ADD CONSTRAINT chk_work_order_flow_action CHECK (action IN ('CREATE','ASSIGN','REASSIGN','ACCEPT','START','SUSPEND','RESUME','SUBMIT','ACCEPT_PASS','ACCEPT_RETURN','CLOSE_UNREPAIRABLE','CANCEL'));
