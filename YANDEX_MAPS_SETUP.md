# Настройка Яндекс MapKit

## Получение API ключа

1. **Перейдите в [Кабинет разработчика Яндекс](https://developer.tech.yandex.ru/)**

2. **Войдите в аккаунт** или создайте новый

3. **Создайте новый проект**:
   - Нажмите "Подключить API"
   - Выберите "MapKit Mobile SDK"
   - Согласитесь с условиями использования

4. **Получите API ключ**:
   - После создания проекта вы получите API ключ
   - Скопируйте его

5. **Добавьте ключ в проект**:
   - Откройте файл `app/src/main/res/values/strings.xml`
   - Найдите строку:
   ```xml
   <string name="yandex_maps_key">YOUR_YANDEX_MAPS_API_KEY</string>
   ```
   - Замените `YOUR_YANDEX_MAPS_API_KEY` на ваш API ключ

## Особенности использования

### Инициализация

MapKit инициализируется в `MainActivity.onCreate()`:

```kotlin
val apiKey = getString(R.string.yandex_maps_key)
MapKitFactory.setApiKey(apiKey)
MapKitFactory.initialize(this)
```

### Жизненный цикл

MapKit требует управления жизненным циклом:

```kotlin
override fun onStart() {
    super.onStart()
    MapKitFactory.getInstance().onStart()
}

override fun onStop() {
    MapKitFactory.getInstance().onStop()
    super.onStop()
}
```

### Использование карты в Compose

Карта интегрируется через `AndroidView`:

```kotlin
AndroidView(
    factory = { YandexMapView(it) },
    update = { mapView ->
        // Обновление карты
    }
)
```

## Миграция с Google Maps

### Основные отличия

1. **Точки координат**:
   - Google: `LatLng(lat, lng)`
   - Яндекс: `Point(lat, lng)`

2. **Инициализация карты**:
   - Google: автоматическая через Google Services
   - Яндекс: требуется явная инициализация с API ключом

3. **Интеграция с Compose**:
   - Google: нативные Composable функции
   - Яндекс: через AndroidView

4. **Управление жизненным циклом**:
   - Google: автоматическое
   - Яндекс: требуется явный вызов onStart()/onStop()

### Что было изменено

1. ✅ Создан класс `Point` для замены Google `LatLng`
2. ✅ Обновлен `MapView.kt` для использования Яндекс MapKit
3. ✅ Обновлен `LocationUtils.kt` для работы с `Point`
4. ✅ Обновлены ViewModels и Screens
5. ✅ Добавлена инициализация MapKit в `MainActivity`
6. ✅ Удалены зависимости Google Maps

## Лимиты и квоты

- **Бесплатный тариф**: 
  - 25,000 запросов в день
  - Подходит для тестирования и небольших приложений

- **Платные тарифы**: 
  - Доступны при превышении лимитов
  - Подробнее на сайте Яндекс

## Полезные ссылки

- [Документация Яндекс MapKit](https://yandex.ru/dev/maps/mapkit/doc/ru/)
- [Примеры использования](https://github.com/yandex/mapkit-android-demo)
- [API Reference](https://yandex.ru/dev/maps/mapkit/doc/ru/android/generated/)
- [Кабинет разработчика](https://developer.tech.yandex.ru/)

## Часто задаваемые вопросы

### Q: Карта не отображается
A: Проверьте:
1. API ключ правильно добавлен в `strings.xml`
2. MapKit инициализирован в `onCreate()`
3. Вызваны методы `onStart()` и `onStop()`
4. Есть разрешения на местоположение

### Q: Ошибка "ApiKey is not set"
A: Убедитесь, что:
1. API ключ добавлен в `strings.xml`
2. `MapKitFactory.setApiKey()` вызван до `initialize()`

### Q: Приложение падает при повороте экрана
A: Убедитесь, что:
1. MapKit правильно обрабатывает жизненный цикл
2. Используется правильное управление состоянием в Compose

## Поддержка

При возникновении проблем:
1. Проверьте логи: `adb logcat | grep MapKit`
2. Убедитесь в валидности API ключа
3. Проверьте подключение к интернету


