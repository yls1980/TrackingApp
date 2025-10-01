# 🔧 Быстрое исправление ошибки Gradle

## Ошибка: "Failed to notify project evaluation listener"

### ✅ Решение (5 минут)

#### Шаг 1: Закройте Android Studio

#### Шаг 2: Откройте терминал в папке проекта TrackingApp

**Windows PowerShell или CMD:**
```bash
cd D:\!yarik\Project\TrackingApp
```

#### Шаг 3: Выполните команды:

```bash
# Обновление Gradle Wrapper
.\gradlew.bat wrapper --gradle-version=8.2

# Очистка проекта
.\gradlew.bat clean

# Проверка конфигурации
.\gradlew.bat tasks
```

#### Шаг 4: Откройте Android Studio

1. **File → Open** → выберите папку `TrackingApp`
2. **File → Sync Project with Gradle Files**
3. Дождитесь завершения синхронизации (может занять 2-5 минут)

#### Шаг 5: Настройте Run Configuration

1. **Run → Edit Configurations**
2. В поле **Module** выберите: `TrackingApp.app` или `app`
3. Нажмите **Apply** → **OK**

### 🎯 Готово!

Теперь можно запускать приложение кнопкой **Run** (зелёная стрелка) или **Shift + F10**

---

## Альтернативное решение

Если первое решение не помогло:

### Вариант А: Полная очистка кеша

```bash
# Закройте Android Studio

# Удалите папки (в корне проекта):
rmdir /s /q .gradle
rmdir /s /q build
rmdir /s /q app\build

# Откройте Android Studio
# File → Invalidate Caches → Invalidate and Restart
# File → Sync Project with Gradle Files
```

### Вариант Б: Проверка JDK

Android Studio должна использовать JDK 17:

1. **File → Settings**
2. **Build, Execution, Deployment → Build Tools → Gradle**
3. **Gradle JDK**: выберите **JDK 17** или **Embedded JDK**
4. **Apply → OK**
5. **File → Sync Project with Gradle Files**

### Вариант В: Ручная установка Gradle

Если `gradlew.bat` не работает:

1. Скачайте [Gradle 8.2](https://gradle.org/releases/)
2. Распакуйте в `C:\Gradle\gradle-8.2`
3. Добавьте в PATH: `C:\Gradle\gradle-8.2\bin`
4. Перезапустите терминал
5. Выполните: `gradle wrapper`

---

## Проверка успешной настройки

После исправления выполните:

```bash
# Проверка версии Gradle
.\gradlew.bat --version

# Должно показать:
# Gradle 8.2
```

```bash
# Сборка проекта
.\gradlew.bat build

# Если успешно - всё работает!
```

---

## Если всё ещё не работает

### Проверьте файлы проекта:

✅ Должны существовать:
- `gradle/wrapper/gradle-wrapper.properties` ← **Создан**
- `gradlew.bat` ← **Создан**
- `build.gradle.kts` ← **Существует**
- `settings.gradle.kts` ← **Существует**
- `gradle.properties` ← **Существует**

### Содержимое gradle-wrapper.properties:

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.2-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

---

## Частые ошибки и решения

### ❌ "gradlew.bat is not recognized"

**Решение:** Используйте полный путь:
```bash
D:\!yarik\Project\TrackingApp\gradlew.bat wrapper --gradle-version=8.2
```

### ❌ "Module not specified" 

**Решение:**
1. Run → Edit Configurations
2. Module: выберите `app`
3. Apply → OK

### ❌ "Could not download gradle-8.2-bin.zip"

**Решение:**
1. Проверьте интернет-соединение
2. Попробуйте использовать VPN
3. Или скачайте вручную и положите в:
   `%USERPROFILE%\.gradle\wrapper\dists\gradle-8.2-bin\`

---

## Контакты для помощи

Если проблема не решена, создайте Issue с:
- Полным текстом ошибки
- Версией Android Studio
- Версией JDK
- Скриншотом ошибки

## Дополнительно

📖 **Полная документация:** `GRADLE_SETUP.md`


