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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import com.nbunone.app.AppViewModel
import com.nbunone.app.data.AppData
import com.nbunone.app.pdf.PdfExporter
import com.nbunone.app.ui.Indigo
import com.nbunone.app.ui.IndigoLight
import com.nbunone.app.ui.Slate
import com.nbunone.app.ui.warnBadgeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(vm: AppViewModel, data: AppData, teamId: String, onBack: () -> Unit) {
    val team = data.teams.firstOrNull { it.id == teamId } ?: return
    val report = data.reports.firstOrNull { it.teamId == teamId }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI 기여도 리포트", fontWeight = FontWeight.Bold)
                        Text(team.name, fontSize = 12.sp, color = Slate)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    if (report != null && !vm.reportLoading) {
                        IconButton(onClick = { vm.generateReport(teamId) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "다시 생성")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            vm.reportLoading -> {
                Column(
                    Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("활동 로그와 동료평가를 교차검증하는 중...", color = Slate, fontSize = 14.sp)
                }
            }
            report == null -> {
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Indigo, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("아직 생성된 리포트가 없습니다", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(
                        "활동 로그·동료평가·역할 데이터를 AI가 교차검증하여\n교수님용 기여도 리포트를 생성합니다",
                        color = Slate, fontSize = 13.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { vm.generateReport(teamId) },
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("리포트 생성하기", fontSize = 16.sp)
                    }
                    vm.reportError?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                }
            }
            else -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .background(if (report.isAi) IndigoLight else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                if (report.isAi) "✨ AI 생성" else "📊 로컬 분석",
                                fontSize = 12.sp,
                                color = if (report.isAi) Indigo else Slate,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(report.generatedAt, fontSize = 12.sp, color = Slate)
                    }
                    Spacer(Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            MarkdownLite(report.content)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                val file = PdfExporter.export(context, team.name, report.generatedAt, report.content)
                                PdfExporter.share(context, file)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("PDF로 저장·공유")
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

/** 간단한 마크다운 렌더러 — 헤딩/불릿/인용만 지원 */
@Composable
fun MarkdownLite(text: String) {
    text.lines().forEach { raw ->
        val line = raw.trimEnd()
        when {
            line.startsWith("### ") -> Text(
                line.removePrefix("### "),
                fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
            )
            line.startsWith("## ") -> Text(
                line.removePrefix("## "),
                fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Indigo,
                modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
            )
            line.startsWith("# ") -> Text(
                line.removePrefix("# "),
                fontWeight = FontWeight.Bold, fontSize = 19.sp,
                modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
            )
            line.startsWith("> ") -> {
                val (nbg, nfg) = warnBadgeColors()
                Text(
                    line.removePrefix("> "),
                    fontSize = 13.sp, color = nfg,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .background(nbg, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                )
            }
            line.startsWith("- ") -> Row(Modifier.padding(vertical = 1.dp)) {
                Text("•  ", fontSize = 14.sp, color = Slate)
                Text(line.removePrefix("- ").replace("**", ""), fontSize = 14.sp)
            }
            line.isBlank() -> Spacer(Modifier.height(4.dp))
            else -> Text(line.replace("**", ""), fontSize = 14.sp, modifier = Modifier.padding(vertical = 1.dp))
        }
    }
}
