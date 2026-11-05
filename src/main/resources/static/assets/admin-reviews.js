import { api, pageData, pageQuery } from './api.js';
import {
    escapeHtml as e,
    icon,
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
    confirmAction
} from './ui.js';

export function moderationNavigation(reviews = false) {
    return (
        `<nav class="tabs moderation-navigation" aria-label="Тип модерации">` +
        `<a class="tab ${reviews ? '' : 'active'}" href="#/admin/moderation" ${reviews ? '' : 'aria-current="page"'}>Товары</a>` +
        `<a class="tab ${reviews ? 'active' : ''}" href="#/admin/review-moderation" ${reviews ? 'aria-current="page"' : ''}>Отзывы</a>` +
        `<a class="tab" href="#/admin/reply-moderation">Ответы</a>` +
        `</nav>`
    );
}

const statuses = {
    PENDING: 'Ожидают проверки',
    APPROVED: 'Одобрены',
    REJECTED: 'Отклонены',
    CANCELLED: 'Отменены'
};
const reviewBadge = (status) =>
    status === 'CANCELLED' ? '<span class="badge badge-CANCELLED">Отменена</span>' : badge(status);
const rating = (value) =>
    `<span class="review-stars" role="img" aria-label="Оценка ${value} из 5">` +
    `<span aria-hidden="true">${'★'.repeat(value)}<span class="empty-stars">${'★'.repeat(5 - value)}</span>` +
    `</span>` +
    `</span>`;

function selectedStatus() {
    const value = new URLSearchParams(location.hash.split('?')[1] || '').get('status');
    return Object.hasOwn(statuses, value) ? value : 'PENDING';
}

export async function reviewModerationPage(view) {
    document.title = 'Модерация отзывов — Рядом';
    const status = selectedStatus();
    view.innerHTML =
        `${moderationNavigation(true)}${pageHeading('КАЧЕСТВО ОБРАТНОЙ СВЯЗИ', 'Модерация отзывов', 'Проверяйте оценки и тексты покупателей перед публикацией.')}<nav class="tabs" aria-label="Статус заявки на отзыв">${Object.entries(
            statuses
        )
            .map(
                ([value, label]) =>
                    `<a class="tab ${value === status ? 'active' : ''}" href="#/admin/review-moderation?status=${value}" ${value === status ? 'aria-current="page"' : ''}>${label}</a>`
            )
            .join('')}</nav>` +
        `<div class="moderation-content">` +
        `</div>`;
    const area = view.querySelector('.moderation-content');
    let revision = 0;
    const load = async (number = 0) => {
        const ownRevision = ++revision;
        loading(area);
        try {
            const query = pageQuery(number, 'submittedAt,asc');
            query.append('sort', 'id,asc');
            query.set('status', status);
            const page = pageData(await api(`/admin/moderation/review-requests?${query}`));
            if (!view.isConnected || ownRevision !== revision) return;
            area.innerHTML = page.items.length
                ? `<div class="moderation-list">${page.items
                      .map(
                          (request) =>
                              `<a class="panel moderation-row review-moderation-row" href="#/admin/review-moderation/${e(request.publicId)}?status=${status}">` +
                              `<div class="moderation-row-info">` +
                              `<span class="eyebrow">ОТЗЫВ · ${e(shortId(request.publicId))}</span>` +
                              `<h2>${e(request.userName)}</h2>${rating(request.rating)}<p class="review-moderation-excerpt">${e(request.text || 'Отзыв без текста')}</p>` +
                              `<p class="muted">Товар ${e(shortId(request.productId))} · ${date(request.submittedAt)}</p>` +
                              `</div>${reviewBadge(request.status)}<span class="row-arrow">${icon('arrow')}</span>` +
                              `</a>`
                      )
                      .join('')}</div>` +
                  `<p class="fine-print">Заявок: ${page.total} · Сначала самые ранние</p>`
                : empty('Здесь всё спокойно', 'Заявок на отзывы с выбранным статусом нет.', null);
            pagination(area, page, load);
        } catch (error) {
            if (view.isConnected && ownRevision === revision) showError(area, error, () => load(number));
        }
    };
    await load();
}

