# Arquitetura - FastDev

## 🏗️ Arquitetura Recomendada: Clean Architecture + MVVM

Para uma aplicação multiplatform de ferramentas de desenvolvimento, recomendo **Clean Architecture** com padrão **MVVM (Model-View-ViewModel)** para gerenciamento de estado.

**Por que MVVM?**
- ✅ Padrão mais aceito no mercado Android
- ✅ Menos boilerplate que MVI
- ✅ Mais simples e direto
- ✅ Amplamente documentado e conhecido

---

## 📐 Camadas da Arquitetura

```
┌─────────────────────────────────────────────────┐
│                 Presentation                    │
│         (UI Screens, ViewModels, States)        │
│              Compose Multiplatform              │
└─────────────────────────────────────────────────┘
                      ↓↑
┌─────────────────────────────────────────────────┐
│                   Domain                        │
│     (Use Cases, Entities, Repositories)         │
│           Pure Kotlin - No Framework            │
└─────────────────────────────────────────────────┘
                      ↓↑
┌─────────────────────────────────────────────────┐
│                    Data                         │
│  (Repository Impl, Data Sources, DTOs, APIs)    │
│         Platform-specific implementations       │
└─────────────────────────────────────────────────┘
```

---

## 📁 Estrutura de Diretórios

### commonMain (Código Compartilhado)

```
commonMain/kotlin/com/kimurashin/fastdev/
│
├── core/                          # Núcleo compartilhado
│   ├── di/                        # Dependency Injection
│   │   └── AppModule.kt
│   ├── util/                      # Utilitários
│   │   ├── Result.kt
│   │   └── Logger.kt
│   └── navigation/                # Navegação
│       └── Navigator.kt
│
├── domain/                        # Camada de Domínio
│   ├── model/                     # Entities (modelos puros)
│   │   ├── LogEntry.kt
│   │   ├── ColorTheme.kt
│   │   └── NetworkRequest.kt
│   ├── repository/                # Contratos de repositórios
│   │   ├── LogRepository.kt
│   │   └── ThemeRepository.kt
│   └── usecase/                   # Casos de uso
│       ├── log/
│       │   ├── GetLogsUseCase.kt
│       │   └── FilterLogsUseCase.kt
│       └── theme/
│           └── GenerateThemeUseCase.kt
│
├── data/                          # Camada de Dados
│   ├── repository/                # Implementações
│   │   ├── LogRepositoryImpl.kt
│   │   └── ThemeRepositoryImpl.kt
│   ├── source/                    # Data Sources
│   │   ├── local/
│   │   │   └── LocalDataSource.kt
│   │   └── remote/
│   │       └── RemoteDataSource.kt
│   └── dto/                       # Data Transfer Objects
│       └── LogDto.kt
│
└── presentation/                  # Camada de Apresentação
    ├── ui/
    │   ├── theme/                 # Tema da aplicação
    │   │   ├── Color.kt
    │   │   ├── Theme.kt
    │   │   └── Typography.kt
    │   ├── components/            # Componentes reutilizáveis
    │   │   ├── FastDevButton.kt
    │   │   └── FastDevCard.kt
    │   └── screens/               # Telas
    │       ├── home/
    │       │   ├── HomeScreen.kt
    │       │   ├── HomeViewModel.kt
    │       │   └── HomeUiState.kt
    │       ├── logviewer/
    │       │   ├── LogViewerScreen.kt
    │       │   ├── LogViewerViewModel.kt
    │       │   └── LogViewerUiState.kt
    │       └── themegen/
    │           ├── ThemeGenScreen.kt
    │           ├── ThemeGenViewModel.kt
    │           └── ThemeGenUiState.kt
    └── App.kt                     # Ponto de entrada da UI
```

### androidMain (Android Específico)

```
androidMain/kotlin/com/kimurashin/fastdev/
├── MainActivity.kt
├── platform/                      # Implementações Android
│   ├── AndroidLogger.kt
│   └── AndroidFileSystem.kt
└── di/
    └── AndroidModule.kt
```

### jvmMain (Desktop Específico)

```
jvmMain/kotlin/com/kimurashin/fastdev/
├── Main.kt
├── platform/                      # Implementações Desktop
│   ├── DesktopLogger.kt
│   └── DesktopFileSystem.kt
└── di/
    └── DesktopModule.kt
```

---

## 🎯 Padrão MVVM (Model-View-ViewModel)

### Componentes Principais

#### 1. **UiState** (Estado da UI)
```kotlin
data class LogViewerUiState(
    val logs: List<LogEntry> = emptyList(),
    val isLoading: Boolean = false,
    val filter: String = "",
    val error: String? = null
)
```

