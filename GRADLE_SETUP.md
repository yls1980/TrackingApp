# Настройка Gradle для TrackingApp

## Исправление ошибок Gradle

### Проблема: "Failed to notify project evaluation listener"

Это происходит когда отсутствуют файлы Gradle Wrapper или есть несовместимость версий.

### Решение:

#### 1. Обновите Gradle Wrapper (через Android Studio):

**В терминале Android Studio:**

```bash
# Windows
gradlew.bat wrapper --gradle-version=8.2 --distribution-type=bin

# Если не работает, используйте полный путь:
.\gradlew.bat wrapper --gradle-version=8.2 --distribution-type=bin
```

#### 2. Синхронизируйте проект:

1. В Android Studio: **File → Sync Project with Gradle Files**
2. Дождитесь завершения синхронизации
3. Если появляются ошибки, читайте дальше

#### 3. Очистите и пересоберите:

```bash
.\gradlew.bat clean
.\gradlew.bat build
```

### Версии в проекте:

- **Gradle**: 8.2
- **Android Gradle Plugin**: 8.2.0
- **Kotlin**: 1.9.20
- **Java**: 1.8 (Java 8)

### Структура файлов Gradle:

```
TrackingApp/
├── build.gradle.kts           # Основной build файл
├── settings.gradle.kts         # Настройки проекта
├── gradle.properties           # Свойства Gradle
├── gradlew.bat                 # Wrapper для Windows
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties  # Конфигурация Wrapper
└── app/
    └── build.gradle.kts        # Build файл модуля app
```

## Частые проблемы и решения

### Проблема 1: Module not specified

**Решение:**
1. File → Sync Project with Gradle Files
2. Run → Edit Configurations
3. Module: выберите `app` или `TrackingApp.app`
4. Apply → OK

### Проблема 2: Gradle sync failed

**Решение:**
```bash
# Очистите кеш Gradle
.\gradlew.bat clean --refresh-dependencies

# Или полностью удалите кеш:
# Закройте Android Studio
# Удалите папки:
# - .gradle в корне проекта
# - %USERPROFILE%\.gradle\caches (Windows)
# Откройте Android Studio и синхронизируйте
```

### Проблема 3: Could not resolve dependencies

**Решение:**
Проверьте интернет-соединение и репозитории в `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
```

### Проблема 4: Unsupported class file version

**Решение:**
Обновите JDK до версии 17 или выше:
1. File → Project Structure → SDK Location
2. Gradle JDK: выберите JDK 17
3. Apply → OK

## Проверка установки

```bash
# Проверить версию Gradle
.\gradlew.bat --version

# Список всех tasks
.\gradlew.bat tasks

# Проверка зависимостей
.\gradlew.bat app:dependencies
```

## Команды для разработки

```bash
# Очистка проекта
.\gradlew.bat clean

# Сборка Debug APK
.\gradlew.bat assembleDebug

# Сборка Release APK
.\gradlew.bat assembleRelease

# Установка на устройство
.\gradlew.bat installDebug

# Запуск тестов
.\gradlew.bat test

# Проверка кода (lint)
.\gradlew.bat lint

# Все вместе
.\gradlew.bat clean build installDebug
```

## Настройка Android Studio

### 1. Gradle JDK

**File → Settings → Build, Execution, Deployment → Build Tools → Gradle**
- Gradle JDK: **JDK 17** или выше
- Use Gradle from: **'gradle-wrapper.properties' file**

### 2. Kotlin Compiler

**File → Settings → Build, Execution, Deployment → Compiler → Kotlin Compiler**
- Target JVM version: **1.8**

### 3. Android SDK

**File → Settings → Appearance & Behavior → System Settings → Android SDK**
- Android SDK Location: проверьте путь
- SDK Platforms: установите **Android 14.0 (API 34)**
- SDK Tools: установите **Android SDK Build-Tools 34**

## Ускорение сборки

Добавьте в `gradle.properties`:

```properties
# Используйте больше памяти
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8

# Включите кеширование
org.gradle.caching=true

# Параллельная сборка
org.gradle.parallel=true

# Инкрементальная компиляция Kotlin
kotlin.incremental=true
```

## Troubleshooting в Android Studio

### Инвалидация кеша:

**File → Invalidate Caches → Invalidate and Restart**

### Переустановка Gradle:

1. Закройте Android Studio
2. Удалите папку `.gradle` в корне проекта
3. Удалите `app/build` и `build`
4. Откройте проект снова
5. File → Sync Project with Gradle Files

## Полезные ссылки

- [Gradle Docs](https://docs.gradle.org/)
- [Android Gradle Plugin](https://developer.android.com/studio/releases/gradle-plugin)
- [Kotlin Gradle Plugin](https://kotlinlang.org/docs/gradle.html)


