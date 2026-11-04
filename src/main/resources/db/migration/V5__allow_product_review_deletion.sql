ALTER TABLE public.product_review_entity
    ADD COLUMN deleted_at timestamp(6) with time zone,
    DROP CONSTRAINT uk_product_review_product_user,
    DROP CONSTRAINT product_review_entity_moderation_status_check,
    ADD CONSTRAINT product_review_entity_moderation_status_check
        CHECK (moderation_status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'));

CREATE UNIQUE INDEX uk_product_review_product_user
    ON public.product_review_entity (product_id, user_id)
    WHERE deleted_at IS NULL;

DROP INDEX public.idx_product_reviews_public_page;
CREATE INDEX idx_product_reviews_public_page
    ON public.product_review_entity (product_id, moderation_status, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;

ALTER TABLE public.product_review_moderation_entity
    DROP CONSTRAINT product_review_moderation_entity_status_check,
    ADD CONSTRAINT product_review_moderation_entity_status_check
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'));
