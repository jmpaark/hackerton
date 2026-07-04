package com.nbunone.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nbunone.app.AppViewModel
import com.nbunone.app.CurrentUser
import com.nbunone.app.data.ActivityLog
import com.nbunone.app.data.AppData
import com.nbunone.app.data.AppRepository
import com.nbunone.app.data.Artifact
import com.nbunone.app.data.LOG_CATEGORIES
import com.nbunone.app.data.MOODS
import com.nbunone.app.data.MilestoneStatus
import com.nbunone.app.data.PeerEval
import com.nbunone.app.data.Submission
import com.nbunone.app.data.Team
import com.nbunone.app.data.computeInsights
import com.nbunone.app.data.dDayLabel
import com.nbunone.app.data.milestoneStatus
import com.nbunone.app.ui.Amber
import com.nbunone.app.ui.BarRow
import com.nbunone.app.ui.ChartColors
import com.nbunone.app.ui.Green
import com.nbunone.app.ui.Indigo
import com.nbunone.app.ui.Red
import com.nbunone.app.ui.Slate
import com.nbunone.app.ui.successColors
import com.nbunone.app.ui.trim
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDetailScreen(vm: AppViewModel, data: AppData, teamId: String, initialTab: Int = 0, onBack: () -> Unit) {
    val team = data.teams.firstOrNull { it.id == teamId } ?: return
    val user = vm.currentUser as? CurrentUser.Student ?: return
    val myMemberId = team.members.firstOrNull { it.name == user.name }?.id
    var tab by remember { mutableIntStateOf(initialTab.coerceIn(0, 4)) }
    val snackbar = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(team.name, fontWeight = FontWeight.Bold)
                        Text(team.projectName, fontSize = 12.sp, color = Slate)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                listOf("개요", "일정", "활동", "평가", "산출물").forEachIndexed { i, label ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label, fontSize = 13.sp) })
                }
            }
            when (tab) {
                0 -> OverviewTab(team = team, data = data, myMemberId = myMemberId, onGoToTab = { tab = it })
                1 -> MilestonesTab(team = team, data = data, myMemberId = myMemberId)
                2 -> LogsTab(team = team, data = data, myMemberId = myMemberId, snackbar = snackbar)
                3 -> EvalTab(team = team, data = data, myMemberId = myMemberId, snackbar = snackbar)
                4 -> ArtifactsTab(team = team, data = data, myMemberId = myMemberId)
            }
        }
    }
}

