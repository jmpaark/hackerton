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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.nbunone.app.CurrentUser
import com.nbunone.app.data.AppData
import com.nbunone.app.data.AppRepository
import com.nbunone.app.ui.Indigo
import com.nbunone.app.ui.Slate

@Composable
fun LoginScreen(vm: AppViewModel, data: AppData, onLoggedIn: (isProfessor: Boolean) -> Unit) {
    var name by remember { mutableStateOf("") }
    val knownNames = data.teams.flatMap { t -> t.members.map { it.name } }.distinct()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(88.dp)
                .background(Indigo, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("1/N", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        Text("N분의1", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("팀플 점수는 N분의 1로 나뉩니다.", color = Slate, fontSize = 14.sp)
        Text("일도 N분의 1로 나뉘었을까요?", color = Slate, fontSize = 14.sp)
        Spacer(Modifier.height(36.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("이름") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (knownNames.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                knownNames.take(4).forEach { n ->
                    AssistChip(onClick = { name = n }, label = { Text(n) })
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (name.isNotBlank()) {
                    vm.login(CurrentUser.Student(name.trim()))
                    onLoggedIn(false)
                }
            },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("학생으로 시작하기", fontSize = 16.sp)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = {
                vm.login(CurrentUser.Professor)
                onLoggedIn(true)
            },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("교수님으로 시작하기", fontSize = 16.sp)
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { AppRepository.loadSeedData() }) {
            Text("데모 데이터 불러오기", color = Slate)
        }
    }
}
