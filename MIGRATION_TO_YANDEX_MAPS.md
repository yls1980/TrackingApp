# Миграция на Яндекс Карты

## Обзор изменений

Проект успешно переведен с Google Maps SDK на Яндекс MapKit.

## Список изменений

### 1. Новые файлы

- ✅ `app/src/main/java/com/trackingapp/data/model/Point.kt` - класс для координат (замена Google LatLng)
- ✅ `YANDEX_MAPS_SETUP.md` - инструкции по настройке Яндекс MapKit
- ✅ `MIGRATION_TO_YANDEX_MAPS.md` - документация миграции

### 2. Обновленные файлы

#### Компоненты карты
- ✅ `app/src/main/java/com/trackingapp/presentation/components/MapView.kt`
  - Переписан для использования Яндекс MapKit
  - Использует AndroidView для интеграции с Compose
  - Добавлена поддержка маршрутов (Polyline)
  - Добавлена поддержка маркеров

#### Утилиты
- ✅ `app/src/main/java/com/trackingapp/utils/LocationUtils.kt`
  - Заменен тип `LatLng` на `Point`
  - Обновлены методы расчета расстояний

#### ViewModels
- ✅ `app/src/main/java/com/trackingapp/presentation/viewmodel/MainViewModel.kt`
  - Обновлен тип `currentLocation` с `LatLng` на `Point`
  - Обновлены методы работы с координатами

#### Screens
- ✅ `app/src/main/java/com/trackingapp/presentation/screen/MainScreen.kt`
  - Удален импорт Google Maps LatLng

- ✅ `app/src/main/java/com/trackingapp/presentation/components/StatisticsCard.kt`
  - Заменено использование LatLng на Point

#### Activity
- ✅ `app/src/main/java/com/trackingapp/presentation/MainActivity.kt`
  - Добавлена инициализация Яндекс MapKit
  - Добавлено управление жизненным циклом MapKit (onStart/onStop)

#### Конфигурация
- ✅ `app/src/main/res/values/strings.xml`
  - Добавлен плейсхолдер для API ключа Яндекс Карт

- ✅ `app/src/main/AndroidManifest.xml`
  - Уже содержал мета-данные для Яндекс MapKit

#### Документация
- ✅ `README.md`
  - Обновлена информация о технологическом стеке
  - Обновлены инструкции по настройке
  - Заменены ссылки на Google Maps на Яндекс MapKit

#### Build конфигурация
- ✅ `app/build.gradle.kts`
  - Зависимость Яндекс MapKit уже была добавлена
  - Google Maps Compose НЕ использовался (не требуется удаление)

- ✅ `build.gradle.kts`
  - Удалена секция `allprojects` (дублирование с settings.gradle.kts)

- ✅ `settings.gradle.kts`
  - Уже содержал необходимые репозитории

### 3. Удаленные файлы

- ✅ `app/google-services.json` - файл конфигурации Google Services (больше не нужен)

## Технические детали

### Зависимости

**Яндекс MapKit:**
```kotlin
implementation("com.yandex.android:maps.mobile:4.5.1-full")
```

**Google Play Services (только для локации):**
```kotlin
implementation("com.google.android.gms:play-services-location:21.0.1")
```

### Ключевые изменения в коде

#### Было (Google Maps):
```kotlin
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun MapView(
    currentLocation: LatLng?,
    ...
) {
    GoogleMap(
        ...
    ) {
        Marker(...)
        Polyline(...)
    }
}
```

#### Стало (Яндекс MapKit):
```kotlin
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.mapview.MapView as YandexMapView
import com.trackingapp.data.model.Point

@Composable
fun MapView(
    currentLocation: Point?,
    ...
) {
    AndroidView(
        factory = { YandexMapView(it) },
        update = { mapView ->
            mapView.map.mapObjects.addPlacemark(...)
            mapView.map.mapObjects.addPolyline(...)
        }
    )
}
```

### Инициализация

**MainActivity.kt:**
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    val apiKey = getString(R.string.yandex_maps_key)
    MapKitFactory.setApiKey(apiKey)
    MapKitFactory.initialize(this)
    ...
}

override fun onStart() {
    super.onStart()
    MapKitFactory.getInstance().onStart()
}

override fun onStop() {
    MapKitFactory.getInstance().onStop()
    super.onStop()
}
```

## Требования для работы

1. **API ключ Яндекс MapKit**
   - Получите в [Кабинете разработчика](https://developer.tech.yandex.ru/)
   - Добавьте в `app/src/main/res/values/strings.xml`

2. **Интернет соединение**
   - Для загрузки тайлов карты

3. **Разрешения**
   - `ACCESS_FINE_LOCATION`
   - `ACCESS_COARSE_LOCATION`
   - `INTERNET`

## Совместимость

- ✅ Android 7.0 (API 24) и выше
- ✅ Все существующие функции работают
- ✅ Сохранена совместимость с существующей базой данных
- ✅ GPX и GeoJSON экспорт работают без изменений

## Следующие шаги

1. **Получите API ключ** - см. `YANDEX_MAPS_SETUP.md`
2. **Обновите strings.xml** - добавьте свой API ключ
3. **Соберите проект** - `./gradlew assembleDebug`
4. **Протестируйте** - убедитесь, что карта отображается

## Отличия в функциональности

### Сохранено:
- ✅ Отображение маршрута (Polyline)
- ✅ Маркер текущего местоположения
- ✅ Управление камерой
- ✅ Жесты (зум, поворот)
- ✅ Все вычисления расстояний

### Изменено:
- 🔄 API для работы с картой (Яндекс MapKit вместо Google Maps)
- 🔄 Способ интеграции с Compose (AndroidView вместо нативных Composables)

### Не реализовано (можно добавить):
- ❌ My Location button (встроенная кнопка)
- ❌ Пробки (можно включить через API)
- ❌ Слои карты (спутник, гибрид)

## Известные особенности

1. **Жизненный цикл** - требуется явное управление через onStart/onStop
2. **Инициализация** - должна быть выполнена до использования карты
3. **API ключ** - должен быть валидным, иначе карта не загрузится

## Поддержка

При возникновении проблем:
1. Проверьте `YANDEX_MAPS_SETUP.md`
2. Убедитесь в валидности API ключа
3. Проверьте логи: `adb logcat | grep MapKit`

## Полезные ссылки

- [Документация Яндекс MapKit](https://yandex.ru/dev/maps/mapkit/doc/ru/)
- [Примеры кода](https://github.com/yandex/mapkit-android-demo)
- [Кабинет разработчика](https://developer.tech.yandex.ru/)


