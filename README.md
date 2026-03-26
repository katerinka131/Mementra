# 📱 Mementra - Дневник воспоминаний с геолокацией

<div align="center">

![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)
![MinSDK](https://img.shields.io/badge/MinSDK-24-orange.svg)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

**Сохраняйте воспоминания, привязанные к местам на карте**

[Особенности](#-особенности) • [Скриншоты](#-скриншоты) • [Технологии](#-технологии) • [Установка](#-установка) • [Архитектура](#-архитектура)

</div>

---

## 📖 О проекте

**Mementra** — это современное Android-приложение для создания и хранения воспоминаний с привязкой к географическим местам. Записывайте важные моменты жизни, добавляйте фото и аудио, отмечайте любимые места на интерактивной карте.

### 🎯 Основная идея

Каждое воспоминание привязано к конкретному месту на карте. Вы можете:
- 📍 Отметить место на карте, где произошло событие
- 📝 Добавить текстовое описание
- 📸 Прикрепить фотографии с миниатюрами
- 🎤 Записать голосовые заметки
- ❤️ Добавить в избранное важные воспоминания
- 🗺️ Просматривать все воспоминания на интерактивной карте

---

## ✨ Особенности

### 🗺️ Интерактивная карта
- Просмотр всех воспоминаний на карте OpenStreetMap
- Добавление новых точек памяти
- Кластеризация маркеров
- Детальная информация при клике на маркер

### 📓 Дневник воспоминаний
- Список всех воспоминаний в хронологическом порядке
- Поиск по названию и описанию
- Сортировка по дате и алфавиту
- Фильтрация по периоду времени

### ❤️ Избранное
- Отдельный раздел для любимых воспоминаний
- Быстрый доступ к важным моментам
- Переключение статуса избранного одним нажатием

### ⚙️ Настройки
- 🌙 Темная/светлая тема
- 🔔 Управление уведомлениями
- 📧 Связь с поддержкой
- 🔗 Поделиться приложением
- ℹ️ Информация о приложении

### 📸 Мультимедиа (в разработке)
- Поддержка фотографий с автоматическими миниатюрами
- Запись и воспроизведение аудио
- Хранение метаданных (размер, длительность)

---

## 🎨 Скриншоты

```
┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│   🗺️ Карта  │  │ 📓 Дневник  │  │ ❤️ Избранное│  │ ⚙️ Настройки│
│             │  │             │  │             │  │             │
│  Маркеры на │  │  Список     │  │  Любимые    │  │  Темная/    │
│  карте с    │  │  всех       │  │  воспомина- │  │  светлая    │
│  воспомина- │  │  воспомина- │  │  ния        │  │  тема       │
│  ниями      │  │  ний        │  │             │  │             │
└─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘
```

---

## 🛠 Технологии

### Язык и платформа
- **Kotlin** - основной язык разработки
- **Android SDK** - минимальная версия API 24 (Android 7.0)
- **Target SDK** - API 34 (Android 14)

### Архитектура
- **MVVM** на экранах карты, дневника и избранного (Fragment → ViewModel → репозиторий)
- **Single Activity** + **Navigation Component** для основного потока; онбординг — отдельная `Activity`
- **Repository** как абстракция данных (см. раздел «База данных» — сейчас в рантайме используется один из двух вариантов)
- **LiveData** для состояния UI; **SingleLiveEvent** для одноразовых событий (например, на карте)
- **Kotlin Coroutines** (`viewModelScope`, `lifecycleScope`) для фоновой работы
- **View Binding** для разметки
- Явного **DI** (Hilt/Koin) в проекте нет — зависимости собираются вручную (например, в `MainActivity`)

### База данных
- **SQLite** — один файл `mementra.db`, локальное хранение
- В коде параллельно существуют **два стека доступа к тем же таблицам** (миграция на Room в процессе):
  - **активный в рантайме:** `AppDatabaseHelper` (`SQLiteOpenHelper`) + **`MemoryPointRepository`** — им пользуются `MainActivity` и фабрики `ViewModel`;
  - **подготовленный стек:** **Room** (`AppDatabase`, entity, DAO) + **`RoomMemoryRepository`** — готов к подключению вместо legacy-репозитория (см. `ROOM_MIGRATION_GUIDE.md`).

### UI/UX
- **Material Design 3** - современный дизайн
- **View Binding** - безопасная работа с view
- **Navigation Component** - навигация между экранами
- **Bottom Navigation** - удобная нижняя панель

### Карты
- **OSMDroid** - OpenStreetMap для Android
- Offline карты
- Кастомные маркеры

### Дополнительные библиотеки
- **Timber** - логирование
- **PermissionX** - управление разрешениями
- **Gson** - работа с JSON
- **Material Components** - UI компоненты

---

## 📦 Установка

### Требования
- Android Studio Arctic Fox или новее
- JDK 11 или выше
- Android SDK 24+
- Gradle 8.0+

### Клонирование репозитория

```bash
git clone https://github.com/katerinka131/Mementra.git
cd Mementra
```

### Сборка проекта

```bash
# Сборка debug версии
./gradlew assembleDebug

# Сборка release версии
./gradlew assembleRelease

# Установка на устройство
./gradlew installDebug
```

### Открытие в Android Studio

1. Откройте Android Studio
2. File → Open → выберите папку проекта
3. Дождитесь синхронизации Gradle
4. Запустите на эмуляторе или реальном устройстве

---

## 🏗 Архитектура

### Структура проекта

```
app/src/main/java/com/example/mementra/
├── MainActivity.kt
├── MementraApplication.kt
├── OnboardingActivity.kt
├── OnboardingAdapter.kt
│
├── database/
│   ├── entities/          # Room-сущности
│   ├── dao/               # Room DAO
│   ├── models/            # MemoryPoint, MemoryEntry (общие модели)
│   ├── AppDatabase.kt     # Room + миграции
│   ├── AppDatabaseHelper.kt   # SQLiteOpenHelper (legacy, рантайм)
│   ├── MemoryPointRepository.kt  # репозиторий поверх Helper (рантайм)
│   ├── RoomMemoryRepository.kt   # репозиторий поверх Room (подключение — TODO)
│   └── UserManager.kt
│
├── fragments/
├── viewmodels/            # + *ViewModelFactory.kt
├── adapters/
└── utils/
```

### Слои приложения

```
┌─────────────────────────────────────┐
│           UI Layer                  │
│  (Fragments, Activities, Adapters)  │
└─────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────┐
│        ViewModel Layer              │
│     (Business Logic, UI State)      │
└─────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────┐
│       Repository Layer              │
│    (Data Access Abstraction)        │
└─────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────┐
│        Data Source Layer            │
│   (Room Database, SharedPrefs)      │
└─────────────────────────────────────┘
```

### Структура пакетов (фактическая)

Исходники лежат в `app/src/main/java/com/example/mementra/`:

| Пакет / область | Назначение |
|-----------------|------------|
| `MainActivity`, `OnboardingActivity`, `MementraApplication` | Точка входа, навигация, тема, логирование (Timber) |
| `fragments/` | UI: карта, дневник, избранное, настройки, запись аудио |
| `viewmodels/` + `*Factory` | Состояние экранов, вызовы репозитория, события для UI |
| `database/` | Репозитории, Room (`AppDatabase`, `dao/`, `entities/`), legacy `AppDatabaseHelper`, `UserManager` |
| `database/models/` | Модели домена/UI (`MemoryPoint`, `MemoryEntry`), общие для слоёв |
| `adapters/` | `RecyclerView` (списки дневника/избранного) |
| `utils/` | Диалоги (`MemoryDialogHelper`), разрешения, уведомления, сетка медиа, `FileProvider`, `SingleLiveEvent` |

### Паттерны по частям приложения

**Карта (`MapFragment` + `MapViewModel`)**  
MVVM: `LiveData` для списка точек и флагов UI, `SingleLiveEvent` для разовых событий (точка добавлена и т.д.). Работа с OSMDroid, маркерами и медиа через `Activity Result API` и `FileProvider`. Часть сценариев (диалоги добавления/деталей) вынесена в `MemoryDialogHelper`.

**Дневник и избранное (`DiaryFragment`, `FavoritesFragment` + соответствующие `ViewModel`)**  
Тот же MVVM + адаптер списка. Поиск, сортировка и обновление списка через `ViewModel` и корутины.

**Настройки (`SettingsFragment`)**  
В основном прямой UI + `SharedPreferences` (тема, уведомления), без отдельного `ViewModel`.

**Онбординг**  
Отдельная `Activity`, адаптер страниц (`OnboardingAdapter`), флаг завершения в `SharedPreferences`.

**Данные**  
Паттерн **Repository**: `MemoryPointRepository` инкапсулирует SQL-запросы к SQLite через `AppDatabaseHelper`. `RoomMemoryRepository` дублирует операции через Room-DAO для будущего переключения.

**Вспомогательные объекты**  
Статические/объектные утилиты (`MemoryDialogHelper`, `PermissionHelper`, `NotificationHelper`, `MediaGridHelper`) — упрощённый **Facade** / набор процедур над Android API, без отдельного use-case слоя.

---

## 🗄️ База данных

### Два слоя доступа к одной БД

Один файл SQLite (`mementra.db`), схема описана в Room-сущностях и поддерживается миграциями Room (например, `MIGRATION_2_3` в `AppDatabase`). **Сейчас UI и ViewModel получают данные через `MemoryPointRepository` + `SQLiteOpenHelper`**, а не через `RoomMemoryRepository`, пока не завершён полный перенос (см. комментарии `@Deprecated` у legacy-классов).

### Схема Room Database (целевая / соответствует таблицам в файле)

```sql
-- Пользователи
users (
    user_id TEXT PRIMARY KEY,
    created_at INTEGER,
    last_active INTEGER,
    name TEXT,
    settings TEXT
)

-- Точки памяти
memory_points (
    point_id INTEGER PRIMARY KEY,
    user_id TEXT,
    title TEXT,
    description TEXT,
    latitude REAL,
    longitude REAL,
    address TEXT,
    created_at INTEGER,
    visit_date INTEGER,
    is_favorite INTEGER,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
)

-- Записи воспоминаний
memory_entries (
    entry_id INTEGER PRIMARY KEY,
    memory_point_id INTEGER,
    type TEXT,              -- "text", "photo", "audio"
    content TEXT,
    created_at INTEGER,
    order_index INTEGER,
    file_path TEXT,         -- путь к файлу
    file_size INTEGER,      -- размер файла
    thumbnail_path TEXT,    -- миниатюра для фото
    duration INTEGER,       -- длительность для аудио
    FOREIGN KEY (memory_point_id) REFERENCES memory_points(point_id)
)

-- Теги
tags (
    tag_id INTEGER PRIMARY KEY,
    user_id TEXT,
    tag_name TEXT,
    tag_color TEXT,
    tag_icon TEXT,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
)

-- Связь многие-ко-многим
memory_point_tags (
    memory_point_id INTEGER,
    tag_id INTEGER,
    PRIMARY KEY (memory_point_id, tag_id)
)
```

---

## 🎨 Дизайн

### Цветовая схема

**Светлая тема:**
- Primary: `#667EEA` (синий-фиолетовый)
- Secondary: `#764BA2` (темно-фиолетовый)
- Background: `#FFFFFF`
- Surface: `#F5F5F5`

**Темная тема:**
- Primary: `#667EEA`
- Secondary: `#764BA2`
- Background: `#121212`
- Surface: `#1E1E1E`

### Иконки

Все иконки выполнены в стиле Material Design:
- Векторная графика (SVG)
- Адаптивные под тему
- Размеры: 24dp (навигация), 16dp (стрелки)

---

## 📝 Разработка

### Ветки

- `main` - стабильная версия
- `feature/*` - новые функции
- `bugfix/*` - исправления ошибок
- `release/*` - подготовка релизов

### Коммиты

Используем Conventional Commits:
```
feat: добавлена поддержка темной темы
fix: исправлены повторяющиеся уведомления
docs: обновлен README
refactor: миграция на Room Database
```

### Тестирование

```bash
# Unit тесты
./gradlew test

# Instrumentation тесты
./gradlew connectedAndroidTest

# Lint проверка
./gradlew lint
```

---

## 🚀 Roadmap

### Версия 1.0 ✅
- [x] Базовый функционал карты
- [x] Создание и просмотр воспоминаний
- [x] Избранное
- [x] Дневник с поиском
- [x] Настройки с темами
- [x] Миграция на Room Database

### Версия 1.1 (в разработке)
- [ ] Загрузка и отображение фотографий
- [ ] Запись и воспроизведение аудио
- [ ] Система тегов
- [ ] Экспорт данных

### Версия 2.0 (планируется)
- [ ] Синхронизация с облаком
- [ ] Совместный доступ к воспоминаниям
- [ ] Временная шкала
- [ ] Статистика и аналитика
- [ ] Виджеты для главного экрана

---

## 📄 Документация

Подробная документация доступна в папке проекта:

- [REFACTORING_SUMMARY.md](REFACTORING_SUMMARY.md) - история рефакторинга
- [ROOM_MIGRATION_GUIDE.md](ROOM_MIGRATION_GUIDE.md) - руководство по миграции на Room
- [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) - детальный отчет о реализации
- [BUGFIX_SUMMARY.md](BUGFIX_SUMMARY.md) - исправленные баги
- [ICONS_UPDATE.md](ICONS_UPDATE.md) - обновление иконок
- [HILT_GUIDE.md](HILT_GUIDE.md) - руководство по Hilt DI

---

## 🤝 Вклад в проект

Мы приветствуем вклад в развитие проекта! 

### Как внести вклад:

1. Fork репозитория
2. Создайте ветку для вашей функции (`git checkout -b feature/AmazingFeature`)
3. Commit изменения (`git commit -m 'feat: add some AmazingFeature'`)
4. Push в ветку (`git push origin feature/AmazingFeature`)
5. Откройте Pull Request

### Правила:

- Следуйте стилю кода проекта
- Добавляйте комментарии к сложному коду
- Обновляйте документацию при необходимости
- Пишите осмысленные commit messages

---

## 📧 Контакты

**Email:** support@mementra.app  
**GitHub:** [katerinka131/Mementra](https://github.com/katerinka131/Mementra)

---

## 📜 Лицензия

Этот проект распространяется под лицензией MIT. Подробности в файле [LICENSE](LICENSE).

```
MIT License

Copyright (c) 2025 Mementra Team

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
```

---

## 🙏 Благодарности

- [OpenStreetMap](https://www.openstreetmap.org/) - картографические данные
- [OSMDroid](https://github.com/osmdroid/osmdroid) - библиотека карт
- [Material Design](https://material.io/) - дизайн система
- Все контрибьюторы проекта

---

<div align="center">

**Сделано с ❤️ командой Mementra**

⭐ Поставьте звезду, если проект вам понравился!

</div>

