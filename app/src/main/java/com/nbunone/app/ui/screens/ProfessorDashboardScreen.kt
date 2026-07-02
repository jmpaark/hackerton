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
import com.nbunone.app.ui.Amber
import com.nbunone.app.ui.Indigo
import com.nbunone.app.ui.IndigoLight
import com.nbunone.app.ui.Slate

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
                Text("등록된 팀이 없습니다", fontWeight = FontWeight.SemiBold)
                Text("로그인 화면의 '데모 데이터 불러오기'로 체험할 수 있어요", color = Slate, fontSize = 13.sp)
            }
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(data.teams) { team ->
                val insights = computeInsights(team, data.logs, data.evals)
                val flags = insights.stats.count { it.flag != null }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenTeam(team.id) },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
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
                                Row(
                                    Modifier
                                        .background(Color(0xFFFEF3C7), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Amber, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("확인 필요 $flags", fontSize = 12.sp, color = Color(0xFF92400E), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
