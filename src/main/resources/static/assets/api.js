const SESSION_KEY = 'ryadom.session';
let session = readSession();

function claims(token) {
    const encoded = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(new TextDecoder().decode(Uint8Array.from(atob(encoded), (char) => char.charCodeAt(0))));
}

function readSession() {
    try {
        const stored = JSON.parse(sessionStorage.getItem(SESSION_KEY));
        if (stored && claims(stored.token).exp * 1000 > Date.now()) return stored;
        sessionStorage.removeItem(SESSION_KEY);
    } catch {
        /* A damaged or expired local session is not authentication. */
    }
    return null;
}

export function currentUser() {
    if (!session) return null;
    try {
        const payload = claims(session.token);
        if (payload.exp * 1000 <= Date.now()) {
            logout();
            return null;
        }
        // Claims are for navigation only. The backend verifies signature and permissions.
        return { id: payload.sub, roles: payload.roles || [], label: session.label };
    } catch {
        logout();
        return null;
    }
}

export function hasRole(role) {
    return currentUser()?.roles.includes(role) || false;
}
export function canShop() {
    return !!currentUser() && !hasRole('ADMIN') && (hasRole('BUYER') || hasRole('SELLER'));
}

export function loginSession(token, label) {
    claims(token);
    session = { token, label };
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
    window.dispatchEvent(new Event('sessionchange'));
}

export function logout() {
    session = null;
    sessionStorage.removeItem(SESSION_KEY);
    window.dispatchEvent(new Event('sessionchange'));
}

export class ApiError extends Error {
    constructor(message, status = 0, errors = []) {
        super(message);
        this.status = status;
        this.errors = errors;
    }
}

export async function api(path, { method = 'GET', body, publicRequest = false, signal } = {}) {
    const headers = new Headers({ Accept: 'application/json' });
    if (!publicRequest && currentUser()) headers.set('Authorization', `Bearer ${session.token}`);
    if (body !== undefined && !(body instanceof FormData)) {
        headers.set('Content-Type', 'application/json');
        body = JSON.stringify(body);
    }
    let response;
    try {
        response = await fetch(`/api${path}`, {
            method,
            headers,
            body,
            signal: signal || AbortSignal.timeout(30000),
            cache: 'no-store'
        });
    } catch (error) {
        if (error.name === 'AbortError') throw error;
        throw new ApiError(
            method === 'GET'
                ? 'Не удалось связаться с сервером. Убедитесь, что бэкенд запущен на порту 8080, и повторите попытку.'
                : 'Соединение прервано. Операция могла выполниться: обновите данные перед повторной отправкой.'
        );
    }
    const text = await response.text();
    let data;
    try {
        data = text ? JSON.parse(text) : null;
    } catch {
        data = null;
    }
    if (!response.ok) {
        if (response.status === 401 && !publicRequest && session) {
            logout();
            window.dispatchEvent(new Event('sessionexpired'));
        }
        const defaults = {
            400: 'Проверьте заполненные поля.',
            401: publicRequest ? 'Неверный логин или пароль.' : 'Войдите в аккаунт, чтобы продолжить.',
            403: 'У вашего аккаунта нет доступа к этому действию.',
            404: 'Запрашиваемые данные не найдены.',
            409: 'Действие недоступно в текущем состоянии. Обновите данные.',
            413: 'Файл слишком большой. Попробуйте уменьшить размер изображения.',
            500: 'Сервер не смог выполнить запрос. Попробуйте позднее.',
            502: 'Бэкенд недоступен. Запустите Spring-приложение на порту 8080.'
        };
        throw new ApiError(
            data?.message || defaults[response.status] || `Ошибка запроса (${response.status})`,
            response.status,
            data?.errors || []
        );
    }
    return data;
}

export function pageData(data) {
    const meta = data?.page || data || {};
    return {
        items: data?.content || [],
        number: meta.number || 0,
        totalPages: meta.totalPages || 0,
        total: meta.totalElements || 0
    };
}

export function pageQuery(page = 0, sort = '') {
    const query = new URLSearchParams({ page: String(page), size: '12' });
    if (sort) query.set('sort', sort);
    return query;
}
