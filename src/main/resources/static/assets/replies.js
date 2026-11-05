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

export function attachReplies(card, reviewId, replyCount, allowReply = true) {
    const summary = replyCount == null ? 'Обсуждение отзыва' : `Обсуждение отзыва · ${e(replyCount)}`;
    card.insertAdjacentHTML(
        'beforeend',
        '<details class="reply-thread">' +
            `<summary>${summary}</summary>` +
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
    const revisions = new WeakMap();
    let number = 0;
    let target = null;
    let sending = false;

    function renderComposer() {
        if (!allowReply) return;
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
                const replyTarget = target;
                try {
                    await api(`/reviews/${reviewId}/replies`, {
                        method: 'POST',
                        body: { text, replyToId: replyTarget?.id ?? null }
                    });
                    form.reset();
                    target = null;
                    paintTarget();
                    toast('Ответ опубликован');
                    if (replyTarget) {
                        await replyTarget.refresh();
                    } else {
                        await load(0);
                    }
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
            ? `Ответ для ${e(target.authorName)} <button type="button" class="text-button">Отменить</button>`
            : '';
        node.querySelector('button')?.addEventListener('click', () => {
            target = null;
            paintTarget();
        });
    }

    async function load(pageNumber = 0) {
        number = pageNumber;
        await loadLevel(list, null, pageNumber, 0, thread);
    }

    async function loadLevel(container, parentId, pageNumber, depth, branch) {
        const revision = (revisions.get(container) ?? 0) + 1;
        revisions.set(container, revision);
        if (!container.children.length) loading(container);

        try {
            const query = pageQuery(pageNumber);
            query.set('size', '20');
            if (parentId != null) query.set('parentId', parentId);

            const page = pageData(await api(`/reviews/${reviewId}/replies/children?${query}`));
            if (!container.isConnected || revisions.get(container) !== revision) return;

            if (!page.items.length && pageNumber > 0) {
                if (parentId == null) number = pageNumber - 1;
                await loadLevel(container, parentId, pageNumber - 1, depth, branch);
                return;
            }

            const existingBranches = new Map();
            container.querySelectorAll(':scope > .reply-level > .reply-node').forEach((node) => {
                existingBranches.set(node.dataset.replyId, node.querySelector(':scope > .reply-branch'));
            });

            const level = document.createElement('div');
            level.className = 'reply-level';

            for (const reply of page.items) {
                const previousBranch = existingBranches.get(String(reply.id));
                level.append(renderReply(reply, depth, previousBranch, () => {
                    return loadLevel(container, parentId, pageNumber, depth, branch);
                }));
            }

            if (page.items.length) {
                container.replaceChildren(level);
            } else {
                container.innerHTML = '<p class="reviews-empty">Ответов пока нет.</p>';
            }

            branch.querySelector(':scope > summary').textContent = parentId == null
                ? `Обсуждение отзыва · ${page.total}`
                : `Ответы · ${page.total}`;

            pagination(container, page, (nextPage) => {
                if (parentId == null) return load(nextPage);
                return loadLevel(container, parentId, nextPage, depth, branch);
            });
        } catch (error) {
            if (container.isConnected && revisions.get(container) === revision) {
                showError(container, error, () => loadLevel(container, parentId, pageNumber, depth, branch));
            }
        }
    }

    function renderReply(reply, depth, previousBranch, reloadLevel) {
        const node = document.createElement('div');
        node.className = 'reply-node';
        node.dataset.replyId = reply.id;

        const message = document.createElement('article');
        message.className = 'reply-message';
        node.append(message);

        const canAnswer = allowReply && canShop() && reply.status !== 'HIDDEN';
        if (reply.status === 'HIDDEN') {
            message.innerHTML = '<p class="fine-print">Сообщение скрыто</p>';
        } else {
            const seller = reply.isSeller ? '<span class="reply-seller">Продавец товара</span>' : '';
            const own = String(reply.authorId) === String(currentUser()?.id)
                ? '<span class="fine-print">Это вы</span>'
                : '';
            const answer = canAnswer
                ? '<button type="button" class="text-button reply-to-message">Ответить</button>'
                : '';
            const reactions = allowReply ? reactionControls(reply) : '';

            message.innerHTML =
                `<header><strong>${e(reply.authorName)}</strong>${seller}${own}` +
                `<time datetime="${e(reply.createdAt)}">${date(reply.createdAt)}</time></header>` +
                `<p class="review-text">${e(reply.text)}</p>${reactions}${answer}`;

            if (allowReply) bindReactionControls(message, reply, reloadLevel);
        }

        const branch = previousBranch || document.createElement('details');
        branch.className = depth < 3 ? 'reply-branch reply-branch-indented' : 'reply-branch';
        if (!previousBranch) {
            branch.innerHTML = '<summary></summary><div class="reply-children" aria-live="polite"></div>';
        }
        branch.hidden = !reply.replyCount;
        branch.querySelector('summary').textContent = `Ответы · ${reply.replyCount}`;

        const children = branch.querySelector(':scope > .reply-children');
        function refreshChildren() {
            branch.hidden = false;
            const wasOpen = branch.open;
            branch.open = true;
            if (!wasOpen && !children.children.length) return;
            return loadLevel(children, reply.id, 0, depth + 1, branch);
        }

        branch.ontoggle = () => {
            if (branch.open && !children.children.length) {
                loadLevel(children, reply.id, 0, depth + 1, branch);
            }
        };
        node.append(branch);

        message.querySelector('.reply-to-message')?.addEventListener('click', () => {
            if (sending) return;
            target = { id: reply.id, authorName: reply.authorName, refresh: refreshChildren };
            paintTarget();
            composer.querySelector('textarea').focus();
        });
        return node;
    }

    function bindReactionControls(message, reply, reloadLevel) {
        const controls = message.querySelector('.review-reactions');
        let pending = false;
        controls.onclick = async (event) => {
            const button = event.target.closest('[data-reaction]');
            if (!button || button.disabled || pending || !canShop()) return;

            pending = true;
            controls.querySelectorAll('button').forEach((control) => (control.disabled = true));
            const next = reply.myReaction === button.dataset.reaction ? null : button.dataset.reaction;
            try {
                await api(`/review-replies/${reply.id}/reaction`, {
                    method: next ? 'PUT' : 'DELETE',
                    ...(next ? { body: { type: next } } : {})
                });
            } catch (error) {
                toast(error.message, 'error');
            } finally {
                pending = false;
                if (controls.isConnected) {
                    await reloadLevel();
                }
            }
        };
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
