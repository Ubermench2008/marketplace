CREATE TABLE public.cart_items (
    quantity integer NOT NULL,
    cart_id bigint NOT NULL,
    id bigint NOT NULL,
    product_id bigint NOT NULL
);

CREATE SEQUENCE public.cart_items_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.order_items (
    quantity integer NOT NULL,
    unit_price numeric(19,2) NOT NULL,
    id bigint NOT NULL,
    order_id bigint NOT NULL,
    product_id bigint NOT NULL,
    CONSTRAINT order_items_quantity_check CHECK ((quantity >= 1))
);

CREATE SEQUENCE public.order_items_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.orders (
    total_price numeric(19,2) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    delivered_at timestamp(6) with time zone,
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    status character varying(255) NOT NULL,
    CONSTRAINT orders_status_check CHECK (((status)::text = ANY ((ARRAY['IN_DELIVERY'::character varying, 'DELIVERED'::character varying, 'CANCELLED'::character varying])::text[])))
);

CREATE SEQUENCE public.orders_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.product_cards (
    id bigint NOT NULL,
    product_id bigint NOT NULL,
    img_url character varying(255)
);

CREATE SEQUENCE public.product_cards_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.product_details_images (
    "position" integer NOT NULL,
    id bigint NOT NULL,
    product_id bigint NOT NULL,
    public_id uuid NOT NULL,
    image_url character varying(255) NOT NULL
);

CREATE SEQUENCE public.product_details_images_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.products (
    price numeric(38,2) NOT NULL,
    id bigint NOT NULL,
    seller_id bigint NOT NULL,
    public_id uuid NOT NULL,
    description character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    slug character varying(255) NOT NULL
);

CREATE SEQUENCE public.products_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.user_carts (
    id bigint NOT NULL,
    user_id bigint NOT NULL
);

CREATE SEQUENCE public.user_carts_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.user_role (
    user_id bigint NOT NULL,
    role character varying(255) NOT NULL,
    CONSTRAINT user_role_role_check CHECK (((role)::text = ANY ((ARRAY['BUYER'::character varying, 'SELLER'::character varying, 'ADMIN'::character varying])::text[])))
);

CREATE TABLE public.users (
    id bigint NOT NULL,
    email character varying(255),
    name character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    telephone_number character varying(255) NOT NULL
);

CREATE SEQUENCE public.users_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT cart_items_cart_id_product_id_key UNIQUE (cart_id, product_id);

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT cart_items_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT order_items_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.product_cards
    ADD CONSTRAINT product_cards_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.product_cards
    ADD CONSTRAINT product_cards_product_id_key UNIQUE (product_id);

ALTER TABLE ONLY public.product_details_images
    ADD CONSTRAINT product_details_images_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.product_details_images
    ADD CONSTRAINT product_details_images_public_id_key UNIQUE (public_id);

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_public_id_key UNIQUE (public_id);

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_slug_key UNIQUE (slug);

ALTER TABLE ONLY public.user_carts
    ADD CONSTRAINT user_carts_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.user_carts
    ADD CONSTRAINT user_carts_user_id_key UNIQUE (user_id);

ALTER TABLE ONLY public.user_role
    ADD CONSTRAINT user_role_pkey PRIMARY KEY (user_id, role);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_telephone_number_key UNIQUE (telephone_number);

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT fk1re40cjegsfvw58xrkdp6bac6 FOREIGN KEY (product_id) REFERENCES public.products(id);

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT fk32ql8ubntj5uh44ph9659tiih FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.user_carts
    ADD CONSTRAINT fk71eql61lq7h25ivtn0eq7mdsc FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.products
    ADD CONSTRAINT fkbgw3lyxhsml3kfqnfr45o0vbj FOREIGN KEY (seller_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT fkbioxgbv59vetrxe0ejfubep1w FOREIGN KEY (order_id) REFERENCES public.orders(id);

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT fkdn7l6hbwg1ofovqm1vp6ay7dg FOREIGN KEY (cart_id) REFERENCES public.user_carts(id);

ALTER TABLE ONLY public.user_role
    ADD CONSTRAINT fkj345gk1bovqvfame88rcx7yyx FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.product_details_images
    ADD CONSTRAINT fkmmfxnmjw501ua51p28k0jnp27 FOREIGN KEY (product_id) REFERENCES public.products(id);

ALTER TABLE ONLY public.product_cards
    ADD CONSTRAINT fkn4h5cmvoi5vgq7fjfrg7p17fe FOREIGN KEY (product_id) REFERENCES public.products(id);

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT fkocimc7dtr037rh4ls4l95nlfi FOREIGN KEY (product_id) REFERENCES public.products(id);
