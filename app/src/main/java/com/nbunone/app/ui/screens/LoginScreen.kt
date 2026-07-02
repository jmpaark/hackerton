package com.nbunone.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
    var role by remember { mutableStateOf("student") }
    val knownNames = data.teams.flatMap { t -> t.members.map { it.name } }.distinct()
    val primary = MaterialTheme.colorScheme.primary
    val context = LocalContext.current

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
            // 로고 — 길게 누르면 데모 데이터 로드 (숨김 제스처)
            Box(
                Modifier
                    .size(96.dp)
                    .shadow(16.dp, RoundedCornerShape(28.dp), spotColor = primary)
                    .background(
                        Brush.linearGradient(listOf(primary, primary.copy(alpha = 0.75f))),
                        RoundedCornerShape(28.dp)
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = {
                            AppRepository.loadSeedData()
                            Toast.makeText(context, "데모 데이터를 불러왔어요", Toast.LENGTH_SHORT).show()
                        })
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("1/N", color = MaterialTheme.colorScheme.onPrimary, fontSize = 30.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(18.dp))
            Text("N분의1", fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
            Spacer(Modifier.height(40.dp))

            // 역할 선택
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RoleCard(
                    emoji = "🎓", title = "학생",
                    description = "기여를 기록하고\n증명해요",
                    selected = role == "student",
                    modifier = Modifier.weight(1f)
                ) { role = "student" }
                RoleCard(
                    emoji = "👨‍🏫", title = "교수님",
                    description = "근거를 바탕으로\n공정하게 평가해요",
                    selected = role == "professor",
                    modifier = Modifier.weight(1f)
                ) { role = "professor" }
            }
            Spacer(Modifier.height(20.dp))

            if (role == "student") {
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
                Spacer(Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    if (role == "professor") {
                        vm.login(CurrentUser.Professor)
                        onLoggedIn(true)
                    } else if (name.isNotBlank()) {
                        vm.login(CurrentUser.Student(name.trim()))
                        onLoggedIn(false)
                    }
                },
                enabled = role == "professor" || name.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Text("시작하기", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun RoleCard(
    emoji: String,
    title: String,
    description: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    Card(
        modifier = modifier
            .then(
                if (selected) Modifier.border(2.dp, primary, RoundedCornerShape(18.dp))
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(emoji, fontSize = 26.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                title, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                color = if (selected) primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(description, fontSize = 11.sp, color = Slate, lineHeight = 15.sp)
        }
    }
}
