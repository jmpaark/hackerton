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
    val professorComment: String = "",
    val githubUrl: String = "",
    val createdByName: String = ""   // 생성자 이름 — 팀원 명단과 달라도 홈에 보이도록
)

@Serializable
data class Artifact(
    val id: String,
    val teamId: String,
    val memberId: String,
    val name: String,
    val date: String,
    val uri: String
)

@Serializable
data class ActivityLog(
    val id: String,
    val teamId: String,
    val memberId: String,
    val date: String,
    val category: String,
    val content: String,
    val hours: Float = 1f,
    val mood: String = ""      // 이모지 체크인 (선택)
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

/** 팀 설문 — 에브리타임 등 외부 커뮤니티에 공유하고 결과를 집계 */
@Serializable
data class Survey(
    val id: String,
    val teamId: String,
    val createdBy: String,
    val question: String,
    val options: List<String>,
    val counts: List<Int>,
    val date: String
) {
    val total: Int get() = counts.sum()
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
    val artifacts: List<Artifact> = emptyList(),
    val surveys: List<Survey> = emptyList(),
    val apiKey: String = "",
    val themeMode: String = "system",   // system | light | dark
    val accentColor: String = "indigo"
)

val LOG_CATEGORIES = listOf("회의", "개발", "자료조사", "문서작성", "발표준비", "기타")

val EVAL_ITEMS = listOf("기여도", "책임감", "협업", "소통")

/** 분위기 체크인 이모지 (점수 5→1) */
val MOODS = listOf("😄", "🙂", "😐", "😕", "😫")

fun moodScore(mood: String): Int = when (mood) {
    "😄" -> 5; "🙂" -> 4; "😐" -> 3; "😕" -> 2; "😫" -> 1; else -> 0
}
