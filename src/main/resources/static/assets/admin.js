import { api, pageData, pageQuery, currentUser } from './api.js';
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
import { orderMarkup, bindOrderActions } from './storefront.js';
import { moderationNavigation } from './admin-reviews.js';

export async function usersPage(view) {
    document.title = 'Пользователи — Рядом';
    view.innerHTML =
        `${pageHeading('УПРАВЛЕНИЕ МАРКЕТПЛЕЙСОМ', 'Пользователи', 'Аккаунты, роли, корзины и заказы.', `<a class="button" href="#/admin/users/new">${icon('plus')} Создать пользователя</a>`)}<div class="users-content">` +
        `</div>`;
    const area = view.querySelector('.users-content');
    const load = async (number = 0) => {
        loading(area);
        try {
            const page = pageData(await api(`/admin/users?${pageQuery(number, 'name,asc')}`));
            area.innerHTML = page.items.length
                ? `<div class="table-wrap panel">` +
                  `<table>` +
                  `<caption class="visually-hidden">Пользователи маркетплейса</caption>` +
                  `<thead>` +
                  `<tr>` +
                  `<th>Пользователь</th>` +
                  `<th>Контакты</th>` +
                  `<th>Роли</th>` +
                  `<th>` +
                  `<span class="visually-hidden">Действия</span>` +
                  `</th>` +
                  `</tr>` +
                  `</thead>` +
                  `<tbody>${page.items
                      .map(
                          (user) =>
                              `<tr>` +
                              `<td>` +
                              `<a class="table-name" href="#/admin/users/${user.id}">` +
                              `<span class="table-avatar">${e(user.name.slice(0, 1).toUpperCase())}</span>${e(user.name)}</a>` +
                              `<span class="table-sub">ID ${user.id}</span>` +
                              `</td>` +
                              `<td>${e(user.telephone)}<span class="table-sub">${e(user.email || 'Email не указан')}</span>` +
                              `</td>` +
                              `<td>` +
                              `<div class="status-row">${user.roles.map(badge).join('')}</div>` +
                              `</td>` +
                              `<td>` +
                              `<a class="icon-button" href="#/admin/users/${user.id}/edit" aria-label="Редактировать ${e(user.name)}">${icon('edit')}</a>` +
                              `</td>` +
                              `</tr>`
                      )
                      .join('')}</tbody>` +
                  `</table>` +
                  `</div>` +
                  `<p class="fine-print">Всего аккаунтов: ${page.total}</p>`
                : empty(
                      'Пользователей пока нет',
                      'Создайте первый аккаунт.',
                      '#/admin/users/new',
                      'Создать пользователя'
                  );
            pagination(area, page, load);
        } catch (error) {
            showError(area, error, () => load(number));
        }
    };
    await load();
}

