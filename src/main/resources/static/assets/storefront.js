import { api, currentUser, hasRole, canShop, loginSession, pageData, pageQuery } from './api.js';
import { productReviews } from './reviews.js';
import {
    escapeHtml as e,
    icon,
    image,
    money,
    date,
    shortId,
    badge,
    loading,
    empty,
    pagination,
    perform,
    toast,
    showError,
    pageHeading,
    gallery,
    formError,
    confirmAction
} from './ui.js';

function notifyCart() {
    window.dispatchEvent(new Event('cartchange'));
}

export function productCard(product, seller = false) {
    const href = seller ? `#/seller/products/${product.id}` : `#/products/${product.id}`;
    return (
        `<article class="product-card">` +
        `<a class="product-image-link" href="${href}">${image(product.imgUrl, product.name)}<span class="product-state">${badge(product.status)}</span>` +
        `</a>` +
        `<div class="product-info">` +
        `<div class="product-price">${money(product.price)}</div>` +
        `<a class="product-name" href="${href}">${e(product.name)}</a>` +
        `<div class="product-card-bottom">` +
        `<span class="muted">${seller ? 'Ваш товар' : 'Находка рядом'}</span>${
            seller
                ? `<a href="${href}" class="icon-button" aria-label="Редактировать ${e(product.name)}">${icon('edit')}</a>`
                : !hasRole('ADMIN')
                  ? `<button type="button" class="add-cart-button" data-add="${e(product.id)}" ${product.status !== 'ACTIVE' ? 'disabled' : ''} aria-label="Добавить ${e(product.name)} в корзину">${icon('plus')}</button>`
                  : ''
        }</div>` +
        `</div>` +
        `</article>`
    );
}

export function bindCartButtons(container) {
    container.querySelectorAll('[data-add]').forEach(
        (button) =>
            (button.onclick = () =>
                perform(button, async () => {
                    if (!currentUser()) {
                        location.hash = '/login?next=/cart';
                        return;
                    }
                    await api('/cart/items', { method: 'POST', body: { productId: button.dataset.add } });
                    notifyCart();
                    toast('Товар добавлен в корзину');
                }))
    );
}

export async function catalogPage(view) {
    document.title = 'Каталог — Рядом';
    view.innerHTML =
        `<section class="catalog-hero">` +
        `<div>` +
        `<div class="eyebrow">` +
        `<span class="live-dot">` +
        `</span> МЕСТО ДЛЯ ВАШИХ НАХОДОК</div>` +
        `<h1>Хорошие вещи.<br>` +
        `<span>В одном месте.</span>` +
        `</h1>` +
        `<p>Выбирайте то, что вам по душе.<br>Собирайте свою корзину — остальное просто.</p>` +
        `<a class="hero-link" href="#catalog-products">Посмотреть товары ${icon('arrow')}</a>` +
        `</div>` +
        `<div class="hero-art" aria-hidden="true">` +
        `<span class="hero-orbit orbit-one">` +
        `</span>` +
        `<span class="hero-orbit orbit-two">` +
        `</span>` +
        `<div class="hero-sticker">сделано<br>для жизни<span>✳</span>` +
        `</div>` +
        `<div class="hero-bag">${icon('bag')}<span>рядом↗</span>` +
        `</div>` +
        `<span class="hero-caption">ВАША СЛЕДУЮЩАЯ НАХОДКА — ЗДЕСЬ</span>` +
        `</div>` +
        `</section>` +
        `<section id="catalog-products">` +
        `<div class="section-toolbar">` +
        `<div>` +
        `<div class="eyebrow">ВЫБИРАЙТЕ СВОЁ</div>` +
        `<h2>Каталог <span class="catalog-total">` +
        `</span>` +
        `</h2>` +
        `</div>` +
        `<label class="sort-label">Сортировка<select id="catalog-sort">` +
        `<option value="product.name,asc">По названию: А–Я</option>` +
        `<option value="product.price,asc">Сначала дешевле</option>` +
        `<option value="product.price,desc">Сначала дороже</option>` +
        `</select>` +
        `</label>` +
        `</div>` +
        `<div class="catalog-content">` +
        `</div>` +
        `</section>` +
        `<section class="small-banner">` +
        `<span class="banner-icon">${icon('box')}</span>` +
        `<div>` +
        `<h3>От находки — к покупке</h3>` +
        `<p>Сохраняйте товары в корзине и следите за заказами в одном месте.</p>` +
        `</div>` +
        `<span class="banner-star" aria-hidden="true">✳</span>` +
        `</section>`;
    // An ordinary anchor would replace our hash route; scroll without changing it.
    view.querySelector('.hero-link').onclick = (event) => {
        event.preventDefault();
        view.querySelector('#catalog-products').scrollIntoView({ behavior: 'smooth' });
    };
    const area = view.querySelector('.catalog-content');
    const sort = view.querySelector('#catalog-sort');
    let revision = 0;
    const load = async (number = 0) => {
        const ownRevision = ++revision;
        loading(area, true);
        try {
            const page = pageData(
                await api(`/catalog/products?${pageQuery(number, sort.value)}`, { publicRequest: true })
            );
            if (ownRevision !== revision) return;
            view.querySelector('.catalog-total').textContent = page.total;
            area.innerHTML = page.items.length
                ? `<div class="product-grid">${page.items.map((product) => productCard(product)).join('')}</div>`
                : empty('Пока без находок', 'Когда продавцы добавят товары, они появятся здесь.', null);
            bindCartButtons(area);
            pagination(area, page, load);
        } catch (error) {
            if (ownRevision === revision) showError(area, error, () => load(number));
        }
    };
    sort.onchange = () => load();
    await load();
}

