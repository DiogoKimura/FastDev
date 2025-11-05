# Exemplo Prático: Log Viewer com MVVM

Este exemplo demonstra a implementação completa de uma feature usando a arquitetura MVVM proposta.

---

## 📁 Estrutura de Arquivos

```
commonMain/kotlin/com/kimurashin/fastdev/
├── domain/
│   ├── model/
│   │   └── LogEntry.kt
│   ├── repository/
│   │   └── LogRepository.kt
│   └── usecase/
│       └── GetLogsUseCase.kt
├── data/
│   ├── repository/
│   │  └── LogRepositoryImpl.kt
│   ├── service/
│   │   └── MyBackgroundService.kt
│   └── receiver/
│       └── ConnectivityReceiver.kt
└── presentation/
    └── screens/
        └── logviewer/
            ├── LogViewerScreen.kt
            ├── LogViewerViewModel.kt
            └── LogViewerUiState.kt
```

---

## 1️⃣ Domain Layer (Lógica de Negócio)

### LogEntry.kt (Model/Entity)
```kotlin
package com.kimurashin.fastdev.domain.model

data class LogEntry(
    val id: String,
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String
)

enum class LogLevel {
    VERBOSE, DEBUG, INFO, WARN, ERROR
}
```

### LogRepository.kt (Contract)
```kotlin
package com.kimurashin.fastdev.domain.repository

import com.kimurashin.fastdev.domain.model.LogEntry
import kotlinx.coroutines.flow.Flow

interface LogRepository {
    fun observeLogs(): Flow<List<LogEntry>>
    suspend fun getLogs(): Result<List<LogEntry>>
    suspend fun filterLogs(query: String): Result<List<LogEntry>>
    suspend fun clearLogs(): Result<Unit>
}
```

### GetLogsUseCase.kt (Business Logic)
```kotlin
package com.kimurashin.fastdev.domain.usecase

import com.kimurashin.fastdev.domain.model.LogEntry
import com.kimurashin.fastdev.domain.repository.LogRepository
import kotlinx.coroutines.flow.Flow

class GetLogsUseCase(
    private val repository: LogRepository
) {
    operator fun invoke(): Flow<List<LogEntry>> {
        return repository.observeLogs()
    }
    
    suspend fun getFiltered(query: String): Result<List<LogEntry>> {
        if (query.isBlank()) {
            return repository.getLogs()
        }
        return repository.filterLogs(query)
    }
}
```

---

## 2️⃣ Data Layer (Implementação)

### LogRepositoryImpl.kt
```kotlin
package com.kimurashin.fastdev.data.repository

import com.kimurashin.fastdev.domain.model.LogEntry
import com.kimurashin.fastdev.domain.model.LogLevel
import com.kimurashin.fastdev.domain.repository.LogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LogRepositoryImpl : LogRepository {
    
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    
    override fun observeLogs(): Flow<List<LogEntry>> {
        return _logs.asStateFlow()
    }
    
    override suspend fun getLogs(): Result<List<LogEntry>> {
        return try {
            // Simula busca de logs do sistema
            val logs = generateMockLogs()
            _logs.value = logs
            Result.success(logs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun filterLogs(query: String): Result<List<LogEntry>> {
        return try {
            val filtered = _logs.value.filter { log ->
                log.message.contains(query, ignoreCase = true) ||
                log.tag.contains(query, ignoreCase = true)
            }
            Result.success(filtered)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun clearLogs(): Result<Unit> {
        return try {
            _logs.value = emptyList()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun generateMockLogs(): List<LogEntry> {
        return listOf(
            LogEntry("1", System.currentTimeMillis(), LogLevel.INFO, "MainActivity", "App started"),
            LogEntry("2", System.currentTimeMillis(), LogLevel.DEBUG, "Network", "Request sent to API"),
            LogEntry("3", System.currentTimeMillis(), LogLevel.ERROR, "Database", "Connection failed"),
        )
    }
}
```

---

## 3️⃣ Presentation Layer (UI + ViewModel)

### LogViewerUiState.kt
```kotlin
package com.kimurashin.fastdev.presentation.screens.logviewer

import com.kimurashin.fastdev.domain.model.LogEntry
import com.kimurashin.fastdev.domain.model.LogLevel

data class LogViewerUiState(
    val logs: List<LogEntry> = emptyList(),
    val isLoading: Boolean = false,
    val filterQuery: String = "",
    val selectedLevel: LogLevel? = null,
    val error: String? = null
)
```