export async function userEditor(view, id) {
    const creating = !id;
    const user = creating
        ? { name: '', telephone: '', email: '', roles: ['BUYER'] }
        : await api(`/admin/users/${encodeURIComponent(id)}`);
    if (!view.isConnected) return;
    document.title = `${creating ? 'Новый пользователь' : user.name} — Рядом`;
    view.innerHTML =
        `<a class="back-link" href="#/admin/users${creating ? '' : `/${id}`}">← ${creating ? 'Пользователи' : 'Профиль пользователя'}</a>${pageHeading('УПРАВЛЕНИЕ АККАУНТАМИ', creating ? 'Новый пользователь' : user.name)}<form class="panel form-panel user-form stack-form">` +
        `<div class="form-error" role="alert" tabindex="-1" hidden>` +
        `</div>` +
        `<div class="form-columns">` +
        `<label>Имя пользователя<input name="name" value="${e(user.name)}" required minlength="3" maxlength="32" pattern="[A-Za-z0-9_]+" autocomplete="off">` +
        `<span class="field-hint">Латиница, цифры и _</span>` +
        `</label>` +
        `<label>Телефон<input name="telephone" type="tel" value="${e(user.telephone)}" required autocomplete="off" placeholder="+7 999 123-45-67">` +
        `</label>` +
        `</div>` +
        `<label>Email <span class="optional">необязательно</span>` +
        `<input name="email" type="email" value="${e(user.email)}" maxlength="254" autocomplete="off">` +
        `</label>` +
        `<label>${creating ? 'Пароль' : 'Новый пароль'}<input name="password" type="password" ${creating ? 'required' : ''} minlength="8" maxlength="72" autocomplete="new-password" placeholder="${creating ? 'Не менее 8 символов' : 'Оставьте пустым, чтобы не менять'}">` +
        `</label>` +
        `<fieldset class="role-fieldset">` +
        `<legend>Роли</legend>${['BUYER', 'SELLER', 'ADMIN'].map((role) => `<label class="checkbox-label"><input name="roles" type="checkbox" value="${role}" ${user.roles.includes(role) ? 'checked' : ''}>${badge(role)}</label>`).join('')}<p class="field-hint">Администратор — отдельный управляющий аккаунт, без корзины.</p>` +
        `</fieldset>` +
        `<div class="button-row">` +
        `<button class="button" type="submit">${icon('check')} ${creating ? 'Создать пользователя' : 'Сохранить изменения'}</button>` +
        `<a class="button secondary" href="#/admin/users${creating ? '' : `/${id}`}">Отмена</a>` +
        `</div>` +
        `</form>`;
    const form = view.querySelector('form');
    form.querySelectorAll('[name="roles"]').forEach(
        (checkbox) =>
            (checkbox.onchange = () => {
                if (!checkbox.checked) return;
                if (checkbox.value === 'ADMIN')
                    form.querySelectorAll('[name="roles"]:not([value="ADMIN"])').forEach(
                        (input) => (input.checked = false)
                    );
                else form.querySelector('[value="ADMIN"]').checked = false;
            })
    );
    form.onsubmit = (event) => {
        event.preventDefault();
        perform(form.querySelector('[type="submit"]'), async () => {
            form.querySelector('.form-error').hidden = true;
            try {
                const data = Object.fromEntries(new FormData(form));
                data.roles = [...form.querySelectorAll('[name="roles"]:checked')].map((input) => input.value);
                if (!data.roles.length) throw new Error('Выберите хотя бы одну роль.');
                if (creating) data.email = data.email.trim() || null;
                if (!creating) {
                    if (!data.password) delete data.password;
                    for (const key of ['name', 'telephone', 'email'])
                        if (data[key] === (user[key] || '')) delete data[key];
                    if (JSON.stringify([...data.roles].sort()) === JSON.stringify([...user.roles].sort()))
                        delete data.roles;
                    if (!Object.keys(data).length) {
                        toast('Изменений нет');
                        return;
                    }
                }
                const saved = await api(creating ? '/admin/users' : `/admin/users/${id}`, {
                    method: creating ? 'POST' : 'PATCH',
                    body: data
                });
                toast(creating ? 'Пользователь создан' : 'Изменения сохранены');
                if (!creating && String(id) === String(currentUser()?.id) && data.roles)
                    toast('Роли в текущем JWT обновятся после повторного входа.');
                location.hash = `/admin/users/${saved.id}`;
            } catch (error) {
                formError(form, error);
            }
        });
    };
}

export async function adminUserPage(view, id) {
    const user = await api(`/admin/users/${encodeURIComponent(id)}`);
    if (!view.isConnected) return;
    document.title = `${user.name} — Рядом`;
    view.innerHTML =
        `<a class="back-link" href="#/admin/users">← Пользователи</a>${pageHeading(
            'ПРОФИЛЬ · ID ' + user.id,
            user.name,
            user.telephone + (user.email ? ` · ${user.email}` : ''),
            `<div class="button-row">` +
                `<a class="button secondary" href="#/admin/users/${id}/edit">${icon('edit')} Изменить</a>` +
                `<button class="button danger delete-user" type="button">${icon('trash')} Удалить</button>` +
                `</div>`
        )}<div class="status-row profile-roles">${user.roles.map(badge).join('')}</div>` +
        `<div class="tabs" role="group" aria-label="Данные пользователя">` +
        `<button type="button" class="tab active" data-section="orders" aria-pressed="true">Заказы</button>` +
        `<button type="button" class="tab" data-section="cart" aria-pressed="false">Корзина</button>` +
        `</div>` +
        `<div class="user-content">` +
        `</div>`;
    const area = view.querySelector('.user-content');
    let section = 'orders';
    let revision = 0;
    const load = async (number = 0) => {
        const ownRevision = ++revision;
        loading(area);
        try {
            if (section === 'cart') {
                if (user.roles.includes('ADMIN')) {
                    area.innerHTML = empty(
                        'У администратора нет корзины',
                        'Административный аккаунт предназначен для управления.',
                        null
                    );
                    return;
                }
                const cart = await api(`/admin/users/${id}/cart`);
                if (ownRevision !== revision) return;
                area.innerHTML = cart.items.length
                    ? `<section class="panel readonly-cart">${cart.items
                          .map(
                              (item) =>
                                  `<div class="cart-item">${image(item.product.imgUrl, item.product.name)}<div>` +
                                  `<a class="item-name" href="#/products/${item.product.id}">${e(item.product.name)}</a>` +
                                  `<div>${badge(item.product.status)}</div>` +
                                  `</div>` +
                                  `<span>${item.quantity} × ${money(item.product.price)}</span>` +
                                  `<strong>${money(item.subtotal)}</strong>` +
                                  `</div>`
                          )
                          .join(
                              ''
                          )}<div class="summary-total">Итого <strong>${money(cart.totalPrice)}</strong>` +
                      `</div>` +
                      `</section>`
                    : empty('Корзина пуста', 'Пользователь ещё не добавил товары.', null);
            } else {
                const page = pageData(
                    await api(`/admin/users/${id}/orders?${pageQuery(number, 'createdAt,desc')}`)
                );
                if (ownRevision !== revision) return;
                area.innerHTML = page.items.length
                    ? page.items.map((order) => orderMarkup(order, true)).join('')
                    : empty('Заказов пока нет', 'Оформленные пользователем заказы появятся здесь.', null);
                bindOrderActions(area, true, () => load(number));
                pagination(area, page, load);
            }
        } catch (error) {
            if (ownRevision === revision) showError(area, error, () => load(number));
        }
    };
    view.querySelectorAll('[data-section]').forEach(
        (button) =>
            (button.onclick = () => {
                section = button.dataset.section;
                view.querySelectorAll('[data-section]').forEach((tab) => {
                    tab.classList.toggle('active', tab === button);
                    tab.setAttribute('aria-pressed', String(tab === button));
                });
                load();
            })
    );
    const remove = view.querySelector('.delete-user');
    remove.onclick = () =>
        perform(remove, async () => {
            if (
                !(await confirmAction(
                    'Удалить пользователя?',
                    `Удаление аккаунта ${user.name} нельзя отменить.`,
                    'Удалить',
                    true
                ))
            )
                return;
            await api(`/admin/users/${id}`, { method: 'DELETE' });
            toast('Пользователь удалён');
            location.hash = '/admin/users';
        });
    await load();
}

