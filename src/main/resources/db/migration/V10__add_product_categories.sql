CREATE SEQUENCE public.product_categories_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE public.product_categories (
    id bigint PRIMARY KEY,
    name varchar(255) NOT NULL UNIQUE,
    slug varchar(255) NOT NULL UNIQUE
);

INSERT INTO public.product_categories (id, name, slug)
VALUES
    (nextval('public.product_categories_seq'), 'Другое', 'other'),
    (nextval('public.product_categories_seq'), 'Спорт и фитнес', 'sport-fitness'),
    (nextval('public.product_categories_seq'), 'Дом и мебель', 'home-furniture'),
    (nextval('public.product_categories_seq'), 'Электроника', 'electronics');

ALTER TABLE public.products ADD COLUMN category_id bigint;

UPDATE public.products
SET category_id = (SELECT id FROM public.product_categories WHERE slug = 'other');

ALTER TABLE public.products
    ALTER COLUMN category_id SET NOT NULL,
    ADD CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES public.product_categories(id);
