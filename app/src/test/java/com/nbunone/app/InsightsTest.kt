package com.nbunone.app

import com.nbunone.app.data.ActivityLog
import com.nbunone.app.data.FlagType
import com.nbunone.app.data.Member
import com.nbunone.app.data.PeerEval
import com.nbunone.app.data.Seed
import com.nbunone.app.data.Team
import com.nbunone.app.data.computeInsights
import com.nbunone.app.data.COURSE_TYPES
import com.nbunone.app.data.Milestone
import com.nbunone.app.data.MilestoneStatus
import com.nbunone.app.data.Submission
import com.nbunone.app.data.dDayLabel
import com.nbunone.app.data.healthScore
import com.nbunone.app.data.milestoneStatus
import com.nbunone.app.data.milestoneTemplate
import com.nbunone.app.data.referenceShares
import com.nbunone.app.data.streakDays
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.math.abs

class InsightsTest {

    private val a = Member("a", "가람")
    private val b = Member("b", "나래")
    private val c = Member("c", "다솜")
    private val team = Team("t", "테스트팀", "테스트 프로젝트", members = listOf(a, b, c))

    private fun log(id: String, who: Member, hours: Float) =
        ActivityLog(id, team.id, who.id, "2026-01-0${id.last()}", "개발", "작업", hours)

    private fun eval(id: String, from: Member, to: Member, score: Int) =
        PeerEval(id, team.id, from.id, to.id, score, score, score, score)

    @Test
    fun `기록도 평가도 낮으면 무임승차 플래그`() {
        val logs = listOf(log("1", a, 10f), log("2", b, 10f), log("3", c, 0.5f))
        val evals = listOf(
            eval("e1", a, c, 1), eval("e2", b, c, 2),
            eval("e3", a, b, 5), eval("e4", c, b, 4),
            eval("e5", b, a, 5), eval("e6", c, a, 4)
        )
        val insights = computeInsights(team, logs, evals)
        assertEquals(FlagType.FREE_RIDER, insights.stats.first { it.member.id == "c" }.flag)
        assertNull(insights.stats.first { it.member.id == "a" }.flag)
        assertNull(insights.stats.first { it.member.id == "b" }.flag)
    }

    @Test
    fun `기록은 많은데 평가가 낮으면 불일치 플래그`() {
        val logs = listOf(log("1", a, 12f), log("2", b, 4f), log("3", c, 4f))
        val evals = listOf(
            eval("e1", b, a, 2), eval("e2", c, a, 2),  // a: 기록 60%인데 평가 2.0
            eval("e3", a, b, 4), eval("e4", c, b, 4),
            eval("e5", a, c, 4), eval("e6", b, c, 4)
        )
        val insights = computeInsights(team, logs, evals)
        assertEquals(FlagType.MISMATCH, insights.stats.first { it.member.id == "a" }.flag)
    }

    @Test
    fun `평가는 높은데 기록이 적으면 기록 누락 플래그`() {
        val logs = listOf(log("1", a, 10f), log("2", b, 9f), log("3", c, 1f))
        val evals = listOf(
            eval("e1", a, c, 5), eval("e2", b, c, 5),  // c: 기록 5%인데 평가 5.0
            eval("e3", a, b, 4), eval("e4", c, b, 4),
            eval("e5", b, a, 4), eval("e6", c, a, 4)
        )
        val insights = computeInsights(team, logs, evals)
        assertEquals(FlagType.UNSUNG, insights.stats.first { it.member.id == "c" }.flag)
    }

    @Test
    fun `자기평가는 집계에서 제외`() {
        val logs = listOf(log("1", a, 5f), log("2", b, 5f), log("3", c, 5f))
        val evals = listOf(
            eval("e1", a, a, 5),               // 자기평가 — 제외되어야 함
            eval("e2", b, a, 1), eval("e3", c, a, 1)
        )
        val insights = computeInsights(team, logs, evals)
        val aStat = insights.stats.first { it.member.id == "a" }
        assertEquals(1.0f, aStat.evalAvg, 0.01f)
        assertEquals(2, aStat.evalCount)
    }