function moderationBadge(status) {
    return status === 'CANCELLED' ? '<span class="badge badge-CANCELLED">Отозвана</span>' : badge(status);
}

export async function moderationPage(view) {
    document.title = 'Модерация — Рядом';
    view.innerHTML =
        `${pageHeading('КАЧЕСТВО КАТАЛОГА', 'Модерация', 'Проверяйте ту версию товара, которую продавец отправил на рассмотрение.')}<div class="tabs" role="group" aria-label="Статус заявки">` +
        `<button class="tab active" data-status="PENDING" type="button" aria-pressed="true">Ожидают проверки</button>` +
        `<button class="tab" data-status="APPROVED" type="button" aria-pressed="false">Одобрены</button>` +
        `<button class="tab" data-status="REJECTED" type="button" aria-pressed="false">Отклонены</button>` +
        `<button class="tab" data-status="CANCELLED" type="button" aria-pressed="false">Отозванные</button>` +
        `</div>` +
        `<div class="moderation-content">` +
        `</div>`;
    const area = view.querySelector('.moderation-content');
    view.insertAdjacentHTML('afterbegin', moderationNavigation());
    let status = 'PENDING';
    let revision = 0;
    const load = async (number = 0) => {
        const ownRevision = ++revision;
        loading(area);
        try {
            const page = pageData(
                await api(
                    `/admin/moderation/product-requests?${pageQuery(number, 'submittedAt,asc')}&status=${status}`
                )
            );
            if (ownRevision !== revision) return;
            area.innerHTML = page.items.length
                ? `<div class="moderation-list">${page.items
                      .map(
                          (request) =>
                              `<a class="panel moderation-row" href="#/admin/moderation/${request.publicId}">${image(request.snapshot?.previewImageUrl, request.snapshot?.name || 'Товар')}<div class="moderation-row-info">` +
                              `<span class="eyebrow">ЗАЯВКА · ${e(shortId(request.publicId))}</span>` +
                              `<h2>${e(request.snapshot?.name || 'Товар')}</h2>` +
                              `<p class="muted">${date(request.submittedAt)}</p>` +
                              `</div>` +
                              `<strong>${money(request.snapshot?.price)}</strong>${moderationBadge(request.status)}<span class="row-arrow">${icon('arrow')}</span>` +
                              `</a>`
                      )
                      .join('')}</div>` +
                  `<p class="fine-print">Заявок: ${page.total} · Сначала самые ранние</p>`
                : empty('Здесь всё спокойно', 'Заявок с выбранным статусом нет.', null);
            pagination(area, page, load);
        } catch (error) {
            if (ownRevision === revision) showError(area, error, () => load(number));
        }
    };
    view.querySelectorAll('[data-status]').forEach(
        (button) =>
            (button.onclick = () => {
                status = button.dataset.status;
                view.querySelectorAll('[data-status]').forEach((tab) => {
                    tab.classList.toggle('active', tab === button);
                    tab.setAttribute('aria-pressed', String(tab === button));
                });
                load();
            })
    );
    await load();
}

