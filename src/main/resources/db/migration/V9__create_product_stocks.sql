CREATE TABLE public.product_stocks (
    product_id bigint PRIMARY KEY REFERENCES public.products(id),
    quantity integer NOT NULL,
    CONSTRAINT ck_product_stocks_quantity_nonnegative CHECK (quantity >= 0)
);

INSERT INTO public.product_stocks (product_id, quantity)
SELECT id, 0
FROM public.products;
