import { api, canShop, currentUser, pageData, pageQuery } from './api.js';
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
    pageHeading
} from './ui.js';

function reactionControls(reply) {
    return `<div class="review-reactions" role="group" aria-label="Реакции на сообщение">${[
        ['LIKE', 'like', 'Нравится', reply.likes],
        ['DISLIKE', 'dislike', 'Не нравится', reply.dislikes]
    ]
        .map(
            ([type, symbol, label, count]) =>
                `<button type="button" class="review-reaction" data-reaction="${type}" aria-pressed="${reply.myReaction === type}" aria-label="${label}: ${count}${reply.myReaction === type ? '. Ваша реакция' : ''}" title="${canShop() ? (reply.myReaction === type ? 'Снять реакцию' : label) : 'Реакции доступны покупателям и продавцам после входа'}" ${canShop() ? '' : 'disabled'}>${icon(symbol)}<span>${count}</span>` +
                `</button>`
        )
        .join('')}</div>`;
}

export function attachReplies(card, reviewId) {
    card.insertAdjacentHTML(
        'beforeend',
        '<details class="reply-thread">' +
            '<summary>Обсуждение отзыва</summary>' +
            '<div class="reply-thread-body">' +
            '<div class="reply-composer">' +
            '</div>' +
            '<div class="reply-list" aria-live="polite">' +
            '</div>' +
            '</div>' +
            '</details>'
    );
    const thread = card.querySelector('.reply-thread');
    const list = thread.querySelector('.reply-list');
    const composer = thread.querySelector('.reply-composer');
    let started = false;
    let revision = 0;
    let number = 0;
    let target = null;
    let sending = false;

    function renderComposer() {
        if (!canShop()) {
            composer.innerHTML = currentUser()
                ? ''
                : '<p class="fine-print">Войдите как покупатель или продавец, чтобы участвовать в обсуждении.</p>';
            return;
        }
        composer.innerHTML =
            '<form class="stack-form reply-form">' +
            '<div class="reply-target" hidden>' +
            '</div>' +
            '<div class="form-error" role="alert" tabindex="-1" hidden>' +
            '</div>' +
            '<label>Ваш ответ<textarea name="text" required maxlength="500" rows="3" placeholder="Напишите ответ, до 500 символов">' +
            '</textarea>' +
            '</label>' +
            '<div class="button-row">' +
            '<button class="button small" type="submit">Отправить</button>' +
            '<button class="button secondary small refresh-replies" type="button">Обновить сообщения</button>' +
            '</div>' +
            '<p class="fine-print">Ответ публикуется сразу. Нарушающие правила сообщения могут быть скрыты модератором.</p>' +
            '</form>';
        const form = composer.querySelector('form');
        const submit = form.querySelector('[type="submit"]');
        form.querySelector('.refresh-replies').onclick = (event) =>
            perform(event.currentTarget, () => load(number));
        form.onsubmit = (event) => {
            event.preventDefault();
            if (sending) return;
            perform(submit, async () => {
                const text = form.elements.text.value.trim();
                if (!text) {
                    formError(form, new Error('Введите текст ответа.'));
                    return;
                }
                sending = true;
                form.elements.text.disabled = true;
                form.querySelector('.form-error').hidden = true;
                try {
                    await api(`/reviews/${reviewId}/replies`, {
                        method: 'POST',
                        body: { text, replyToId: target?.id ?? null }
                    });
                    form.reset();
                    target = null;
                    paintTarget();
                    toast('Ответ опубликован');
                    await load(0);
                } catch (error) {
                    formError(form, error);
                    if (error.status === 0) {
                        submit.dataset.locked = 'true';
                        const reset = document.createElement('button');
                        reset.type = 'button';
                        reset.className = 'text-button';
                        reset.textContent = 'Проверить сообщения и открыть новую форму';
                        reset.onclick = () =>
                            perform(reset, async () => {
                                await load(0);
                                target = null;
                                renderComposer();
                            });
                        form.querySelector('.form-error').append(reset);
                    }
                } finally {
                    sending = false;
                    form.elements.text.disabled = false;
                }
            });
        };
    }

    function paintTarget() {
        const node = composer.querySelector('.reply-target');
        if (!node) return;
        node.hidden = !target;
        node.innerHTML = target
            ? `Ответ ${e(target.authorName)} · сообщение #${e(target.id)} <button type="button" class="text-button">Отменить</button>`
            : '';
        node.querySelector('button')?.addEventListener('click', () => {
            target = null;
            paintTarget();
        });
    }

    async function load(pageNumber = 0) {
        number = pageNumber;
        const ownRevision = ++revision;
        loading(list);
        try {
            const query = pageQuery(number);
            query.set('size', '20');
            const page = pageData(await api(`/reviews/${reviewId}/replies?${query}`));
            if (!card.isConnected || ownRevision !== revision) return;
            if (!page.items.length && number > 0) {
                await load(number - 1);
                return;
            }
            list.innerHTML =
                `<p class="fine-print">Ответов: ${page.total} · Сначала наиболее полезные</p>` +
                `<div class="reply-messages">${
                    page.items
                        .map(
                            (reply) =>
                                `<article class="reply-message" data-reply-id="${e(reply.id)}">` +
                                `<header>` +
                                `<strong>${e(reply.authorName)}</strong>${reply.isSeller ? '<span class="reply-seller">Продавец товара</span>' : ''}${String(reply.authorId) === String(currentUser()?.id) ? '<span class="fine-print">Это вы</span>' : ''}<time datetime="${e(reply.createdAt)}">${date(reply.createdAt)}</time>` +
                                `</header>${reply.replyToId ? `<p class="reply-reference">В ответ на сообщение #${e(reply.replyToId)}</p>` : ''}<p class="review-text">${e(reply.text)}</p>${reactionControls(reply)}${canShop() ? '<button type="button" class="text-button reply-to-message">Ответить</button>' : ''}</article>`
                        )
                        .join('') || '<p class="reviews-empty">Обсуждение пока пусто.</p>'
                }</div>`;
            pagination(list, page, load);
            list.querySelectorAll('.reply-message').forEach((message, index) => {
                const reply = page.items[index];
                message.querySelector('.reply-to-message')?.addEventListener('click', () => {
                    if (sending) return;
                    target = reply;
                    paintTarget();
                    composer.querySelector('textarea').focus();
                });
                const controls = message.querySelector('.review-reactions');
                let pending = false;
                controls.onclick = async (event) => {
                    const button = event.target.closest('[data-reaction]');
                    if (!button || button.disabled || pending || !canShop()) return;
                    pending = true;
                    controls.querySelectorAll('button').forEach((control) => (control.disabled = true));
                    const next =
                        reply.myReaction === button.dataset.reaction ? null : button.dataset.reaction;
                    try {
                        await api(`/review-replies/${reply.id}/reaction`, {
                            method: next ? 'PUT' : 'DELETE',
                            ...(next ? { body: { type: next } } : {})
                        });
                        await load(number);
                    } catch (error) {
                        toast(error.message, 'error');
                        if (card.isConnected) await load(number);
                    } finally {
                        pending = false;
                    }
                };
            });
        } catch (error) {
            if (card.isConnected && ownRevision === revision) showError(list, error, () => load(number));
        }
    }
    thread.addEventListener('toggle', () => {
        if (thread.open && !started) {
            started = true;
            renderComposer();
            load();
        }
    });
}

