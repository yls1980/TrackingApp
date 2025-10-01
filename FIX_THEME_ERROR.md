# 🎨 Исправление ошибки темы Material3

## ✅ Что было исправлено:

### 1. Упрощён `themes.xml`
Убрана зависимость от `Theme.Material3.DayNight`, которая вызывала ошибки. Теперь используется простая тема для Activity, а вся стилизация идёт через Jetpack Compose.

### 2. Обновлены зависимости Compose
- Compose BOM: `2023.10.01` → `2024.02.00`
- Material3: добавлена явная версия `1.2.0`

### 3. Создана тема для Dark Mode
Добавлен файл `values-night/themes.xml` для поддержки тёмной темы.

---

## 📋 Следующие шаги:

### Шаг 1: Инициализация Gradle Wrapper

**Вариант A - Через Android Studio (Рекомендуется):**

1. Откройте проект в Android Studio
2. **File → Sync Project with Gradle Files**
3. Дождитесь завершения синхронизации
4. **Build → Rebuild Project**

**Вариант B - Через скрипт:**

Запустите файл `init-gradle.bat` (двойной клик)

Это загрузит необходимый файл `gradle-wrapper.jar`

### Шаг 2: Очистка и сборка

После синхронизации в Android Studio:

**Build → Clean Project**
**Build → Rebuild Project**

Или в терминале:
```bash
.\gradlew.bat clean build
```

### Шаг 3: Запуск приложения

1. **Run → Edit Configurations**
2. Module: выберите `app`
3. **Apply → OK**
4. Нажмите **Run** (зелёная стрелка)

---

## 🔍 Что изменилось в файлах:

### `app/src/main/res/values/themes.xml`
**Было:**
```xml
<style name="Base.Theme.TrackingApp" parent="Theme.Material3.DayNight">
    <!-- Много атрибутов Material3 -->
</style>
```

**Стало:**
```xml
<style name="Theme.TrackingApp" parent="android:Theme.Material.Light.NoActionBar">
    <item name="android:statusBarColor">@color/md_theme_light_primary</item>
</style>
```

### `app/build.gradle.kts`
**Обновлены зависимости:**
```kotlin
implementation(platform("androidx.compose:compose-bom:2024.02.00"))
implementation("androidx.compose.material3:material3:1.2.0")
```

---

## ❓ Почему возникла ошибка?

Material3 темы в XML (`Theme.Material3.DayNight`) требуют:
- Специальных зависимостей Material Components
- Правильной конфигурации атрибутов

Поскольку приложение использует **Jetpack Compose**, XML темы нужны только для:
- Цвета статус-бара
- Базовой темы Activity

Вся остальная стилизация происходит в Compose через `MaterialTheme`.

---

## 🚀 Проверка работы

После сборки проверьте:

✅ Приложение запускается  
✅ Тема применяется корректно  
✅ Тёмная/светлая тема работает  
✅ Нет ошибок в логах  

```bash
# Проверка логов
adb logcat | grep -i "error\|exception"
```

---

## 🛠️ Если всё ещё не работает

### Проблема: gradle-wrapper.jar не найден

**Решение:**

1. Запустите `init-gradle.bat`
2. Или скачайте вручную:
   - URL: https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar
   - Сохраните в: `gradle/wrapper/gradle-wrapper.jar`

### Проблема: Sync failed в Android Studio

**Решение:**

1. **File → Invalidate Caches → Invalidate and Restart**
2. После перезапуска: **File → Sync Project with Gradle Files**

### Проблема: Ошибки компиляции Compose

**Решение:**

Убедитесь в версии Kotlin:
```kotlin
// build.gradle.kts
id("org.jetbrains.kotlin.android") version "1.9.20"
```

---

## 📚 Документация

- **Исправление Gradle:** `FIX_GRADLE_ERROR.md`
- **Настройка проекта:** `GRADLE_SETUP.md`
- **Яндекс Карты:** `QUICKSTART_YANDEX_MAPS.md`


