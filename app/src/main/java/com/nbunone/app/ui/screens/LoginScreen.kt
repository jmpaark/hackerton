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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    val primary = MaterialTheme.colorScheme.primary
    val context = LocalContext.current
    val canLogin = role == "professor" || name.isNotBlank()

    // 실제 로그인 — 데모 세션이면 종료(실제 데이터 복원) 후 진입
    val enter: () -> Unit = {
        AppRepository.exitDemo()
        if (role == "professor") {
            vm.login(CurrentUser.Professor); onLoggedIn(true)
        } else {
            vm.login(CurrentUser.Student(name.trim())); onLoggedIn(false)
        }
    }
    val socialLogin: (String) -> Unit = { provider ->
        Toast.makeText(context, "$provider 계정으로 로그인했어요", Toast.LENGTH_SHORT).show()
        enter()
    }
    // 데모 둘러보기 — 시드 데이터를 세션에만 로드(저장 안 됨)
    val loadDemo: () -> Unit = {
        AppRepository.enterDemo()
        if (role == "professor") {
            Toast.makeText(context, "데모 · 교수님으로 둘러보기", Toast.LENGTH_SHORT).show()
            vm.login(CurrentUser.Professor); onLoggedIn(true)
        } else {
            Toast.makeText(context, "데모 · 소이(팀장)으로 둘러보기", Toast.LENGTH_SHORT).show()
            vm.login(CurrentUser.Student("소이")); onLoggedIn(false)
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
            // 로고 — 길게 누르면 데모 둘러보기 (숨김 제스처)
            Box(
                Modifier
                    .size(84.dp)
                    .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = primary)
                    .background(
                        Brush.linearGradient(listOf(primary, primary.copy(alpha = 0.75f))),
                        RoundedCornerShape(24.dp)
                    )
                    .pointerInput(role) {
                        detectTapGestures(onLongPress = { loadDemo() })
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("1/N", color = MaterialTheme.colorScheme.onPrimary, fontSize = 26.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(14.dp))
            Text("N분의1", fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
            Spacer(Modifier.height(30.dp))

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
            Spacer(Modifier.height(16.dp))

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
                Spacer(Modifier.height(6.dp))
                Text(
                    "카카오·구글로 로그인하고, 팀에서 쓸 이름만 입력하면 시작돼요",
                    fontSize = 11.sp, color = Slate, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
            } else {
                Spacer(Modifier.height(6.dp))
            }

            // ── 간편 로그인 (카카오 / 구글) ──
            Button(
                onClick = { socialLogin("카카오") },
                enabled = canLogin,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFEE500),
                    contentColor = Color(0xFF191600),
                    disabledContainerColor = Color(0xFFFEE500).copy(alpha = 0.45f),
                    disabledContentColor = Color(0xFF191600).copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("💬", fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text("카카오로 시작하기", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { socialLogin("구글") },
                enabled = canLogin,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(
                    "G",
                    fontSize = 17.sp, fontWeight = FontWeight.Black,
                    color = if (canLogin) Color(0xFF4285F4) else Slate
                )
                Spacer(Modifier.width(8.dp))
                Text("Google로 시작하기", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(18.dp))
            Text("또는", fontSize = 11.sp, color = Slate)
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = loadDemo,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Text("🎬  데모 데이터로 둘러보기 (심사용 샘플)", fontSize = 12.5.sp)
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
