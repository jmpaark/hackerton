package com.nbunone.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
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
import com.nbunone.app.data.PeerEval
import com.nbunone.app.data.Team
import com.nbunone.app.data.computeInsights
import com.nbunone.app.ui.BarRow
import com.nbunone.app.ui.ChartColors
import com.nbunone.app.ui.Indigo
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
    var tab by remember { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
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
                listOf("개요", "활동", "평가", "산출물").forEachIndexed { i, label ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
                }
            }
            when (tab) {
                0 -> OverviewTab(team = team, data = data, myMemberId = myMemberId)
                1 -> LogsTab(team = team, data = data, myMemberId = myMemberId)
                2 -> EvalTab(team = team, data = data, myMemberId = myMemberId, snackbar = snackbar)
                3 -> ArtifactsTab(team = team, data = data, myMemberId = myMemberId)
            }
        }
    }
}

@Composable
private fun OverviewTab(team: Team, data: AppData, myMemberId: String?) {
    val insights = computeInsights(team, data.logs, data.evals)
    var github by remember(team.id) { mutableStateOf(team.githubUrl) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
        Text("GitHub 저장소", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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

        Spacer(Modifier.height(4.dp))
        Text("팀 설문", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        SurveySection(team = team, data = data, myMemberId = myMemberId)
        Spacer(Modifier.height(8.dp))
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

private fun shareSurvey(context: Context, teamName: String, survey: com.nbunone.app.data.Survey) {
    val text = buildString {
        appendLine("📊 [$teamName 팀] 설문 부탁드려요!")
        appendLine()
        appendLine("Q. ${survey.question}")
        survey.options.forEachIndexed { i, opt -> appendLine("${i + 1}) $opt") }
        appendLine()
        append("댓글로 번호를 남겨주세요 🙏")
    }
    runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "설문 공유"))
    }
}

@Composable
private fun LogsTab(team: Team, data: AppData, myMemberId: String?) {
    var category by remember { mutableStateOf(LOG_CATEGORIES.first()) }
    var content by remember { mutableStateOf("") }
    var hours by remember { mutableFloatStateOf(1f) }
    var mood by remember { mutableStateOf("") }
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