export async function moderationDetails(view, id) {
    const request = await api(`/admin/moderation/product-requests/${encodeURIComponent(id)}`);
    if (!view.isConnected) return;
    document.title = `Заявка ${shortId(id)} — Рядом`;
    const snapshot = request.snapshot;
    const pending = request.status === 'PENDING';
    const cancelled = request.status === 'CANCELLED';
    view.innerHTML =
        `<a class="back-link" href="#/admin/moderation">← Заявки на модерацию</a>${pageHeading('ЗАЯВКА · ' + shortId(id), snapshot.name, 'Отправлено: ' + date(request.submittedAt), moderationBadge(request.status))}<div class="notice">Это сохранённый снимок товара на момент подачи заявки, а не его текущая версия.</div>` +
        `<div class="moderation-layout">` +
        `<section>` +
        `<div class="snapshot-gallery">` +
        `</div>` +
        `<article class="panel form-panel snapshot-description">` +
        `<div class="eyebrow">СНИМОК ТОВАРА</div>` +
        `<h2>${e(snapshot.name)}</h2>` +
        `<p class="muted">${e(snapshot.slug)}</p>` +
        `<div class="details-price">${money(snapshot.price)}</div>` +
        `<p class="preserve-whitespace">${e(snapshot.description)}</p>` +
        `</article>` +
        `</section>` +
        `<aside class="panel form-panel review-panel">` +
        `<h2>${pending ? 'Ваше решение' : cancelled ? 'Заявка отозвана' : 'Результат рассмотрения'}</h2>${
            pending
                ? `<p class="muted">Проверьте описание и все фотографии. Решение по этой заявке нельзя изменить.</p>` +
                  `<button type="button" class="button approve-request">${icon('check')} Одобрить</button>` +
                  `<hr>` +
                  `<form class="stack-form reject-form">` +
                  `<div class="form-error" role="alert" tabindex="-1" hidden>` +
                  `</div>` +
                  `<label>Причина отклонения<textarea name="reason" rows="5" required maxlength="2000" placeholder="Укажите, что продавцу необходимо исправить.">` +
                  `</textarea>` +
                  `</label>` +
                  `<button type="submit" class="button danger">Отклонить с причиной</button>` +
                  `</form>`
                : cancelled
                  ? '<p class="muted">Продавец отозвал заявку. Она не рассматривалась модератором. Снимок сохранён в истории.</p>'
                  : `<div class="status-row">${moderationBadge(request.status)}</div>` +
                    `<dl class="review-meta">` +
                    `<dt>Рассмотрено</dt>` +
                    `<dd>${date(request.reviewedAt)}</dd>` +
                    `<dt>Модератор</dt>` +
                    `<dd>${e(request.reviewedBy?.reviewerName || 'Не указан')}</dd>` +
                    `</dl>${request.rejectionReason ? `<div class="notice warning preserve-whitespace">${e(request.rejectionReason)}</div>` : ''}`
        }<a class="text-link" href="#/products/${request.productId}">Открыть текущую версию товара ↗</a>` +
        `</aside>` +
        `</div>`;
    gallery(
        view.querySelector('.snapshot-gallery'),
        [snapshot.previewImageUrl, ...snapshot.detailsImageUrls].filter(Boolean),
        snapshot.name
    );
    view.insertAdjacentHTML('afterbegin', moderationNavigation());
    let reviewing = false;
    if (pending) {
        const reviewProductRequest = async (action, reason) => {
            const approving = action === 'approve';
            const confirmed = await confirmAction(
                approving ? 'Одобрить заявку?' : 'Отклонить заявку?',
                approving
                    ? 'Решение будет записано в историю. Публикация товара — отдельная операция.'
                    : 'Причина будет сохранена вместе с решением по этой версии товара.',
                approving ? 'Одобрить' : 'Отклонить',
                !approving
            );
            if (!confirmed) return;

            await api(`/admin/moderation/product-requests/${id}/${action}`, {
                method: 'POST',
                ...(approving ? {} : { body: { reason } })
            });
            toast(approving ? 'Заявка одобрена' : 'Заявка отклонена');
            await moderationDetails(view, id);
        };

        const approve = view.querySelector('.approve-request');
        approve.onclick = () =>
            perform(approve, async () => {
                if (reviewing) return;
                reviewing = true;
                try {
                    await reviewProductRequest('approve');
                } finally {
                    reviewing = false;
                }
            });
        const form = view.querySelector('.reject-form');
        form.onsubmit = (event) => {
            event.preventDefault();
            perform(form.querySelector('[type="submit"]'), async () => {
                if (reviewing) return;
                const reason = form.elements.reason.value.trim();
                if (!reason) {
                    formError(form, new Error('Укажите причину отклонения.'));
                    return;
                }
                reviewing = true;
                try {
                    await reviewProductRequest('reject', reason);
                } catch (error) {
                    formError(form, error);
                } finally {
                    reviewing = false;
                }
            });
        };
    }
}
