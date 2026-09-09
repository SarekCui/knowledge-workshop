ALTER TABLE group_participant
    DROP INDEX uk_participant_request,
    DROP COLUMN request_id;

ALTER TABLE trade_order
    DROP INDEX uk_order_request,
    DROP COLUMN business_request_id,
    ADD UNIQUE KEY uk_order_group_user (group_id, user_id);