export async function productPage(view, id) {
    const product = await api(`/products/${encodeURIComponent(id)}`, { publicRequest: true });
    if (!view.isConnected) return;
    document.title = `${product.name} — Рядом`;
    const active = product.status === 'ACTIVE';
    view.innerHTML =
        `<a class="back-link" href="#/catalog">← Каталог</a>` +
        `<div class="product-layout">` +
        `<section class="product-gallery" aria-label="Фотографии товара">` +
        `</section>` +
        `<section class="product-details">` +
        `<div class="eyebrow">ВАША НОВАЯ НАХОДКА</div>` +
        `<div class="status-row">${badge(product.status)}</div>` +
        `<h1>${e(product.name)}</h1>` +
        `<span class="product-slug">${e(product.slug)}</span>` +
        `<div class="details-price">${money(product.price)}</div>${!hasRole('ADMIN') ? `<button type="button" class="button buy-button" ${!active ? 'disabled' : ''}>${icon('cart')} ${active ? 'Добавить в корзину' : 'Сейчас недоступен'}</button>` : ''}<p class="fine-print">${active ? 'Окончательная стоимость фиксируется при оформлении заказа.' : 'Этот товар сейчас нельзя заказать.'}</p>` +
        `<div class="description">` +
        `<h2>О товаре</h2>` +
        `<p>${e(product.description)}</p>` +
        `</div>` +
        `<div class="details-note">${icon('bag')}<span>Всё необходимое — рядом.<br>Ваши покупки и история заказов в личном кабинете.</span>` +
        `</div>` +
        `</section>` +
        `</div>`;
    gallery(
        view.querySelector('.product-gallery'),
        [...new Set([product.previewImageUrl, ...(product.imageUrlList || [])].filter(Boolean))],
        product.name
    );
    const button = view.querySelector('.buy-button');
    if (button && active) {
        button.dataset.add = product.id;
        bindCartButtons(view);
    }
    const reviews = document.createElement('section');
    reviews.className = 'product-reviews';
    reviews.setAttribute('aria-label', 'Отзывы о товаре');
    view.append(reviews);
    await productReviews(reviews, product.id);
}

