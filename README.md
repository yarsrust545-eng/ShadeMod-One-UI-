# ShadeMod One UI (APK-модуль для KernelSU + LSPosed)

Модуль для кастомизации шторки Samsung One UI и добавления плавных пружинных анимаций ползунков (в стиле iOS / HyperOS / Pixel).

## Как собрать готовый `.apk`

### Вариант 1: Через GitHub Actions (без Android Studio)
1. Создайте репозиторий на GitHub и загрузите туда содержимое этого архива.
2. Откройте вкладку **Actions** -> **Build LSPosed APK** -> **Run workflow**.
3. После завершения сборки скачайте готовый `app-debug.apk` из раздела **Artifacts**.

### Вариант 2: В Android Studio
1. Распакуйте архив и откройте папку в **Android Studio**.
2. Дождитесь синхронизации Gradle и нажмите **Build -> Build Bundle(s) / APK(s) -> Build APK(s)**.

## Установка на Samsung с KernelSU
1. Установите в KernelSU Manager модуль **ZygiskNext** (или **ReZygisk**) и **LSPosed** (Zygisk).
2. Установите собранный `app-debug.apk`.
3. Откройте **LSPosed Manager -> Модули -> ShadeMod One UI**, включите модуль и отметьте в области действия **System UI** (`com.android.systemui`).
4. Откройте приложение **ShadeMod One UI**, выберите стиль анимации ползунка и настройки шторки, затем нажмите **Перезапустить SystemUI (KernelSU)**.
