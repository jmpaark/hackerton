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
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.LibraryAdd
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nbunone.app.AppViewModel
import com.nbunone.app.CurrentUser
import com.nbunone.app.data.AppData
import com.nbunone.app.data.Team
import com.nbunone.app.data.parseDateOrNull
import com.nbunone.app.data.streakDays
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
    onLogout: () -> Unit
) {
    val user = vm.currentUser as? CurrentUser.Student ?: return
    val myTeams = data.teams.filter { t -> t.members.any { it.name == user.name } }
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
            // ── 빠른 실행 ──
            item {
                Text("빠른 실행", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction(
                        icon = Icons.Default.EditNote, label = "활동 기록",
                        enabled = firstTeam != null,
                        modifier = Modifier.weight(1f)
                    ) { firstTeam?.let { onOpenTeam(it.id, 1) } }
                    QuickAction(
                        icon = Icons.Default.HowToVote, label = "동료평가",
                        enabled = firstTeam != null,
                        modifier = Modifier.weight(1f)
                    ) { firstTeam?.let { onOpenTeam(it.id, 2) } }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction(
                        icon = Icons.Default.Description, label = "산출물",
                        enabled = firstTeam != null,
                        modifier = Modifier.weight(1f)
                    ) { firstTeam?.let { onOpenTeam(it.id, 3) } }
                    QuickAction(
                        icon = Icons.Default.LibraryAdd, label = "팀 만들기",
                        enabled = true,
                        modifier = Modifier.weight(1f)
                    ) { onCreateTeam() }
                }
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

            // ── 내 팀 ──
            item {
                Spacer(Modifier.height(4.dp))
                Text("내 팀", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            if (myTeams.isEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(
                            Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = Slate, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("아직 소속된 팀이 없어요", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("위의 '팀 만들기'로 시작하거나 팀장에게 등록을 요청하세요", color = Slate, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(myTeams) { team ->
                    TeamCard(team = team, data = data, onClick = { onOpenTeam(team.id, 0) })
                }
            }
        }
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (enabled) 1.dp else 0.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(42.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else Slate
            )
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
                    Text(team.projectName, color = Slate, fontSize = 13.sp)
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
