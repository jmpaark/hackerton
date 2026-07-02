package com.nbunone.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nbunone.app.AppViewModel
import com.nbunone.app.data.AppData
import com.nbunone.app.data.computeInsights
import com.nbunone.app.data.healthScore
import com.nbunone.app.ui.Amber
import com.nbunone.app.ui.Green
import com.nbunone.app.ui.Indigo
import com.nbunone.app.ui.IndigoLight
import com.nbunone.app.ui.Red
import com.nbunone.app.ui.Slate
import com.nbunone.app.ui.warnBadgeColors

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessorDashboardScreen(
    vm: AppViewModel,
    data: AppData,
    onOpenTeam: (String) -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit
) {
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
        if (data.teams.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("아직 등록된 팀이 없습니다", fontWeight = FontWeight.SemiBold)
                Text("학생이 팀을 만들면 여기에 표시됩니다", color = Slate, fontSize = 13.sp)
                Text("체험: 설정 → 데모 데이터 불러오기", color = Slate, fontSize = 12.sp)
            }
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 상단 요약 통계
            item {
                val today = java.time.LocalDate.now()
                val totalFlags = data.teams.sumOf { t ->
                    computeInsights(t, data.logs, data.evals).stats.count { it.flag != null }
                }
                val avgHealth = if (data.teams.isEmpty()) 0
                else data.teams.map { healthScore(it, data.logs, data.evals, today) }.average().toInt()
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("담당 팀", "${data.teams.size}", Modifier.weight(1f))
                    StatCard("확인 필요", "$totalFlags", Modifier.weight(1f), highlight = totalFlags > 0)
                    StatCard("평균 건강도", "$avgHealth", Modifier.weight(1f))
                }
            }
            items(data.teams) { team ->
                val insights = computeInsights(team, data.logs, data.evals)
                val flags = insights.stats.count { it.flag != null }
                val health = healthScore(team, data.logs, data.evals, java.time.LocalDate.now())
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
                                Modifier.size(44.dp).background(IndigoLight, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(team.name.take(1), color = Indigo, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(team.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(team.projectName, color = Slate, fontSize = 13.sp)
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
