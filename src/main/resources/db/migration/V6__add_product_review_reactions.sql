CREATE SEQUENCE public.product_review_reaction_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE public.product_review_reaction_entity (
    id bigint NOT NULL PRIMARY KEY,
    review_id bigint NOT NULL REFERENCES public.product_review_entity(id),
    user_id bigint NOT NULL REFERENCES public.users(id),
    type varchar(255) NOT NULL CHECK (type IN ('LIKE', 'DISLIKE')),
    CONSTRAINT uk_product_review_reaction_review_user UNIQUE (review_id, user_id)
);

CREATE INDEX idx_product_review_reaction_user
    ON public.product_review_reaction_entity (user_id, review_id);
