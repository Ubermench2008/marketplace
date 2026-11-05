import { api, pageData, pageQuery } from './api.js';
import {
    escapeHtml as e,
    icon,
    image,
    badge,
    loading,
    empty,
    pagination,
    perform,
    toast,
    showError,
    pageHeading,
    formError,
    confirmAction
} from './ui.js';
import { productCard } from './storefront.js';

export async function sellerPage(view) {
    document.title = 'Мои товары — Рядом';
    view.innerHTML =
        `${pageHeading('КАБИНЕТ ПРОДАВЦА', 'Мои товары', 'Создавайте, редактируйте и отправляйте товары на проверку.', `<a class="button" href="#/seller/products/new">${icon('plus')} Добавить товар</a>`)}<div class="notice">Новый товар создаётся снятым с продажи. После одобрения откройте товар и нажмите «Выставить на продажу».</div>` +
        `<div class="seller-content">` +
        `</div>`;
    const area = view.querySelector('.seller-content');
    const load = async (number = 0) => {
        loading(area, true);
        try {
            // The seller repository is rooted at ProductCardEntity, hence product.name.
            const page = pageData(await api(`/seller/products?${pageQuery(number, 'product.name,asc')}`));
            area.innerHTML = page.items.length
                ? `<div class="product-grid">${page.items.map((product) => productCard(product, true)).join('')}</div>`
                : empty(
                      'Начните с первого товара',
                      'Добавьте описание, превью и хотя бы одно подробное фото.',
                      '#/seller/products/new',
                      'Создать товар'
                  );
            pagination(area, page, load);
        } catch (error) {
            showError(area, error, () => load(number));
        }
    };
    await load();
}

function checkImage(file) {
    if (!file.size) throw new Error(`Файл «${file.name}» пуст.`);
    if (!['image/webp', 'image/png', 'image/jpeg'].includes(file.type))
        throw new Error(`«${file.name}»: выберите WEBP, JPEG или PNG.`);
}

const maxStockQuantity = 2147483647;

async function loadStockEditor(panel, productId) {
    const path = `/seller/products/${encodeURIComponent(productId)}/stock`;
    loading(panel);
    try {
        const stock = await api(path);
        if (!panel.isConnected) return;

        panel.innerHTML =
            '<div class="stock-editor-heading">' +
            '<div><h2>Складской остаток</h2>' +
            '<p class="fine-print">Оформление заказа уменьшает остаток, отмена возвращает товар на склад.</p></div>' +
            `<p class="stock-editor-count" aria-live="polite"><strong class="stock-quantity">${e(stock.quantity)}</strong> шт.</p>` +
            '</div><form class="stack-form">' +
            '<div class="form-error" role="alert" tabindex="-1" hidden></div>' +
            '<div class="form-columns">' +
            '<label>Операция<select name="operation">' +
            '<option value="add">Пополнить</option><option value="remove">Списать</option>' +
            '</select></label>' +
            '<label>Количество, шт.' +
            `<input name="amount" type="number" min="1" max="${maxStockQuantity}" step="1" required>` +
            '</label></div>' +
            '<p class="fine-print stock-preview" aria-live="polite"></p>' +
            '<div class="button-row"><button class="button small" type="submit">Пополнить</button>' +
            '<button class="text-button refresh-stock" type="button">Обновить остаток</button></div>' +
            '</form>';

        const form = panel.querySelector('form');
        const input = form.elements.amount;
        const operation = form.elements.operation;
        const submit = form.querySelector('[type="submit"]');
        const refresh = form.querySelector('.refresh-stock');
        const quantity = panel.querySelector('.stock-quantity');
        const preview = form.querySelector('.stock-preview');
        let currentQuantity = stock.quantity;

        const deltaForInput = () => operation.value === 'remove' ? -input.valueAsNumber : input.valueAsNumber;
        const updatePreview = () => {
            submit.textContent = operation.value === 'remove' ? 'Списать' : 'Пополнить';
            preview.textContent = '';
            if (!input.value || !input.validity.valid) return;

            const nextQuantity = currentQuantity + deltaForInput();
            if (nextQuantity < 0) {
                preview.textContent = 'По последним данным, столько товара на складе нет.';
            } else if (nextQuantity > maxStockQuantity) {
                preview.textContent = 'Превышен максимальный остаток.';
            } else {
                preview.textContent = `По текущим данным останется: ${nextQuantity} шт.`;
            }
        };

        refresh.onclick = () => loadStockEditor(panel, productId);
        operation.onchange = updatePreview;
        input.oninput = updatePreview;

        form.onsubmit = (event) => {
            event.preventDefault();
            perform(submit, async () => {
                if (!form.reportValidity()) return;
                const delta = deltaForInput();

                form.querySelector('.form-error').hidden = true;
                input.disabled = true;
                operation.disabled = true;
                refresh.disabled = true;
                try {
                    const updated = await api(path, { method: 'PATCH', body: { delta } });
                    currentQuantity = updated.quantity;
                    quantity.textContent = currentQuantity;
                    input.value = '';
                    updatePreview();
                    toast('Остаток обновлён');
                } catch (error) {
                    if (error.status === 409 || error.status === 0) {
                        submit.dataset.locked = 'true';
                        const message = error.status === 409
                            ? 'Не удалось применить изменение. Обновите остаток и проверьте количество.'
                            : 'Связь прервалась. Обновите остаток перед повторной попыткой.';
                        formError(form, new Error(message));
                    } else {
                        formError(form, error);
                    }
                } finally {
                    input.disabled = false;
                    operation.disabled = false;
                    refresh.disabled = false;
                }
            });
        };
    } catch (error) {
        if (panel.isConnected) showError(panel, error, () => loadStockEditor(panel, productId));
    }
}

