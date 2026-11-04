CREATE SEQUENCE public.product_review_reply START WITH 1 INCREMENT BY 50;

CREATE TABLE public.product_review_reply_entity (
    id bigint NOT NULL PRIMARY KEY,
    review_id bigint NOT NULL REFERENCES public.product_review_entity(id),
    user_id bigint NOT NULL REFERENCES public.users(id),
    reply_to bigint REFERENCES public.product_review_reply_entity(id),
    is_seller boolean NOT NULL,
    text varchar(500),
    created_at timestamp(6) with time zone NOT NULL
);

CREATE INDEX idx_product_review_reply_page
    ON public.product_review_reply_entity (review_id, created_at, id);
