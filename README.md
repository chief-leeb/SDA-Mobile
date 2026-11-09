# SDA Mobile

Мобильный аналог Steam Desktop Authenticator для Android: коды Steam Guard, подтверждения обменов и маркета, добавление аутентификатора и перенос maFile.

[Скачать APK](https://github.com/chief-leeb/SDA-Mobile/releases/latest) · [Сообщить об ошибке](https://github.com/chief-leeb/SDA-Mobile/issues)

## Возможности

- Генерация кодов Steam Guard офлайн, с учётом смещения времени Steam.
- Подтверждения обменов и сделок маркета: список, принятие и отклонение вручную.
- Добавление мобильного аутентификатора на аккаунт: вход, Steam Guard, привязка телефона, SMS-активация.
- Импорт и экспорт maFile в формате SDA (незашифрованный JSON).
- Несколько аккаунтов, выбор активного, синхронизация времени.
- Отзыв Steam Guard по коду отзыва и удаление аккаунта с устройства.

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