@Composable
private fun OverviewTab(team: Team, data: AppData, myMemberId: String?, onGoToTab: (Int) -> Unit) {
    val insights = computeInsights(team, data.logs, data.evals)
    var github by remember(team.id) { mutableStateOf(team.githubUrl) }
    var showAddMember by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newRole by remember { mutableStateOf("") }
    var showGithub by remember { mutableStateOf(false) }
    var showSurvey by remember { mutableStateOf(false) }

    // 다음 할 일 계산
    val myLogCount = if (myMemberId != null) data.logs.count { it.teamId == team.id && it.memberId == myMemberId } else 0
    val myEvalDone = if (myMemberId != null) data.evals.any { it.teamId == team.id && it.evaluatorId == myMemberId } else true
    val nextStep: Triple<String, String, Int>? = when {
        myMemberId == null -> null
        myLogCount == 0 -> Triple("첫 활동을 기록해보세요 ✍️", "오늘 한 일을 남기는 것이 증명의 시작이에요", 2)
        !myEvalDone && team.members.size > 1 -> Triple("동료평가를 제출해주세요 🤝", "팀원의 기여를 익명으로 평가할 수 있어요", 3)
        else -> null
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 다음 할 일 안내
        nextStep?.let { (title, desc, targetTab) ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onGoToTab(targetTab) }, shape = RoundedCornerShape(12.dp)) {
                        Text("바로 가기", fontSize = 13.sp)
                    }
                }
            }
        }

        if (team.description.isNotBlank()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Text(
                    team.description, Modifier.padding(12.dp), fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Text("팀원 및 역할", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        team.members.forEachIndexed { i, m ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).background(ChartColors[i % ChartColors.size].copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(m.name.take(1), color = ChartColors[i % ChartColors.size], fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(m.name, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(6.dp))
                            if (m.role.isNotBlank()) InfoChip(m.role)
                        }
                        if (m.responsibilities.isNotBlank()) {
                            Text(m.responsibilities, fontSize = 12.sp, color = Slate)
                        }
                    }
                }
            }
        }

        // 팀원 추가 (팀원만 가능)
        if (myMemberId != null) {
            if (showAddMember) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "팀원이 로그인할 때 쓸 이름과 똑같이 입력해야 팀원 홈에 이 팀이 보여요",
                            fontSize = 11.sp, color = Slate
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newName, onValueChange = { newName = it },
                                label = { Text("이름") }, singleLine = true, modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newRole, onValueChange = { newRole = it },
                                label = { Text("역할") }, singleLine = true, modifier = Modifier.weight(1f)
                            )
                        }
                        Button(
                            onClick = {
                                AppRepository.addMember(
                                    team.id,
                                    com.nbunone.app.data.Member(
                                        id = AppRepository.newId(),
                                        name = newName.trim(),
                                        role = newRole.trim()
                                    )
                                )
                                newName = ""; newRole = ""; showAddMember = false
                            },
                            enabled = newName.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("팀원 추가하기") }
                    }
                }
            } else {
                OutlinedButton(onClick = { showAddMember = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("+ 팀원 추가")
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text("활동 현황", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                val maxH = insights.stats.maxOfOrNull { it.totalHours } ?: 0f
                insights.stats.forEachIndexed { i, s ->
                    BarRow(
                        label = "${s.member.name} (${s.logCount}건)",
                        value = s.totalHours,
                        max = maxH,
                        color = ChartColors[i % ChartColors.size],
                        valueText = "${s.totalHours.trim()}h"
                    )
                }
                if (insights.totalLogs == 0) {
                    Text("아직 활동 기록이 없습니다", color = Slate, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        CollapsibleHeader(
            title = "GitHub 저장소",
            subtitle = if (team.githubUrl.isBlank()) "등록하면 커밋 기여 분석 가능" else "등록됨 ✓",
            expanded = showGithub
        ) { showGithub = !showGithub }
        if (showGithub) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("저장소를 등록하면 교수님 화면에서 커밋 기여 분석을 볼 수 있어요", fontSize = 12.sp, color = Slate)
                    OutlinedTextField(
                        value = github,
                        onValueChange = { github = it },
                        label = { Text("https://github.com/...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(onClick = { AppRepository.setGithubUrl(team.id, github.trim()) }) {
                        Text(if (team.githubUrl == github.trim() && github.isNotBlank()) "저장됨 ✓" else "저장")
                    }
                }
            }
        }

        val surveyCount = data.surveys.count { it.teamId == team.id }
        CollapsibleHeader(
            title = "팀 설문",
            subtitle = if (surveyCount > 0) "${surveyCount}건 · 에브리타임 공유 가능" else "설문 만들고 커뮤니티에 공유",
            expanded = showSurvey
        ) { showSurvey = !showSurvey }
        if (showSurvey) {
            SurveySection(team = team, data = data, myMemberId = myMemberId)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CollapsibleHeader(title: String, subtitle: String, expanded: Boolean, onToggle: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(subtitle, fontSize = 11.sp, color = Slate)
            }
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null, tint = Slate
            )
        }
    }
}

@Composable
private fun SurveySection(team: Team, data: AppData, myMemberId: String?) {
    val context = LocalContext.current
    val surveys = data.surveys.filter { it.teamId == team.id }.sortedByDescending { it.date }
    var question by remember { mutableStateOf("") }
    var optionsText by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Text(
                "설문을 만들어 에브리타임 등 커뮤니티에 공유하고, 응답 결과를 집계해 프로젝트 근거 데이터로 쓰세요",
                Modifier.padding(12.dp), fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        surveys.forEach { survey ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text("Q. ${survey.question}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("응답 ${survey.total}건 · ${survey.date}", fontSize = 11.sp, color = Slate)
                        }
                        if (survey.createdBy == myMemberId) {
                            IconButton(onClick = { AppRepository.deleteSurvey(survey.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Slate, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    val maxCount = (survey.counts.maxOrNull() ?: 1).coerceAtLeast(1)
                    survey.options.forEachIndexed { i, opt ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                BarRow(
                                    label = opt,
                                    value = survey.counts[i].toFloat(),
                                    max = maxCount.toFloat(),
                                    color = ChartColors[i % ChartColors.size],
                                    valueText = "${survey.counts[i]}"
                                )
                            }
                            if (myMemberId != null) {
                                IconButton(
                                    onClick = { AppRepository.adjustSurveyCount(survey.id, i, +1) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "+1", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    OutlinedButton(onClick = { shareSurvey(context, team.name, survey) }) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("에브리타임에 공유", fontSize = 13.sp)
                    }
                }
            }
        }

        if (showForm) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = question, onValueChange = { question = it },
                        label = { Text("질문") }, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = optionsText, onValueChange = { optionsText = it },
                        label = { Text("선택지 (쉼표로 구분)") },
                        placeholder = { Text("예: 있다, 없다") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val opts = optionsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            if (myMemberId != null && question.isNotBlank() && opts.size >= 2) {
                                AppRepository.addSurvey(
                                    com.nbunone.app.data.Survey(
                                        AppRepository.newId(), team.id, myMemberId,
                                        question.trim(), opts, List(opts.size) { 0 }, today()
                                    )
                                )
                                question = ""; optionsText = ""; showForm = false
                            }
                        },
                        enabled = myMemberId != null && question.isNotBlank() &&
                                optionsText.split(",").count { it.isNotBlank() } >= 2,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("설문 만들기") }
                }
            }
        } else {
            OutlinedButton(onClick = { showForm = true }, modifier = Modifier.fillMaxWidth()) {
                Text("+ 새 설문 만들기")
            }
        }
    }
}

private const val EVERYTIME_PACKAGE = "com.everytime.v2"

/**
 * 에브리타임 실제 연동.
 * 에타는 공개 API가 없으므로 (1) 설문 내용을 클립보드에 복사하고
 * (2) 에타 앱이 설치돼 있으면 앱으로 바로 공유/실행, (3) 없으면 스토어/일반 공유로 폴백한다.
 */
private fun shareSurvey(context: Context, teamName: String, survey: com.nbunone.app.data.Survey) {
    val text = buildString {
        appendLine("📊 [$teamName 팀] 설문 부탁드려요!")
        appendLine()
        appendLine("Q. ${survey.question}")
        survey.options.forEachIndexed { i, opt -> appendLine("${i + 1}) $opt") }
        appendLine()
        append("댓글로 번호를 남겨주세요 🙏 (N분의1 앱)")
    }

    // 1) 클립보드 복사 — 에타 글쓰기 창에 바로 붙여넣을 수 있게
    runCatching {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("N분의1 설문", text))
    }

    val pm = context.packageManager
    val everytimeLaunch = pm.getLaunchIntentForPackage(EVERYTIME_PACKAGE)

    // 2) 에브리타임 설치됨 → 앱으로 직접 공유 시도 (실패 시 앱 실행)
    if (everytimeLaunch != null) {
        val sendToEverytime = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage(EVERYTIME_PACKAGE)
        }
        val ok = runCatching { context.startActivity(sendToEverytime); true }.getOrElse {
            // 에타가 공유(SEND)를 안 받으면 앱만 열고 붙여넣기 안내
            runCatching { context.startActivity(everytimeLaunch); true }.getOrDefault(false)
        }
        if (ok) {
            Toast.makeText(context, "설문 내용을 복사했어요 · 에브리타임에서 붙여넣기 하세요", Toast.LENGTH_LONG).show()
            return
        }
    }

    // 3) 에타 미설치 → 스토어로 유도 (설치 후 붙여넣기)
    val store = runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$EVERYTIME_PACKAGE"))
        ); true
    }.getOrElse {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$EVERYTIME_PACKAGE"))
            ); true
        }.getOrDefault(false)
    }
    if (store) {
        Toast.makeText(context, "에브리타임이 없어요 · 설치 후 붙여넣으면 돼요 (내용 복사됨)", Toast.LENGTH_LONG).show()
        return
    }

    // 4) 최후 폴백 — 일반 공유 시트 (카톡·문자 등)
    runCatching {
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) },
                "설문 공유"
            )
        )
    }
}

