# ZelenBo

<p align="center">
  <img src="app/src/main/res/drawable/ic_launcher_foreground.xml" alt="ZelenBo logo" width="120" />
</p>

<p align="center">
  <a href="https://github.com/flowerswithoppo-cmyk/ZelenBo/actions/workflows/build.yml">
    <img alt="Build" src="https://github.com/flowerswithoppo-cmyk/ZelenBo/actions/workflows/build.yml/badge.svg" />
  </a>
  <a href="https://github.com/flowerswithoppo-cmyk/ZelenBo/releases">
    <img alt="Release" src="https://img.shields.io/github/v/release/flowerswithoppo-cmyk/ZelenBo" />
  </a>
  <a href="https://github.com/flowerswithoppo-cmyk/ZelenBo/releases">
    <img alt="Downloads" src="https://img.shields.io/github/downloads/flowerswithoppo-cmyk/ZelenBo/total" />
  </a>
</p>

Android-приложение для улучшения стабильности сетевого подключения через локальный VPN-перехват DNS, DoH/DoT и встроенный SOCKS5-прокси.

## Что уже реализовано

- Jetpack Compose UI (Dashboard / Services / Settings / Logs / About).
- Clean Architecture + MVVM + Hilt DI.
- Локальный `VpnService` с DNS-перехватом.
- DNS-резолвинг через DoH / DoT / fallback UDP.
- Split-логика для доменов (в первую очередь Telegram).
- Локальный SOCKS5 сервер на `127.0.0.1:1080`.
- GitHub Actions для сборки debug/release APK и релиза по тегу `v*`.

## Скриншоты

> Добавьте реальные скриншоты после первого запуска:

- `docs/screenshots/dashboard.png`
- `docs/screenshots/services.png`
- `docs/screenshots/settings.png`
- `docs/screenshots/logs.png`

## Установка APK

1. Откройте [Releases](https://github.com/flowerswithoppo-cmyk/ZelenBo/releases).
2. Скачайте `app-release.apk` (или debug artifact из Actions).
3. Установите APK на Android 7.0+ (minSdk 24), разрешив установку из неизвестных источников.

## Сборка из исходников

### Требования

- JDK 17
- Android SDK (compile/target 34)
- Gradle 8.8

### Локально

```bash
./gradlew :app:assembleDebug
```

### Через GitHub Actions

Workflow: `.github/workflows/build.yml`

Для подписи release добавьте Secrets в репозиторий:

- `KEYSTORE_BASE64`
- `KEY_ALIAS`
- `KEY_PASSWORD`
- `STORE_PASSWORD`

## FAQ

### Почему в Actions run мог завершиться за 0 секунд?

Обычно это проблема с настройками Actions в репозитории или правами. Проверьте:

- Settings -> Actions -> разрешены workflow runs.
- Workflow можно запускать вручную через `workflow_dispatch`.

### Поддерживается ли пакетная DPI-эвейжн логика как zapret?

В этой версии реализован безопасный MVP (DNS/прокси оптимизация). Низкоуровневая пакетная подмена TLS/ClientHello не реализована.

## Лицензия

Проект распространяется под лицензией [MIT](LICENSE).

