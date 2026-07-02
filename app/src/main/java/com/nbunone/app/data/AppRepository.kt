package com.nbunone.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

object AppRepository {
    private const val KEY_API = "claude_api_key"

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private lateinit var file: File
    private var securePrefs: SharedPreferences? = null
    private val diskExecutor = Executors.newSingleThreadExecutor()
    @Volatile private var initialized = false

    private val _data = MutableStateFlow(AppData())
    val data: StateFlow<AppData> = _data

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            file = File(context.filesDir, "nbunone.json")
            // API 키는 평문 JSON이 아닌 암호화 저장소에 보관
            securePrefs = runCatching {
                val masterKey = MasterKey.Builder(context.applicationContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context.applicationContext,
                    "nbunone_secure",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            }.getOrElse {
                // 키스토어 미지원 기기 폴백
                context.getSharedPreferences("nbunone_secure_fallback", Context.MODE_PRIVATE)
            }
            val loaded = if (file.exists()) {
                runCatching { json.decodeFromString<AppData>(file.readText()) }.getOrNull() ?: AppData()
            } else AppData()
            _data.value = loaded.copy(apiKey = securePrefs?.getString(KEY_API, "") ?: "")
            initialized = true
        }
    }

    @Synchronized
    private fun update(transform: (AppData) -> AppData) {
        val next = transform(_data.value)
        _data.value = next
        val toPersist = next.copy(apiKey = "") // API 키는 JSON에 저장하지 않음
        diskExecutor.execute {
            runCatching {
                // 원자적 쓰기: 임시 파일에 쓴 뒤 rename (중간에 죽어도 원본 보존)
                val tmp = File(file.parentFile, "${file.name}.tmp")
                tmp.writeText(json.encodeToString(toPersist))
                if (!tmp.renameTo(file)) {
                    file.delete()
                    tmp.renameTo(file)
                }
            }
        }
    }

    fun newId(): String = UUID.randomUUID().toString().take(8)

    fun addTeam(team: Team) = update { it.copy(teams = it.teams + team) }

    fun updateTeam(team: Team) = update { d ->
        d.copy(teams = d.teams.map { if (it.id == team.id) team else it })
    }

    fun addLog(log: ActivityLog) = update { it.copy(logs = it.logs + log) }

    fun deleteLog(logId: String) = update { d ->
        d.copy(logs = d.logs.filterNot { it.id == logId })
    }

    fun submitEvals(teamId: String, evaluatorId: String, evals: List<PeerEval>) = update { d ->
        val remaining = d.evals.filterNot { it.teamId == teamId && it.evaluatorId == evaluatorId }
        d.copy(evals = remaining + evals)
    }

    fun saveReport(report: Report) = update { d ->
        d.copy(reports = d.reports.filterNot { it.teamId == report.teamId } + report)
    }

    fun setProfessorComment(teamId: String, comment: String) = update { d ->
        d.copy(teams = d.teams.map { if (it.id == teamId) it.copy(professorComment = comment) else it })
    }

    fun setApiKey(key: String) {
        securePrefs?.edit()?.putString(KEY_API, key)?.apply()
        update { it.copy(apiKey = key) }
    }

    fun setTheme(mode: String, accent: String) = update { it.copy(themeMode = mode, accentColor = accent) }

    fun setGithubUrl(teamId: String, url: String) = update { d ->
        d.copy(teams = d.teams.map { if (it.id == teamId) it.copy(githubUrl = url) else it })
    }

    fun addArtifact(artifact: Artifact) = update { it.copy(artifacts = it.artifacts + artifact) }

    fun deleteArtifact(artifactId: String) = update { d ->
        d.copy(artifacts = d.artifacts.filterNot { it.id == artifactId })
    }

    fun loadSeedData() = update { old ->
        Seed.build().copy(apiKey = old.apiKey, themeMode = old.themeMode, accentColor = old.accentColor)
    }

    fun clearAll() = update { AppData(apiKey = it.apiKey, themeMode = it.themeMode, accentColor = it.accentColor) }
}
