import { api, currentUser, canShop, pageData, pageQuery } from './api.js';
import { attachReplies } from './replies.js';
import {
    escapeHtml as e,
    icon,
    date,
    loading,
    pagination,
    perform,
    toast,
    showError,
    formError,
    confirmAction
} from './ui.js';

function stars(rating) {
    return (
        `<span class="review-stars" role="img" aria-label="Оценка ${rating} из 5">` +
        `<span aria-hidden="true">${'★'.repeat(rating)}<span class="empty-stars">${'★'.repeat(5 - rating)}</span>` +
        `</span>` +
        `</span>`
    );
}

function reactionButtons(review, selected, enabled) {
    return `<div class="review-reactions" role="group" aria-label="Полезен ли отзыв?">${[
        ['LIKE', 'like', 'Полезный отзыв', review.likes],
        ['DISLIKE', 'dislike', 'Неполезный отзыв', review.dislikes]
    ]
        .map(
            ([type, image, label, count]) =>
                `<button type="button" class="review-reaction" data-reaction="${type}" aria-pressed="${selected === type}" aria-label="${label}: ${count ?? 0}${selected === type ? '. Ваша реакция; нажмите, чтобы убрать' : ''}" title="${enabled ? (selected === type ? 'Убрать реакцию' : label) : currentUser() ? 'Реакции доступны покупателям и продавцам' : 'Войдите, чтобы поставить реакцию'}" ${enabled ? '' : 'disabled'}>${icon(image)}<span>${count ?? 0}</span>` +
                `</button>`
        )
        .join('')}</div>`;
}

function reviewCard(review, own = null) {
    const labels = { PENDING: 'На модерации', APPROVED: 'Опубликован', REJECTED: 'Отклонён' };
    return (
        `<article class="review-card ${own ? 'own-review' : ''}">` +
        `<div class="review-card-top">` +
        `<div class="review-author">` +
        `<span class="review-avatar" aria-hidden="true">${e(review.userName?.slice(0, 1).toUpperCase() || '•')}</span>` +
        `<div>` +
        `<h3>${own ? 'Ваша оценка' : e(review.userName)}</h3>` +
        `<time datetime="${e(review.createdAt)}">${date(review.createdAt)}</time>` +
        `</div>` +
        `</div>${own ? `<span class="review-status status-${e(own.moderationStatus)}"><span aria-hidden="true">●</span> ${e(labels[own.moderationStatus] || own.moderationStatus)}</span>` : ''}</div>${stars(review.rating)}${review.text ? `<p class="review-text">${e(review.text)}</p>` : ''}${own?.moderationStatus === 'PENDING' ? '<p class="review-hint">Пока виден только вам. Можно изменить до решения модератора.</p>' : ''}${own?.rejectionReason ? `<p class="review-rejection">Причина отклонения: ${e(own.rejectionReason)}</p>` : ''}${
            own
                ? `<div class="review-controls">` +
                  `<button type="button" class="text-button edit-review" ${own.moderationStatus !== 'PENDING' ? 'disabled title="Изменить можно только до рассмотрения"' : ''}>${icon('edit')} Изменить</button>` +
                  `<button type="button" class="text-button delete-review">${icon('trash')} Удалить</button>` +
                  `</div>`
                : ''
        }</article>`
    );
}

