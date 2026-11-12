# Фронтенд «Рядом»

Исходники: [HTML, CSS и JavaScript](../src/main/resources/static). Отдельной сборки не требуется.

## Запуск

Запустить бэкэнд и открыть [localhost:8080](http://localhost:8080/).

Чтобы запустить фронтенд отдельно, выполнить в корне:

```powershell
node frontend/serve.mjs
```

Открыть [localhost:5173](http://localhost:5173/). Запросы к API и изображениям проксируются на бэкенд, который работает на порту 8080.

Проверка синтаксиса и импортов:

```powershell
node frontend/check.mjs
```
