package com.nbunone.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nbunone.app.AppViewModel
import com.nbunone.app.data.AppData
import com.nbunone.app.data.computeInsights
import com.nbunone.app.data.dDayLabel
import com.nbunone.app.data.healthScore
import com.nbunone.app.data.parseDateOrNull
import com.nbunone.app.ui.Amber
import com.nbunone.app.ui.Green
import com.nbunone.app.ui.Red
import com.nbunone.app.ui.Slate
import com.nbunone.app.ui.warnBadgeColors
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessorDashboardScreen(
    vm: AppViewModel,
    data: AppData,
    onOpenTeam: (String) -> Unit,
    onOpenCourse: (String) -> Unit,
    onCreateCourse: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val today = LocalDate.now()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("교수님 대시보드", fontWeight = FontWeight.Bold)
                        Text("팀별 기여도를 근거와 함께 확인하세요", fontSize = 12.sp, color = Slate)
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "설정")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "로그아웃")
                    }
                }
            )
        }
    ) { padding ->
        if (data.teams.isEmpty() && data.courses.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("📚", fontSize = 44.sp)
                Spacer(Modifier.height(12.dp))
                Text("과목 개설부터 시작해요", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(
                    "과목을 만들면 마일스톤 일정이 자동 생성되고,\n학생 팀이 연결되면 진행 현황이 표시됩니다",
                    fontSize = 13.sp, color = Slate, lineHeight = 19.sp
                )
                Spacer(Modifier.height(16.dp))
                androidx.compose.material3.Button(
                    onClick = onCreateCourse,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("과목 개설하기", fontSize = 16.sp) }
                Spacer(Modifier.height(10.dp))
                Text("체험: 설정 → 데모 데이터 불러오기", color = Slate, fontSize = 12.sp)
            }
            return@Scaffold
        }

        // 팀을 '관심 필요한 순'으로 정렬 (플래그 많고 건강도 낮은 팀 우선)
        val rankedTeams = data.teams.map { team ->
            val flags = computeInsights(team, data.logs, data.evals).stats.count { it.flag != null }
            val health = healthScore(team, data.logs, data.evals, today)
            Triple(team, flags, health)
        }.sortedWith(compareByDescending<Triple<com.nbunone.app.data.Team, Int, Int>> { it.second }.thenBy { it.third })

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── 한눈에 보는 요약 ──
            item {
                val totalFlags = rankedTeams.sumOf { it.second }
                val avgHealth = if (rankedTeams.isEmpty()) 0 else rankedTeams.map { it.third }.average().toInt()
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("담당 팀", "${data.teams.size}", Modifier.weight(1f))
                    StatCard("확인 필요", "$totalFlags", Modifier.weight(1f), highlight = totalFlags > 0)
                    StatCard("평균 건강도", "$avgHealth", Modifier.weight(1f))
                }
            }

            // ── ⏰ 이번 주 마감 ──
            item {
                val weekDeadlines = data.milestones.mapNotNull { m ->
                    val due = parseDateOrNull(m.dueDate) ?: return@mapNotNull null
                    if (due.isBefore(today) || due.isAfter(today.plusDays(7))) return@mapNotNull null
                    val course = data.courses.firstOrNull { it.id == m.courseId } ?: return@mapNotNull null
                    val courseTeams = data.teams.filter { it.courseId == course.id }
                    val submitted = courseTeams.count { t -> data.submissions.any { it.milestoneId == m.id && it.teamId == t.id } }
                    Triple(m, course, submitted to courseTeams.size)
                }.sortedBy { it.first.dueDate }.take(2)

                if (weekDeadlines.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        weekDeadlines.forEach { (m, course, progress) ->
                            val (bg, fg) = warnBadgeColors()
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { onOpenCourse(course.id) },
                                colors = CardDefaults.cardColors(containerColor = bg)
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("⏰", fontSize = 18.sp)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "${m.title} · ${dDayLabel(m.dueDate, today)}",
                                            fontWeight = FontWeight.Bold, fontSize = 14.sp, color = fg
                                        )
                                        Text(
                                            "${course.name} · 제출 ${progress.first}/${progress.second}팀",
                                            fontSize = 12.sp, color = fg
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 내 과목 ──
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("내 과목", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Text(
                        "+ 과목 개설",
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onCreateCourse() }.padding(4.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (data.courses.isEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Text(
                            "과목을 개설하면 유형(졸업작품/중간·기말)에 맞는 마일스톤이 자동 생성돼요",
                            Modifier.padding(12.dp), fontSize = 12.sp, color = Slate
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        data.courses.forEach { course ->
                            val courseTeams = data.teams.count { it.courseId == course.id }
                            val nextMs = data.milestones
                                .filter { it.courseId == course.id }
                                .mapNotNull { m -> parseDateOrNull(m.dueDate)?.let { m to it } }
                                .filter { !it.second.isBefore(today) }
                                .minByOrNull { it.second }
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { onOpenCourse(course.id) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("📚", fontSize = 22.sp)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(course.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(
                                            "${course.semester} · ${course.type} · 팀 ${courseTeams}개",
                                            fontSize = 12.sp, color = Slate
                                        )
                                        nextMs?.let { (m, _) ->
                                            Text(
                                                "다음 마감: ${m.title} (${dDayLabel(m.dueDate, today)})",
                                                fontSize = 12.sp, color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 담당 팀 (관심 필요 순) ──
            item {
                Spacer(Modifier.height(4.dp))
                Text("담당 팀", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (rankedTeams.any { it.second > 0 }) {
                    Text("관심이 필요한 팀부터 표시됩니다", fontSize = 11.sp, color = Slate)
                }
            }
            items(rankedTeams) { (team, flags, health) ->
                val healthColor = when {
                    health >= 70 -> Green
                    health >= 40 -> Amber
                    else -> Red
                }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenTeam(team.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(44.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(team.name.take(1), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(team.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                val courseName = data.courses.firstOrNull { it.id == team.courseId }?.name
                                Text(
                                    if (courseName != null) "${team.projectName} · $courseName" else team.projectName,
                                    color = Slate, fontSize = 13.sp
                                )
                            }
                            if (flags > 0) {
                                val (wbg, wfg) = warnBadgeColors()
                                Row(
                                    Modifier
                                        .background(wbg, RoundedCornerShape(20.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Amber, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("확인 필요 $flags", fontSize = 12.sp, color = wfg, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        val insights = computeInsights(team, data.logs, data.evals)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                Modifier
                                    .background(healthColor.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(8.dp).background(healthColor, CircleShape))
                                Spacer(Modifier.width(5.dp))
                                Text("건강도 $health", fontSize = 12.sp, color = healthColor, fontWeight = FontWeight.Bold)
                            }
                            InfoChip("팀원 ${team.members.size}명")
                            InfoChip("활동 ${insights.totalLogs}건")
                            InfoChip("평가 ${insights.evalDone}/${team.members.size}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                value, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = if (highlight) Amber else MaterialTheme.colorScheme.primary
            )
            Text(label, fontSize = 11.sp, color = Slate)
        }
    }
}
