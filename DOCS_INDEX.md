# 📑 Индекс документации XTrack

Навигация по всей документации проекта.

---

## 🚀 Быстрый старт

### Для новых пользователей
1. [README.md](README.md) - начните отсюда
2. [QUICKSTART_YANDEX_MAPS.md](QUICKSTART_YANDEX_MAPS.md) - быстрая настройка
3. [TESTING.md](TESTING.md) - проверка работы

### Для новых разработчиков
1. [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) - полное руководство
2. [ARCHITECTURE.md](ARCHITECTURE.md) - понимание архитектуры
3. [API_REFERENCE.md](API_REFERENCE.md) - справочник по API

---

## 📚 Основная документация

### 📖 Общая информация

| Документ | Описание | Целевая аудитория |
|----------|----------|-------------------|
| [README.md](README.md) | Общий обзор проекта, возможности, быстрый старт | Все |
| [DOCUMENTATION.md](DOCUMENTATION.md) | Полная документация проекта (150+ страниц) | Разработчики |

### 🏗️ Архитектура и дизайн

| Документ | Описание | Содержание |
|----------|----------|-----------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Архитектурная документация | • Clean Architecture + MVVM<br>• Потоки данных<br>• Database schema<br>• DI структура<br>• State management<br>• Performance |

### 💻 API и разработка

| Документ | Описание | Содержание |
|----------|----------|-----------|
| [API_REFERENCE.md](API_REFERENCE.md) | Справочник API | • Data models<br>• Repositories<br>• ViewModels<br>• Compose components<br>• Utilities<br>• Примеры кода |
| [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) | Руководство разработчика | • Настройка окружения<br>• Coding guidelines<br>• Работа с БД<br>• Добавление функций<br>• Отладка<br>• Best practices |

---

## 🔧 Настройка и конфигурация

### Настройка проекта

| Документ | Описание | Когда использовать |
|----------|----------|-------------------|
| [GRADLE_SETUP.md](GRADLE_SETUP.md) | Настройка Gradle | Первая сборка проекта |
| [YANDEX_MAPS_SETUP.md](YANDEX_MAPS_SETUP.md) | Настройка Yandex MapKit | Настройка карт |
| [QUICKSTART_YANDEX_MAPS.md](QUICKSTART_YANDEX_MAPS.md) | Быстрый старт с картами | Быстрая интеграция |

### Миграции

| Документ | Описание | Когда использовать |
|----------|----------|-------------------|
| [MIGRATION_TO_YANDEX_MAPS.md](MIGRATION_TO_YANDEX_MAPS.md) | Миграция на Yandex Maps | Переход с Google Maps |

---

## 🐛 Решение проблем

### Исправление ошибок

| Документ | Описание | Проблемы |
|----------|----------|----------|
| [QUICK_FIX.md](QUICK_FIX.md) | Быстрые решения | Общие проблемы |
| [FIX_GRADLE_ERROR.md](FIX_GRADLE_ERROR.md) | Исправление Gradle | Ошибки сборки |
| [FIX_THEME_ERROR.md](FIX_THEME_ERROR.md) | Исправление темы | Проблемы UI |
| [DEBUG_GUIDE.md](DEBUG_GUIDE.md) | Руководство по отладке | Debugging |

---

## 🧪 Тестирование

| Документ | Описание | Содержание |
|----------|----------|-----------|
| [TESTING.md](TESTING.md) | Общее тестирование | Проверка функционала |

## 🚀 Публикация и сборка

| Документ | Описание | Содержание |
|----------|----------|-----------|
| [BUILD_RELEASE.md](BUILD_RELEASE.md) | Сборка релизных версий | • Создание подписанного APK<br>• Создание AAB<br>• Настройка keystore<br>• Версионирование<br>• Устранение проблем |
| [GOOGLE_PLAY_PUBLISH.md](GOOGLE_PLAY_PUBLISH.md) | Публикация в Play Store | • Создание keystore<br>• Подписание APK/AAB<br>• Регистрация в Play Console<br>• Подготовка материалов<br>• Отправка на проверку<br>• После публикации |
| [PUBLISH_CHECKLIST.md](PUBLISH_CHECKLIST.md) | Чек-лист публикации | • Быстрые шаги<br>• Детальный чек-лист<br>• Шаблоны текстов<br>• Таймлайн<br>• Частые ошибки |
| [CHANGELOG.md](CHANGELOG.md) | История изменений | • Все версии<br>• Добавленные функции<br>• Исправленные ошибки<br>• Технические детали |

---

## 📊 Структура документации по темам

### 1. Архитектура и дизайн

