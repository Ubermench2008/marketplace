ALTER TABLE public.product_moderation
    DROP CONSTRAINT product_moderation_status_check,
    ADD CONSTRAINT product_moderation_status_check
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'));

DROP INDEX public.idx_product_moderation_unreviewed_product;

CREATE UNIQUE INDEX idx_product_moderation_pending_product
    ON public.product_moderation (product_id)
    WHERE status = 'PENDING';