export async function productReviews(container, productId) {
    container.innerHTML =
        '<div class="section-toolbar">' +
        '<div>' +
        '<div class="eyebrow">ОПЫТ ПОКУПАТЕЛЕЙ</div>' +
        '<h2>Отзывы <span class="catalog-total review-total">' +
        '</span>' +
        '</h2>' +
        '</div>' +
        '</div>' +
        '<div class="reviews-content">' +
        '</div>';
    const area = container.querySelector('.reviews-content');
    let own = null;
    let number = 0;
    let revision = 0;
    let editing = false;
    let mutationPending = false;

    const load = async (nextPage = number) => {
        number = nextPage;
        const ownRevision = ++revision;
        loading(area);
        try {
            // A missing personal review is an ordinary empty state, not a failed list request.
            const personal = canShop()
                ? api(`/products/${productId}/reviews/my`).catch((error) => {
                      if (error.status === 404) return null;
                      throw error;
                  })
                : Promise.resolve(null);
            const query = pageQuery(number, 'createdAt,desc');
            query.append('sort', 'id,desc');
            const [publicData, personalData] = await Promise.all([
                api(`/products/${productId}/reviews?${query}`, { publicRequest: true }),
                personal
            ]);
            if (ownRevision !== revision || !container.isConnected) return;
            const page = pageData(publicData);
            own = personalData;
            if (!page.items.length && page.number > 0) {
                await load(page.number - 1);
                return;
            }
            const visibleReviews = [
                ...(own ? [own.review] : []),
                ...page.items.filter((review) => String(review.id) !== String(own?.review.id))
            ];
            const selectedReactions =
                canShop() && visibleReviews.length
                    ? await api(
                          `/reviews/reactions/my?${new URLSearchParams({ reviewIds: visibleReviews.map((review) => review.id).join(',') })}`
                      )
                    : [];
            if (ownRevision !== revision || !container.isConnected) return;
            const reactionsByReview = new Map(
                selectedReactions.map((reaction) => [String(reaction.reviewId), reaction.type])
            );
            container.querySelector('.review-total').textContent = page.total;
            const others = page.items.filter((review) => String(review.id) !== String(own?.review.id));
            const invitation = !currentUser()
                ? `<div class="review-invitation">` +
                  `<span>${icon('user')} Войдите, чтобы поделиться своей оценкой.</span>` +
                  `<a class="button secondary small" href="#/login?next=${encodeURIComponent('/products/' + productId)}">Войти</a>` +
                  `</div>`
                : canShop() && !own
                  ? '<div class="review-invitation">' +
                    '<div>' +
                    '<h3>Как вам товар?</h3>' +
                    '<p>Поделитесь мнением. Отзыв появится после проверки.</p>' +
                    '</div>' +
                    '<button type="button" class="button small create-review">Оставить отзыв</button>' +
                    '</div>'
                  : '';
            area.innerHTML =
                `${own ? reviewCard(own.review, own) : ''}<div class="review-form-slot">` +
                `</div>${invitation}<div class="public-reviews">${others.map((review) => reviewCard(review)).join('')}${!page.total ? '<p class="reviews-empty">Опубликованных отзывов пока нет. Ваша оценка может стать первой.</p>' : !others.length && own ? '<p class="reviews-empty">Других отзывов на этой странице пока нет.</p>' : ''}</div>`;
            pagination(area, page, load);
            area.querySelectorAll('.review-card').forEach((card, index) => {
                const review = visibleReviews[index];
                if (own && index === 0 && own.moderationStatus !== 'APPROVED') return;
                attachReplies(card, review.id);
                let selected = reactionsByReview.get(String(review.id)) || null;
                card.insertAdjacentHTML('beforeend', reactionButtons(review, selected, canShop()));
                const controls = card.querySelector('.review-reactions');
                let pending = false;
                controls.onclick = async (event) => {
                    const button = event.target.closest('[data-reaction]');
                    if (!button || button.disabled || pending || !canShop()) return;
                    pending = true;
                    controls.setAttribute('aria-busy', 'true');
                    controls.querySelectorAll('button').forEach((control) => (control.disabled = true));
                    const next = selected === button.dataset.reaction ? null : button.dataset.reaction;
                    try {
                        await api(`/reviews/${review.id}/reaction`, {
                            method: next ? 'PUT' : 'DELETE',
                            ...(next ? { body: { type: next } } : {})
                        });
                        if (ownRevision !== revision || !card.isConnected) return;
                        if (selected === 'LIKE') review.likes = Math.max(0, (review.likes || 0) - 1);
                        if (selected === 'DISLIKE') review.dislikes = Math.max(0, (review.dislikes || 0) - 1);
                        if (next === 'LIKE') review.likes = (review.likes || 0) + 1;
                        if (next === 'DISLIKE') review.dislikes = (review.dislikes || 0) + 1;
                        selected = next;
                        const refreshed = document.createElement('div');
                        refreshed.innerHTML = reactionButtons(review, selected, canShop());
                        controls.innerHTML = refreshed.firstElementChild.innerHTML;
                    } catch (error) {
                        toast(error.message, 'error');
                        // A timeout may happen after commit; re-read instead of guessing the reaction state.
                        if (ownRevision === revision && container.isConnected) await load(number);
                    } finally {
                        pending = false;
                        controls.removeAttribute('aria-busy');
                        controls
                            .querySelectorAll('button')
                            .forEach((control) => (control.disabled = !canShop()));
                    }
                };
            });
            area.querySelector('.create-review')?.addEventListener('click', () => {
                editing = false;
                openForm();
            });
            area.querySelector('.edit-review')?.addEventListener('click', () => {
                editing = true;
                openForm();
            });
            const remove = area.querySelector('.delete-review');
            if (remove)
                remove.onclick = () =>
                    perform(remove, async () => {
                        if (mutationPending) return;
                        mutationPending = true;
                        try {
                            if (
                                !(await confirmAction(
                                    'Удалить отзыв?',
                                    'Отзыв исчезнет со страницы товара. История модерации сохранится; вы сможете оставить новый отзыв.',
                                    'Удалить',
                                    true
                                ))
                            )
                                return;
                            await api(`/reviews/${own.review.id}`, { method: 'DELETE' });
                            toast('Отзыв удалён');
                            await load(0);
                        } finally {
                            mutationPending = false;
                        }
                    });
        } catch (error) {
            if (ownRevision === revision) showError(area, error, () => load(number));
        }
    };

    function openForm() {
        if (mutationPending) return;
        const slot = area.querySelector('.review-form-slot');
        if (slot.querySelector('form')) {
            slot.querySelector('form').focus();
            return;
        }
        const initial = editing ? own.review : { rating: null, text: '' };
        slot.innerHTML =
            `<form class="review-form stack-form" tabindex="-1">` +
            `<div class="review-form-heading">` +
            `<h3>${editing ? 'Изменить вашу оценку' : 'Ваша оценка товара'}</h3>` +
            `<button type="button" class="icon-button close-review-form" aria-label="Закрыть форму отзыва">${icon('close')}</button>` +
            `</div>` +
            `<div class="form-error" role="alert" tabindex="-1" hidden>` +
            `</div>` +
            `<fieldset class="rating-fieldset">` +
            `<legend>Оценка <span class="required-hint">обязательно</span>` +
            `</legend>` +
            `<div class="rating-options">${[1, 2, 3, 4, 5]
                .map(
                    (value) =>
                        `<label class="rating-option">` +
                        `<input type="radio" name="rating" value="${value}" required ${initial.rating === value ? 'checked' : ''}>` +
                        `<span aria-hidden="true">★</span>` +
                        `<span class="visually-hidden">${value} из 5</span>` +
                        `</label>`
                )
                .join('')}</div>` +
            `</fieldset>` +
            `<label>Ваш отзыв <span class="optional">необязательно</span>` +
            `<textarea name="text" rows="4" minlength="10" maxlength="2000" placeholder="Что понравилось? На что стоит обратить внимание?">${e(initial.text)}</textarea>` +
            `<span class="field-hint">Если добавляете текст — от 10 до 2000 символов.</span>` +
            `</label>` +
            `<div class="button-row">` +
            `<button type="submit" class="button">${editing ? 'Сохранить изменения' : 'Отправить на модерацию'}</button>` +
            `<button type="button" class="button secondary close-review-form">Отмена</button>` +
            `</div>` +
            `<p class="fine-print">До решения модератора отзыв виден только вам и может быть изменён.</p>` +
            `</form>`;
        const form = slot.querySelector('form');
        form.querySelectorAll('.close-review-form').forEach(
            (button) =>
                (button.onclick = () => {
                    if (!mutationPending) slot.replaceChildren();
                })
        );
        form.querySelectorAll('[name="rating"]').forEach(
            (input) => (input.onchange = () => paintRating(form))
        );
        paintRating(form);
        form.onsubmit = (event) => {
            event.preventDefault();
            perform(form.querySelector('[type="submit"]'), async () => {
                if (mutationPending) return;
                mutationPending = true;
                form.querySelector('.form-error').hidden = true;
                try {
                    const values = new FormData(form);
                    const rating = Number(values.get('rating'));
                    const text = values.get('text').trim();
                    if (text && text.length < 10)
                        throw new Error('Текст отзыва должен содержать не менее 10 символов.');
                    const body = editing
                        ? {
                              ...(rating !== own.review.rating ? { rating } : {}),
                              ...(text !== (own.review.text || '') ? { text } : {})
                          }
                        : { rating, text: text || null };
                    if (editing && !Object.keys(body).length) {
                        toast('Изменений нет');
                        slot.replaceChildren();
                        return;
                    }
                    form.querySelectorAll('input, textarea, button').forEach(
                        (control) => (control.disabled = true)
                    );
                    await api(editing ? `/reviews/${own.review.id}` : `/products/${productId}/reviews`, {
                        method: editing ? 'PATCH' : 'POST',
                        body
                    });
                    toast(editing ? 'Отзыв изменён' : 'Отзыв отправлен на модерацию');
                    await load(0);
                } catch (error) {
                    formError(form, error);
                    if (error.status === 409) {
                        toast('Состояние отзыва изменилось. Данные обновлены.', 'error');
                        await load(number);
                    } else if (error.status === 0) {
                        // Never repeat a potentially successful creation after an unknown network outcome.
                        const refresh = document.createElement('button');
                        refresh.type = 'button';
                        refresh.className = 'button secondary';
                        refresh.textContent = 'Проверить состояние отзыва';
                        refresh.onclick = () => load(0);
                        form.querySelector('.form-error').append(document.createElement('br'), refresh);
                        form.querySelector('[type="submit"]').dataset.locked = 'true';
                    }
                } finally {
                    mutationPending = false;
                    form.querySelectorAll('input, textarea, button').forEach(
                        (control) => (control.disabled = false)
                    );
                }
            });
        };
        form.focus({ preventScroll: true });
        slot.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    function paintRating(form) {
        const selected = Number(form.querySelector('[name="rating"]:checked')?.value || 0);
        form.querySelectorAll('.rating-option').forEach((label) =>
            label.classList.toggle('filled', Number(label.querySelector('input').value) <= selected)
        );
    }

    await load(0);
}