export async function authPage(view, mode) {
    const register = mode === 'register';
    document.title = `${register ? 'Регистрация' : 'Вход'} — Рядом`;
    const params = new URLSearchParams(location.hash.split('?')[1] || '');
    const next = params.get('next');
    const nextQuery = next ? `?next=${encodeURIComponent(next)}` : '';
    view.innerHTML =
        `<div class="auth-layout">` +
        `<section class="auth-story">` +
        `<div class="eyebrow">РАДЫ, ЧТО ВЫ РЯДОМ</div>` +
        `<h1>Маленькие находки.<br>` +
        `<span>Большая радость.</span>` +
        `</h1>` +
        `<p>Соберите корзину из того, что нравится.<br>А мы сохраним ваши покупки в одном месте.</p>` +
        `<div class="auth-symbol" aria-hidden="true">✳</div>` +
        `<div class="auth-story-footer">Ваше пространство для хороших вещей ↗</div>` +
        `</section>` +
        `<section class="auth-form-wrap">` +
        `<a class="back-link" href="#/catalog">← Вернуться в каталог</a>` +
        `<h2>${register ? 'Давайте знакомиться' : 'С возвращением'}</h2>` +
        `<p class="muted">${register ? 'Создайте аккаунт покупателя — это займёт минуту.' : 'Войдите по телефону или электронной почте.'}</p>` +
        `<form class="stack-form">` +
        `<div class="form-error" role="alert" tabindex="-1" hidden>` +
        `</div>${
            register
                ? `<label>Имя пользователя<input name="name" autocomplete="username" required minlength="3" maxlength="32" pattern="[A-Za-z0-9_]+" placeholder="Например, alex_01">` +
                  `<span class="field-hint">3–32 символа: латинские буквы, цифры и _</span>` +
                  `</label>` +
                  `<label>Телефон<input name="telephone" type="tel" autocomplete="tel" required placeholder="+7 999 123-45-67">` +
                  `</label>` +
                  `<label>Электронная почта <span class="optional">необязательно</span>` +
                  `<input name="email" type="email" autocomplete="email" maxlength="254" placeholder="you@example.com">` +
                  `</label>`
                : `<label>Телефон или email<input name="login" autocomplete="username" required placeholder="+7 999 123-45-67 или email"></label>`
        }<label>Пароль<div class="password-field">` +
        `<input name="password" type="password" autocomplete="${register ? 'new-password' : 'current-password'}" required ${register ? 'minlength="8" maxlength="72"' : ''} placeholder="${register ? 'Не менее 8 символов' : 'Ваш пароль'}">` +
        `<button class="text-button" type="button" id="toggle-password" aria-label="Показать пароль" aria-pressed="false">Показать</button>` +
        `</div>` +
        `</label>` +
        `<button class="button" type="submit">${register ? 'Создать аккаунт' : 'Войти'} ${icon('arrow')}</button>` +
        `</form>` +
        `<p class="auth-switch">${register ? 'Уже есть аккаунт?' : 'Пока нет аккаунта?'} <a href="#/${register ? 'login' : 'register'}${nextQuery}">${register ? 'Войти' : 'Зарегистрироваться'}</a>` +
        `</p>` +
        `</section>` +
        `</div>`;
    const form = view.querySelector('form');
    view.querySelector('#toggle-password').onclick = (event) => {
        const input = form.elements.password;
        const show = input.type === 'password';
        input.type = show ? 'text' : 'password';
        event.currentTarget.textContent = show ? 'Скрыть' : 'Показать';
        event.currentTarget.setAttribute('aria-pressed', String(show));
    };
    form.onsubmit = (event) => {
        event.preventDefault();
        const submit = form.querySelector('[type="submit"]');
        perform(submit, async () => {
            form.querySelector('.form-error').hidden = true;
            const body = Object.fromEntries(new FormData(form));
            try {
                if (register) {
                    body.email = body.email.trim() || null;
                    await api('/auth/register', { method: 'POST', body, publicRequest: true });
                    toast('Аккаунт создан. Теперь войдите.');
                    location.hash = `/login${nextQuery}`;
                } else {
                    body.login = body.login.trim();
                    const result = await api('/auth/login', { method: 'POST', body, publicRequest: true });
                    loginSession(result.accessToken, body.login);
                    location.hash = hasRole('ADMIN')
                        ? '/admin/users'
                        : next?.startsWith('/') && !next.startsWith('//')
                          ? next
                          : '/catalog';
                }
            } catch (error) {
                formError(form, error);
            }
        });
    };
}

