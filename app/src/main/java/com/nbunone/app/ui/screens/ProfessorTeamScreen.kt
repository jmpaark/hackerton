package com.nbunone.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nbunone.app.AppViewModel
import com.nbunone.app.data.AppData
import com.nbunone.app.data.AppRepository
import com.nbunone.app.data.computeInsights
import com.nbunone.app.data.moodScore
import com.nbunone.app.data.parseDateOrNull
import com.nbunone.app.ui.ContributionHeatmap
import com.nbunone.app.ui.flagColors
import com.nbunone.app.ui.Amber
import com.nbunone.app.ui.BarRow
import com.nbunone.app.ui.ChartColors
import com.nbunone.app.ui.DonutChart
import com.nbunone.app.ui.Green
import com.nbunone.app.ui.Indigo
import com.nbunone.app.ui.Red
import com.nbunone.app.ui.Slate
import com.nbunone.app.ui.trim

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessorTeamScreen(
    vm: AppViewModel,
    data: AppData,
    teamId: String,
    onBack: () -> Unit,
    onOpenReport: () -> Unit
) {
    val team = data.teams.firstOrNull { it.id == teamId } ?: return
    val insights = computeInsights(team, data.logs, data.evals)
    val hasReport = data.reports.any { it.teamId == teamId }
    var comment by remember(teamId) { mutableStateOf(team.professorComment) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(team.name, fontWeight = FontWeight.Bold)
                        Text(team.projectName, fontSize = 12.sp, color = Slate)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ⚠️ 확인 필요 플래그
            insights.stats.filter { it.flag != null }.forEach { s ->
                val flag = s.flag!!
                val (bg, fg) = flagColors(flag)
                Card(colors = CardDefaults.cardColors(containerColor = bg)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("${s.member.name} · ${flag.label}", fontWeight = FontWeight.Bold, color = fg, fontSize = 14.sp)
                            Text(flag.detail, fontSize = 12.sp, color = fg)
                            Text(
                                "활동 비중 ${(s.logShare * 100).toInt()}% · 동료평가 ${"%.1f".format(s.evalAvg)}/5.0",
                                fontSize = 12.sp, color = fg, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 활동 시간 분포 도넛
            SectionCard(title = "활동 시간 분포") {
                DonutChart(entries = insights.stats.map { it.member.name to it.totalHours })
            }

            // 동료평가 평균
            SectionCard(title = "동료평가 평균 (5점 만점)") {
                if (insights.evalDone == 0) {
                    Text("아직 제출된 동료평가가 없습니다", color = Slate, fontSize = 13.sp)
                } else {
                    insights.stats.forEachIndexed { i, s ->
                        val color = when {
                            s.evalAvg >= 4f -> Green
                            s.evalAvg >= 3f -> ChartColors[i % ChartColors.size]
                            else -> Red
                        }
                        BarRow(
                            label = s.member.name,
                            value = s.evalAvg,
                            max = 5f,
                            color = color,
                            valueText = if (s.evalCount > 0) "%.1f".format(s.evalAvg) else "-"
                        )
                    }
                    Text(
                        "평가 제출: ${insights.evalDone}/${team.members.size}명 · 익명 집계",
                        fontSize = 11.sp, color = Slate
                    )
                }
            }

            // 활동 유형별
            if (insights.categoryHours.isNotEmpty()) {
                SectionCard(title = "활동 유형별 시간") {
                    val maxV = insights.categoryHours.values.max()
                    insights.categoryHours.entries.sortedByDescending { it.value }.forEachIndexed { i, (cat, h) ->
                        BarRow(label = cat, value = h, max = maxV, color = ChartColors[i % ChartColors.size], valueText = "${h.trim()}h")
                    }
                }
            }

            // 팀 활동 잔디
            val teamLogs = data.logs.filter { it.teamId == teamId }
            if (teamLogs.isNotEmpty()) {
                SectionCard(title = "팀 활동 잔디") {
                    val hoursByDate = teamLogs
                        .groupBy { parseDateOrNull(it.date) }
                        .filterKeys { it != null }
                        .map { (k, v) -> k!! to v.sumOf { it.hours.toDouble() }.toFloat() }
                        .toMap()
                    ContributionHeatmap(hoursByDate = hoursByDate)
                }
            }

            // 팀 분위기 체크인
            val moods = teamLogs.filter { it.mood.isNotBlank() }
                .sortedByDescending { it.date }
            if (moods.isNotEmpty()) {
                SectionCard(title = "팀 분위기") {
                    val avg = moods.map { moodScore(it.mood) }.average().toFloat()
                    val avgEmoji = when {
                        avg >= 4.5f -> "😄"; avg >= 3.5f -> "🙂"; avg >= 2.5f -> "😐"
                        avg >= 1.5f -> "😕"; else -> "😫"
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(avgEmoji, fontSize = 34.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "평균 ${"%.1f".format(avg)}/5.0",
                                fontWeight = FontWeight.Bold, fontSize = 16.sp,
                                color = if (avg < 2.5f) Red else MaterialTheme.colorScheme.onSurface
                            )
                            Text("체크인 ${moods.size}건 기준", fontSize = 12.sp, color = Slate)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("최근: ", fontSize = 12.sp, color = Slate)
                        Text(moods.take(10).joinToString(" ") { it.mood }, fontSize = 16.sp)
                    }
                    if (avg < 2.5f) {
                        Spacer(Modifier.height(6.dp))
                        Text("⚠️ 팀 분위기가 낮습니다. 갈등이나 번아웃 조짐일 수 있어요.", fontSize = 12.sp, color = Red)
                    }
                }
            }

            // 팀이 수집한 설문 결과
            val surveys = data.surveys.filter { it.teamId == teamId }
            if (surveys.isNotEmpty()) {
                SectionCard(title = "팀 설문 결과 (${surveys.size}건)") {
                    surveys.forEach { survey ->
                        Text("Q. ${survey.question}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("응답 ${survey.total}건", fontSize = 11.sp, color = Slate)
                        val maxCount = (survey.counts.maxOrNull() ?: 1).coerceAtLeast(1)
                        survey.options.forEachIndexed { i, opt ->
                            BarRow(
                                label = opt, value = survey.counts[i].toFloat(),
                                max = maxCount.toFloat(),
                                color = ChartColors[i % ChartColors.size],
                                valueText = "${survey.counts[i]}"
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }

            // 익명 코멘트
            val allComments = insights.stats.flatMap { s -> s.comments.map { s.member.name to it } }
            if (allComments.isNotEmpty()) {
                SectionCard(title = "동료 코멘트 (익명)") {
                    allComments.forEach { (target, c) ->
                        Row(Modifier.padding(vertical = 3.dp)) {
                            Text("→ $target", fontSize = 12.sp, color = Indigo, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(56.dp))
                            Text("“$c”", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                        }
                    }
                }
            }

            // GitHub 커밋 분석
            if (team.githubUrl.isNotBlank()) {
                SectionCard(title = "GitHub 커밋 분석") {
                    val stats = vm.githubStats[teamId]
                    when {
                        vm.githubLoadingTeam == teamId -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(10.dp))
                                Text("커밋 내역을 분석하는 중...", fontSize = 13.sp, color = Slate)
                            }
                        }
                        stats != null -> {
                            Text(
                                "${stats.repo} · 최근 ${stats.totalCommits}개 커밋",
                                fontSize = 12.sp, color = Slate
                            )
                            Spacer(Modifier.height(6.dp))
                            val maxC = stats.byAuthor.maxOfOrNull { it.second } ?: 1
                            stats.byAuthor.take(6).forEachIndexed { i, (author, count) ->
                                BarRow(
                                    label = author, value = count.toFloat(), max = maxC.toFloat(),
                                    color = ChartColors[i % ChartColors.size], valueText = "${count}건"
                                )
                            }
                            OutlinedButton(onClick = { vm.analyzeGithub(teamId, team.githubUrl) }) {
                                Text("다시 분석")
                            }
                        }
                        else -> {
                            Text(team.githubUrl, fontSize = 12.sp, color = Slate)
                            Spacer(Modifier.height(6.dp))
                            vm.githubError?.let {
                                Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.height(6.dp))
                            }
                            OutlinedButton(onClick = { vm.analyzeGithub(teamId, team.githubUrl) }) {
                                Text("커밋 기여 분석하기")
                            }
                        }
                    }
                }
            }

            // 교수자 코멘트
            SectionCard(title = "교수자 메모") {
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("이 팀에 대한 메모") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { AppRepository.setProfessorComment(teamId, comment) }) {
                    Text("메모 저장")
                }
            }

            // AI 리포트
            Button(
                onClick = onOpenReport,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (hasReport) "AI 기여도 리포트 보기" else "AI 기여도 리포트 생성", fontSize = 16.sp)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}
