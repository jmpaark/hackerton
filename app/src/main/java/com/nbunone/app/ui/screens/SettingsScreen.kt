package com.nbunone.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nbunone.app.AppViewModel
import com.nbunone.app.data.AppData
import com.nbunone.app.data.AppRepository
import com.nbunone.app.ui.IndigoLight
import com.nbunone.app.ui.Slate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: AppViewModel, data: AppData, onBack: () -> Unit) {
    var key by remember { mutableStateOf(data.apiKey) }
    var saved by remember { mutableStateOf(false) }

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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("AI 리포트 설정", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Card(colors = CardDefaults.cardColors(containerColor = IndigoLight)) {
                Text(
                    "Claude API 키를 입력하면 AI가 활동 로그와 동료평가를 교차검증한 리포트를 생성합니다. " +
                            "키가 없어도 룰 기반 로컬 분석 리포트를 사용할 수 있습니다.",
                    Modifier.padding(12.dp), fontSize = 13.sp
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

            Spacer(Modifier.height(16.dp))
            Text("데모 데이터", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            OutlinedButton(onClick = { AppRepository.loadSeedData() }, modifier = Modifier.fillMaxWidth()) {
                Text("데모 데이터 불러오기 (기존 데이터 대체)")
            }
            OutlinedButton(onClick = { AppRepository.clearAll() }, modifier = Modifier.fillMaxWidth()) {
                Text("모든 데이터 초기화")
            }
            Text("초기화해도 API 키는 유지됩니다", color = Slate, fontSize = 12.sp)
        }
    }
}
