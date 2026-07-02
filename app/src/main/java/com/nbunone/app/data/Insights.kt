package com.nbunone.app.data

enum class FlagType(val label: String, val detail: String) {
    MISMATCH("기록·평가 불일치", "활동 기록은 많지만 동료평가가 낮습니다. 기록의 실질 기여 여부 확인이 필요합니다."),
    FREE_RIDER("참여 저조", "활동 기록과 동료평가가 모두 낮습니다. 무임승차 가능성이 있어 면담을 권장합니다."),
    UNSUNG("기록 누락 가능성", "동료평가는 높지만 활동 기록이 적습니다. 기여가 기록되지 않았을 수 있습니다.")
}

data class MemberStats(
    val member: Member,
    val logCount: Int,
    val totalHours: Float,
    val logShare: Float,        // 0..1, 팀 전체 활동시간 중 비중
    val evalAvg: Float,         // 1..5, 동료들이 준 평균 (자기평가 제외)
    val evalByItem: Map<String, Float>,
    val evalCount: Int,
    val comments: List<String>,
    val flag: FlagType?
)

data class TeamInsights(
    val team: Team,
    val stats: List<MemberStats>,
    val totalLogs: Int,
    val totalHours: Float,
    val evalDone: Int,          // 평가 제출한 팀원 수
    val categoryHours: Map<String, Float>
)

fun computeInsights(team: Team, allLogs: List<ActivityLog>, allEvals: List<PeerEval>): TeamInsights {
    val logs = allLogs.filter { it.teamId == team.id }
    val evals = allEvals.filter { it.teamId == team.id }
    val teamHours = logs.sumOf { it.hours.toDouble() }.toFloat()
    val n = team.members.size.coerceAtLeast(1)
    val expectedShare = 1f / n

    val stats = team.members.map { m ->
        val myLogs = logs.filter { it.memberId == m.id }
        val hours = myLogs.sumOf { it.hours.toDouble() }.toFloat()
        val share = if (teamHours > 0f) hours / teamHours else 0f
        val received = evals.filter { it.targetId == m.id && it.evaluatorId != m.id }
        val avg = if (received.isNotEmpty()) received.map { it.average }.average().toFloat() else 0f
        val byItem = if (received.isNotEmpty()) mapOf(
            "기여도" to received.map { it.contribution }.average().toFloat(),
            "책임감" to received.map { it.responsibility }.average().toFloat(),
            "협업" to received.map { it.collaboration }.average().toFloat(),
            "소통" to received.map { it.communication }.average().toFloat()
        ) else emptyMap()

        val flag = when {
            received.isEmpty() || teamHours == 0f -> null
            share >= expectedShare * 1.15f && avg < 3.0f -> FlagType.MISMATCH
            share < expectedShare * 0.5f && avg < 3.0f -> FlagType.FREE_RIDER
            share < expectedShare * 0.6f && avg >= 4.0f -> FlagType.UNSUNG
            else -> null
        }

        MemberStats(
            member = m,
            logCount = myLogs.size,
            totalHours = hours,
            logShare = share,
            evalAvg = avg,
            evalByItem = byItem,
            evalCount = received.size,
            comments = received.map { it.comment }.filter { it.isNotBlank() },
            flag = flag
        )
    }

    val categoryHours = logs.groupBy { it.category }
        .mapValues { (_, v) -> v.sumOf { it.hours.toDouble() }.toFloat() }

    val evalDone = team.members.count { m -> evals.any { it.evaluatorId == m.id } }

    return TeamInsights(team, stats, logs.size, teamHours, evalDone, categoryHours)
}
