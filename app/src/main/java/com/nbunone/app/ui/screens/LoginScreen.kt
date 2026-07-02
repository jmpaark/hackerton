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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nbunone.app.AppViewModel
import com.nbunone.app.CurrentUser
import com.nbunone.app.data.AppData
import com.nbunone.app.data.AppRepository
import com.nbunone.app.ui.Slate

@Composable
fun LoginScreen(vm: AppViewModel, data: AppData, onLoggedIn: (isProfessor: Boolean) -> Unit) {
    var name by remember { mutableStateOf("") }
    val knownNames = data.teams.flatMap { t -> t.members.map { it.name } }.distinct()
    val primary = MaterialTheme.colorScheme.primary

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to MaterialTheme.colorScheme.primaryContainer,
                    0.45f to MaterialTheme.colorScheme.background,
                    1f to MaterialTheme.colorScheme.background
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 히어로 로고
            Box(
                Modifier
                    .size(96.dp)
                    .shadow(16.dp, RoundedCornerShape(28.dp), spotColor = primary)
                    .background(
                        Brush.linearGradient(listOf(primary, primary.copy(alpha = 0.75f))),
                        RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("1/N", color = MaterialTheme.colorScheme.onPrimary, fontSize = 30.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(20.dp))
            Text("N분의1", fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
            Spacer(Modifier.height(8.dp))
            Text("팀플 점수는 N분의 1로 나뉩니다.", color = Slate, fontSize = 14.sp)
            Row {
                Text("일도 ", color = Slate, fontSize = 14.sp)
                Text("N분의 1", color = primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("로 나뉘었을까요?", color = Slate, fontSize = 14.sp)
            }
            Spacer(Modifier.height(40.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("이름") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            if (knownNames.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                ) {
                    knownNames.take(4).forEach { n ->
                        AssistChip(
                            onClick = { name = n },
                            label = { Text(n) },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        vm.login(CurrentUser.Student(name.trim()))
                        onLoggedIn(false)
                    }
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Text("학생으로 시작하기", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    vm.login(CurrentUser.Professor)
                    onLoggedIn(true)
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Text("교수님으로 시작하기", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(28.dp))
            TextButton(onClick = { AppRepository.loadSeedData() }) {
                Text("데모 데이터 불러오기", color = Slate, fontSize = 13.sp)
            }
        }
    }
}