export async function reviewModerationDetails(view, id) {
    const request = await api(`/admin/moderation/review-requests/${encodeURIComponent(id)}`);
    if (!view.isConnected) return;
    document.title = `Модерация отзыва ${shortId(id)} — Рядом`;
    const pending = request.status === 'PENDING';
    const cancelled = request.status === 'CANCELLED';
    view.innerHTML =
        `${moderationNavigation(true)}<a class="back-link" href="#/admin/review-moderation?status=${selectedStatus()}">← Заявки на отзывы</a>${pageHeading('ЗАЯВКА · ' + shortId(id), 'Отзыв покупателя', 'Отправлено: ' + date(request.submittedAt), reviewBadge(request.status))}<div class="review-decision-notice" role="alert">` +
        `</div>` +
        `<div class="moderation-layout">` +
        `<article class="panel form-panel review-moderation-snapshot">` +
        `<div class="eyebrow">${pending ? 'НА РАССМОТРЕНИИ' : 'СОХРАНЁННАЯ ВЕРСИЯ ОТЗЫВА'}</div>` +
        `<h2>${e(request.userName)}</h2>${rating(request.rating)}<p class="review-text">${e(request.text || 'Автор оставил только оценку, без текста.')}</p>` +
        `<dl class="review-meta">` +
        `<dt>Товар</dt>` +
        `<dd>` +
        `<a class="text-link" href="#/products/${e(request.productId)}">Открыть товар ↗</a>` +
        `</dd>` +
        `<dt>Автор</dt>` +
        `<dd>` +
        `<a class="text-link" href="#/admin/users/${e(request.userId)}">${e(request.userName)} ↗</a>` +
        `</dd>` +
        `<dt>Версия отзыва</dt>` +
        `<dd>${e(request.reviewVersion)}</dd>` +
        `</dl>` +
        `<p class="fine-print">${pending ? 'Автор может изменить отзыв до рассмотрения. При изменении потребуется заново открыть актуальную версию заявки.' : 'Это версия, сохранённая в истории модерации, а не текущие данные товара.'}</p>` +
        `</article>` +
        `<aside class="panel form-panel review-panel review-decision">` +
        `<h2>${pending ? 'Ваше решение' : cancelled ? 'Заявка отменена' : 'Результат рассмотрения'}</h2>${
            pending
                ? `<p class="muted">После одобрения отзыв автоматически появится на странице товара.</p>` +
                  `<button type="button" class="button approve-review">${icon('check')} Одобрить отзыв</button>` +
                  `<hr>` +
                  `<form class="stack-form reject-review-form">` +
                  `<label>Причина отклонения<textarea name="reason" rows="5" required maxlength="2000" placeholder="Объясните автору, почему отзыв не может быть опубликован.">` +
                  `</textarea>` +
                  `</label>` +
                  `<button type="submit" class="button danger">Отклонить отзыв</button>` +
                  `</form>`
                : cancelled
                  ? '<p class="muted">Автор удалил отзыв до рассмотрения. Заявка закрыта без решения модератора.</p>'
                  : `<div class="status-row">${reviewBadge(request.status)}</div>` +
                    `<dl class="review-meta">` +
                    `<dt>Рассмотрено</dt>` +
                    `<dd>${date(request.reviewedAt)}</dd>` +
                    `<dt>Модератор</dt>` +
                    `<dd>${e(request.reviewedBy?.reviewerName || 'Не указан')}</dd>` +
                    `</dl>${request.rejectionReason ? `<div class="notice warning preserve-whitespace">${e(request.rejectionReason)}</div>` : ''}`
        }</aside>` +
        `</div>`;
    if (!pending) return;
    let reviewing = false;
    const controls = () => view.querySelectorAll('.review-decision button, .review-decision textarea');
    const setBusy = (busy) =>
        controls().forEach((control) => {
            control.disabled = busy || control.dataset.locked === 'true';
        });
    const decide = async (action, reason) => {
        if (reviewing) return;
        reviewing = true;
        setBusy(true);
        try {
            const approving = action === 'approve';
            if (
                !(await confirmAction(
                    approving ? 'Одобрить отзыв?' : 'Отклонить отзыв?',
                    approving
                        ? 'Эта версия отзыва станет доступна всем посетителям товара.'
                        : 'Причина будет сохранена в истории и показана автору.',
                    approving ? 'Одобрить' : 'Отклонить',
                    !approving
                ))
            )
                return;
            await api(`/admin/moderation/review-requests/${encodeURIComponent(id)}/${action}`, {
                method: 'POST',
                body: { reviewVersion: request.reviewVersion, ...(approving ? {} : { reason }) }
            });
            toast(approving ? 'Отзыв одобрен и опубликован' : 'Отзыв отклонён');
            await reviewModerationDetails(view, id);
        } catch (error) {
            if (!view.isConnected) return;
            const mustReload =
                error.status === 404 || error.status === 409 || error.status === 0 || error.status >= 500;
            if (mustReload)
                controls().forEach((control) => {
                    control.dataset.locked = 'true';
                });
            const notice = view.querySelector('.review-decision-notice');
            notice.innerHTML =
                `<div class="notice warning">` +
                `<p>${e(error.status === 409 ? 'Заявка изменилась: автор отредактировал или удалил отзыв, либо решение уже принято. Откройте актуальную версию и проверьте её перед новым решением.' : error.message)}</p>${mustReload ? '<button type="button" class="button secondary reload-review-request">Обновить заявку</button>' : ''}</div>`;
            notice.querySelector('button')?.addEventListener('click', (event) =>
                perform(event.currentTarget, async () => {
                    await reviewModerationDetails(view, id);
                })
            );
            notice.scrollIntoView({ behavior: 'smooth', block: 'center' });
        } finally {
            reviewing = false;
            setBusy(false);
        }
    };
    const approve = view.querySelector('.approve-review');
    approve.onclick = () => perform(approve, () => decide('approve'));
    const form = view.querySelector('.reject-review-form');
    form.onsubmit = (event) => {
        event.preventDefault();
        const reason = form.elements.reason.value.trim();
        if (!reason) {
            form.elements.reason.setCustomValidity('Укажите причину отклонения.');
            form.elements.reason.reportValidity();
            return;
        }
        perform(form.querySelector('[type="submit"]'), () => decide('reject', reason));
    };
    form.elements.reason.oninput = () => form.elements.reason.setCustomValidity('');
}
