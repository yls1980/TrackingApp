# 🐛 Руководство по отладке на Android устройстве

## ⚡ Быстрый старт

### 1. Подключение телефона

1. **Включите режим разработчика:**
   - Настройки → О телефоне → Нажмите 7 раз на "Номер сборки"

2. **Включите отладку по USB:**
   - Настройки → Для разработчиков → Отладка по USB (ON)

3. **Подключите USB кабель**
   - Выберите режим "Передача файлов"
   - Разрешите отладку на телефоне

### 2. Запуск в Android Studio

1. Откройте проект
2. Дождитесь синхронизации Gradle
3. Выберите устройство вверху (рядом с кнопкой Run)
4. Нажмите **▶ Run** (Shift+F10) или зеленую кнопку

### 3. Установка APK напрямую

Если Android Studio не видит устройство:

```bash
# Соберите APK
.\gradlew assembleDebug

# Найдите APK
app\build\outputs\apk\debug\app-debug.apk

# Скопируйте на телефон и установите вручную
```

## 🔧 Решение проблем

### Устройство не определяется

**Windows:**

1. Установите USB драйверы:
   - Откройте Android Studio
   - Tools → SDK Manager → SDK Tools
   - Поставьте галочку "Google USB Driver"
   - Install

2. Проверьте через командную строку:
```bash
.\gradlew installDebug
```

3. Проверьте ADB:
```bash
# В PowerShell в папке проекта
cd %LOCALAPPDATA%\Android\Sdk\platform-tools
.\adb.exe devices
```

Должно показать:
```
List of devices attached
XXXXXXXXXXX     device
```

### Ошибка "Unauthorized"

На телефоне нажмите "Разрешить отладку по USB" и поставьте галочку "Всегда разрешать"

### Ошибка "Offline"

```bash
.\adb.exe kill-server
.\adb.exe start-server
.\adb.exe devices
```

### Несколько устройств подключено

```bash
# Посмотрите список
.\adb.exe devices

# Установите на конкретное устройство
.\adb.exe -s DEVICE_ID install app\build\outputs\apk\debug\app-debug.apk
```

## 📊 Просмотр логов

### В Android Studio:

1. Внизу нажмите вкладку **Logcat**
2. Выберите ваше устройство и процесс `com.trackingapp`
3. Фильтры:
   - `tag:ErrorLogger` - наши логи ошибок
   - `tag:TrackingService` - логи сервиса GPS
   - `level:error` - только ошибки

### Через командную строку:

```bash
# Все логи
.\adb.exe logcat

# Только наше приложение
.\adb.exe logcat | findstr "com.trackingapp"

# Только ошибки
.\adb.exe logcat *:E

# Сохранить в файл
.\adb.exe logcat > logs.txt
```

## 🎯 Полезные команды ADB

```bash
# Установить APK
.\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk

# Удалить приложение
.\adb.exe uninstall com.trackingapp

# Очистить данные приложения
.\adb.exe shell pm clear com.trackingapp

# Посмотреть установленные приложения
.\adb.exe shell pm list packages | findstr trackingapp

# Скопировать файл с телефона (логи)
.\adb.exe pull /sdcard/Android/data/com.trackingapp/files/logs/app_errors.log ./

# Открыть shell на телефоне
.\adb.exe shell

# Перезагрузить устройство
.\adb.exe reboot
```

## 🔍 Отладка в реальном времени

### Breakpoints (точки останова):

1. Откройте файл Kotlin в Android Studio
2. Кликните слева от номера строки - появится красная точка
3. Запустите приложение через **🐛 Debug** (Shift+F9)
4. Когда выполнение дойдет до breakpoint, приложение остановится
5. Можно смотреть значения переменных, выполнять код пошагово

### Полезные горячие клавиши:

- `F8` - Step Over (следующая строка)
- `F7` - Step Into (войти в функцию)
- `Shift+F8` - Step Out (выйти из функции)
- `F9` - Resume (продолжить выполнение)
- `Ctrl+F8` - Toggle Breakpoint

## 📱 Тестирование GPS

**Важно!** Для работы GPS нужно:

1. ✅ Разрешить доступ к местоположению в настройках приложения
2. ✅ Включить GPS на телефоне
3. ✅ Быть на открытой местности (GPS не работает в помещении)

### Симуляция GPS в Android Studio:

1. Запустите приложение
2. Внизу нажмите вкладку **Emulator/Device**
3. Три точки `...` → **Location** 
4. Выберите точку на карте или введите координаты
5. Нажмите **Set Location**

## 🎓 Дополнительные советы

### Горячие клавиши Android Studio:

- `Shift+F10` - Запустить приложение (Run)
- `Shift+F9` - Запустить с отладкой (Debug)
- `Ctrl+F9` - Собрать проект (Build)
- `Ctrl+Shift+F10` - Запустить текущий файл/тест

### Очистка и пересборка:

```bash
# Очистить всё
.\gradlew clean

# Собрать заново
.\gradlew assembleDebug

# Или в одну команду
.\gradlew clean assembleDebug
```

### Проверка производительности:

1. Android Studio → View → Tool Windows → Profiler
2. Можно мониторить:
   - CPU usage
   - Memory usage
   - Network activity
   - Battery consumption

## 🆘 Если ничего не помогло

1. **Перезагрузите всё:**
   - Закройте Android Studio
   - Отключите телефон
   - Перезагрузите компьютер
   - Подключите заново

2. **Проверьте USB кабель:**
   - Некоторые кабели только для зарядки
   - Попробуйте другой кабель

3. **Установите APK вручную:**
   - Соберите: `.\gradlew assembleDebug`
   - Скопируйте `app-debug.apk` на телефон
   - Установите вручную (разрешите установку из неизвестных источников)

4. **Используйте беспроводную отладку** (Android 11+):
   - Настройки → Для разработчиков → Беспроводная отладка
   - В Android Studio: Pair Device Using WiFi

