# ⚡ Быстрое исправление ошибок (2 минуты)

## 🎯 Текущие ошибки исправлены:

✅ **Ошибка темы Material3** - исправлена  
✅ **Android resource linking failed** - исправлена  
✅ **themes.xml упрощён** для Compose  
✅ **Зависимости обновлены**  

---

## 🚀 Что делать СЕЙЧАС:

### Вариант 1: Через Android Studio (РЕКОМЕНДУЕТСЯ)

1. **Откройте проект** в Android Studio (если ещё не открыт)

2. **Синхронизируйте:**
   - **File → Sync Project with Gradle Files**
   - Подождите 1-3 минуты

3. **Пересоберите:**
   - **Build → Clean Project**
   - **Build → Rebuild Project**

4. **Настройте запуск:**
   - **Run → Edit Configurations**
   - Module: выберите **`app`**
   - **Apply → OK**

5. **Запустите:**
   - Нажмите зелёную стрелку **Run**

---

### Вариант 2: Если появляется ошибка gradle-wrapper.jar

**Выполните один раз:**

Двойной клик по файлу: **`init-gradle.bat`**

Это загрузит необходимый файл. Затем повторите Вариант 1.

---

## ✅ Готово!

После этого приложение должно собраться и запуститься.

---

## 📱 Не забудьте API ключ!

Перед первым запуском добавьте Яндекс MapKit API ключ:

**Файл:** `app/src/main/res/values/strings.xml`

```xml
<string name="yandex_maps_key">ВАШ_КЛЮЧ_ЗДЕСЬ</string>
```

**Получить ключ:** https://developer.tech.yandex.ru/

---

## 🆘 Если не помогло:

1. Закройте Android Studio
2. Удалите папки:
   - `.gradle`
   - `build`
   - `app/build`
3. Откройте Android Studio снова
4. **File → Invalidate Caches → Invalidate and Restart**
5. **File → Sync Project with Gradle Files**

---

## 📚 Подробная документация:

- **Ошибка темы:** `FIX_THEME_ERROR.md`
- **Ошибка Gradle:** `FIX_GRADLE_ERROR.md`  
- **Настройка Gradle:** `GRADLE_SETUP.md`
- **Яндекс Карты:** `QUICKSTART_YANDEX_MAPS.md`

