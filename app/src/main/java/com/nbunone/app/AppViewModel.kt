package com.nbunone.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nbunone.app.ai.ReportGenerator
import com.nbunone.app.data.AppRepository
import com.nbunone.app.data.Report
import com.nbunone.app.data.Team
import com.nbunone.app.data.computeInsights
import com.nbunone.app.github.GitHubAnalyzer
import com.nbunone.app.github.GitHubStats
import kotlinx.coroutines.launch

sealed class CurrentUser {
    data object Professor : CurrentUser()
    data class Student(val name: String) : CurrentUser()
}

class AppViewModel : ViewModel() {
    val data = AppRepository.data

    var currentUser by mutableStateOf<CurrentUser?>(null)
        private set

    var reportLoading by mutableStateOf(false)
        private set
    var reportError by mutableStateOf<String?>(null)

    var githubStats by mutableStateOf<Map<String, GitHubStats>>(emptyMap())
        private set
    var githubLoadingTeam by mutableStateOf<String?>(null)
        private set
    var githubError by mutableStateOf<String?>(null)

    fun analyzeGithub(teamId: String, url: String) {
        githubLoadingTeam = teamId
        githubError = null
        viewModelScope.launch {
            GitHubAnalyzer.analyze(url)
                .onSuccess { githubStats = githubStats + (teamId to it) }
                .onFailure { githubError = it.message }
            githubLoadingTeam = null
        }
    }

    fun login(user: CurrentUser) { currentUser = user }
    fun logout() { AppRepository.exitDemo(); currentUser = null }

    fun myTeams(): List<Team> {
        val user = currentUser as? CurrentUser.Student ?: return emptyList()
        return data.value.teams.filter { t ->
            t.members.any { it.name == user.name } || t.createdByName == user.name
        }
    }

    fun memberIdIn(team: Team): String? {
        val user = currentUser as? CurrentUser.Student ?: return null
        return team.members.firstOrNull { it.name == user.name }?.id
    }

    fun generateReport(teamId: String) {
        val d = data.value
        val team = d.teams.firstOrNull { it.id == teamId } ?: return
        val insights = computeInsights(team, d.logs, d.evals)
        reportLoading = true
        reportError = null
        viewModelScope.launch {
            val result = ReportGenerator.generate(insights, d.logs, d.evals, d.apiKey)
            result.onSuccess { (content, isAi) ->
                AppRepository.saveReport(Report(teamId, nowString(), content, isAi))
            }.onFailure { e ->
                reportError = e.message ?: "리포트 생성에 실패했습니다"
            }
            reportLoading = false
        }
    }

    private fun nowString(): String {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.KOREA)
        return fmt.format(java.util.Date())
    }
}
