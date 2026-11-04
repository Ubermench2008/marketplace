CREATE SEQUENCE public.product_review_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE public.product_review_moderation_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE public.product_review_entity (
    id bigint NOT NULL PRIMARY KEY,
    product_id bigint NOT NULL REFERENCES public.products(id),
    user_id bigint NOT NULL REFERENCES public.users(id),
    rating integer NOT NULL CHECK (rating BETWEEN 1 AND 5),
    text varchar(2000),
    created_at timestamp(6) with time zone NOT NULL,
    moderation_status varchar(255) NOT NULL
        CHECK (moderation_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT uk_product_review_product_user UNIQUE (product_id, user_id)
);

CREATE TABLE public.product_review_moderation_entity (
    id bigint NOT NULL PRIMARY KEY,
    public_id uuid NOT NULL UNIQUE,
    product_review_id bigint NOT NULL REFERENCES public.product_review_entity(id),
    status varchar(255) NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    submitted_at timestamp(6) with time zone NOT NULL,
    reviewed_at timestamp(6) with time zone,
    reviewed_by_id bigint REFERENCES public.users(id),
    rejection_reason varchar(2000),
    review_rating integer NOT NULL CHECK (review_rating BETWEEN 1 AND 5),
    review_text varchar(2000),
    review_version bigint NOT NULL CHECK (review_version >= 0)
);

CREATE INDEX idx_product_reviews_public_page
    ON public.product_review_entity (product_id, moderation_status, created_at DESC, id DESC);

CREATE UNIQUE INDEX idx_review_moderation_pending_review
    ON public.product_review_moderation_entity (product_review_id)
    WHERE status = 'PENDING';

CREATE INDEX idx_review_moderation_queue
    ON public.product_review_moderation_entity (status, submitted_at, id);

CREATE INDEX idx_review_moderation_review_history
    ON public.product_review_moderation_entity (product_review_id, submitted_at DESC, id DESC);
