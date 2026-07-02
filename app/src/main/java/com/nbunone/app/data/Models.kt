package com.nbunone.app.data

import kotlinx.serialization.Serializable

@Serializable
data class Member(
    val id: String,
    val name: String,
    val studentId: String = "",
    val role: String = "",
    val responsibilities: String = ""
)

@Serializable
data class Team(
    val id: String,
    val name: String,
    val projectName: String,
    val description: String = "",
    val members: List<Member> = emptyList(),
    val professorComment: String = ""
)

@Serializable
data class ActivityLog(
    val id: String,
    val teamId: String,
    val memberId: String,
    val date: String,
    val category: String,
    val content: String,
    val hours: Float = 1f
)

@Serializable
data class PeerEval(
    val id: String,
    val teamId: String,
    val evaluatorId: String,
    val targetId: String,
    val contribution: Int,
    val responsibility: Int,
    val collaboration: Int,
    val communication: Int,
    val comment: String = ""
) {
    val average: Float get() = (contribution + responsibility + collaboration + communication) / 4f
}

@Serializable
data class Report(
    val teamId: String,
    val generatedAt: String,
    val content: String,
    val isAi: Boolean
)

@Serializable
data class AppData(
    val teams: List<Team> = emptyList(),
    val logs: List<ActivityLog> = emptyList(),
    val evals: List<PeerEval> = emptyList(),
    val reports: List<Report> = emptyList(),
    val apiKey: String = ""
)

val LOG_CATEGORIES = listOf("회의", "개발", "자료조사", "문서작성", "발표준비", "기타")

val EVAL_ITEMS = listOf("기여도", "책임감", "협업", "소통")
