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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.nbunone.app.data.FlagType
import com.nbunone.app.data.computeInsights
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
                val bg = when (flag) {
                    FlagType.MISMATCH -> Color(0xFFFEF3C7)
                    FlagType.FREE_RIDER -> Color(0xFFFEE2E2)
                    FlagType.UNSUNG -> Color(0xFFDBEAFE)
                }
                val fg = when (flag) {
                    FlagType.MISMATCH -> Color(0xFF92400E)
                    FlagType.FREE_RIDER -> Color(0xFF991B1B)
                    FlagType.UNSUNG -> Color(0xFF1E40AF)
                }
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

            // 익명 코멘트
            val allComments = insights.stats.flatMap { s -> s.comments.map { s.member.name to it } }
            if (allComments.isNotEmpty()) {
                SectionCard(title = "동료 코멘트 (익명)") {
                    allComments.forEach { (target, c) ->
                        Row(Modifier.padding(vertical = 3.dp)) {
                            Text("→ $target", fontSize = 12.sp, color = Indigo, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(56.dp))
                            Text("“$c”", fontSize = 12.sp, color = Color(0xFF334155))
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}