#### 2. **ViewModel** (Gerenciador de estado e lógica de apresentação)
```kotlin
class LogViewerViewModel(
    private val getLogsUseCase: GetLogsUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LogViewerUiState())
    val uiState: StateFlow<LogViewerUiState> = _uiState.asStateFlow()
    
    init {
        loadLogs()
    }
    
    fun loadLogs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            getLogsUseCase()
                .onSuccess { logs ->
                    _uiState.update { it.copy(
                        logs = logs,
                        isLoading = false
                    )}
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        error = error.message,
                        isLoading = false
                    )}
                }
        }
    }
    
    fun filterLogs(query: String) {
        _uiState.update { it.copy(filter = query) }
        loadLogs()
    }
    
    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
    }
}
```

#### 3. **View/Screen** (Composable que observa o estado)
```kotlin
@Composable
fun LogViewerScreen(viewModel: LogViewerViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    // UI reage automaticamente às mudanças no uiState
    LogViewerContent(
        logs = uiState.logs,
        isLoading = uiState.isLoading,
        onRefresh = { viewModel.loadLogs() },
        onFilterChange = { viewModel.filterLogs(it) }
    )
}
```

### Vantagens do MVVM

✅ **Simples**: Menos boilerplate que MVI (sem Events e Effects separados)  
✅ **Direto**: Funções públicas no ViewModel chamadas diretamente pela UI  
✅ **Popular**: Padrão oficial do Android recomendado pelo Google  
✅ **Testável**: ViewModels podem ser testados isoladamente  
✅ **Reativo**: UI reage automaticamente ao StateFlow  

---

## 🔧 Tecnologias e Bibliotecas

### Core
- **Kotlin Multiplatform** - Compartilhamento de código
- **Compose Multiplatform** - UI declarativa
- **Coroutines** - Programação assíncrona
- **StateFlow/SharedFlow** - Gerenciamento de estado reativo

### Dependency Injection
- **Koin** (recomendado para KMP) - DI simples e multiplataforma
- Alternativa: **Kodein**

### Networking
- **Ktor Client** - HTTP client multiplataforma
- **Kotlinx.serialization** - Serialização JSON

### Storage
- **SQLDelight** - Database multiplataforma (para Database Inspector)
- **Multiplatform Settings** - Preferences/SharedPreferences

### Logging
- **Kermit** - Logger multiplataforma
- **Napier** - Alternativa

---

## 🎨 Benefícios da Arquitetura

### ✅ Separação de Responsabilidades
- Cada camada tem sua responsabilidade clara
- Fácil manutenção e testes

### ✅ Testabilidade
- Use Cases testáveis sem dependências de UI
- ViewModels testáveis com injeção de dependências

### ✅ Reusabilidade
- Lógica de negócio compartilhada entre Android/Desktop/iOS
- Componentes UI reutilizáveis

### ✅ Escalabilidade
- Fácil adicionar novas features
- Estrutura modular permite crescimento

### ✅ Manutenibilidade
- Código organizado e previsível
- Padrões consistentes em todo projeto

### ✅ Simplicidade
- Menos boilerplate que MVI
- Mais direto e objetivo
- Padrão amplamente conhecido no mercado

---

## 🚀 Fluxo de Dados (MVVM)

```
User Action (Click no botão)
       ↓
   ViewModel.loadLogs()
       ↓
   Use Case (GetLogsUseCase)
       ↓
   Repository (LogRepository)
       ↓
   Data Source (Local/Remote)
       ↓
   Repository retorna Result
       ↓
   ViewModel atualiza UiState
       ↓
   UI observa StateFlow e recompõe
```

---

## 📝 Convenções de Código

### Nomenclatura
- **Screens**: `[Feature]Screen.kt` (ex: `LogViewerScreen.kt`)
- **ViewModels**: `[Feature]ViewModel.kt`
- **UiStates**: `[Feature]UiState.kt`
- **Use Cases**: `[Action][Entity]UseCase.kt` (ex: `GetLogsUseCase.kt`)

### Pacotes
- Um pacote por feature na camada de presentation
- Agrupar por domínio, não por tipo de arquivo

---

## 🧪 Estratégia de Testes

### Unit Tests (commonTest)
- Use Cases
- ViewModels
- Repository implementations

### UI Tests
- androidTest para Android
- jvmTest para Desktop

### Test Coverage Mínimo
- Domain: 80%+
- Data: 70%+
- Presentation: 60%+

---

*Última atualização: 2025-11-03*

