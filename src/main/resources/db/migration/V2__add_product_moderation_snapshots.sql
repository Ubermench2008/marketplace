ALTER TABLE public.products
    ADD COLUMN status varchar(255) NOT NULL DEFAULT 'OFF',
    ADD COLUMN moderation_status varchar(255) NOT NULL DEFAULT 'NOT_SUBMITTED';

ALTER TABLE public.products
    ALTER COLUMN status DROP DEFAULT,
    ALTER COLUMN moderation_status DROP DEFAULT,
    ADD CONSTRAINT products_status_check CHECK (status IN ('ACTIVE', 'OFF', 'ARCHIVED')),
    ADD CONSTRAINT products_moderation_status_check
        CHECK (moderation_status IN ('NOT_SUBMITTED', 'PENDING', 'APPROVED', 'REJECTED'));

CREATE SEQUENCE public.product_moderation_seq
    START WITH 1
    INCREMENT BY 50;

CREATE TABLE public.product_moderation (
    id bigint NOT NULL PRIMARY KEY,
    public_id uuid NOT NULL UNIQUE,
    product_id bigint NOT NULL REFERENCES public.products(id),
    status varchar(255) NOT NULL,
    submitted_at timestamp(6) with time zone NOT NULL,
    reviewed_at timestamp(6) with time zone,
    reviewed_by_id bigint REFERENCES public.users(id),
    rejection_reason varchar(2000),
    snapshot_name varchar(255) NOT NULL,
    snapshot_slug varchar(255) NOT NULL,
    snapshot_description text NOT NULL,
    snapshot_price numeric(38,2) NOT NULL,
    snapshot_preview_image_url varchar(255) NOT NULL,
    CONSTRAINT product_moderation_status_check CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE TABLE public.product_moderation_images (
    moderation_id bigint NOT NULL REFERENCES public.product_moderation(id),
    "position" integer NOT NULL,
    image_url varchar(255) NOT NULL,
    PRIMARY KEY (moderation_id, "position")
);

CREATE UNIQUE INDEX idx_product_moderation_unreviewed_product
    ON public.product_moderation (product_id)
    WHERE reviewed_at IS NULL;

CREATE INDEX idx_product_moderation_pending_queue
    ON public.product_moderation (submitted_at, id)
    WHERE status = 'PENDING';

CREATE INDEX idx_product_moderation_preview_image
    ON public.product_moderation (snapshot_preview_image_url);

CREATE INDEX idx_product_moderation_image_url
    ON public.product_moderation_images (image_url);
