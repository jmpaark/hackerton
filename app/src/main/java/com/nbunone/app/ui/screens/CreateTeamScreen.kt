package com.nbunone.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateListOf
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
import com.nbunone.app.CurrentUser
import com.nbunone.app.data.AppRepository
import com.nbunone.app.data.Member
import com.nbunone.app.data.Team
import com.nbunone.app.ui.Slate

private data class MemberDraft(var name: String, var role: String, var resp: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTeamScreen(vm: AppViewModel, onDone: () -> Unit) {
    val me = (vm.currentUser as? CurrentUser.Student)?.name ?: ""
    var teamName by remember { mutableStateOf("") }
    var projectName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var githubUrl by remember { mutableStateOf("") }
    val members = remember {
        mutableStateListOf(
            mutableStateOf(MemberDraft(me, "팀장", ""))
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("팀 만들기", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = teamName, onValueChange = { teamName = it },
                label = { Text("팀 이름") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = projectName, onValueChange = { projectName = it },
                label = { Text("프로젝트명") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text("프로젝트 설명 (선택)") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = githubUrl, onValueChange = { githubUrl = it },
                label = { Text("GitHub 저장소 주소 (선택)") },
                placeholder = { Text("https://github.com/팀/저장소") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(4.dp))
            Text("팀원 및 역할", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("팀원별 역할과 담당 업무를 입력하세요", color = Slate, fontSize = 12.sp)

            members.forEachIndexed { idx, state ->
                val draft = state.value
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("팀원 ${idx + 1}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Spacer(Modifier.weight(1f))
                            if (idx > 0) {
                                IconButton(onClick = { members.removeAt(idx) }) {
                                    Icon(Icons.Default.Close, contentDescription = "삭제", tint = Slate)
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = draft.name,
                                onValueChange = { state.value = draft.copy(name = it) },
                                label = { Text("이름") }, singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = draft.role,
                                onValueChange = { state.value = draft.copy(role = it) },
                                label = { Text("역할") }, singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = draft.resp,
                            onValueChange = { state.value = draft.copy(resp = it) },
                            label = { Text("담당 업무") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = { members.add(mutableStateOf(MemberDraft("", "", ""))) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("+ 팀원 추가") }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val team = Team(
                        id = AppRepository.newId(),
                        name = teamName.trim(),
                        projectName = projectName.trim(),
                        description = description.trim(),
                        githubUrl = githubUrl.trim(),
                        createdByName = me,
                        members = members.map { s ->
                            Member(
                                id = AppRepository.newId(),
                                name = s.value.name.trim(),
                                role = s.value.role.trim(),
                                responsibilities = s.value.resp.trim()
                            )
                        }.filter { it.name.isNotBlank() }
                    )
                    AppRepository.addTeam(team)
                    onDone()
                },
                enabled = teamName.isNotBlank() && projectName.isNotBlank() &&
                        members.any { it.value.name.isNotBlank() },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("팀 생성 완료", fontSize = 16.sp) }
        }
    }
}
