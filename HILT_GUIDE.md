# 🔧 Руководство по использованию Hilt в Mementra

## Что такое Hilt?

**Hilt** - это библиотека для Dependency Injection (внедрения зависимостей) от Google, построенная на основе Dagger. Она упрощает управление зависимостями в Android приложениях.

## Преимущества использования Hilt

✅ **Автоматическое создание зависимостей** - не нужно вручную создавать объекты  
✅ **Единственный источник правды** - все зависимости в одном месте  
✅ **Упрощение тестирования** - легко заменить реальные зависимости на моки  
✅ **Меньше boilerplate кода** - не нужны Factory классы для ViewModels  
✅ **Жизненный цикл** - автоматическое управление временем жизни объектов  

---

## Структура Hilt в проекте

### 1. Application класс

```kotlin
@HiltAndroidApp
class MementraApplication : Application() {
    // Hilt автоматически инициализируется
}
```

### 2. Модуль зависимостей

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideAppDatabaseHelper(
        @ApplicationContext context: Context
    ): AppDatabaseHelper {
        return AppDatabaseHelper(context)
    }
    
    @Provides
    @Singleton
    fun provideMemoryPointRepository(
        databaseHelper: AppDatabaseHelper
    ): MemoryPointRepository {
        return MemoryPointRepository(databaseHelper)
    }
}
```

### 3. ViewModel с Hilt

```kotlin
@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: MemoryPointRepository,
    private val userId: String
) : ViewModel() {
    // Hilt автоматически предоставит зависимости
}
```

### 4. Activity с Hilt

```kotlin
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    @Inject
    lateinit var userManager: UserManager
    
    @Inject
    lateinit var memoryRepository: MemoryPointRepository
    
    // Зависимости автоматически инжектятся
}
```

### 5. Fragment с Hilt

```kotlin
@AndroidEntryPoint
class MapFragment : Fragment() {
    
    // Автоматическое получение ViewModel через Hilt
    private val viewModel: MapViewModel by viewModels()
    
    // Все зависимости предоставляются автоматически
}
```

---

## Как добавить новую зависимость

### Шаг 1: Создайте класс

```kotlin
class MyNewService(
    private val context: Context,
    private val repository: MemoryPointRepository
) {
    fun doSomething() {
        // ...
    }
}
```

### Шаг 2: Добавьте провайдер в AppModule

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideMyNewService(
        @ApplicationContext context: Context,
        repository: MemoryPointRepository
    ): MyNewService {
        return MyNewService(context, repository)
    }
}
```

### Шаг 3: Используйте в нужном месте

```kotlin
@AndroidEntryPoint
class SomeActivity : AppCompatActivity() {
    
    @Inject
    lateinit var myNewService: MyNewService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        myNewService.doSomething()
    }
}
```

---

## Scope (области видимости)

Hilt поддерживает разные области видимости:

### @Singleton
```kotlin
@Provides
@Singleton
fun provideDatabase(): Database {
    // Создается один раз для всего приложения
}
```

### @ActivityScoped
```kotlin
@Provides
@ActivityScoped
fun provideActivityService(): ActivityService {
    // Создается один раз для Activity
}
```

### @FragmentScoped
```kotlin
@Provides
@FragmentScoped
fun provideFragmentService(): FragmentService {
    // Создается один раз для Fragment
}
```

---

## Типичные сценарии использования

### 1. Инъекция в ViewModel

**До Hilt:**
```kotlin
val factory = MapViewModelFactory(repository, userId)
viewModel = ViewModelProvider(this, factory)[MapViewModel::class.java]
```

**С Hilt:**
```kotlin
private val viewModel: MapViewModel by viewModels()
```

### 2. Инъекция в Activity

**До Hilt:**
```kotlin
val userManager = UserManager(this)
val repository = MemoryPointRepository(AppDatabaseHelper(this))
```

**С Hilt:**
```kotlin
@Inject lateinit var userManager: UserManager
@Inject lateinit var repository: MemoryPointRepository
```

### 3. Инъекция Context

```kotlin
@Provides
fun provideMyService(
    @ApplicationContext context: Context  // Контекст приложения
): MyService {
    return MyService(context)
}
```

---

## Отладка

### Проверка графа зависимостей

Hilt генерирует код во время компиляции. Если есть ошибки:

1. **Rebuild Project** - очистите и пересоберите проект
2. **Проверьте аннотации** - все компоненты должны быть помечены
3. **Проверьте модули** - все зависимости должны быть предоставлены

### Частые ошибки

❌ **Забыли @AndroidEntryPoint**
```kotlin
// НЕПРАВИЛЬНО
class MyActivity : AppCompatActivity() {
    @Inject lateinit var service: MyService  // НЕ СРАБОТАЕТ
}

// ПРАВИЛЬНО
@AndroidEntryPoint
class MyActivity : AppCompatActivity() {
    @Inject lateinit var service: MyService  // ✅
}
```

❌ **Забыли @HiltViewModel**
```kotlin
// НЕПРАВИЛЬНО
class MyViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel()

// ПРАВИЛЬНО
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel()
```

❌ **Зависимость не предоставлена в модуле**
```kotlin
// Если используете MyService, но не добавили провайдер в AppModule
// Получите ошибку компиляции
```

---

## Тестирование с Hilt

### Unit тесты

```kotlin
@HiltAndroidTest
class MyViewModelTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var repository: MemoryPointRepository
    
    @Before
    fun init() {
        hiltRule.inject()
    }
    
    @Test
    fun testSomething() {
        // Используйте инжектированные зависимости
    }
}
```

### Замена зависимостей для тестов

```kotlin
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AppModule::class]
)
object FakeAppModule {
    
    @Provides
    @Singleton
    fun provideFakeRepository(): MemoryPointRepository {
        return FakeMemoryPointRepository()
    }
}
```

---

## Миграция существующего кода на Hilt

### Шаг 1: Добавьте зависимости
```kotlin
// build.gradle.kts
plugins {
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")
}
```

### Шаг 2: Аннотируйте Application
```kotlin
@HiltAndroidApp
class MyApplication : Application()
```

### Шаг 3: Создайте модуль
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // Добавьте провайдеры
}
```

### Шаг 4: Обновите компоненты
- Добавьте `@AndroidEntryPoint` к Activities и Fragments
- Добавьте `@HiltViewModel` к ViewModels
- Замените ручное создание на `@Inject`

---

## Полезные ссылки

- [Официальная документация Hilt](https://dagger.dev/hilt/)
- [Codelab по Hilt](https://developer.android.com/codelabs/android-hilt)
- [Hilt на Android Developers](https://developer.android.com/training/dependency-injection/hilt-android)

---

**Создано:** 18 октября 2025  
**Версия:** 1.0  
**Проект:** Mementra