    @Test
    fun `데이터가 없으면 플래그도 없다`() {
        val insights = computeInsights(team, emptyList(), emptyList())
        assertTrue(insights.stats.all { it.flag == null })
        assertEquals(0, insights.totalLogs)
        assertEquals(0f, insights.totalHours, 0.001f)
    }

    @Test
    fun `활동 비중의 합은 1`() {
        val logs = listOf(log("1", a, 3f), log("2", b, 5f), log("3", c, 2f))
        val insights = computeInsights(team, logs, emptyList())
        val shareSum = insights.stats.sumOf { it.logShare.toDouble() }
        assertTrue("비중 합=$shareSum", abs(shareSum - 1.0) < 0.001)
    }

    @Test
    fun `스트릭 계산 - 오늘 기록 없으면 어제부터 센다`() {
        val today = LocalDate.of(2026, 7, 2)
        // 오늘 포함 3일 연속
        assertEquals(3, streakDays(setOf(today, today.minusDays(1), today.minusDays(2)), today))
        // 오늘은 아직 안 썼지만 어제까지 2일 연속 → 스트릭 유지
        assertEquals(2, streakDays(setOf(today.minusDays(1), today.minusDays(2)), today))
        // 중간에 하루 빠지면 끊김
        assertEquals(1, streakDays(setOf(today, today.minusDays(2)), today))
        // 기록 없음
        assertEquals(0, streakDays(emptySet(), today))
    }

    @Test
    fun `팀 건강도 - 균형 잡힌 팀이 몰빵 팀보다 높다`() {
        val today = LocalDate.of(2026, 7, 2)
        fun logOn(id: String, who: Member, daysAgo: Long, hours: Float) =
            ActivityLog(id, team.id, who.id, today.minusDays(daysAgo).toString(), "개발", "x", hours)

        val allEvals = listOf(
            eval("e1", a, b, 4), eval("e2", b, a, 4), eval("e3", c, a, 4)
        )
        val balanced = listOf(logOn("1", a, 1, 5f), logOn("2", b, 2, 5f), logOn("3", c, 1, 5f))
        val skewed = listOf(logOn("1", a, 1, 14f), logOn("2", b, 2, 0.5f), logOn("3", c, 20, 0.5f))

        val hBalanced = healthScore(team, balanced, allEvals, today)
        val hSkewed = healthScore(team, skewed, allEvals, today)
        assertTrue("균형($hBalanced) > 몰빵($hSkewed)", hBalanced > hSkewed)
        assertTrue(hBalanced in 0..100 && hSkewed in 0..100)
        assertEquals(0, healthScore(team, emptyList(), emptyList(), today))
    }

    @Test
    fun `참고 기여도 - 합이 약 100이고 기여 많은 쪽이 높다`() {
        val logs = listOf(log("1", a, 10f), log("2", b, 5f), log("3", c, 1f))
        val evals = listOf(
            eval("e1", b, a, 5), eval("e2", c, a, 5),
            eval("e3", a, b, 4), eval("e4", c, b, 4),
            eval("e5", a, c, 2), eval("e6", b, c, 2)
        )
        val shares = referenceShares(computeInsights(team, logs, evals))
        val sum = shares.sumOf { it.second }
        assertTrue("합=$sum", sum in 98..102)  // 반올림 오차 허용
        val byId = shares.associate { it.first.id to it.second }
        assertTrue(byId["a"]!! > byId["b"]!! && byId["b"]!! > byId["c"]!!)
    }