### LogViewerViewModel.kt
```kotlin
package com.kimurashin.fastdev.presentation.screens.logviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kimurashin.fastdev.domain.usecase.GetLogsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LogViewerViewModel(
    private val getLogsUseCase: GetLogsUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LogViewerUiState())
    val uiState: StateFlow<LogViewerUiState> = _uiState.asStateFlow()
    
    init {
        observeLogs()
    }
    
    // Funções públicas chamadas diretamente pela UI
    fun loadLogs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            getLogsUseCase.getFiltered(_uiState.value.filterQuery)
                .onSuccess { logs ->
                    _uiState.update { it.copy(
                        logs = logs,
                        isLoading = false,
                        error = null
                    )}
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        error = error.message ?: "Unknown error",
                        isLoading = false
                    )}
                }
        }
    }
    
    fun refreshLogs() {
        _uiState.update { it.copy(filterQuery = "") }
        loadLogs()
    }
    
    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
    }
    
    fun filterLogs(query: String) {
        _uiState.update { it.copy(filterQuery = query) }
        loadLogs()
    }
    
    fun filterByLevel(level: com.kimurashin.fastdev.domain.model.LogLevel?) {
        _uiState.update { state ->
            state.copy(
                selectedLevel = level,
                logs = if (level != null) {
                    state.logs.filter { it.level == level }
                } else {
                    state.logs
                }
            )
        }
    }
    
    fun selectLog(logId: String) {
        // Navigate to detail or show dialog
        println("Log selected: $logId")
    }
    
    private fun observeLogs() {
        viewModelScope.launch {
            getLogsUseCase()
                .catch { error ->
                    _uiState.update { it.copy(
                        error = error.message,
                        isLoading = false
                    )}
                }
                .collect { logs ->
                    _uiState.update { it.copy(
                        logs = logs,
                        isLoading = false,
                        error = null
                    )}
                }
        }
    }
}
```

### LogViewerScreen.kt
```kotlin
package com.kimurashin.fastdev.presentation.screens.logviewer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kimurashin.fastdev.domain.model.LogLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(
    viewModel: LogViewerViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Viewer") },
                actions = {
                    IconButton(onClick = { viewModel.refreshLogs() }) {
                        Text("🔄")
                    }
                    IconButton(onClick = { viewModel.clearLogs() }) {
                        Text("🗑️")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Search Field
            OutlinedTextField(
                value = uiState.filterQuery,
                onValueChange = { viewModel.filterLogs(it) },
                label = { Text("Filter logs...") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Level Filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LogLevel.entries.forEach { level ->
                    FilterChip(
                        selected = uiState.selectedLevel == level,
                        onClick = {
                            viewModel.filterByLevel(
                                if (uiState.selectedLevel == level) null else level
                            )
                        },
                        label = { Text(level.name) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Logs List
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> {
                    Text(
                        text = "Error: ${uiState.error}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                uiState.logs.isEmpty() -> {
                    Text("No logs found")
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.logs) { log ->
                            LogItem(
                                log = log,
                                onClick = { viewModel.selectLog(log.id) }
                            )
                        }
                    }
                }
            }
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.loadLogs()
    }
}

@Composable
fun LogItem(
    log: com.kimurashin.fastdev.domain.model.LogEntry,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = log.tag,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = log.level.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = when (log.level) {
                        LogLevel.ERROR -> MaterialTheme.colorScheme.error
                        LogLevel.WARN -> MaterialTheme.colorScheme.tertiary
                        LogLevel.INFO -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = log.message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
```

---

## 4️⃣ Dependency Injection (Koin)

### AppModule.kt
```kotlin
package com.kimurashin.fastdev.core.di

import com.kimurashin.fastdev.data.repository.LogRepositoryImpl
import com.kimurashin.fastdev.domain.repository.LogRepository
import com.kimurashin.fastdev.domain.usecase.GetLogsUseCase
import com.kimurashin.fastdev.presentation.screens.logviewer.LogViewerViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Repositories
    single<LogRepository> { LogRepositoryImpl() }
    
    // Use Cases
    factory { GetLogsUseCase(get()) }
    
    // ViewModels
    viewModel { LogViewerViewModel(get()) }
}
```

---

## 🚀 Diferenças MVVM vs MVI

### MVVM (Mais Simples)
```kotlin
// UI chama funções diretamente
viewModel.loadLogs()
viewModel.filterLogs("error")
viewModel.clearLogs()
```

### MVI (Mais Verboso)
```kotlin
// UI envia events
viewModel.onEvent(LogViewerEvent.LoadLogs)
viewModel.onEvent(LogViewerEvent.FilterChanged("error"))
viewModel.onEvent(LogViewerEvent.ClearLogs)
```

**Vantagem MVVM**: Menos código, mais direto, padrão de mercado! 🎯

---

## ✅ Resultado

Esta implementação fornece:

✅ **Separação clara de responsabilidades**  
✅ **Estado unidirecional e previsível**  
✅ **Fácil de testar cada camada isoladamente**  
✅ **Reativo com Kotlin Flow**  
✅ **Menos boilerplate que MVI**  
✅ **Reutilizável entre Android e Desktop**

---

*Aplique este padrão MVVM para todas as features do FastDev!*

