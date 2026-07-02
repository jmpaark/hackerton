package com.nbunone.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.nbunone.app.data.AppData
import com.nbunone.app.data.AppRepository
import com.nbunone.app.ui.ACCENTS
import com.nbunone.app.ui.LocalDark
import com.nbunone.app.ui.Slate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: AppViewModel, data: AppData, onBack: () -> Unit) {
    var key by remember { mutableStateOf(data.apiKey) }
    var saved by remember { mutableStateOf(false) }
    val dark = LocalDark.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            // ─── 테마 ───
            Text("테마", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("모드", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("system" to "시스템", "light" to "☀️ 라이트", "dark" to "🌙 다크").forEach { (mode, label) ->
                            FilterChip(
                                selected = data.themeMode == mode,
                                onClick = { AppRepository.setTheme(mode, data.accentColor) },
                                label = { Text(label) }
                            )
                        }
                    }
                    Text("테마 색상", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate)
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        ACCENTS.forEach { accent ->
                            val selected = data.accentColor == accent.key
                            val color = if (dark) accent.dark else accent.light
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .then(
                                        if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    )
                                    .padding(if (selected) 5.dp else 0.dp)
                                    .background(color, CircleShape)
                                    .clickable { AppRepository.setTheme(data.themeMode, accent.key) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(Icons.Default.Check, contentDescription = accent.label, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                    Text(
                        ACCENTS.firstOrNull { it.key == data.accentColor }?.label ?: "",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ─── AI ───
            Spacer(Modifier.height(4.dp))
            Text("AI 리포트", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Text(
                    "Claude API 키를 입력하면 AI가 활동 로그와 동료평가를 교차검증한 리포트를 생성합니다. " +
                            "키가 없어도 룰 기반 로컬 분석 리포트를 사용할 수 있습니다.",
                    Modifier.padding(12.dp), fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            OutlinedTextField(
                value = key,
                onValueChange = { key = it; saved = false },
                label = { Text("Claude API 키 (sk-ant-...)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    AppRepository.setApiKey(key.trim())
                    saved = true
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text(if (saved) "저장됨 ✓" else "저장") }

            // ─── 데이터 ───
            Spacer(Modifier.height(4.dp))
            Text("데이터", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            OutlinedButton(onClick = { AppRepository.loadSeedData() }, modifier = Modifier.fillMaxWidth()) {
                Text("데모 데이터 불러오기 (기존 데이터 대체)")
            }
            OutlinedButton(onClick = { AppRepository.clearAll() }, modifier = Modifier.fillMaxWidth()) {
                Text("모든 데이터 초기화")
            }
            Text("초기화해도 API 키와 테마 설정은 유지됩니다", color = Slate, fontSize = 12.sp)

            Spacer(Modifier.height(20.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("N분의1 v1.0", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate)
                Text("팀플 기여도 증명 플랫폼 · 제1회 SW 해커톤", fontSize = 11.sp, color = Slate)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
