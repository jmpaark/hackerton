package com.nbunone.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

object AppRepository {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private lateinit var file: File

    private val _data = MutableStateFlow(AppData())
    val data: StateFlow<AppData> = _data

    fun init(context: Context) {
        file = File(context.filesDir, "nbunone.json")
        if (file.exists()) {
            runCatching { _data.value = json.decodeFromString<AppData>(file.readText()) }
        }
    }

    private fun update(transform: (AppData) -> AppData) {
        _data.value = transform(_data.value)
        runCatching { file.writeText(json.encodeToString(_data.value)) }
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

    fun setApiKey(key: String) = update { it.copy(apiKey = key) }

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
