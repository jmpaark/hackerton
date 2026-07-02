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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import com.nbunone.app.CurrentUser
import com.nbunone.app.data.AppData
import com.nbunone.app.data.Team
import com.nbunone.app.ui.Indigo
import com.nbunone.app.ui.IndigoLight
import com.nbunone.app.ui.Slate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: AppViewModel,
    data: AppData,
    onCreateTeam: () -> Unit,
    onOpenTeam: (String) -> Unit,
    onLogout: () -> Unit
) {
    val user = vm.currentUser as? CurrentUser.Student ?: return
    val myTeams = data.teams.filter { t -> t.members.any { it.name == user.name } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("내 팀", fontWeight = FontWeight.Bold)
                        Text("${user.name}님, 오늘의 활동을 기록하세요", fontSize = 12.sp, color = Slate)
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "로그아웃")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateTeam,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("팀 만들기") },
                containerColor = Indigo,
                contentColor = Color.White
            )
        }
    ) { padding ->
        if (myTeams.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Groups, contentDescription = null, tint = Slate, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(12.dp))
                Text("아직 소속된 팀이 없어요", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text("팀을 만들거나, 팀장에게 등록을 요청하세요", color = Slate, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(myTeams) { team ->
                    TeamCard(team = team, data = data, onClick = { onOpenTeam(team.id) })
                }
            }
        }
    }
}

@Composable
private fun TeamCard(team: Team, data: AppData, onClick: () -> Unit) {
    val logCount = data.logs.count { it.teamId == team.id }
    val evalDone = team.members.count { m -> data.evals.any { it.teamId == team.id && it.evaluatorId == m.id } }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
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
                Spacer(Modifier.padding(6.dp))
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
            .background(Color(0xFFF1F5F9), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 12.sp, color = Color(0xFF475569))
    }
}
