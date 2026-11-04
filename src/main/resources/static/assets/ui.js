export const escapeHtml = (value) =>
    String(value ?? '').replace(
        /[&<>"']/g,
        (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char]
    );
export const money = (value) =>
    new Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'RUB', maximumFractionDigits: 2 }).format(
        value ?? 0
    );
export const date = (value) =>
    value
        ? new Intl.DateTimeFormat('ru-RU', { dateStyle: 'medium', timeStyle: 'short' }).format(
              new Date(value)
          )
        : '—';
export const shortId = (id) => String(id ?? '').slice(0, 8);
const names = {
    ACTIVE: 'В продаже',
    OFF: 'Снят с продажи',
    ARCHIVED: 'В архиве',
    NOT_SUBMITTED: 'Не отправлен',
    PENDING: 'На модерации',
    APPROVED: 'Одобрен',
    REJECTED: 'Отклонён',
    IN_DELIVERY: 'В доставке',
    DELIVERED: 'Доставлен',
    CANCELLED: 'Отменён',
    BUYER: 'Покупатель',
    SELLER: 'Продавец',
    ADMIN: 'Администратор'
};
export const statusName = (status) => names[status] || status || 'Не указан';
export const badge = (status) =>
    `<span class="badge badge-${escapeHtml(status)}">${escapeHtml(statusName(status))}</span>`;
const paths = {
    like: '<path d="M7 10v11H3V10h4Zm0 0 5-7a2 2 0 0 1 2 2l-1 5h6a2 2 0 0 1 2 2l-2 7a3 3 0 0 1-3 2H7"/>',
    dislike: '<path d="M7 14V3H3v11h4Zm0 0 5 7a2 2 0 0 0 2-2l-1-5h6a2 2 0 0 0 2-2l-2-7a3 3 0 0 0-3-2H7"/>',
    bag: '<path d="M6 7h12l2 14H4L6 7Z"/><path d="M9 7V5a3 3 0 0 1 6 0v2"/>',
    cart: '<path d="M3 3h2l3 13h10l3-10H6"/><circle cx="9" cy="20" r="1"/><circle cx="18" cy="20" r="1"/>',
    user: '<circle cx="12" cy="8" r="4"/><path d="M4 21v-2a8 8 0 0 1 16 0v2"/>',
    arrow: '<path d="M5 12h14m-6-6 6 6-6 6"/>',
    box: '<path d="m12 3 9 5-9 5-9-5 9-5Zm-9 5v10l9 5 9-5V8M12 13v10M7.5 5.5l9 5"/>',
    check: '<path d="m5 12 4 4L19 6"/>',
    close: '<path d="m6 6 12 12M6 18 18 6"/>',
    plus: '<path d="M12 5v14M5 12h14"/>',
    trash: '<path d="M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7M14 10v7"/>',
    image: '<rect x="3" y="3" width="18" height="18" rx="3"/><circle cx="8" cy="8" r="1"/><path d="m3 17 5-5 4 4 4-6 5 7"/>',
    edit: '<path d="m16 3 5 5-12 12-6 1 1-6L16 3ZM14 5l5 5"/>',
    grid:
        '<rect x="3" y="3" width="7" height="7" rx="1"/>' +
        '<rect x="14" y="3" width="7" height="7" rx="1"/>' +
        '<rect x="3" y="14" width="7" height="7" rx="1"/>' +
        '<rect x="14" y="14" width="7" height="7" rx="1"/>',
    shield: '<path d="m12 3 8 3v6c0 5-8 9-8 9s-8-4-8-9V6l8-3Z"/><path d="m8 12 3 3 5-6"/>'
};
export const icon = (name) =>
    `<svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">${paths[name] || paths.box}</svg>`;

export function mediaUrl(url) {
    return typeof url === 'string' && url.startsWith('/media/') && !url.includes('\\') ? url : '';
}

export function image(url, alt, className = '') {
    const src = mediaUrl(url);
    return `<div class="image-frame ${className}">${src ? `<img src="${escapeHtml(src)}" alt="${escapeHtml(alt)}" loading="lazy" decoding="async">` : `<span class="image-placeholder">${icon('image')}<span>Нет изображения</span></span>`}</div>`;
}

document.addEventListener(
    'error',
    (event) => {
        if (event.target instanceof HTMLImageElement) {
            const frame = event.target.closest('.image-frame');
            if (frame)
                frame.innerHTML = `<span class="image-placeholder">${icon('image')}<span>Фото недоступно</span></span>`;
        }
    },
    true
);

export function toast(message, type = 'success') {
    const node = document.createElement('div');
    node.className = `toast toast-${type}`;
    node.setAttribute('role', type === 'error' ? 'alert' : 'status');
    node.innerHTML = `${icon(type === 'error' ? 'close' : 'check')}<span>${escapeHtml(message)}</span><button type="button" class="icon-button" aria-label="Закрыть">${icon('close')}</button>`;
    node.querySelector('button').onclick = () => node.remove();
    document.querySelector('#notifications').append(node);
    setTimeout(() => node.remove(), type === 'error' ? 12000 : 5500);
}