export async function replyModerationPage(view) {
    document.title = 'Модерация ответов — Рядом';
    const status =
        new URLSearchParams(location.hash.split('?')[1]).get('status') === 'HIDDEN' ? 'HIDDEN' : 'PUBLISHED';
    view.innerHTML =
        `<nav class="tabs">` +
        `<a class="tab" href="#/admin/moderation">Товары</a>` +
        `<a class="tab" href="#/admin/review-moderation">Отзывы</a>` +
        `<a class="tab active" href="#/admin/reply-moderation">Ответы</a>` +
        `</nav>${pageHeading('ПОСТМОДЕРАЦИЯ', 'Ответы на отзывы', 'Сообщения публикуются сразу. Скрывайте нарушения или восстанавливайте ошибочно скрытые ответы.')}<nav class="tabs">${[
            ['PUBLISHED', 'Опубликованные'],
            ['HIDDEN', 'Скрытые']
        ]
            .map(
                ([value, title]) =>
                    `<a class="tab ${status === value ? 'active' : ''}" href="#/admin/reply-moderation?status=${value}">${title}</a>`
            )
            .join('')}</nav>` +
        `<div class="reply-moderation-list">` +
        `</div>`;
    const area = view.querySelector('.reply-moderation-list');
    let revision = 0;
    async function load(number = 0) {
        const ownRevision = ++revision;
        loading(area);
        try {
            const query = pageQuery(number, 'createdAt,desc');
            query.append('sort', 'id,desc');
            query.set('status', status);
            const page = pageData(await api(`/moderation/review-replies?${query}`));
            if (!view.isConnected || revision !== ownRevision) return;
            if (!page.items.length && number > 0) {
                await load(number - 1);
                return;
            }
            area.innerHTML =
                page.items
                    .map(
                        (reply) =>
                            `<article class="panel form-panel reply-message">` +
                            `<header>` +
                            `<strong>${e(reply.authorName)}</strong>${reply.isSeller ? '<span class="reply-seller">Продавец товара</span>' : ''}<time>${date(reply.createdAt)}</time>` +
                            `</header>` +
                            `<p class="fine-print">Сообщение #${e(reply.id)} · Отзыв #${e(reply.reviewId)}${reply.replyToId ? ` · Ответ на #${e(reply.replyToId)}` : ''}</p>` +
                            `<p class="review-text">${e(reply.text)}</p>` +
                            `<button type="button" class="button ${status === 'PUBLISHED' ? 'danger' : 'secondary'} small change-reply-status">${status === 'PUBLISHED' ? 'Скрыть' : 'Восстановить'}</button>` +
                            `<div class="reply-context">` +
                            `</div>` +
                            `</article>`
                    )
                    .join('') || '<p class="reviews-empty">Сообщений с выбранным статусом нет.</p>';
            area.querySelectorAll('.reply-message').forEach((card, index) => {
                const reply = page.items[index];
                attachReplies(card.querySelector('.reply-context'), reply.reviewId);
                const button = card.querySelector('.change-reply-status');
                button.onclick = () =>
                    perform(button, async () => {
                        try {
                            await api(`/moderation/review-replies/${reply.id}`, {
                                method: 'PATCH',
                                body: { status: status === 'PUBLISHED' ? 'HIDDEN' : 'PUBLISHED' }
                            });
                            toast(status === 'PUBLISHED' ? 'Ответ скрыт' : 'Ответ восстановлен');
                            await load(number);
                        } catch (error) {
                            toast(error.message, 'error');
                            await load(number);
                        }
                    });
            });
            pagination(area, page, load);
        } catch (error) {
            if (view.isConnected && ownRevision === revision) showError(area, error, () => load(number));
        }
    }
    await load();
}