```
ARCHITECTURE.md
├── Clean Architecture
├── MVVM Pattern
├── Dependency Injection
├── Data Flow
└── Component Hierarchy

DOCUMENTATION.md (раздел "Архитектура")
├── Общая архитектура
├── Слои приложения
└── Структура пакетов
```

### 2. Модели данных

```
API_REFERENCE.md
├── Track
├── TrackPoint
├── MapNote
├── AppSettings
└── BufferedTrackPoint

DOCUMENTATION.md (раздел "Структура базы данных")
├── Схема БД
├── Таблицы
├── Связи
└── Миграции
```

### 3. UI и Compose

```
API_REFERENCE.md (раздел "Compose Components")
├── MapView
├── StatisticsCard
└── TrackItem

ARCHITECTURE.md (раздел "UI Component Hierarchy")
├── Композиция экранов
├── State hoisting
└── Recomposition optimization

DEVELOPER_GUIDE.md (раздел "Compose Guidelines")
├── Композиция
├── State management
└── Производительность
```

### 4. Repositories и Data Layer

```
API_REFERENCE.md (раздел "Repositories")
├── TrackRepository
├── SettingsRepository
├── MapNoteRepository
└── BufferedTrackPointRepository

DEVELOPER_GUIDE.md (раздел "Работа с базой данных")
├── Создание Entity
├── Создание DAO
├── Создание Repository
└── Миграции
```

### 5. ViewModels и State

```
API_REFERENCE.md (раздел "ViewModels")
├── MainViewModel
├── TracksListViewModel
├── TrackDetailViewModel
└── SettingsViewModel

ARCHITECTURE.md (раздел "State Management")
├── StateFlow pattern
├── Events handling
└── State hoisting
```

### 6. Сервисы и фоновая работа

```
DOCUMENTATION.md (раздел "Сервисы")
├── LocationTrackingService
├── BufferedPointsSyncService
└── Жизненный цикл

ARCHITECTURE.md (раздел "Service Architecture")
├── Lifecycle
├── Location updates
└── Notifications
```

### 7. Навигация

```
DOCUMENTATION.md (раздел "Навигация")
├── Граф навигации
├── Маршруты
└── Навигационные действия

ARCHITECTURE.md (раздел "Navigation Architecture")
├── Navigation graph
└── Navigation flow
```

### 8. Парсеры и импорт/экспорт

```
API_REFERENCE.md (раздел "Parsers")
├── GpxParser / GpxGenerator
├── GeoJsonParser / GeoJsonGenerator
└── TrackImportParser

DOCUMENTATION.md (раздел "Работа с файлами")
├── FileProvider
├── Экспорт GPX/GeoJSON
└── Импорт треков
```

---

## 🎯 Сценарии использования документации

### Я новый разработчик, с чего начать?

1. [README.md](README.md) - общее понимание проекта
2. [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) - настройка окружения
3. [ARCHITECTURE.md](ARCHITECTURE.md) - понимание архитектуры
4. [API_REFERENCE.md](API_REFERENCE.md) - изучение API
5. Начните с простой задачи из Issues

### Мне нужно добавить новую функцию

1. [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) → "Добавление нового функционала"
2. [API_REFERENCE.md](API_REFERENCE.md) - изучите существующие паттерны
3. [ARCHITECTURE.md](ARCHITECTURE.md) - убедитесь, что понимаете архитектуру
4. Следуйте примерам в DEVELOPER_GUIDE.md

### У меня проблема при сборке

1. [QUICK_FIX.md](QUICK_FIX.md) - проверьте быстрые решения
2. [FIX_GRADLE_ERROR.md](FIX_GRADLE_ERROR.md) - если проблема с Gradle
3. [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) → "Troubleshooting"
4. Создайте Issue, если не помогло

### Мне нужно понять, как работает GPS-запись

1. [ARCHITECTURE.md](ARCHITECTURE.md) → "Service Architecture" → "LocationTrackingService"
2. [DOCUMENTATION.md](DOCUMENTATION.md) → "Сервисы и фоновая работа"
3. Изучите код `LocationTrackingService.kt`
4. [API_REFERENCE.md](API_REFERENCE.md) → "Services"

### Мне нужно работать с базой данных

1. [DOCUMENTATION.md](DOCUMENTATION.md) → "Структура базы данных"
2. [ARCHITECTURE.md](ARCHITECTURE.md) → "Database Architecture"
3. [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) → "Работа с базой данных"
4. [API_REFERENCE.md](API_REFERENCE.md) → "Data Models"

### Мне нужно создать новый экран

