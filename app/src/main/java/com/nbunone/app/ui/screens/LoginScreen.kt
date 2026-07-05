package com.nbunone.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
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
    val primary = MaterialTheme.colorScheme.primary
    val context = LocalContext.current

    // 실제 로그인 — 최근 로그인에 기록 후 진입
    val enter: (Boolean, String) -> Unit = { isProfessor, studentName ->
        if (isProfessor) {
            AppRepository.pushRecentLogin("professor", "")
            vm.login(CurrentUser.Professor)
            onLoggedIn(true)
        } else {
            val n = studentName.trim()
            AppRepository.pushRecentLogin("student", n)
            vm.login(CurrentUser.Student(n))
            onLoggedIn(false)
        }
    }

    // 데모 데이터로 둘러보기 (심사·시연용 샘플)
    val loadDemo: () -> Unit = {
        AppRepository.loadSeedData()
        if (role == "professor") {
            Toast.makeText(context, "데모 · 교수님으로 입장합니다", Toast.LENGTH_SHORT).show()
            vm.login(CurrentUser.Professor)
            onLoggedIn(true)
        } else {
            Toast.makeText(context, "데모 · 소이(팀장)으로 입장합니다", Toast.LENGTH_SHORT).show()
            vm.login(CurrentUser.Student("소이"))
            onLoggedIn(false)
        }
    }

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
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 로고 — 길게 누르면 데모 데이터 로드 (숨김 제스처)
            Box(
                Modifier
                    .size(88.dp)
                    .shadow(16.dp, RoundedCornerShape(26.dp), spotColor = primary)
                    .background(
                        Brush.linearGradient(listOf(primary, primary.copy(alpha = 0.75f))),
                        RoundedCornerShape(26.dp)
                    )
                    .pointerInput(role) {
                        detectTapGestures(onLongPress = { loadDemo() })
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("1/N", color = MaterialTheme.colorScheme.onPrimary, fontSize = 28.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(16.dp))
            Text("N분의1", fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
            Spacer(Modifier.height(32.dp))

            // ── 간편 로그인 (최근 로그인 기록이 있을 때) ──
            if (data.recentLogins.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("간편 로그인", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text("탭 한 번으로 계속하기", fontSize = 11.sp, color = Slate)
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    data.recentLogins.forEach { rl ->
                        val isProf = rl.role == "professor"
                        val label = if (isProf) "교수님" else rl.name.ifBlank { "학생" }
                        val emoji = if (isProf) "👨‍🏫" else "🎓"
                        AssistChip(
                            onClick = { enter(isProf, rl.name) },
                            label = { Text("$emoji  $label", fontSize = 13.sp) },
                            shape = RoundedCornerShape(12.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
                Spacer(Modifier.height(22.dp))
                Text("또는 새로 시작", fontSize = 11.sp, color = Slate)
                Spacer(Modifier.height(12.dp))
            }

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
                    placeholder = { Text("팀에서 사용할 이름을 입력하세요") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    if (role == "professor") enter(true, "")
                    else if (name.isNotBlank()) enter(false, name)
                },
                enabled = role == "professor" || name.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Text("시작하기", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = loadDemo,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("🎬  데모 데이터로 둘러보기 (심사용 샘플)", fontSize = 13.sp)
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