export async function cartPage(view) {
    document.title = 'Корзина — Рядом';
    view.innerHTML = `${pageHeading('ВАШИ НАХОДКИ', 'Корзина', 'Всё, что понравилось, — в одном месте.')}<div class="cart-content"></div>`;
    const area = view.querySelector('.cart-content');
    let pending = false;
    const load = async () => {
        loading(area);
        try {
            const cart = await api('/cart');
            if (!cart.items.length) {
                area.innerHTML = empty(
                    'Здесь пока пусто',
                    'Найдите что-нибудь для себя и добавьте в корзину.'
                );
                return;
            }
            const unavailable = cart.items.some((item) => item.product.status !== 'ACTIVE');
            const quantity = cart.items.reduce((sum, item) => sum + item.quantity, 0);
            area.innerHTML =
                `<div class="cart-layout">` +
                `<section class="panel cart-items">` +
                `<div class="panel-toolbar">` +
                `<h2>Ваши товары <span class="count">${quantity}</span>` +
                `</h2>` +
                `<button class="text-button clear-cart" type="button">Очистить корзину</button>` +
                `</div>${cart.items
                    .map(
                        (item) =>
                            `<article class="cart-item" data-item="${item.id}">` +
                            `<a href="#/products/${e(item.product.id)}">${image(item.product.imgUrl, item.product.name)}</a>` +
                            `<div class="cart-item-info">` +
                            `<a class="item-name" href="#/products/${e(item.product.id)}">${e(item.product.name)}</a>${badge(item.product.status)}<span class="muted">${money(item.product.price)} / шт.</span>` +
                            `</div>` +
                            `<div class="quantity-control">` +
                            `<button type="button" data-quantity="${item.quantity - 1}" data-item-id="${item.id}" ${item.quantity <= 1 ? 'disabled' : ''} aria-label="Уменьшить количество ${e(item.product.name)}">−</button>` +
                            `<span aria-label="Количество">${item.quantity}</span>` +
                            `<button type="button" data-quantity="${item.quantity + 1}" data-item-id="${item.id}" aria-label="Увеличить количество ${e(item.product.name)}">+</button>` +
                            `</div>` +
                            `<strong>${money(item.subtotal)}</strong>` +
                            `<button type="button" class="icon-button remove-item" data-id="${item.id}" aria-label="Удалить ${e(item.product.name)}">${icon('trash')}</button>` +
                            `</article>`
                    )
                    .join('')}</section>` +
                `<aside class="panel order-summary">` +
                `<div class="eyebrow">ПОЧТИ ВАШЕ</div>` +
                `<h2>Ваш заказ</h2>` +
                `<div class="summary-line">` +
                `<span>Товаров</span>` +
                `<span>${quantity}</span>` +
                `</div>` +
                `<div class="summary-total">` +
                `<span>Итого</span>` +
                `<strong>${money(cart.totalPrice)}</strong>` +
                `</div>${unavailable ? '<p class="notice warning">В корзине есть недоступные товары. Удалите их, чтобы оформить заказ.</p>' : ''}<button type="button" class="button checkout" ${unavailable ? 'disabled' : ''}>Оформить заказ ${icon('arrow')}</button>` +
                `<p class="fine-print">Заказ оформляется сразу, без онлайн-оплаты. Товары переместятся из корзины в ваши заказы.</p>` +
                `</aside>` +
                `</div>`;
            const mutate = (button, task) =>
                perform(button, async () => {
                    if (pending) return;
                    pending = true;
                    try {
                        await task();
                        notifyCart();
                        await load();
                    } finally {
                        pending = false;
                    }
                });
            area.querySelectorAll('[data-quantity]').forEach(
                (button) =>
                    (button.onclick = () =>
                        mutate(button, () =>
                            api(`/cart/items/${button.dataset.itemId}`, {
                                method: 'PATCH',
                                body: { quantity: Number(button.dataset.quantity) }
                            })
                        ))
            );
            area.querySelectorAll('.remove-item').forEach(
                (button) =>
                    (button.onclick = () =>
                        mutate(button, () => api(`/cart/items/${button.dataset.id}`, { method: 'DELETE' })))
            );
            const clear = area.querySelector('.clear-cart');
            clear.onclick = () =>
                mutate(clear, async () => {
                    if (
                        await confirmAction(
                            'Очистить корзину?',
                            'Все позиции будут удалены из корзины.',
                            'Очистить',
                            true
                        )
                    )
                        await api('/cart/items', { method: 'DELETE' });
                });
            const checkout = area.querySelector('.checkout');
            checkout.onclick = () =>
                perform(checkout, async () => {
                    if (pending) return;
                    pending = true;
                    try {
                        if (
                            !(await confirmAction(
                                'Оформить заказ?',
                                `Сумма заказа — ${money(cart.totalPrice)}. После оформления корзина будет очищена.`,
                                'Оформить'
                            ))
                        )
                            return;
                        const order = await api('/orders', { method: 'POST' });
                        notifyCart();
                        toast(`Заказ №${order.id} оформлен`);
                        location.hash = `/orders/${order.id}`;
                    } finally {
                        pending = false;
                    }
                });
        } catch (error) {
            showError(area, error, load);
        }
    };
    await load();
}

