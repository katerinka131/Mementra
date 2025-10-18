# 🔄 Руководство по миграции на Room Database

## ✅ Что уже сделано

### 1. Добавлены зависимости Room
- `androidx.room:room-runtime:2.6.1`
- `androidx.room:room-ktx:2.6.1`
- `androidx.room:room-compiler:2.6.1` (KSP)

### 2. Созданы Entity классы
- `UserEntity` - пользователи
- `MemoryPointEntity` - точки памяти
- `MemoryEntryEntity` - записи воспоминаний (с поддержкой фото и аудио)
- `TagEntity` - теги
- `MemoryPointTagCrossRef` - связь многие-ко-многим

### 3. Созданы DAO интерфейсы
- `UserDao` - операции с пользователями
- `MemoryPointDao` - операции с точками памяти
- `MemoryEntryDao` - операции с записями
- `TagDao` - операции с тегами

### 4. Создан AppDatabase
- Room Database класс с миграцией с версии 2 на 3
- Singleton pattern для доступа к БД
- Поддержка in-memory БД для тестирования

### 5. Создан RoomMemoryRepository
- Новый репозиторий для работы через Room
- Поддержка Flow для реактивного обновления
- Маппинг между Entity и Model классами

### 6. Добавлена поддержка медиа-контента
**Фото:**
- `filePath` - путь к файлу изображения
- `fileSize` - размер файла
- `thumbnailPath` - путь к миниатюре

**Аудио:**
- `filePath` - путь к аудиофайлу
- `duration` - длительность в миллисекундах
- `fileSize` - размер файла

## 📋 Следующие шаги для полной миграции

### Шаг 1: Обновить MainActivity для использования Room

```kotlin
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var userManager: UserManager
    private lateinit var database: AppDatabase
    private lateinit var memoryRepository: RoomMemoryRepository
    
    val userId: String by lazy { userManager.getUserId() }
    val memoryRepo: RoomMemoryRepository get() = memoryRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Инициализация Room Database
        database = AppDatabase.getInstance(this)
        
        // Инициализация зависимостей
        userManager = UserManager(this)
        memoryRepository = RoomMemoryRepository(
            database.memoryPointDao(),
            database.memoryEntryDao()
        )
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupNavigation()
        
        // Обновляем время последней активности
        userManager.updateLastActive(userId)
    }
}
```

### Шаг 2: Обновить ViewModelFactory классы

Заменить `MemoryPointRepository` на `RoomMemoryRepository` во всех Factory классах.

### Шаг 3: Обновить Fragments

Все фрагменты уже используют ViewModel, поэтому изменения минимальны - только тип репозитория.

### Шаг 4: Удалить старые файлы

После успешной миграции можно удалить:
- `AppDatabaseHelper.kt` (старый SQLiteOpenHelper)
- `MemoryPointRepository.kt` (старый репозиторий)
- Все ViewModelFactory классы (если используется Hilt)

## 🎯 Преимущества миграции на Room

### 1. Производительность
- Оптимизированные запросы
- Кэширование
- Асинхронные операции из коробки

### 2. Безопасность типов
- Compile-time проверка SQL запросов
- Type-safe DAO методы
- Автоматическая генерация кода

### 3. Реактивность
- Flow для реактивного обновления UI
- LiveData поддержка
- Автоматическое обновление при изменении данных

### 4. Тестируемость
- In-memory база для тестов
- Легко мокировать DAO
- Изолированное тестирование

### 5. Миграции
- Автоматическая миграция схемы
- Валидация миграций
- Fallback стратегии

## 📝 Пример использования нового API

### Добавление воспоминания с фото

```kotlin
// Создаем точку памяти
val memoryPoint = MemoryPoint(
    userId = userId,
    title = "Поездка в горы",
    description = "Незабываемые впечатления",
    latitude = 43.5855,
    longitude = 39.7231,
    visitDate = System.currentTimeMillis()
)

val pointId = repository.addMemoryPoint(memoryPoint)

// Добавляем фото
val photoEntry = MemoryEntry(
    memoryPointId = pointId,
    type = MemoryEntry.TYPE_PHOTO,
    content = "Вид с вершины",
    filePath = "/storage/emulated/0/Pictures/mountain.jpg",
    fileSize = 2048576, // 2MB
    thumbnailPath = "/storage/emulated/0/Pictures/.thumbnails/mountain_thumb.jpg"
)

repository.addMemoryEntry(photoEntry)
```

### Добавление аудиозаписи

```kotlin
val audioEntry = MemoryEntry(
    memoryPointId = pointId,
    type = MemoryEntry.TYPE_AUDIO,
    content = "Голосовая заметка о поездке",
    filePath = "/storage/emulated/0/Music/voice_note.m4a",
    fileSize = 524288, // 512KB
    duration = 45000 // 45 секунд
)

repository.addMemoryEntry(audioEntry)
```

### Реактивное обновление UI

```kotlin
// В ViewModel
val memoryPointsFlow: Flow<List<MemoryPoint>> = 
    repository.getMemoryPointsFlow(userId)

// В Fragment
viewLifecycleOwner.lifecycleScope.launch {
    viewModel.memoryPointsFlow.collect { memories ->
        // UI автоматически обновляется при изменении данных
        updateUI(memories)
    }
}
```

## ⚠️ Важные замечания

1. **Миграция данных**: Текущая миграция сохраняет все существующие данные
2. **Backward compatibility**: Старый `MemoryPointRepository` временно сохранен для совместимости
3. **Тестирование**: Обязательно протестируйте миграцию на тестовых данных
4. **Backup**: Рекомендуется создать резервную копию БД перед миграцией

## 🚀 Статус миграции

- [x] Добавлены зависимости Room
- [x] Созданы Entity классы
- [x] Созданы DAO интерфейсы
- [x] Создан AppDatabase с миграцией
- [x] Создан RoomMemoryRepository
- [x] Добавлена поддержка фото (путь, размер, миниатюра)
- [x] Добавлена поддержка аудио (путь, длительность, размер)
- [x] Обновлены модели данных
- [x] Обновлены ViewModels
- [ ] Обновить MainActivity для использования Room
- [ ] Обновить ViewModelFactory (или перейти на Hilt)
- [ ] Протестировать миграцию
- [ ] Удалить старые файлы

---

**Дата создания:** 18 октября 2025  
**Версия:** 1.0  
**Автор:** AI Assistant (Claude Sonnet 4.5)

