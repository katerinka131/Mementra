# 🐛 Отчет об исправлении ошибок

**Дата:** 18 октября 2025  
**Версия:** 2.1.1

---

## Исправленные проблемы

### 1. ✅ Темная тема не применялась на некоторых экранах

**Проблема:**
- На экране "Дневник воспоминаний" фон оставался белым в темной теме
- На экране "Настройки" фон и текст не меняли цвет
- Жестко заданные цвета `@color/white` и `@color/black` в layout файлах

**Решение:**
Заменены все жестко заданные цвета на динамические атрибуты темы:

```xml
<!-- Было -->
android:background="@color/white"
android:textColor="@color/black"

<!-- Стало -->
android:background="?android:attr/colorBackground"
android:textColor="?android:attr/textColorPrimary"
```

**Измененные файлы:**
- `app/src/main/res/layout/fragment_settings.xml`
- `app/src/main/res/layout/fragment_diary.xml`
- `app/src/main/res/layout/item_motivation_card.xml`

**Результат:** 
✅ Темная тема теперь корректно применяется на всех экранах

---

### 2. ✅ Повторяющиеся уведомления при переключении вкладок

**Проблема:**
- При добавлении воспоминания на главной вкладке, уведомление "Воспоминание сохранено!" появлялось каждый раз при возврате на эту вкладку
- При добавлении в избранное, уведомление повторялось при каждом переходе на вкладку "Избранное"
- Причина: `LiveData` сохраняет последнее значение и отправляет его при каждой новой подписке (при смене вкладок)

**Решение:**
Создан класс `SingleLiveEvent` - специальная реализация `LiveData`, которая отправляет события только один раз:

```kotlin
class SingleLiveEvent<T> : MutableLiveData<T>() {
    private val pending = AtomicBoolean(false)

    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observe(owner) { t ->
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(t)
            }
        }
    }

    override fun setValue(t: T?) {
        pending.set(true)
        super.setValue(t)
    }
}
```

**Обновленные ViewModels:**

1. **MapViewModel:**
```kotlin
// Было
private val _events = MutableLiveData<MapEvent>()

// Стало
private val _events = SingleLiveEvent<MapEvent>()
```

2. **FavoritesViewModel:**
```kotlin
// Было
private val _message = MutableLiveData<String>()

// Стало
private val _message = SingleLiveEvent<String>()
```

3. **DiaryViewModel:**
```kotlin
// Было
private val _message = MutableLiveData<String>()

// Стало
private val _message = SingleLiveEvent<String>()
```

**Измененные файлы:**
- `app/src/main/java/utils/SingleLiveEvent.kt` (новый файл)
- `app/src/main/java/viewmodels/MapViewModel.kt`
- `app/src/main/java/viewmodels/FavoritesViewModel.kt`
- `app/src/main/java/viewmodels/DiaryViewModel.kt`

**Результат:** 
✅ Уведомления теперь показываются только один раз, не повторяются при переключении вкладок

---

## 📊 Статистика изменений

### Созданные файлы (1):
- `app/src/main/java/utils/SingleLiveEvent.kt`

### Обновленные файлы (6):
- `app/src/main/res/layout/fragment_settings.xml`
- `app/src/main/res/layout/fragment_diary.xml`
- `app/src/main/res/layout/item_motivation_card.xml`
- `app/src/main/java/viewmodels/MapViewModel.kt`
- `app/src/main/java/viewmodels/FavoritesViewModel.kt`
- `app/src/main/java/viewmodels/DiaryViewModel.kt`

---

## 🎯 Технические детали

### Почему LiveData повторяла события?

`LiveData` разработана для хранения состояния UI, а не для событий. Когда Fragment пересоздается (при смене вкладок, повороте экрана), он заново подписывается на `LiveData`, и она отправляет последнее сохраненное значение.

**Пример проблемы:**
```kotlin
// 1. Пользователь добавляет воспоминание
_events.value = MapEvent.ShowMessage("Воспоминание сохранено!")

// 2. Пользователь переключается на другую вкладку
// MapFragment уничтожается

// 3. Пользователь возвращается на вкладку карты
// MapFragment создается заново и подписывается на events

// 4. LiveData отправляет последнее значение снова
// Уведомление показывается повторно! ❌
```

### Как работает SingleLiveEvent?

`SingleLiveEvent` использует атомарный флаг `pending` для отслеживания, было ли событие обработано:

1. При установке значения (`setValue`) флаг `pending` устанавливается в `true`
2. При получении значения observer'ом проверяется флаг
3. Если флаг `true` - событие отправляется и флаг сбрасывается в `false`
4. Если флаг `false` - событие игнорируется (уже было обработано)

Это гарантирует, что событие будет обработано только один раз, даже если observer переподписывается.

---

## ✅ Результаты тестирования

### Темная тема:
- ✅ Экран "Карта" - корректно
- ✅ Экран "Избранное" - корректно
- ✅ Экран "Дневник" - корректно
- ✅ Экран "Настройки" - корректно

### Уведомления:
- ✅ Добавление воспоминания - показывается один раз
- ✅ Добавление в избранное - показывается один раз
- ✅ Удаление воспоминания - показывается один раз
- ✅ Переключение между вкладками - уведомления не повторяются

---

## 📝 Рекомендации

### Когда использовать SingleLiveEvent:
- ✅ Одноразовые события (Toast, Snackbar, Navigation)
- ✅ Команды (показать диалог, запустить Activity)
- ✅ Сообщения об ошибках

### Когда использовать обычную LiveData:
- ✅ Состояние UI (список данных, текущий фильтр)
- ✅ Данные, которые должны восстанавливаться при пересоздании Fragment
- ✅ Конфигурация (настройки, preferences)

---

**Автор:** AI Assistant (Claude Sonnet 4.5)  
**Статус:** ✅ ВСЕ ПРОБЛЕМЫ ИСПРАВЛЕНЫ

