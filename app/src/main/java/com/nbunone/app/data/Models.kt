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
    val createdByName: String = "",  // 생성자 이름 — 팀원 명단과 달라도 홈에 보이도록
    val courseId: String = ""        // 소속 과목 (미니 LMS)
)

/** 과목 — 졸업작품/중간·기말 프로젝트를 담는 미니 LMS 단위 */
@Serializable
data class Course(
    val id: String,
    val name: String,
    val semester: String,
    val type: String,            // COURSE_TYPES 중 하나
    val description: String = ""
)

/** 마일스톤 — 과목의 과제·발표·제출 일정 */
@Serializable
data class Milestone(
    val id: String,
    val courseId: String,
    val title: String,
    val dueDate: String,         // yyyy-MM-dd
    val description: String = ""
)

/** 마일스톤 제출 — 팀 단위 제출 (산출물 첨부 가능) */
@Serializable
data class Submission(
    val id: String,
    val milestoneId: String,
    val teamId: String,
    val memberId: String,        // 제출자
    val date: String,
    val note: String = "",
    val artifactName: String = "",
    val artifactUri: String = ""
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
    val courses: List<Course> = emptyList(),
    val milestones: List<Milestone> = emptyList(),
    val submissions: List<Submission> = emptyList(),
    val apiKey: String = "",
    val themeMode: String = "system",   // system | light | dark
    val accentColor: String = "indigo"
)

val LOG_CATEGORIES = listOf("회의", "개발", "자료조사", "문서작성", "발표준비", "기타")

val EVAL_ITEMS = listOf("기여도", "책임감", "협업", "소통")

/** 분위기 체크인 이모지 (점수 5→1) */
val MOODS = listOf("😄", "🙂", "😐", "😕", "😫")

/** 과목 유형 */
const val TYPE_CAPSTONE = "졸업작품(캡스톤)"
const val TYPE_MIDFINAL = "중간·기말 프로젝트"
const val TYPE_GENERAL = "일반 팀플"
val COURSE_TYPES = listOf(TYPE_CAPSTONE, TYPE_MIDFINAL, TYPE_GENERAL)

/** 과목 유형별 마일스톤 템플릿 — (제목, 오늘로부터 며칠 뒤 마감) */
fun milestoneTemplate(type: String): List<Pair<String, Long>> = when (type) {
    TYPE_CAPSTONE -> listOf(
        "주제 확정" to 7L,
        "수행 계획서 제출" to 21L,
        "중간 발표" to 45L,
        "시제품 데모" to 75L,
        "최종 발표" to 100L,
        "논문·최종 보고서 제출" to 110L
    )
    TYPE_MIDFINAL -> listOf(
        "팀 구성·주제 확정" to 7L,
        "제안서 제출" to 14L,
        "중간 발표 (중간고사 대체)" to 30L,
        "최종 발표 (기말고사 대체)" to 60L,
        "최종 보고서 제출" to 65L
    )
    else -> listOf(
        "계획 수립" to 7L,
        "중간 점검" to 21L,
        "최종 제출" to 42L
    )
}

fun moodScore(mood: String): Int = when (mood) {
    "😄" -> 5; "🙂" -> 4; "😐" -> 3; "😕" -> 2; "😫" -> 1; else -> 0
}