export function orderMarkup(order, admin = false) {
    return (
        `<article class="panel order-card">` +
        `<div class="order-card-heading">` +
        `<div>` +
        `<h2>Заказ №${e(order.id)}</h2>` +
        `<p class="muted">${date(order.createdAt)}${admin ? ` · Пользователь #${e(order.userId)}` : ''}</p>` +
        `</div>${badge(order.status)}</div>` +
        `<div class="order-lines">${order.items
            .map(
                (item) =>
                    `<div class="order-line">` +
                    `<span class="order-product-icon">${icon('box')}</span>` +
                    `<a href="#/products/${e(item.productId)}">Товар · ${e(shortId(item.productId))}</a>` +
                    `<span>${item.quantity} × ${money(item.unitPrice)}</span>` +
                    `<strong>${money(item.subtotal)}</strong>` +
                    `</div>`
            )
            .join(
                ''
            )}</div>${order.deliveredAt ? `<p class="muted">Доставлен: ${date(order.deliveredAt)}</p>` : ''}<div class="order-card-footer">` +
        `<span>Итого <strong>${money(order.totalPrice)}</strong>` +
        `</span>` +
        `<div class="button-row">${order.status === 'IN_DELIVERY' ? `<button type="button" class="button secondary small" data-cancel-order="${order.id}">Отменить заказ</button>${admin ? `<button type="button" class="button small" data-deliver-order="${order.id}">Отметить доставленным</button>` : ''}` : ''}</div>` +
        `</div>` +
        `</article>`
    );
}

export function bindOrderActions(container, admin, onChanged) {
    container.querySelectorAll('[data-cancel-order], [data-deliver-order]').forEach(
        (button) =>
            (button.onclick = () =>
                perform(button, async () => {
                    const cancel = !!button.dataset.cancelOrder;
                    const id = button.dataset.cancelOrder || button.dataset.deliverOrder;
                    if (
                        !(await confirmAction(
                            cancel ? 'Отменить заказ?' : 'Заказ доставлен?',
                            cancel
                                ? 'Отмена необратима. Товары не вернутся в корзину автоматически.'
                                : 'После этого отменить заказ будет нельзя.',
                            cancel ? 'Отменить заказ' : 'Подтвердить доставку',
                            cancel
                        ))
                    )
                        return;
                    await api(`${admin ? '/admin' : ''}/orders/${id}/${cancel ? 'cancel' : 'deliver'}`, {
                        method: 'POST'
                    });
                    toast(cancel ? 'Заказ отменён' : 'Доставка подтверждена');
                    await onChanged();
                }))
    );
}

export async function ordersPage(view, id) {
    document.title = 'Мои заказы — Рядом';
    if (id) {
        const order = await api(`/orders/${encodeURIComponent(id)}`);
        view.innerHTML = `<a class="back-link" href="#/orders">← Все заказы</a>${pageHeading('ВАШИ ПОКУПКИ', `Заказ №${order.id}`)}${orderMarkup(order)}`;
        bindOrderActions(view, false, () => ordersPage(view, id));
        return;
    }
    view.innerHTML =
        `${pageHeading('ВАШИ ПОКУПКИ', 'Заказы', 'Следите за доставкой и возвращайтесь к истории покупок.')}<div class="tabs" role="group" aria-label="Раздел заказов">` +
        `<button type="button" class="tab active" data-type="current" aria-pressed="true">Текущие</button>` +
        `<button type="button" class="tab" data-type="history" aria-pressed="false">История</button>` +
        `</div>` +
        `<div class="orders-content">` +
        `</div>`;
    const area = view.querySelector('.orders-content');
    let type = 'current';
    let revision = 0;
    const load = async (number = 0) => {
        const ownRevision = ++revision;
        loading(area);
        try {
            const page = pageData(await api(`/orders/${type}?${pageQuery(number, 'createdAt,desc')}`));
            if (ownRevision !== revision) return;
            area.innerHTML = page.items.length
                ? page.items.map((order) => orderMarkup(order)).join('')
                : empty(
                      type === 'current' ? 'Ждём ваши первые покупки' : 'История пока пуста',
                      type === 'current'
                          ? 'Оформите заказ из корзины — здесь появится его статус.'
                          : 'Завершённые и отменённые заказы будут здесь.'
                  );
            bindOrderActions(area, false, () => load(number));
            pagination(area, page, load);
        } catch (error) {
            if (ownRevision === revision) showError(area, error, () => load(number));
        }
    };
    view.querySelectorAll('[data-type]').forEach(
        (button) =>
            (button.onclick = () => {
                type = button.dataset.type;
                view.querySelectorAll('[data-type]').forEach((tab) => {
                    tab.classList.toggle('active', tab === button);
                    tab.setAttribute('aria-pressed', String(tab === button));
                });
                load();
            })
    );
    await load();
}