export async function productEditor(view, id) {
    const creating = id === 'new';
    let product = creating
        ? { name: '', slug: '', description: '', price: '', status: 'OFF', moderationStatus: 'NOT_SUBMITTED' }
        : await api(`/products/${encodeURIComponent(id)}`);
    let images = creating
        ? []
        : (await api(`/products/${id}/details-images`)).sort((a, b) => a.position - b.position);
    if (!view.isConnected) return;
    let removedIds = new Set();
    let preview = null;
    let previewUrl = '';
    let imageChanged = false;
    let saving = false;
    let uncertain = false;
    const objectUrls = new Set();
    const objectUrl = (file) => {
        const url = URL.createObjectURL(file);
        objectUrls.add(url);
        return url;
    };
    let beforeUnload = null;
    const cleanup = () => {
        objectUrls.forEach((url) => URL.revokeObjectURL(url));
        if (beforeUnload) window.removeEventListener('beforeunload', beforeUnload);
        delete view.beforeLeave;
    };
    view.addEventListener('dispose', cleanup, { once: true });
    const editable = creating || (product.status === 'OFF' && product.moderationStatus !== 'PENDING');
    document.title = `${creating ? 'Новый товар' : 'Редактирование товара'} — Рядом`;
    view.innerHTML =
        `<a class="back-link" href="#/seller">← Мои товары</a>${pageHeading('КАБИНЕТ ПРОДАВЦА', creating ? 'Новый товар' : 'Редактирование товара', creating ? 'Расскажите о товаре и покажите его со всех сторон.' : product.name)}<div class="editor-status">${badge(product.status)}${badge(product.moderationStatus)}</div>${!editable ? '<div class="notice warning">Редактирование доступно только для снятого с продажи товара без активной заявки на модерацию.</div>' : ''}<form class="product-form">` +
        `<div class="form-error" role="alert" tabindex="-1" hidden>` +
        `</div>` +
        `<div class="editor-layout">` +
        `<div class="editor-main">` +
        `<section class="panel form-panel">` +
        `<h2>` +
        `<span class="section-number">01</span> Основная информация</h2>` +
        `<fieldset ${!editable ? 'disabled' : ''}>` +
        `<label>Название<input name="name" required maxlength="150" value="${e(product.name)}" placeholder="Например, настольная лампа">` +
        `</label>` +
        `<div class="form-columns">` +
        `<label>Цена, ₽<input name="price" type="number" min="0.01" max="999999999.99" step="0.01" required value="${e(product.price)}" placeholder="0,00">` +
        `</label>` +
        `<label>Slug<input name="slug" required maxlength="150" pattern="[a-z0-9-]+" value="${e(product.slug)}" placeholder="table-lamp">` +
        `<span class="field-hint">Латинские строчные буквы, цифры и дефисы.</span>` +
        `</label>` +
        `</div>` +
        (creating
            ? '<label>Начальный остаток, шт.' +
              `<input name="initialQuantity" type="number" min="0" max="${maxStockQuantity}" step="1" value="0" required>` +
              '<span class="field-hint">Укажите, сколько единиц уже есть на складе. Ноль допустим.</span></label>'
            : '') +
        `<label>Описание<textarea name="description" rows="6" required minlength="50" maxlength="255" placeholder="Материалы, особенности и всё, что стоит знать покупателю. Минимум 50 символов.">${e(product.description)}</textarea>` +
        `<span class="field-hint">50–255 символов — ограничение текущей таблицы товара.</span>` +
        `</label>` +
        `</fieldset>` +
        `</section>` +
        `<section class="panel form-panel">` +
        `<h2>` +
        `<span class="section-number">02</span> Подробные фотографии</h2>` +
        `<p class="muted">Хотя бы одно фото. Меняйте порядок стрелками или перетаскиванием.</p>` +
        `<div class="editor-images">` +
        `</div>${
            editable
                ? `<label class="upload-zone">${icon('plus')}<strong>Добавить фотографии</strong>` +
                  `<span>WEBP, JPEG или PNG</span>` +
                  `<input class="visually-hidden" type="file" id="details-upload" accept="image/webp,image/jpeg,image/png" multiple>` +
                  `</label>`
                : ''
        }</section>` +
        `</div>` +
        `<aside class="editor-aside">` +
        `<section class="panel form-panel">` +
        `<h2>` +
        `<span class="section-number">03</span> Превью</h2>` +
        `<p class="muted">Главное фото в каталоге.</p>` +
        `<div class="preview-box">${creating ? '<div class="image-placeholder">Превью ещё не выбрано</div>' : '<div class="image-placeholder">Текущее превью видно в «Моих товарах»</div>'}</div>${
            editable
                ? `<label class="button secondary upload-button">${icon('image')} ${creating ? 'Выбрать превью' : 'Заменить превью'}<input class="visually-hidden" type="file" id="preview-upload" accept="image/webp,image/jpeg,image/png">` +
                  `</label>`
                : ''
        }</section>` +
        `<section class="panel form-panel save-panel">` +
        `<h3>${creating ? 'Готово к первому шагу?' : 'Сохранение изменений'}</h3>` +
        `<p class="fine-print">${creating ? 'Товар появится в вашем кабинете со статусом «Снят с продажи».' : 'Изменение данных сбросит одобрение. Отправьте обновлённый товар на модерацию повторно.'}</p>${editable ? `<button type="submit" class="button save-product">${icon('check')} ${creating ? 'Создать товар' : 'Сохранить изменения'}</button>` : ''}<p class="fine-print">Изменения полей и фотографий отправляются отдельными запросами. При ошибке часть изменений может быть сохранена.</p>` +
        `</section>${!creating ? '<section class="panel form-panel lifecycle"><h3>Действия с товаром</h3><div class="lifecycle-actions"></div></section>' : ''}</aside>` +
        `</div>` +
        `</form>`;
    const form = view.querySelector('form');
    if (!creating) {
        const stockPanel = document.createElement('section');
        stockPanel.className = 'panel form-panel stock-editor';
        form.before(stockPanel);
        loadStockEditor(stockPanel, id);
    }
    const imagesArea = view.querySelector('.editor-images');
    const drawImages = () => {
        imagesArea.innerHTML = images.length
            ? images
                  .map(
                      (item, index) =>
                          `<div class="editor-image" data-index="${index}" draggable="${editable}">${item.file ? `<div class="image-frame"><img src="${e(item.localUrl)}" alt="Новое фото ${index + 1}"></div>` : image(item.imgUrl, `Фото ${index + 1}`)}<div class="image-position">${index + 1}${item.file ? ' · новое' : ''}</div>${
                              editable
                                  ? `<div class="image-controls">` +
                                    `<button type="button" data-move="-1" data-index="${index}" ${index === 0 ? 'disabled' : ''} aria-label="Фото ${index + 1} влево">←</button>` +
                                    `<button type="button" data-move="1" data-index="${index}" ${index === images.length - 1 ? 'disabled' : ''} aria-label="Фото ${index + 1} вправо">→</button>` +
                                    `<button type="button" data-remove="${index}" aria-label="Удалить фото ${index + 1}">${icon('trash')}</button>` +
                                    `</div>`
                                  : ''
                          }</div>`
                  )
                  .join('')
            : '<p class="muted">Здесь будут фотографии товара.</p>';
        const move = (from, to) => {
            if (saving) return;
            images.splice(to, 0, images.splice(from, 1)[0]);
            imageChanged = true;
            drawImages();
        };
        imagesArea
            .querySelectorAll('[data-move]')
            .forEach(
                (button) =>
                    (button.onclick = () =>
                        move(
                            Number(button.dataset.index),
                            Number(button.dataset.index) + Number(button.dataset.move)
                        ))
            );
        imagesArea.querySelectorAll('[data-remove]').forEach(
            (button) =>
                (button.onclick = () => {
                    if (saving) return;
                    const item = images.splice(Number(button.dataset.remove), 1)[0];
                    if (item.uuid) removedIds.add(item.uuid);
                    imageChanged = true;
                    drawImages();
                })
        );
        imagesArea.querySelectorAll('[draggable="true"]').forEach((node) => {
            node.ondragstart = (event) => event.dataTransfer.setData('text/plain', node.dataset.index);
            node.ondragover = (event) => event.preventDefault();
            node.ondrop = (event) => {
                event.preventDefault();
                const from = Number(event.dataTransfer.getData('text/plain'));
                const to = Number(node.dataset.index);
                if (Number.isInteger(from) && from >= 0 && from < images.length && from !== to)
                    move(from, to);
            };
        });
    };
    drawImages();
    if (product.previewImageUrl)
        view.querySelector('.preview-box').innerHTML = image(product.previewImageUrl, 'Текущее превью');
    view.querySelector('#details-upload')?.addEventListener('change', (event) => {
        try {
            const files = [...event.target.files];
            files.forEach(checkImage);
            files.forEach((file) => images.push({ file, localUrl: objectUrl(file) }));
            imageChanged = true;
            drawImages();
        } catch (error) {
            toast(error.message, 'error');
        }
        event.target.value = '';
    });
    view.querySelector('#preview-upload')?.addEventListener('change', (event) => {
        const file = event.target.files[0];
        if (!file) return;
        try {
            checkImage(file);
            preview = file;
            previewUrl = objectUrl(file);
            view.querySelector('.preview-box').innerHTML =
                `<div class="image-frame"><img src="${e(previewUrl)}" alt="Новое превью"></div><p class="field-hint">${e(file.name)}</p>`;
        } catch (error) {
            toast(error.message, 'error');
        }
        event.target.value = '';
    });
    const values = () => ({
        name: form.elements.name.value,
        slug: form.elements.slug.value,
        description: form.elements.description.value,
        price: Number(form.elements.price.value)
    });
    const changes = () =>
        Object.fromEntries(
            Object.entries(values()).filter(([key, value]) =>
                key === 'price' ? value !== Number(product.price) : value !== product[key]
            )
        );
    const dirty = () => Object.keys(changes()).length || imageChanged || preview;
    let saved = false;
    const unsaved = () =>
        !saved &&
        editable &&
        (creating
            ? Object.values(values()).some((value) => value !== '' && value !== 0) ||
              form.elements.initialQuantity.value !== '0' ||
              images.length ||
              preview
            : dirty());
    view.beforeLeave = async () =>
        !unsaved() ||
        (await confirmAction(
            'Уйти без сохранения?',
            'Несохранённые изменения товара будут потеряны.',
            'Уйти'
        ));
    beforeUnload = (event) => {
        if (unsaved()) {
            event.preventDefault();
            event.returnValue = '';
        }
    };
    window.addEventListener('beforeunload', beforeUnload);
    if (!creating) {
        const actions = view.querySelector('.lifecycle-actions');
        actions.innerHTML = `${product.status === 'OFF' && product.moderationStatus === 'APPROVED' ? '<button type="button" class="button" data-action="publish">Выставить на продажу</button>' : ''}${product.status === 'ACTIVE' ? '<button type="button" class="button secondary" data-action="withdraw">Снять с продажи</button>' : ''}${product.status === 'OFF' && ['NOT_SUBMITTED', 'REJECTED'].includes(product.moderationStatus) ? '<button type="button" class="button" data-action="moderation-requests">Отправить на модерацию</button>' : ''}${product.status === 'OFF' && product.moderationStatus !== 'PENDING' ? '<button type="button" class="button secondary" data-action="archive">Архивировать</button>' : ''}${
            product.moderationStatus === 'PENDING'
                ? '<p class="muted">Товар ожидает рассмотрения модератором.</p>' +
                  '<button type="button" class="button secondary" data-action="cancel-moderation">Отозвать и вернуться к редактированию</button>'
                : ''
        }${product.status === 'ARCHIVED' ? '<p class="muted">Товар в архиве. API восстановления пока не реализован.</p>' : ''}`;
        actions.querySelectorAll('[data-action]').forEach(
            (button) =>
                (button.onclick = () =>
                    perform(button, async () => {
                        if (dirty()) {
                            toast('Сначала сохраните изменения товара.', 'error');
                            return;
                        }
                        const action = button.dataset.action;
                        const titles = {
                            publish: 'Выставить товар на продажу?',
                            archive: 'Архивировать товар?',
                            withdraw: 'Снять товар с продажи?',
                            'moderation-requests': 'Отправить товар на модерацию?',
                            'cancel-moderation': 'Отозвать заявку с модерации?'
                        };
                        const descriptions = {
                            publish:
                                'Покупатели смогут добавить товар в корзину. Для редактирования потребуется сначала снять его с продажи.',
                            archive: 'Данные товара, фотографии и история сохранятся.',
                            withdraw: 'Данные товара, фотографии и история сохранятся.',
                            'moderation-requests': 'На время проверки редактирование будет недоступно.',
                            'cancel-moderation':
                                'Заявка и её снимок останутся в истории. Вы сможете исправить товар и отправить новую заявку.'
                        };
                        if (!(await confirmAction(titles[action], descriptions[action]))) return;
                        const cancelling = action === 'cancel-moderation';
                        try {
                            await api(
                                `/products/${id}/${cancelling ? 'moderation-requests/current/cancel' : action}`,
                                {
                                    method: 'POST'
                                }
                            );
                        } catch (error) {
                            if (cancelling && error.status === 409) {
                                cleanup();
                                await productEditor(view, id);
                            }
                            throw error;
                        }
                        toast(
                            cancelling
                                ? 'Заявка отозвана. Можно редактировать товар.'
                                : action === 'publish'
                                  ? 'Товар выставлен на продажу'
                                  : 'Статус товара обновлён'
                        );
                        cleanup();
                        await productEditor(view, id);
                    }))
        );
    }
    form.onsubmit = (event) => {
        event.preventDefault();
        const button = form.querySelector('.save-product');
        if (!button || uncertain) return;
        perform(button, async () => {
            const fieldsets = [...form.querySelectorAll('fieldset')];
            try {
                form.querySelector('.form-error').hidden = true;
                if (!images.length) throw new Error('Добавьте хотя бы одну подробную фотографию.');
                if (creating && !preview) throw new Error('Выберите превью товара.');
                if (!creating && !dirty()) {
                    toast('Изменений нет — всё уже сохранено');
                    return;
                }
                saving = true;
                fieldsets.forEach((fieldset) => (fieldset.disabled = true));
                // Freeze image controls while multiple requests form one user action.
                form.querySelectorAll('.image-controls button, input[type="file"], [data-action]').forEach(
                    (control) => (control.disabled = true)
                );
                if (creating) {
                    const productData = {
                        ...values(),
                        initialQuantity: form.elements.initialQuantity.valueAsNumber
                    };
                    const body = new FormData();
                    body.append(
                        'product',
                        new Blob([JSON.stringify(productData)], { type: 'application/json' })
                    );
                    body.append('previewImage', preview);
                    images.forEach((item) => body.append('detailsImages', item.file));
                    const result = await api('/products', { method: 'POST', body });
                    saved = true;
                    toast('Товар создан');
                    location.hash = `/seller/products/${result.id}`;
                    return;
                }
                const patch = changes();
                if (Object.keys(patch).length)
                    product = await api(`/products/${id}`, { method: 'PATCH', body: patch });
                if (preview) {
                    const body = new FormData();
                    body.append('previewImage', preview);
                    const card = await api(`/products/${id}/preview-image`, { method: 'PUT', body });
                    preview = null;
                    view.querySelector('.preview-box').innerHTML = image(card.imgUrl, 'Превью товара');
                }
                const newImages = images.filter((item) => item.file);
                if (newImages.length) {
                    const body = new FormData();
                    newImages.forEach((item) => body.append('images', item.file));
                    const added = await api(`/products/${id}/details-images`, { method: 'POST', body });
                    added.forEach((saved, index) => Object.assign(newImages[index], saved, { file: null }));
                }
                for (const imageId of [...removedIds]) {
                    await api(`/products/${id}/details-images/${imageId}`, { method: 'DELETE' });
                    removedIds.delete(imageId);
                }
                if (imageChanged) {
                    const persisted = (await api(`/products/${id}/details-images`)).sort(
                        (a, b) => a.position - b.position
                    );
                    const imageIds = images.map((item) => item.uuid);
                    if (JSON.stringify(persisted.map((item) => item.uuid)) !== JSON.stringify(imageIds)) {
                        await api(`/products/${id}/details-images/order`, {
                            method: 'PUT',
                            body: { imageIds }
                        });
                    }
                }
                toast('Изменения сохранены');
                cleanup();
                await productEditor(view, id);
            } catch (error) {
                formError(form, error);
                if (error.status === 0) {
                    uncertain = true;
                    button.dataset.locked = 'true';
                    const reload = document.createElement('button');
                    reload.type = 'button';
                    reload.className = 'button secondary';
                    reload.textContent = 'Проверить данные на сервере';
                    reload.onclick = async () => {
                        if (
                            await confirmAction(
                                'Перезагрузить данные?',
                                'Несохранённые изменения формы будут потеряны.'
                            )
                        ) {
                            if (creating) {
                                saved = true;
                                location.hash = '/seller';
                            } else {
                                cleanup();
                                await productEditor(view, id);
                            }
                        }
                    };
                    form.querySelector('.form-error').append(document.createElement('br'), reload);
                }
            } finally {
                saving = false;
                fieldsets.forEach((fieldset) => (fieldset.disabled = false));
                form.querySelectorAll('.image-controls button, input[type="file"], [data-action]').forEach(
                    (control) => (control.disabled = false)
                );
                drawImages();
            }
        });
    };
}
