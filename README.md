# 🗺️ XTrack - GPS Tracking Application

Современное Android приложение для отслеживания GPS-треков с расширенными возможностями создания заметок, экспорта данных и детальной аналитики маршрутов.

## 📱 Возможности

### Основной функционал
- ✅ **Запись GPS-треков** в реальном времени с высокой точностью
- ✅ **Отображение маршрута** на карте (Yandex Maps)
- ✅ **Фоновая запись** с постоянным уведомлением
- ✅ **Детальная статистика**: расстояние, скорость, время, набор высоты
- ✅ **Экспорт треков** в GPX и GeoJSON

### Заметки на карте
- 📝 **Текстовые заметки** с координатами
- 📷 **Фото-заметки** (камера или галерея)
- 🎥 **Видео-заметки** с привязкой к маршруту
- 🗺️ **Просмотр заметок** на карте с кластеризацией

### Продвинутые функции
- 🔄 **Импорт/Экспорт** треков (GPX, GeoJSON)
- ⏸️ **Автоматическая пауза** при остановке
- 📊 **Графики** высоты и скорости
- 🔔 **Уведомления** о пройденном расстоянии
- 💾 **Буферизация точек** при отсутствии сети
- 🐛 **Система логирования** для диагностики

## 🛠️ Технологии

| Категория | Технология | Версия |
|-----------|-----------|---------|
| **Язык** | Kotlin | 1.9.24 |
| **UI** | Jetpack Compose | 2024.02.00 |
| **Архитектура** | MVVM + Clean Architecture | - |
| **DI** | Hilt/Dagger | 2.48 |
| **Database** | Room | 2.6.1 |
| **Maps** | Yandex MapKit | 4.5.1-full |
| **Location** | Google Play Services | 21.0.1 |
| **Async** | Coroutines + Flow | 1.7.3 |
| **Navigation** | Navigation Compose | 2.7.5 |
| **Background** | WorkManager | 2.9.0 |

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

## 📚 Документация

Полная документация проекта доступна в следующих файлах:

### Для пользователей
- **[README.md](README.md)** - общий обзор и быстрый старт
- **[QUICK_FIX.md](QUICK_FIX.md)** - быстрое решение проблем
- **[TESTING.md](TESTING.md)** - инструкции по тестированию

### Для разработчиков
- **[DOCUMENTATION.md](DOCUMENTATION.md)** - полная документация проекта
  - Архитектура приложения
  - Модули и компоненты
  - Функциональные возможности
  - Структура базы данных
  - Навигация
  - Сервисы и фоновая работа
  
- **[API_REFERENCE.md](API_REFERENCE.md)** - справочник по API
  - Модели данных
  - Repositories
  - ViewModels
  - Compose компоненты
  - Утилиты
  - Примеры использования
  
- **[ARCHITECTURE.md](ARCHITECTURE.md)** - архитектурная документация
  - Clean Architecture + MVVM
  - Потоки данных
  - Database architecture
  - Dependency Injection
  - State management
  - UI component hierarchy
  - Performance optimizations
  
- **[DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md)** - руководство для разработчиков
  - Настройка окружения
  - Coding guidelines
  - Работа с базой данных
  - Добавление нового функционала
  - Отладка
  - Best practices
  - Troubleshooting

### Миграции и настройка
- **[MIGRATION_TO_YANDEX_MAPS.md](MIGRATION_TO_YANDEX_MAPS.md)** - миграция на Yandex Maps
- **[YANDEX_MAPS_SETUP.md](YANDEX_MAPS_SETUP.md)** - настройка Yandex MapKit
- **[GRADLE_SETUP.md](GRADLE_SETUP.md)** - настройка Gradle
- **[FIX_GRADLE_ERROR.md](FIX_GRADLE_ERROR.md)** - исправление ошибок Gradle
- **[FIX_THEME_ERROR.md](FIX_THEME_ERROR.md)** - исправление ошибок темы

## 🏗️ Архитектура

Приложение построено на Clean Architecture с использованием MVVM паттерна:

```
┌─────────────────────────────────────────────────┐
│           Presentation Layer                    │
│  (Screens, ViewModels, Components)             │
└─────────────────┬───────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────┐
│            Domain Layer                         │
│         (Repositories, Use Cases)               │
└─────────────────┬───────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────┐
│             Data Layer                          │
│  (Database, Parsers, Services)                  │
└─────────────────────────────────────────────────┘
```

Подробнее см. [ARCHITECTURE.md](ARCHITECTURE.md)

## 🤝 Вклад

Pull requests приветствуются! Для больших изменений сначала откройте issue для обсуждения.

### Процесс разработки

1. Прочитайте [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md)
2. Создайте feature branch
3. Следуйте coding guidelines
4. Добавьте тесты
5. Обновите документацию
6. Создайте Pull Request

## 📄 Лицензия

MIT License - смотрите [LICENSE](LICENSE) файл

---

Made with ❤️ using Kotlin & Jetpack Compose

**Версия:** 1.0  
**Последнее обновление:** 10 октября 2024
