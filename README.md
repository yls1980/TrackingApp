# 🗺️ TrackingApp - GPS Tracking Application

Android приложение для отслеживания GPS-треков с использованием Yandex MapKit.

## 📱 Возможности

- ✅ Запись GPS-треков в реальном времени
- ✅ Отображение маршрута на карте (Yandex Maps)
- ✅ Экспорт треков в GPX и GeoJSON
- ✅ Статистика: расстояние, скорость, время, набор высоты
- ✅ Фоновая запись с уведомлением
- ✅ Настройки точности и интервала GPS
- ✅ Логирование ошибок для диагностики
- ✅ База данных Room для хранения треков

## 🛠️ Технологии

- **Kotlin** - основной язык разработки
- **Jetpack Compose** - современный UI фреймворк
- **Yandex MapKit** - карты и геолокация
- **Room Database** - локальное хранилище
- **Hilt** - dependency injection
- **Coroutines & Flow** - асинхронность
- **WorkManager** - фоновые задачи
- **Material Design 3** - дизайн система

## 🚀 Сборка и запуск

### Требования

- Android Studio Hedgehog | 2023.1.1 или новее
- JDK 17 или новее
- Android SDK 34
- Gradle 8.13

### Настройка

1. Клонируйте репозиторий:
```bash
git clone https://github.com/ваш-username/TrackingApp.git
cd TrackingApp
```

2. Получите API ключ Yandex MapKit:
   - Зарегистрируйтесь на https://developer.tech.yandex.ru/
   - Создайте MapKit API ключ
   - Добавьте ключ в `app/src/main/res/values/strings.xml`:
```xml
<string name="yandex_maps_key">ВАШ_API_КЛЮЧ</string>
```

3. Соберите проект:
```bash
./gradlew assembleDebug
```

4. Установите на устройство:
```bash
./gradlew installDebug
```

## 📂 Структура проекта

```
app/
├── src/main/java/com/trackingapp/
│   ├── data/           # Модели данных, база данных, репозитории
│   ├── di/             # Dependency Injection модули
│   ├── presentation/   # UI (экраны, компоненты, ViewModel)
│   ├── service/        # Фоновый сервис отслеживания
│   └── utils/          # Утилиты и логирование
└── src/main/res/       # Ресурсы (layouts, drawables, strings)
```

## 🔧 Решение проблем

### Ошибка KAPT с Java 17+

Проект уже настроен для работы с Java 17+. Если возникают проблемы с KAPT:
- Проверьте `gradle.properties` - должны быть настроены JVM аргументы
- Остановите Gradle daemon: `./gradlew --stop`
- Очистите проект: `./gradlew clean`

### Просмотр логов ошибок

Приложение автоматически записывает все ошибки в файл:
1. Откройте Настройки → Диагностика → Просмотр логов ошибок
2. Нажмите "Поделиться" для отправки разработчику

## 📝 Лицензия

MIT License - смотрите [LICENSE](LICENSE) файл

## 👨‍💻 Автор

Разработано с использованием современных Android практик и best practices.

## 🤝 Вклад

Pull requests приветствуются! Для больших изменений сначала откройте issue для обсуждения.

---

Made with ❤️ using Kotlin & Jetpack Compose
