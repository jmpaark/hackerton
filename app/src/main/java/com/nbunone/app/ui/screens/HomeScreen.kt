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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nbunone.app.AppViewModel
import com.nbunone.app.CurrentUser
import com.nbunone.app.data.AppData
import com.nbunone.app.data.Team
import com.nbunone.app.data.dDayLabel
import com.nbunone.app.data.parseDateOrNull
import com.nbunone.app.data.streakDays
import com.nbunone.app.ui.Amber
import com.nbunone.app.ui.ContributionHeatmap
import com.nbunone.app.ui.Slate
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: AppViewModel,
    data: AppData,
    onCreateTeam: () -> Unit,
    onOpenTeam: (teamId: String, tab: Int) -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val user = vm.currentUser as? CurrentUser.Student ?: return
    val myTeams = data.teams.filter { t ->
        t.members.any { it.name == user.name } || t.createdByName == user.name
    }
    val myMemberIds = myTeams.mapNotNull { t -> t.members.firstOrNull { it.name == user.name }?.id }.toSet()
    val myLogs = data.logs.filter { it.memberId in myMemberIds }
    val myDates = myLogs.mapNotNull { parseDateOrNull(it.date) }.toSet()
    val streak = streakDays(myDates, LocalDate.now())
    val firstTeam = myTeams.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("안녕하세요, ${user.name}님 👋", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            if (streak > 0) "오늘도 기여를 증명해볼까요?" else "첫 기록으로 증명을 시작하세요",
                            fontSize = 12.sp, color = Slate
                        )
                    }
                },
                actions = {
                    if (streak > 0) {
                        Row(
                            Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔥", fontSize = 14.sp)
                            Spacer(Modifier.width(3.dp))
                            Text(
                                "${streak}일 연속",
                                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
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
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (myTeams.isEmpty()) {
                // ── 첫 시작: 팀 만들기 하나에 집중 ──
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.size(64.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.LibraryAdd, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            Text("팀 만들기부터 시작해요", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "팀을 만들고 팀원을 등록하면\n기록·평가·리포트가 차례로 열립니다",
                                fontSize = 13.sp, color = Slate,
                                lineHeight = 19.sp
                            )
                            Spacer(Modifier.height(18.dp))
                            Button(
                                onClick = onCreateTeam,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                            ) { Text("팀 만들기", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }
                item {
                    Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            "1" to "팀 만들기 — 팀원과 역할을 등록해요",
                            "2" to "활동 기록 — 매일 한 일을 남겨요",
                            "3" to "동료평가 — 프로젝트가 끝나면 서로 평가해요"
                        ).forEach { (num, text) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(22.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(num, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        Text(
                            "이미 팀이 있다면? 팀장에게 내 이름(로그인 이름과 동일)으로 등록을 요청하세요",
                            fontSize = 11.sp, color = Slate
                        )
                    }
                }
            } else {
                // ── 다가오는 마감 ──
                item {
                    val today = LocalDate.now()
                    val upcoming = myTeams.flatMap { team ->
                        data.milestones
                            .filter { it.courseId == team.courseId && team.courseId.isNotBlank() }
                            .filter { m -> data.submissions.none { it.milestoneId == m.id && it.teamId == team.id } }
                            .mapNotNull { m ->
                                val due = parseDateOrNull(m.dueDate) ?: return@mapNotNull null
                                if (due.isBefore(today.minusDays(7))) null else Triple(team, m, due)
                            }
                    }.sortedBy { it.third }.take(2)

                    if (upcoming.isNotEmpty()) {
                        Text("다가오는 마감", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            upcoming.forEach { (team, m, due) ->
                                val overdue = due.isBefore(today)
                                val soon = !overdue && !due.isAfter(today.plusDays(3))
                                val badgeColor = when {
                                    overdue -> MaterialTheme.colorScheme.error
                                    soon -> Amber
                                    else -> MaterialTheme.colorScheme.primary
                                }
                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable { onOpenTeam(team.id, 1) },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("📌", fontSize = 18.sp)
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(m.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("${team.name} · 마감 ${m.dueDate}", fontSize = 12.sp, color = Slate)
                                        }
                                        Box(
                                            Modifier.background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                dDayLabel(m.dueDate, today),
                                                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = badgeColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 빠른 실행 ──
                item {
                    Text("빠른 실행", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickAction(Icons.Default.EditNote, "활동 기록", Modifier.weight(1f)) {
                            firstTeam?.let { onOpenTeam(it.id, 2) }
                        }
                        QuickAction(Icons.Default.HowToVote, "동료평가", Modifier.weight(1f)) {
                            firstTeam?.let { onOpenTeam(it.id, 3) }
                        }
                        QuickAction(Icons.Default.Description, "산출물", Modifier.weight(1f)) {
                            firstTeam?.let { onOpenTeam(it.id, 4) }
                        }
                        QuickAction(Icons.Default.LibraryAdd, "팀 추가", Modifier.weight(1f)) {
                            onCreateTeam()
                        }
                    }
                }

                // ── 내 팀 ──
                item {
                    Spacer(Modifier.height(4.dp))
                    Text("내 팀", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                items(myTeams) { team ->
                    TeamCard(team = team, data = data, onClick = { onOpenTeam(team.id, 0) })
                }

                // ── 내 기여도 잔디 ──
                item {
                    Spacer(Modifier.height(4.dp))
                    Text("내 기여도 잔디", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            val hoursByDate = myLogs
                                .groupBy { parseDateOrNull(it.date) }
                                .filterKeys { it != null }
                                .map { (k, v) -> k!! to v.sumOf { it.hours.toDouble() }.toFloat() }
                                .toMap()
                            ContributionHeatmap(hoursByDate = hoursByDate)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "총 ${myLogs.size}건 · ${"%.1f".format(myLogs.sumOf { it.hours.toDouble() })}시간의 기여가 기록되어 있어요",
                                fontSize = 12.sp, color = Slate
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(38.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun TeamCard(team: Team, data: AppData, onClick: () -> Unit) {
    val logCount = data.logs.count { it.teamId == team.id }
    val evalDone = team.members.count { m -> data.evals.any { it.teamId == team.id && it.evaluatorId == m.id } }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
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
                Column {
                    Text(team.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    val courseName = data.courses.firstOrNull { it.id == team.courseId }?.name
                    Text(
                        if (courseName != null) "${team.projectName} · $courseName" else team.projectName,
                        color = Slate, fontSize = 13.sp
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip("팀원 ${team.members.size}명")
                InfoChip("활동 ${logCount}건")
                InfoChip("평가 $evalDone/${team.members.size}")
            }
        }
    }
}

@Composable
fun InfoChip(text: String) {
    Box(
        Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