    @Test
    fun `마일스톤 상태와 D-day 계산`() {
        val today = LocalDate.of(2026, 7, 2)
        val ms = Milestone("m1", "c1", "중간 발표", "2026-07-05")
        // 미제출 + 3일 후 마감 → 임박
        assertEquals(MilestoneStatus.DUE_SOON, milestoneStatus(ms, "t1", emptyList(), today))
        // 제출됨
        val sub = Submission("s1", "m1", "t1", "a", "2026-07-01")
        assertEquals(MilestoneStatus.SUBMITTED, milestoneStatus(ms, "t1", listOf(sub), today))
        // 다른 팀 제출은 무관
        assertEquals(MilestoneStatus.DUE_SOON, milestoneStatus(ms, "t2", listOf(sub), today))
        // 기한 지남
        assertEquals(
            MilestoneStatus.OVERDUE,
            milestoneStatus(Milestone("m2", "c1", "x", "2026-07-01"), "t1", emptyList(), today)
        )
        // 여유 있음
        assertEquals(
            MilestoneStatus.UPCOMING,
            milestoneStatus(Milestone("m3", "c1", "x", "2026-08-01"), "t1", emptyList(), today)
        )
        assertEquals("D-3", dDayLabel("2026-07-05", today))
        assertEquals("D-DAY", dDayLabel("2026-07-02", today))
        assertEquals("D+2", dDayLabel("2026-06-30", today))
    }

    @Test
    fun `과목 유형별 마일스톤 템플릿`() {
        COURSE_TYPES.forEach { type ->
            val template = milestoneTemplate(type)
            assertTrue("$type 템플릿이 비어있음", template.isNotEmpty())
            // 마감일 오름차순
            assertEquals(template.map { it.second }.sorted(), template.map { it.second })
        }
        assertTrue(milestoneTemplate(COURSE_TYPES[0]).size >= 5)  // 졸업작품은 학기 전체 일정
    }

    @Test
    fun `시드 데이터 참조 무결성`() {
        val d = Seed.build()
        assertEquals(2, d.teams.size)
        val memberIds = d.teams.flatMap { t -> t.members.map { t.id to it.id } }.toSet()
        d.logs.forEach { l ->
            assertTrue("로그 ${l.id}의 memberId가 팀에 없음", memberIds.contains(l.teamId to l.memberId))
        }
        d.evals.forEach { e ->
            assertTrue(memberIds.contains(e.teamId to e.evaluatorId))
            assertTrue(memberIds.contains(e.teamId to e.targetId))
            assertTrue("자기평가 금지", e.evaluatorId != e.targetId)
            assertTrue(e.contribution in 1..5 && e.responsibility in 1..5)
        }
        d.artifacts.forEach { a ->
            assertTrue(memberIds.contains(a.teamId to a.memberId))
        }
        d.surveys.forEach { s ->
            assertTrue(memberIds.contains(s.teamId to s.createdBy))
            assertEquals("선택지와 집계 수가 일치해야 함", s.options.size, s.counts.size)
            assertTrue(s.options.size >= 2)
        }
        // LMS: 과목·마일스톤·제출 참조 무결성
        val courseIds = d.courses.map { it.id }.toSet()
        val milestoneIds = d.milestones.map { it.id }.toSet()
        d.teams.forEach { t ->
            if (t.courseId.isNotBlank()) assertTrue("팀 ${t.name}의 과목 없음", courseIds.contains(t.courseId))
        }
        d.milestones.forEach { m ->
            assertTrue(courseIds.contains(m.courseId))
            assertTrue("마일스톤 ${m.title} 날짜 형식 오류", com.nbunone.app.data.parseDateOrNull(m.dueDate) != null)
        }
        d.submissions.forEach { s ->
            assertTrue(milestoneIds.contains(s.milestoneId))
            assertTrue(memberIds.contains(s.teamId to s.memberId))
            // 제출한 마일스톤은 팀의 과목에 속해야 함
            val ms = d.milestones.first { it.id == s.milestoneId }
            val team = d.teams.first { it.id == s.teamId }
            assertEquals(team.courseId, ms.courseId)
        }
        // 시나리오 검증: 민준 무임승차, 도윤 불일치
        val t1 = d.teams.first { it.name == "N분의1" }
        val i1 = computeInsights(t1, d.logs, d.evals)
        assertEquals(FlagType.FREE_RIDER, i1.stats.first { it.member.name == "민준" }.flag)
        val t2 = d.teams.first { it.name == "캡스톤A" }
        val i2 = computeInsights(t2, d.logs, d.evals)
        assertEquals(FlagType.MISMATCH, i2.stats.first { it.member.name == "도윤" }.flag)
    }
}
