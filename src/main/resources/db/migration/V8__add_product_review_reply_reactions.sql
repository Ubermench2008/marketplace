ALTER TABLE public.product_review_reply_entity
    ADD COLUMN status varchar(255) NOT NULL DEFAULT 'PUBLISHED'
        CHECK (status IN ('PUBLISHED', 'HIDDEN'));

CREATE SEQUENCE public.product_review_reply_reaction_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE public.product_review_reply_reaction_entity (
    id bigint NOT NULL PRIMARY KEY,
    reply_id bigint NOT NULL REFERENCES public.product_review_reply_entity(id),
    user_id bigint NOT NULL REFERENCES public.users(id),
    type varchar(255) NOT NULL CHECK (type IN ('LIKE', 'DISLIKE')),
    CONSTRAINT uk_reply_reaction_reply_user UNIQUE (reply_id, user_id)
);

CREATE INDEX idx_reply_reaction_likes
    ON public.product_review_reply_reaction_entity (reply_id, type);
