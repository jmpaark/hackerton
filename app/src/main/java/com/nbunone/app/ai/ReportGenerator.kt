package com.nbunone.app.ai

import com.nbunone.app.data.ActivityLog
import com.nbunone.app.data.PeerEval
import com.nbunone.app.data.TeamInsights
import com.nbunone.app.ui.trim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object ReportGenerator {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    /** @return Pair(리포트 본문 markdown-lite, AI 생성 여부) */
    suspend fun generate(
        insights: TeamInsights,
        logs: List<ActivityLog>,
        evals: List<PeerEval>,
        apiKey: String
    ): Result<Pair<String, Boolean>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.success(buildLocalReport(insights) to false)
        }
        runCatching {
            callClaude(insights, logs, apiKey) to true
        }.recoverCatching {
            // API 실패 시 데모가 끊기지 않도록 로컬 분석으로 대체
            val local = "> ⚠️ AI 호출에 실패하여 로컬 분석 리포트로 대체되었습니다. (${it.message})\n\n" +
                    buildLocalReport(insights)
            local to false
        }
    }

    private fun callClaude(insights: TeamInsights, logs: List<ActivityLog>, apiKey: String): String {
        val body = buildJsonObject {
            put("model", "claude-opus-4-8")
            put("max_tokens", 4096)
            putJsonObject("thinking") { put("type", "adaptive") }
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("content", buildPrompt(insights, logs))
                })
            })
        }

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), body)
                .toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string() ?: throw IllegalStateException("빈 응답")
            if (!response.isSuccessful) {
                val msg = runCatching {
                    json.parseToJsonElement(text).jsonObject["error"]!!
                        .jsonObject["message"]!!.jsonPrimitive.content
                }.getOrDefault("HTTP ${response.code}")
                throw IllegalStateException(msg)
            }
            val root = json.parseToJsonElement(text).jsonObject
            if (root["stop_reason"]?.jsonPrimitive?.content == "refusal") {
                throw IllegalStateException("요청이 거부되었습니다")
            }
            return root["content"]!!.jsonArray
                .map { it.jsonObject }
                .filter { it["type"]?.jsonPrimitive?.content == "text" }
                .joinToString("\n") { it["text"]!!.jsonPrimitive.content }
        }
    }

    private fun buildPrompt(insights: TeamInsights, logs: List<ActivityLog>): String {
        val team = insights.team
        val sb = StringBuilder()
        sb.appendLine("당신은 대학 팀 프로젝트의 기여도를 분석하는 조교입니다. 아래 데이터를 교차검증하여 교수님용 기여도 리포트를 한국어로 작성하세요.")
        sb.appendLine()
        sb.appendLine("## 팀 정보")
        sb.appendLine("- 팀명: ${team.name} / 프로젝트: ${team.projectName}")
        team.members.forEach { m ->
            sb.appendLine("- ${m.name} (역할: ${m.role}, 담당: ${m.responsibilities})")
        }
        sb.appendLine()
        sb.appendLine("## 팀원별 통계")
        insights.stats.forEach { s ->
            sb.appendLine("- ${s.member.name}: 활동 ${s.logCount}건/${s.totalHours.trim()}시간 (팀 내 비중 ${(s.logShare * 100).toInt()}%), 동료평가 평균 ${"%.1f".format(s.evalAvg)}/5.0")
            s.evalByItem.forEach { (k, v) -> sb.appendLine("    - $k: ${"%.1f".format(v)}") }
            s.comments.forEach { c -> sb.appendLine("    - 동료 코멘트: \"$c\"") }
        }
        sb.appendLine()
        sb.appendLine("## 활동 로그 (본인 기록)")
        logs.filter { it.teamId == team.id }.sortedBy { it.date }.forEach { l ->
            val name = team.members.firstOrNull { it.id == l.memberId }?.name ?: "?"
            sb.appendLine("- [${l.date}] $name / ${l.category} / ${l.hours.trim()}h: ${l.content}")
        }
        sb.appendLine()
        sb.appendLine(
            """
            ## 작성 지침
            1. "총평" — 팀 전체의 협업 상태를 3~4문장으로 요약
            2. "팀원별 분석" — 각 팀원에 대해: 기여 요약, 근거(로그·평가 인용), 특이사항
            3. "교차검증 결과" — 본인 기록과 동료평가가 불일치하는 팀원을 반드시 지적하고 이유를 설명 (기록은 많은데 평가가 낮은 경우, 기록은 적은데 평가가 높은 경우 등)
            4. "참고 기여도" — 팀원별 참고 기여 비율(%)을 제안하되, 합계 100%. 이는 확정 점수가 아닌 참고 의견임을 명시
            5. "권장 사항" — 교수님이 취할 수 있는 후속 조치 (면담 권장 등)

            형식: 마크다운 헤딩(##)과 목록(-)만 사용. 점수를 단정하지 말고 근거 기반의 참고 의견으로 서술할 것.
            """.trimIndent()
        )
        return sb.toString()
    }

    /** API 키가 없을 때 사용하는 룰 기반 리포트 */
    fun buildLocalReport(insights: TeamInsights): String {
        val sb = StringBuilder()
        val n = insights.team.members.size.coerceAtLeast(1)
        sb.appendLine("## 총평")
        sb.appendLine("- 총 활동 ${insights.totalLogs}건 / ${insights.totalHours.trim()}시간이 기록되었습니다.")
        sb.appendLine("- 동료평가 제출: ${insights.evalDone}/${n}명")
        val flagged = insights.stats.filter { it.flag != null }
        if (flagged.isEmpty()) {
            sb.appendLine("- 기록과 동료평가 간 큰 불일치는 발견되지 않았습니다.")
        } else {
            sb.appendLine("- ${flagged.size}명의 팀원에게 확인이 필요한 신호가 감지되었습니다.")
        }
        sb.appendLine()
        sb.appendLine("## 팀원별 분석")
        insights.stats.forEach { s ->
            sb.appendLine("### ${s.member.name} (${s.member.role})")
            sb.appendLine("- 활동 기록: ${s.logCount}건, ${s.totalHours.trim()}시간 (팀 내 비중 ${(s.logShare * 100).toInt()}%)")
            sb.appendLine("- 동료평가: ${if (s.evalCount > 0) "%.1f/5.0".format(s.evalAvg) else "미집계"}")
            s.flag?.let { sb.appendLine("- ⚠️ ${it.label}: ${it.detail}") }
        }
        sb.appendLine()
        sb.appendLine("## 교차검증 결과")
        if (flagged.isEmpty()) {
            sb.appendLine("- 특이사항 없음")
        } else {
            flagged.forEach { s ->
                sb.appendLine("- ${s.member.name}: ${s.flag!!.label} — 활동 비중 ${(s.logShare * 100).toInt()}% vs 동료평가 ${"%.1f".format(s.evalAvg)}/5.0")
            }
        }
        sb.appendLine()
        sb.appendLine("## 참고 기여도 (참고 의견)")
        // 활동 비중 60% + 동료평가 40% 가중 결합
        val weights = insights.stats.map { s ->
            val evalNorm = if (s.evalAvg > 0f) s.evalAvg / 5f else 1f / n
            s.member.name to (s.logShare * 0.6f + evalNorm / insights.stats.sumOf {
                (if (it.evalAvg > 0f) it.evalAvg / 5f else 1f / n).toDouble()
            }.toFloat() * 0.4f)
        }
        val total = weights.sumOf { it.second.toDouble() }.toFloat().takeIf { it > 0f } ?: 1f
        weights.forEach { (name, w) ->
            sb.appendLine("- $name: ${(w / total * 100).toInt()}%")
        }
        sb.appendLine()
        sb.appendLine("※ 위 비율은 활동 기록(60%)과 동료평가(40%)를 결합한 참고 수치이며, 확정 점수가 아닙니다.")
        return sb.toString()
    }
}