@Composable
private fun LogsTab(team: Team, data: AppData, myMemberId: String?, snackbar: SnackbarHostState) {
    var category by remember { mutableStateOf(LOG_CATEGORIES.first()) }
    var content by remember { mutableStateOf("") }
    var hours by remember { mutableFloatStateOf(1f) }
    var mood by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val logs = data.logs.filter { it.teamId == team.id }.sortedByDescending { it.date }
    val memberById = team.members.associateBy { it.id }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("오늘 한 일 기록하기", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        LOG_CATEGORIES.take(4).forEach { c ->
                            FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c, fontSize = 12.sp) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LOG_CATEGORIES.drop(4).forEach { c ->
                            FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c, fontSize = 12.sp) })
                        }
                    }
                    OutlinedTextField(
                        value = content, onValueChange = { content = it },
                        label = { Text("무엇을 했나요?") }, modifier = Modifier.fillMaxWidth()
                    )
                    Column {
                        Text("소요 시간: ${hours.trim()}시간", fontSize = 13.sp)
                        Slider(value = hours, onValueChange = { hours = (it * 2).roundToInt() / 2f }, valueRange = 0.5f..8f)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("오늘 팀플 기분", fontSize = 13.sp)
                        Spacer(Modifier.width(10.dp))
                        MOODS.forEach { m ->
                            val selected = mood == m
                            Text(
                                m, fontSize = if (selected) 24.sp else 19.sp,
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { mood = if (selected) "" else m }
                                    .padding(4.dp)
                            )
                        }
                    }
                    Button(
                        onClick = {
                            if (myMemberId != null && content.isNotBlank()) {
                                AppRepository.addLog(
                                    ActivityLog(AppRepository.newId(), team.id, myMemberId, today(), category, content.trim(), hours, mood)
                                )
                                content = ""
                                hours = 1f
                                mood = ""
                                scope.launch { snackbar.showSnackbar("기록 완료! 오늘도 증명 +1 🌱") }
                            }
                        },
                        enabled = myMemberId != null && content.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("기록 남기기") }
                    if (myMemberId == null) {
                        Text("이 팀의 팀원이 아니라서 기록할 수 없어요", color = Slate, fontSize = 12.sp)
                    }
                }
            }
        }
        items(logs) { log ->
            val member = memberById[log.memberId]
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(member?.name ?: "?", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            InfoChip(log.category)
                            Text("${log.hours.trim()}h", fontSize = 12.sp, color = Indigo, fontWeight = FontWeight.SemiBold)
                            if (log.mood.isNotBlank()) Text(log.mood, fontSize = 14.sp)
                        }
                        Text(log.content, fontSize = 13.sp)
                        Text(log.date, fontSize = 11.sp, color = Slate)
                    }
                    if (log.memberId == myMemberId) {
                        IconButton(onClick = { AppRepository.deleteLog(log.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Slate, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EvalTab(
    team: Team,
    data: AppData,
    myMemberId: String?,
    snackbar: SnackbarHostState
) {
    if (myMemberId == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("이 팀의 팀원만 평가할 수 있어요", color = Slate)
        }
        return
    }
    val targets = team.members.filter { it.id != myMemberId }
    val alreadySubmitted = data.evals.any { it.teamId == team.id && it.evaluatorId == myMemberId }
    val scores = remember(team.id, myMemberId) {
        mutableStateMapOf<String, IntArray>().apply {
            targets.forEach { t ->
                val prev = data.evals.firstOrNull {
                    it.teamId == team.id && it.evaluatorId == myMemberId && it.targetId == t.id
                }
                put(t.id, prev?.let { intArrayOf(it.contribution, it.responsibility, it.collaboration, it.communication) }
                    ?: intArrayOf(3, 3, 3, 3))
            }
        }
    }
    val comments = remember(team.id, myMemberId) {
        mutableStateMapOf<String, String>().apply {
            targets.forEach { t ->
                val prev = data.evals.firstOrNull {
                    it.teamId == team.id && it.evaluatorId == myMemberId && it.targetId == t.id
                }
                put(t.id, prev?.comment ?: "")
            }
        }
    }
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableIntStateOf(0) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (alreadySubmitted) {
            val (bg, fg) = successColors()
            Card(colors = CardDefaults.cardColors(containerColor = bg)) {
                Text(
                    "동료평가를 제출했어요. 수정 후 다시 제출할 수 있습니다.",
                    Modifier.padding(12.dp), fontSize = 13.sp, color = fg
                )
            }
        } else {
            Text("팀원의 기여를 솔직하게 평가해주세요. 평가는 익명으로 집계됩니다.", color = Slate, fontSize = 13.sp)
        }

        val itemLabels = listOf("기여도", "책임감", "협업", "소통")
        targets.forEach { t ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(t.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.width(6.dp))
                        if (t.role.isNotBlank()) InfoChip(t.role)
                    }
                    refresh // recompose trigger
                    itemLabels.forEachIndexed { idx, label ->
                        val arr = scores[t.id]!!
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(label, fontSize = 13.sp, modifier = Modifier.width(52.dp))
                            Slider(
                                value = arr[idx].toFloat(),
                                onValueChange = {
                                    arr[idx] = it.toInt().coerceIn(1, 5)
                                    refresh++
                                },
                                valueRange = 1f..5f,
                                steps = 3,
                                modifier = Modifier.weight(1f)
                            )
                            Text("${arr[idx]}점", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(32.dp))
                        }
                    }
                    OutlinedTextField(
                        value = comments[t.id] ?: "",
                        onValueChange = { comments[t.id] = it },
                        label = { Text("한 줄 코멘트 (선택)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Button(
            onClick = {
                val evals = targets.map { t ->
                    val arr = scores[t.id]!!
                    PeerEval(
                        id = AppRepository.newId(),
                        teamId = team.id,
                        evaluatorId = myMemberId,
                        targetId = t.id,
                        contribution = arr[0],
                        responsibility = arr[1],
                        collaboration = arr[2],
                        communication = arr[3],
                        comment = comments[t.id]?.trim() ?: ""
                    )
                }
                AppRepository.submitEvals(team.id, myMemberId, evals)
                scope.launch { snackbar.showSnackbar("동료평가가 제출되었습니다") }
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(if (alreadySubmitted) "다시 제출하기" else "평가 제출하기", fontSize = 16.sp) }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ArtifactsTab(team: Team, data: AppData, myMemberId: String?) {
    val context = LocalContext.current
    val memberById = team.members.associateBy { it.id }
    val artifacts = data.artifacts.filter { it.teamId == team.id }.sortedByDescending { it.date }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null && myMemberId != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            AppRepository.addArtifact(
                Artifact(
                    id = AppRepository.newId(),
                    teamId = team.id,
                    memberId = myMemberId,
                    name = displayName(context, uri),
                    date = today(),
                    uri = uri.toString()
                )
            )
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("산출물 업로드", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("보고서, 발표자료, 코드 압축본 등 결과물을 첨부해 기여의 물증을 남기세요", fontSize = 12.sp, color = Slate)
                    Button(
                        onClick = { launcher.launch(arrayOf("*/*")) },
                        enabled = myMemberId != null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("파일 선택하기")
                    }
                }
            }
        }
        if (artifacts.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Text("아직 등록된 산출물이 없어요", color = Slate, fontSize = 13.sp)
                }
            }
        }
        items(artifacts) { a ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { openArtifact(context, a) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Description, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(a.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                        Text(
                            "${memberById[a.memberId]?.name ?: "?"} · ${a.date}",
                            fontSize = 12.sp, color = Slate
                        )
                    }
                    if (a.memberId == myMemberId) {
                        IconButton(onClick = { AppRepository.deleteArtifact(a.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Slate, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MilestonesTab(team: Team, data: AppData, myMemberId: String?) {
    val context = LocalContext.current
    val today = java.time.LocalDate.now()
    val course = data.courses.firstOrNull { it.id == team.courseId }

    // 과목 미연결 → 과목 선택
    if (course == null) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Text(
                    "과목에 연결하면 마일스톤 일정(제안서·중간발표·최종발표 등)이 표시되고, 교수님이 진행 현황을 볼 수 있어요",
                    Modifier.padding(12.dp), fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            if (data.courses.isEmpty()) {
                Text("아직 개설된 과목이 없어요. 교수님이 과목을 개설하면 여기서 선택할 수 있습니다.", color = Slate, fontSize = 13.sp)
            } else {
                Text("과목 선택", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                data.courses.forEach { c ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (myMemberId != null) AppRepository.setTeamCourse(team.id, c.id)
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(c.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text("${c.semester} · ${c.type}", fontSize = 12.sp, color = Slate)
                            }
                            InfoChip("선택")
                        }
                    }
                }
            }
        }
        return
    }

    val milestones = data.milestones.filter { it.courseId == course.id }
        .sortedBy { it.dueDate }
    var openFormFor by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }
    var attachedName by remember { mutableStateOf("") }
    var attachedUri by remember { mutableStateOf("") }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            attachedName = displayName(context, uri)
            attachedUri = uri.toString()
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(course.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("${course.semester} · ${course.type}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                }
                val done = milestones.count { milestoneStatus(it, team.id, data.submissions, today) == MilestoneStatus.SUBMITTED }
                Text("$done/${milestones.size} 완료", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        milestones.forEach { m ->
            val status = milestoneStatus(m, team.id, data.submissions, today)
            val submission = data.submissions.firstOrNull { it.milestoneId == m.id && it.teamId == team.id }
            val (statusColor, statusLabel) = when (status) {
                MilestoneStatus.SUBMITTED -> Green to "제출 완료"
                MilestoneStatus.OVERDUE -> Red to "기한 지남"
                MilestoneStatus.DUE_SOON -> Amber to "마감 임박"
                MilestoneStatus.UPCOMING -> MaterialTheme.colorScheme.onSurfaceVariant to "예정"
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(statusColor, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(m.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("마감 ${m.dueDate} · $statusLabel", fontSize = 12.sp, color = Slate)
                        }
                        if (status != MilestoneStatus.SUBMITTED) {
                            Box(
                                Modifier.background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(dDayLabel(m.dueDate, today), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = statusColor)
                            }
                        }
                    }

                    if (submission != null) {
                        Column(
                            Modifier.fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            val submitter = team.members.firstOrNull { it.id == submission.memberId }?.name ?: "?"
                            Text("✓ $submitter · ${submission.date} 제출", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Green)
                            if (submission.note.isNotBlank()) Text(submission.note, fontSize = 12.sp)
                            if (submission.artifactName.isNotBlank()) {
                                Text("📎 ${submission.artifactName}", fontSize = 12.sp, color = Indigo)
                            }
                            if (myMemberId != null) {
                                Text(
                                    "제출 취소",
                                    fontSize = 11.sp, color = Slate,
                                    modifier = Modifier.padding(top = 4.dp).clickable { AppRepository.deleteSubmission(submission.id) }
                                )
                            }
                        }
                    } else if (myMemberId != null) {
                        if (openFormFor == m.id) {
                            OutlinedTextField(
                                value = note, onValueChange = { note = it },
                                label = { Text("제출 메모 (선택)") }, modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(onClick = { filePicker.launch(arrayOf("*/*")) }) {
                                    Text(if (attachedName.isBlank()) "📎 파일 첨부" else "📎 ${attachedName.take(14)}…", fontSize = 12.sp)
                                }
                                Button(onClick = {
                                    AppRepository.addSubmission(
                                        Submission(
                                            AppRepository.newId(), m.id, team.id, myMemberId,
                                            today(), note.trim(), attachedName, attachedUri
                                        )
                                    )
                                    if (attachedName.isNotBlank()) {
                                        AppRepository.addArtifact(
                                            Artifact(AppRepository.newId(), team.id, myMemberId, attachedName, today(), attachedUri)
                                        )
                                    }
                                    note = ""; attachedName = ""; attachedUri = ""; openFormFor = null
                                }) { Text("제출") }
                            }
                        } else {
                            OutlinedButton(onClick = { openFormFor = m.id; note = ""; attachedName = ""; attachedUri = "" }) {
                                Text("제출하기", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
        if (milestones.isEmpty()) {
            Text("아직 등록된 마일스톤이 없어요. 교수님이 일정을 등록하면 표시됩니다.", color = Slate, fontSize = 13.sp)
        }
        Spacer(Modifier.height(8.dp))
    }
}

private fun displayName(context: Context, uri: Uri): String = runCatching {
    context.contentResolver.query(uri, null, null, null, null)?.use { c ->
        val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (c.moveToFirst() && i >= 0) c.getString(i) else null
    }
}.getOrNull() ?: (uri.lastPathSegment ?: "파일")

private fun openArtifact(context: Context, artifact: Artifact) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(artifact.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}