export function errorMessage(error) {
    const details = error.errors?.map((item) => `${item.field}: ${item.message}`).join(' · ');
    return details ? `${error.message} ${details}` : error.message;
}

export function showError(container, error, retry) {
    container.innerHTML =
        `<div class="empty-state error-state">${icon('box')}<h2>Не получилось загрузить</h2>` +
        `<p>${escapeHtml(errorMessage(error))}</p>${retry ? '<button class="button secondary" type="button">Попробовать ещё раз</button>' : ''}</div>`;
    if (retry) container.querySelector('button').onclick = retry;
}

export function loading(container, cards = false) {
    container.innerHTML = cards
        ? `<div class="product-grid" aria-label="Загрузка товаров" aria-busy="true">${Array.from({ length: 8 }, () => '<div class="skeleton-card"><div class="skeleton skeleton-image"></div><div class="skeleton skeleton-line"></div><div class="skeleton skeleton-line short"></div></div>').join('')}</div>`
        : '<div class="loading" role="status"><span class="spinner"></span> Загружаем…</div>';
}

export function empty(title, text, link = '#/catalog', label = 'Перейти в каталог') {
    return (
        `<div class="empty-state">${icon('bag')}<h2>${escapeHtml(title)}</h2>` +
        `<p>${escapeHtml(text)}</p>${link ? `<a class="button" href="${escapeHtml(link)}">${escapeHtml(label)} ${icon('arrow')}</a>` : ''}</div>`
    );
}

export function pagination(container, page, onChange) {
    if (page.totalPages <= 1) return;
    const nav = document.createElement('nav');
    nav.className = 'pagination';
    nav.setAttribute('aria-label', 'Страницы');
    nav.innerHTML =
        `<button class="button secondary" ${page.number === 0 ? 'disabled' : ''}>← Назад</button>` +
        `<span>${page.number + 1} / ${page.totalPages}</span>` +
        `<button class="button secondary" ${page.number + 1 >= page.totalPages ? 'disabled' : ''}>Далее →</button>`;
    nav.firstElementChild.onclick = () => onChange(page.number - 1);
    nav.lastElementChild.onclick = () => onChange(page.number + 1);
    container.append(nav);
}

export async function perform(button, job) {
    if (button.disabled) return;
    button.disabled = true;
    button.setAttribute('aria-busy', 'true');
    try {
        await job();
    } catch (error) {
        if (error.name !== 'AbortError') toast(errorMessage(error), 'error');
    } finally {
        button.disabled = button.dataset.locked === 'true';
        button.removeAttribute('aria-busy');
    }
}

export async function confirmAction(title, description, label = 'Подтвердить', danger = false) {
    return new Promise((resolve) => {
        const dialog = document.createElement('dialog');
        dialog.className = 'dialog';
        dialog.innerHTML =
            `<form method="dialog">` +
            `<h2>${escapeHtml(title)}</h2>` +
            `<p>${escapeHtml(description)}</p>` +
            `<div class="dialog-actions">` +
            `<button class="button secondary" value="cancel" autofocus>Отмена</button>` +
            `<button class="button ${danger ? 'danger' : ''}" value="confirm">${escapeHtml(label)}</button>` +
            `</div>` +
            `</form>`;
        document.body.append(dialog);
        dialog.addEventListener(
            'close',
            () => {
                const confirmed = dialog.returnValue === 'confirm';
                dialog.remove();
                resolve(confirmed);
            },
            { once: true }
        );
        dialog.showModal();
    });
}

export const pageHeading = (eyebrow, title, text = '', action = '') =>
    `<div class="page-heading"><div><div class="eyebrow">${escapeHtml(eyebrow)}</div><h1>${escapeHtml(title)}</h1>${text ? `<p>${escapeHtml(text)}</p>` : ''}</div>${action}</div>`;

export function formError(form, error) {
    const box = form.querySelector('.form-error');
    box.hidden = false;
    box.textContent = errorMessage(error);
    box.focus();
}

export function gallery(container, urls, name) {
    let selected = 0;
    const draw = () => {
        container.innerHTML = `${image(urls[selected], name, 'gallery-main')}<div class="gallery-thumbs">${urls.map((url, index) => `<button type="button" class="gallery-thumb ${index === selected ? 'selected' : ''}" data-index="${index}" aria-label="Фото ${index + 1}" aria-pressed="${index === selected}">${image(url, name)}</button>`).join('')}</div>`;
        container.querySelectorAll('[data-index]').forEach(
            (button) =>
                (button.onclick = () => {
                    selected = Number(button.dataset.index);
                    draw();
                })
        );
    };
    draw();
}
