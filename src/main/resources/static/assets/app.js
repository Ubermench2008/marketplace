import { currentUser, hasRole, canShop, logout, api } from './api.js';
import { icon, escapeHtml, toast, loading, empty, showError } from './ui.js';
import { catalogPage, productPage, authPage, cartPage, ordersPage } from './storefront.js';
import { sellerPage, productEditor } from './seller.js';
import { usersPage, userEditor, adminUserPage, moderationPage, moderationDetails } from './admin.js';
import { reviewModerationPage, reviewModerationDetails } from './admin-reviews.js';
import { replyModerationPage } from './replies.js';

function header() {
    const user = currentUser();
    const active = location.hash;
    const nav = (href, label, symbol, selected = active.startsWith(href)) =>
        `<a href="${href}" class="nav-link ${selected ? 'active' : ''}">${icon(symbol)}<span>${label}</span></a>`;
    document.querySelector('#header').innerHTML =
        `<div class="header-inner">` +
        `<a class="brand" href="#/catalog" aria-label="Рядом — в каталог">` +
        `<span class="brand-mark">${icon('grid')}</span>` +
        `<span class="wordmark">рядом<span>↗</span>` +
        `</span>` +
        `</a>` +
        `<nav class="main-nav" aria-label="Главное меню">${
            hasRole('ADMIN')
                ? nav('#/admin/users', 'Пользователи', 'user') +
                  nav(
                      '#/admin/moderation',
                      'Модерация',
                      'shield',
                      active.startsWith('#/admin/moderation') ||
                          active.startsWith('#/admin/review-moderation')
                  )
                : nav('#/catalog', 'Каталог', 'grid') +
                  (canShop() ? nav('#/orders', 'Заказы', 'box') : '') +
                  (hasRole('SELLER') ? nav('#/seller', 'Мои товары', 'bag') : '')
        }</nav>` +
        `<div class="header-actions">${canShop() ? `<a class="cart-link" href="#/cart" aria-label="Корзина">${icon('cart')}<span class="cart-word">Корзина</span><span class="cart-count" hidden>0</span></a>` : ''}${
            user
                ? `<div class="account">` +
                  `<span class="avatar">${icon('user')}</span>` +
                  `<span class="account-label">${escapeHtml(hasRole('ADMIN') ? 'Администратор' : user.label)}</span>` +
                  `<button class="text-button" id="logout" type="button">Выйти</button>` +
                  `</div>`
                : `<a class="button small" href="#/login">${icon('user')} Войти</a>`
        }</div>` +
        `</div>`;
    document.querySelector('#logout')?.addEventListener('click', () => {
        logout();
        location.hash = '/catalog';
        render();
    });
    refreshCartBadge();
}

async function refreshCartBadge() {
    if (!canShop()) return;
    try {
        const data = await api('/cart');
        const count = data.items.reduce((sum, item) => sum + item.quantity, 0);
        const badge = document.querySelector('.cart-count');
        if (badge) {
            badge.textContent = count;
            badge.hidden = !count;
        }
    } catch {
        /* The cart page handles errors; navigation stays usable. */
    }
}

let renderedHash = null;
let navigationRevision = 0;

async function render() {
    const ownRevision = ++navigationRevision;
    const main = document.querySelector('#main');
    if (
        renderedHash !== null &&
        renderedHash !== location.hash &&
        main.firstElementChild?.beforeLeave &&
        currentUser()
    ) {
        const canLeave = await main.firstElementChild.beforeLeave();
        if (ownRevision !== navigationRevision) return;
        if (!canLeave) {
            history.replaceState(null, '', renderedHash || '#/catalog');
            return;
        }
    }
    renderedHash = location.hash;
    header();
    main.firstElementChild?.dispatchEvent(new Event('dispose'));
    const view = document.createElement('section');
    view.className = 'page container';
    main.replaceChildren(view);
    window.scrollTo({ top: 0, left: 0, behavior: 'instant' });
    const route = (location.hash.slice(1) || '/catalog').split('?')[0];
    const parts = route.split('/').filter(Boolean);
    document.title = 'Рядом — маркетплейс';
    const protectedPage = ['cart', 'orders', 'seller', 'admin'].includes(parts[0]);
    if (protectedPage && !currentUser()) {
        const redirect = `#/login?next=${encodeURIComponent(route)}`;
        view.innerHTML = empty(
            'Войдите в аккаунт',
            'Здесь будут ваши товары, покупки и заказы.',
            redirect,
            'Войти'
        );
        return;
    }
    if (
        (parts[0] === 'admin' && !hasRole('ADMIN')) ||
        (parts[0] === 'seller' && (!hasRole('SELLER') || hasRole('ADMIN'))) ||
        (['cart', 'orders'].includes(parts[0]) && !canShop())
    ) {
        view.innerHTML = empty(
            'Раздел недоступен',
            'У вашего аккаунта нет доступа к этому разделу.',
            hasRole('ADMIN') ? '#/admin/users' : '#/catalog',
            'На главную'
        );
        return;
    }
    loading(view);
    try {
        if (!parts.length || parts[0] === 'catalog') await catalogPage(view);
        else if (parts[0] === 'login' || parts[0] === 'register') await authPage(view, parts[0]);
        else if (parts[0] === 'products' && parts[1]) await productPage(view, parts[1]);
        else if (parts[0] === 'cart') await cartPage(view);
        else if (parts[0] === 'orders') await ordersPage(view, parts[1]);
        else if (parts[0] === 'seller' && parts[1] === 'products' && parts[2])
            await productEditor(view, parts[2]);
        else if (parts[0] === 'seller') await sellerPage(view);
        else if (parts[0] === 'admin' && parts[1] === 'moderation')
            await (parts[2] ? moderationDetails(view, parts[2]) : moderationPage(view));
        else if (parts[0] === 'admin' && parts[1] === 'review-moderation')
            await (parts[2] ? reviewModerationDetails(view, parts[2]) : reviewModerationPage(view));
        else if (parts[0] === 'admin' && parts[1] === 'reply-moderation') await replyModerationPage(view);
        else if (parts[0] === 'admin' && parts[1] === 'users' && parts[2] === 'new') await userEditor(view);
        else if (parts[0] === 'admin' && parts[1] === 'users' && parts[3] === 'edit')
            await userEditor(view, parts[2]);
        else if (parts[0] === 'admin' && parts[1] === 'users' && parts[2])
            await adminUserPage(view, parts[2]);
        else if (parts[0] === 'admin' && parts[1] === 'users') await usersPage(view);
        else view.innerHTML = empty('Такой страницы нет', 'Проверьте адрес или вернитесь в каталог.');
    } catch (error) {
        showError(view, error, render);
    }
}

window.addEventListener('hashchange', render);
window.addEventListener('cartchange', refreshCartBadge);
window.addEventListener('sessionchange', header);
window.addEventListener('sessionexpired', () => {
    toast('Сессия завершена. Войдите снова.', 'error');
    render();
});
document.querySelector('.skip-link').onclick = (event) => {
    event.preventDefault();
    document.querySelector('#main').focus();
};
// No refresh-token endpoint exists: access-token expiration requires another login.
setInterval(() => {
    const hadSession = !!document.querySelector('#logout');
    if (hadSession && !currentUser()) render();
}, 15000);
render();
