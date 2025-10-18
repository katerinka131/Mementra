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
- **MVVM** (Model-View-ViewModel) - чистая архитектура
- **Repository Pattern** - слой доступа к данным
- **LiveData** & **Flow** - реактивное программирование
- **Coroutines** - асинхронные операции

### База данных
- **Room Database** - современная ORM для Android
- **SQLite** - локальное хранение данных
- Поддержка миграций и типобезопасных запросов

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
app/src/main/java/
├── com/example/mementra/
│   ├── MainActivity.kt              # Главная Activity
│   ├── MementraApplication.kt       # Application класс
│   └── OnboardingActivity.kt        # Экран приветствия
│
├── database/
│   ├── entities/                    # Room Entity классы
│   │   ├── UserEntity.kt
│   │   ├── MemoryPointEntity.kt
│   │   ├── MemoryEntryEntity.kt
│   │   └── TagEntity.kt
│   ├── dao/                         # Data Access Objects
│   │   ├── UserDao.kt
│   │   ├── MemoryPointDao.kt
│   │   └── MemoryEntryDao.kt
│   ├── models/                      # UI модели
│   │   ├── MemoryPoint.kt
│   │   └── MemoryEntry.kt
│   ├── AppDatabase.kt               # Room Database
│   ├── RoomMemoryRepository.kt      # Новый репозиторий
│   ├── MemoryPointRepository.kt     # Старый репозиторий (deprecated)
│   └── UserManager.kt               # Управление пользователями
│
├── fragments/                       # UI фрагменты
│   ├── MapFragment.kt               # Карта
│   ├── DiaryFragment.kt             # Дневник
│   ├── FavoritesFragment.kt         # Избранное
│   └── SettingsFragment.kt          # Настройки
│
├── viewmodels/                      # ViewModels
│   ├── MapViewModel.kt
│   ├── DiaryViewModel.kt
│   ├── FavoritesViewModel.kt
│   └── *ViewModelFactory.kt
│
└── utils/                           # Утилиты
    ├── MemoryDialogHelper.kt        # Диалоги
    ├── PermissionHelper.kt          # Разрешения
    └── SingleLiveEvent.kt           # Одноразовые события
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

---

## 🗄️ База данных

### Схема Room Database

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

