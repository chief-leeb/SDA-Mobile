# SDA Mobile

[![Android CI](https://github.com/chief-leeb/SDA-Mobile/actions/workflows/android.yml/badge.svg)](https://github.com/chief-leeb/SDA-Mobile/actions/workflows/android.yml)

**Русский** · [English](README.en.md)

Мобильный аналог Steam Desktop Authenticator для Android: коды Steam Guard, подтверждения обменов и маркета, добавление аутентификатора и перенос maFile. Интерфейс на русском и английском.

[Скачать APK](https://github.com/chief-leeb/SDA-Mobile/releases/latest) · [Сообщить об ошибке](https://github.com/chief-leeb/SDA-Mobile/issues)

## Статус

Версия 1.1.0. Сборка и lint проходят, на физическом устройстве приложение пока не тестировалось. Перед использованием с основным аккаунтом проверьте его на резервном аккаунте.

## Возможности

- Генерация кодов Steam Guard офлайн, с учётом смещения времени Steam.
- Подтверждения обменов и сделок маркета: список, принятие и отклонение вручную.
- Добавление мобильного аутентификатора на аккаунт: вход, Steam Guard, привязка телефона, SMS-активация.
- Импорт и экспорт maFile в формате SDA (незашифрованный JSON).
- Несколько аккаунтов, выбор активного, синхронизация времени.
- Отзыв Steam Guard по коду отзыва и удаление аккаунта с устройства.
- Переключение языка интерфейса в настройках: русский или английский. При первом запуске язык выбирается по локали устройства.

## Отношение к Steam Desktop Authenticator

Это независимая реализация для Android на Kotlin, а не форк [Jessecar96/SteamDesktopAuthenticator](https://github.com/Jessecar96/SteamDesktopAuthenticator). Общего кода и истории коммитов с C#-версией для десктопа нет. Формат maFile совместим, поэтому файлы можно переносить в обе стороны.

Идентификатор приложения: `io.github.chiefleeb.sdamobile`. Начиная с 1.1.0 package name изменён, поэтому новая версия устанавливается как отдельное приложение и не обновляет 1.0.0. Перед удалением старой версии экспортируйте maFile и импортируйте его в новую.

## Безопасность

- Хранилище `vault.bin` зашифровано AES-256-GCM ключом Android Keystore (alias `sda_mobile_vault_v1`).
- `android:allowBackup="false"`, окно приложения защищено `FLAG_SECURE`.
- Секреты и пароли не отправляются никуда, кроме официальных серверов Steam.
- Нет облачного бэкенда, аналитики и автоподтверждений.
- Пароль Steam используется только для входа и не сохраняется.
- Ключ подписи, `keystore.properties` и локальные артефакты исключены из Git через `.gitignore`.

## Сборка

Требования:

- JDK 17
- Android SDK: platform 35, build-tools 35.0.0
- Android SDK path в `local.properties` или переменной `ANDROID_HOME`

Debug APK:

```powershell
$env:JAVA_HOME = "C:\path\to\jdk-17"
$env:ANDROID_HOME = "C:\path\to\android-sdk"
.\gradlew.bat assembleDebug
```

Release APK:

1. Создайте `keystore.properties` по примеру `keystore.properties.example`.
2. Укажите путь к своему keystore, alias и пароли.
3. Соберите APK:

```powershell
.\gradlew.bat assembleRelease
```

Готовый файл: `app/build/outputs/apk/release/app-release.apk`.

`keystore.properties` и сам keystore не коммитятся. Для публикации используйте собственный ключ и не добавляйте его в репозиторий.

## Авторы

- **chief-leeb** - владелец проекта, постановка задачи и приёмка.
- **DeepSeek V4.1 Flash** (OpenCode) - архитектура, код, документация и релизная сборка.

## Ограничения

- Поддерживаются только незашифрованные maFile. Зашифрованные файлы SDA нужно сначала расшифровать в SDA.
- Автоподтверждение отсутствует: каждое действие подтверждается вручную.
- Приложение не связано с Valve. Steam и Steam Guard являются товарными знаками Valve Corporation.

## Лицензия

MIT. См. [LICENSE](LICENSE).
