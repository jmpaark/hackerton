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
    @Volatile private var securePrefs: SharedPreferences? = null
    private val diskExecutor = Executors.newSingleThreadExecutor()
    @Volatile private var initialized = false

    // 데모 세션 — 켜져 있으면 디스크에 저장하지 않는다(실제 데이터 오염 방지)
    @Volatile private var demoModeFlag = false
    val isDemo: Boolean get() = demoModeFlag

    private val _data = MutableStateFlow(AppData())
    val data: StateFlow<AppData> = _data

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            initialized = true
            val app = context.applicationContext
            file = File(app.filesDir, "nbunone.json")
            // 저장 데이터(JSON)는 크기가 작아 동기로 즉시 로드한다.
            // → 콜드스타트 직후의 쓰기/시드로드가 지연된 파일 로드에 덮어써지는 레이스를 원천 차단
            val loaded = if (file.exists()) {
                runCatching { json.decodeFromString<AppData>(file.readText()) }.getOrNull() ?: AppData()
            } else AppData()
            _data.value = loaded
            // 키스토어(암호화 저장소)만 느리므로 백그라운드에서 API 키를 불러온다 (ANR 방지)
            diskExecutor.execute {
                securePrefs = runCatching {
                    val masterKey = MasterKey.Builder(app)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()
                    EncryptedSharedPreferences.create(
                        app,
                        "nbunone_secure",
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )
                }.getOrElse {
                    // 키스토어 미지원 기기 폴백
                    app.getSharedPreferences("nbunone_secure_fallback", Context.MODE_PRIVATE)
                }
                val key = securePrefs?.getString(KEY_API, "") ?: ""
                if (key.isNotEmpty()) _data.value = _data.value.copy(apiKey = key)
            }
        }
    }

    @Synchronized
    private fun update(transform: (AppData) -> AppData) {
        val next = transform(_data.value)
        _data.value = next
        if (demoModeFlag) return // 데모 세션은 디스크에 저장하지 않음
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

    fun addMember(teamId: String, member: Member) = update { d ->
        d.copy(teams = d.teams.map {
            if (it.id == teamId) it.copy(members = it.members + member) else it
        })
    }

    fun removeMember(teamId: String, memberId: String) = update { d ->
        d.copy(
            teams = d.teams.map {
                if (it.id == teamId) it.copy(members = it.members.filterNot { m -> m.id == memberId }) else it
            },
            logs = d.logs.filterNot { it.teamId == teamId && it.memberId == memberId },
            evals = d.evals.filterNot { it.teamId == teamId && (it.evaluatorId == memberId || it.targetId == memberId) },
            submissions = d.submissions.filterNot { it.teamId == teamId && it.memberId == memberId }
        )
    }

    fun deleteTeam(teamId: String) = update { d ->
        d.copy(
            teams = d.teams.filterNot { it.id == teamId },
            logs = d.logs.filterNot { it.teamId == teamId },
            evals = d.evals.filterNot { it.teamId == teamId },
            submissions = d.submissions.filterNot { it.teamId == teamId },
            artifacts = d.artifacts.filterNot { it.teamId == teamId },
            surveys = d.surveys.filterNot { it.teamId == teamId },
            reports = d.reports.filterNot { it.teamId == teamId }
        )
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

    // ─── 과목 / 마일스톤 / 제출 (미니 LMS) ───

    fun addCourse(course: Course, milestones: List<Milestone>) = update {
        it.copy(courses = it.courses + course, milestones = it.milestones + milestones)
    }

    fun deleteCourse(courseId: String) = update { d ->
        d.copy(
            courses = d.courses.filterNot { it.id == courseId },
            milestones = d.milestones.filterNot { it.courseId == courseId },
            teams = d.teams.map { if (it.courseId == courseId) it.copy(courseId = "") else it }
        )
    }

    fun updateCourse(course: Course) = update { d ->
        d.copy(courses = d.courses.map { if (it.id == course.id) course else it })
    }

    fun addMilestone(milestone: Milestone) = update { it.copy(milestones = it.milestones + milestone) }

    fun updateMilestone(milestone: Milestone) = update { d ->
        d.copy(milestones = d.milestones.map { if (it.id == milestone.id) milestone else it })
    }

    fun deleteMilestone(milestoneId: String) = update { d ->
        d.copy(
            milestones = d.milestones.filterNot { it.id == milestoneId },
            submissions = d.submissions.filterNot { it.milestoneId == milestoneId }
        )
    }

    fun setTeamCourse(teamId: String, courseId: String) = update { d ->
        d.copy(teams = d.teams.map { if (it.id == teamId) it.copy(courseId = courseId) else it })
    }

    fun addSubmission(submission: Submission) = update { d ->
        // 팀당 마일스톤 1건 — 재제출 시 교체
        val remaining = d.submissions.filterNot {
            it.milestoneId == submission.milestoneId && it.teamId == submission.teamId
        }
        d.copy(submissions = remaining + submission)
    }

    fun deleteSubmission(submissionId: String) = update { d ->
        d.copy(submissions = d.submissions.filterNot { it.id == submissionId })
    }

    fun addSurvey(survey: Survey) = update { it.copy(surveys = it.surveys + survey) }

    fun deleteSurvey(surveyId: String) = update { d ->
        d.copy(surveys = d.surveys.filterNot { it.id == surveyId })
    }

    fun adjustSurveyCount(surveyId: String, optionIndex: Int, delta: Int) = update { d ->
        d.copy(surveys = d.surveys.map { s ->
            if (s.id == surveyId && optionIndex in s.counts.indices) {
                s.copy(counts = s.counts.mapIndexed { i, c ->
                    if (i == optionIndex) (c + delta).coerceAtLeast(0) else c
                })
            } else s
        })
    }

    fun addArtifact(artifact: Artifact) = update { it.copy(artifacts = it.artifacts + artifact) }

    fun deleteArtifact(artifactId: String) = update { d ->
        d.copy(artifacts = d.artifacts.filterNot { it.id == artifactId })
    }

    fun loadSeedData() = update { old ->
        Seed.build().copy(
            apiKey = old.apiKey, themeMode = old.themeMode,
            accentColor = old.accentColor, recentLogins = old.recentLogins
        )
    }

    /** 데모 둘러보기 시작 — 시드 데이터를 메모리에만 올리고 저장하지 않는다.
     *  실제(교수 개설 과목 등) 데이터는 디스크에 그대로 보존되고, 종료/재실행 시 복원된다. */
    fun enterDemo() {
        val cur = _data.value
        demoModeFlag = true
        _data.value = Seed.build().copy(
            apiKey = cur.apiKey, themeMode = cur.themeMode,
            accentColor = cur.accentColor, recentLogins = cur.recentLogins
        )
    }

    /** 데모 종료 — 디스크의 실제 데이터를 다시 불러온다. (일반 로그인·로그아웃 시 호출) */
    fun exitDemo() {
        if (!demoModeFlag) return
        demoModeFlag = false
        val cur = _data.value
        val loaded = if (::file.isInitialized && file.exists()) {
            runCatching { json.decodeFromString<AppData>(file.readText()) }.getOrNull() ?: AppData()
        } else AppData()
        _data.value = loaded.copy(apiKey = cur.apiKey)
    }

    fun clearAll() = update {
        AppData(
            apiKey = it.apiKey, themeMode = it.themeMode,
            accentColor = it.accentColor, recentLogins = it.recentLogins
        )
    }

    /** 간편 로그인용 최근 로그인 기록 (역할+이름, 중복 제거, 최근 4개) */
    fun pushRecentLogin(role: String, name: String) = update { d ->
        val entry = RecentLogin(role, name.trim())
        val deduped = d.recentLogins.filterNot { it.role == role && it.name == name.trim() }
        d.copy(recentLogins = (listOf(entry) + deduped).take(4))
    }
}