1. [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) → "Типичные задачи" → "Добавление нового экрана"
2. [API_REFERENCE.md](API_REFERENCE.md) → "Compose Components"
3. [ARCHITECTURE.md](ARCHITECTURE.md) → "UI Component Hierarchy"
4. Изучите существующие экраны

### Мне нужно оптимизировать производительность

1. [ARCHITECTURE.md](ARCHITECTURE.md) → "Performance Optimizations"
2. [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) → "Best Practices"
3. [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) → "Compose Guidelines" → "Производительность"
4. Используйте Android Studio Profilers

---

## 🔍 Поиск информации

### По технологии

| Технология | Где искать |
|-----------|-----------|
| **Kotlin** | DEVELOPER_GUIDE.md → "Kotlin Style Guide" |
| **Compose** | DEVELOPER_GUIDE.md → "Compose Guidelines"<br>ARCHITECTURE.md → "UI Component Hierarchy" |
| **Room** | DOCUMENTATION.md → "Структура базы данных"<br>DEVELOPER_GUIDE.md → "Работа с базой данных" |
| **Hilt** | ARCHITECTURE.md → "Dependency Injection"<br>DOCUMENTATION.md → "DI модули" |
| **Coroutines** | DEVELOPER_GUIDE.md → "Kotlin Style Guide" → "Coroutines"<br>ARCHITECTURE.md → "State Management" |
| **Navigation** | DOCUMENTATION.md → "Навигация"<br>ARCHITECTURE.md → "Navigation Architecture" |
| **Yandex Maps** | YANDEX_MAPS_SETUP.md<br>DOCUMENTATION.md → "API и интеграции" |

### По компоненту

| Компонент | Где искать |
|-----------|-----------|
| **ViewModel** | API_REFERENCE.md → "ViewModels"<br>ARCHITECTURE.md → "State Management" |
| **Repository** | API_REFERENCE.md → "Repositories"<br>DEVELOPER_GUIDE.md → "Создание Repository" |
| **Service** | DOCUMENTATION.md → "Сервисы"<br>ARCHITECTURE.md → "Service Architecture" |
| **DAO** | API_REFERENCE.md → "Data Models"<br>DEVELOPER_GUIDE.md → "Создание новой Entity" |
| **Parser** | API_REFERENCE.md → "Parsers"<br>DEVELOPER_GUIDE.md → "Добавление нового парсера" |

---

## 📈 Уровни документации

### 🟢 Начинающий уровень

Начните с этих документов:
1. [README.md](README.md)
2. [QUICKSTART_YANDEX_MAPS.md](QUICKSTART_YANDEX_MAPS.md)
3. [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) (первые разделы)

### 🟡 Средний уровень

После освоения основ:
1. [ARCHITECTURE.md](ARCHITECTURE.md)
2. [API_REFERENCE.md](API_REFERENCE.md)
3. [DOCUMENTATION.md](DOCUMENTATION.md) (нужные разделы)

### 🔴 Продвинутый уровень

Для глубокого понимания:
1. [ARCHITECTURE.md](ARCHITECTURE.md) (все разделы)
2. [DOCUMENTATION.md](DOCUMENTATION.md) (полностью)
3. Исходный код с комментариями

---

## 📝 Обновление документации

### Когда обновлять

- ✅ Добавлена новая функция → обновите DOCUMENTATION.md и API_REFERENCE.md
- ✅ Изменена архитектура → обновите ARCHITECTURE.md
- ✅ Добавлен новый компонент → обновите API_REFERENCE.md
- ✅ Исправлена частая проблема → добавьте в QUICK_FIX.md
- ✅ Изменены coding guidelines → обновите DEVELOPER_GUIDE.md

### Как обновлять

1. Найдите соответствующий файл из этого индекса
2. Обновите раздел с новой информацией
3. Проверьте ссылки на другие документы
4. Обновите версию и дату в конце документа
5. Добавьте запись в CHANGELOG.md (если есть)

---

## 🔗 Полезные ссылки

### Внешние ресурсы

- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- [Yandex MapKit](https://yandex.ru/dev/maps/mapkit/doc/)

### Внутренние ресурсы

- [Исходный код](app/src/main/java/com/xtrack/)
- [Тесты](app/src/test/java/com/xtrack/)
- [Ресурсы](app/src/main/res/)

---

## 📞 Контакты и поддержка

### Вопросы по документации

- Создайте Issue с меткой `documentation`
- Опишите, что именно непонятно
- Предложите улучшения

### Вклад в документацию

- Fork репозитория
- Внесите изменения
- Создайте Pull Request
- Опишите, что и зачем изменили

---

**Версия индекса:** 1.1  
**Последнее обновление:** 19 октября 2024

**Документация поддерживается командой XTrack**

