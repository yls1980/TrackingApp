# Сборка Release версии XTrack

## 📦 Создание подписанного APK

### Подготовка

1. **Убедитесь, что файл `keystore.properties` настроен:**
   ```properties
   storeFile=D:\\!yarik\\AndroidApp\\xtrack
   storePassword=ваш_пароль_keystore
   keyAlias=key0
   keyPassword=ваш_пароль_ключа
   ```

2. **Файл `keystore.properties` НЕ должен попадать в Git** (уже добавлен в `.gitignore`)

### Сборка APK для распространения среди друзей

```bash
./gradlew assembleRelease
```

**Готовый APK будет здесь:**
```
app/build/outputs/apk/release/app-release.apk
```

### Сборка AAB для Google Play Store

```bash
./gradlew bundleRelease
```

**Готовый AAB будет здесь:**
```
app/build/outputs/bundle/release/app-release.aab
```

## 🔧 Настройка подписи (первый раз)

### Если у вас уже есть keystore файл:

1. Создайте файл `keystore.properties` в корне проекта
2. Укажите путь к вашему keystore
3. Узнайте алиас ключа:
   ```bash
   keytool -list -keystore путь/к/вашему/keystore
   ```
4. Укажите правильный алиас в `keystore.properties`

### Если нужно создать новый keystore:

#### Через Android Studio (рекомендуется):
1. **Build → Generate Signed Bundle / APK**
2. **Выберите "Android App Bundle"**
3. **Нажмите "Create new..."**
4. **Заполните форму:**
   - Key store path: `D:\!yarik\AndroidApp\xtrack-new.jks`
   - Password: [надежный пароль]
   - Alias: `xtrack-key`
   - Validity: 25 лет

#### Через командную строку:
```bash
keytool -genkey -v -keystore xtrack-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias xtrack-key
```

## ⚠️ Важно: Сохраните keystore!

**Создайте резервные копии:**
1. Google Drive / OneDrive
2. USB флешка
3. Внешний жёсткий диск

**Сохраните пароли в безопасном месте!**
Если потеряете keystore, не сможете обновить приложение в Google Play Store!

## 🚀 Версионирование

Перед каждой новой сборкой обновите версию в `app/build.gradle.kts`:

```kotlin
defaultConfig {
    versionCode = 2  // Увеличьте на 1
    versionName = "1.1"  // Обновите версию
}
```

## 🐛 Устранение проблем

### Ошибка: "Unable to delete directory"
```bash
./gradlew --stop
Remove-Item -Path "app\build" -Recurse -Force
```

### Ошибка: "Missing classes detected while running R8"
Отключите обфускацию в `app/build.gradle.kts`:
```kotlin
buildTypes {
    release {
        isMinifyEnabled = false
        isShrinkResources = false
    }
}
```

### Ошибка: "No key with alias 'xxx' found"
Проверьте алиас:
```bash
keytool -list -keystore путь/к/keystore
```
Обновите `keyAlias` в `keystore.properties`

## 📱 Тестирование APK

### Установка на устройство через ADB:
```bash
adb install app/build/outputs/apk/release/app-release.apk
```

### Отправка друзьям:
- Telegram / WhatsApp
- Google Drive
- Email

**Размер APK:** ~55 MB  
**Требования:** Android 7.0+ (API 24+)

## 📊 Проверка размера APK

```bash
# Подробная информация о APK
./gradlew :app:assembleRelease --scan

# Анализ размера в Android Studio:
# Build → Analyze APK
```

## 🎯 Чек-лист перед релизом

- [ ] Обновлен `versionCode` и `versionName`
- [ ] Протестирована сборка на реальном устройстве
- [ ] Проверены все основные функции:
  - [ ] Запись трека
  - [ ] Остановка трека
  - [ ] Просмотр треков
  - [ ] Добавление заметок
  - [ ] Экспорт GPX/GeoJSON
  - [ ] Импорт треков
- [ ] Создана резервная копия keystore
- [ ] Обновлена документация (если нужно)

