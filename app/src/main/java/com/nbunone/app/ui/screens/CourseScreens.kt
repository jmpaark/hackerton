package com.nbunone.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nbunone.app.data.AppData
import com.nbunone.app.data.AppRepository
import com.nbunone.app.data.COURSE_TYPES
import com.nbunone.app.data.Course
import com.nbunone.app.data.Milestone
import com.nbunone.app.data.MilestoneStatus
import com.nbunone.app.data.dDayLabel
import com.nbunone.app.data.milestoneStatus
import com.nbunone.app.data.milestoneTemplate
import com.nbunone.app.data.parseDateOrNull
import com.nbunone.app.ui.Amber
import com.nbunone.app.ui.Green
import com.nbunone.app.ui.Red
import com.nbunone.app.ui.Slate
import com.nbunone.app.ui.TrackBg
import java.time.LocalDate

/** 과목 개설 — 유형 선택 시 마일스톤 템플릿 자동 생성 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseCreateScreen(onBack: () -> Unit, onCreated: (courseId: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var semester by remember { mutableStateOf("2026-1학기") }
    var type by remember { mutableStateOf(COURSE_TYPES.first()) }
    val today = LocalDate.now()
    val template = milestoneTemplate(type)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("과목 개설", fontWeight = FontWeight.Bold) },
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
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("과목명") },
                placeholder = { Text("예: 캡스톤디자인, 소프트웨어공학") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = semester, onValueChange = { semester = it },
                label = { Text("학기") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            Text("과목 유형", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("유형에 맞는 마일스톤 일정이 자동으로 만들어져요", color = Slate, fontSize = 12.sp)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                COURSE_TYPES.forEach { t ->
                    FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t, fontSize = 12.sp) })
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("자동 생성될 마일스톤", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    template.forEach { (title, days) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text(title, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Text(today.plusDays(days).toString(), fontSize = 12.sp, color = Slate)
                        }
                    }
                    Text("마감일은 개설 후 과목 화면에서 수정할 수 있어요", fontSize = 11.sp, color = Slate)
                }
            }

            Button(
                onClick = {
                    val courseId = AppRepository.newId()
                    val course = Course(courseId, name.trim(), semester.trim(), type)
                    val milestones = template.map { (title, days) ->
                        Milestone(AppRepository.newId(), courseId, title, today.plusDays(days).toString())
                    }
                    AppRepository.addCourse(course, milestones)
                    onCreated(courseId)
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("과목 개설하기", fontSize = 16.sp) }
        }
    }
}

/** 과목 상세 — 마일스톤 관리 + 팀 진행 매트릭스 (교수) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    data: AppData,
    courseId: String,
    onBack: () -> Unit,
    onOpenTeam: (String) -> Unit
) {
    val course = data.courses.firstOrNull { it.id == courseId } ?: return
    val milestones = data.milestones.filter { it.courseId == courseId }.sortedBy { it.dueDate }
    val teams = data.teams.filter { it.courseId == courseId }
    val today = LocalDate.now()

    var showAdd by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newDue by remember { mutableStateOf(today.plusDays(14).toString()) }
    var editingCourse by remember { mutableStateOf(false) }
    var cName by remember(courseId) { mutableStateOf(course.name) }
    var cSem by remember(courseId) { mutableStateOf(course.semester) }
    var cType by remember(courseId) { mutableStateOf(course.type) }
    var confirmDelete by remember { mutableStateOf(false) }
    var editMsId by remember { mutableStateOf<String?>(null) }
    var emTitle by remember { mutableStateOf("") }
    var emDue by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(course.name, fontWeight = FontWeight.Bold)
                        Text("${course.semester} · ${course.type}", fontSize = 12.sp, color = Slate)
                    }
                },
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
            // 팀 × 마일스톤 진행 매트릭스
            Text("진행 현황", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (teams.isEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(
                        "아직 이 과목에 연결된 팀이 없어요. 학생이 팀 일정 탭에서 과목을 선택하면 여기에 표시됩니다.",
                        Modifier.padding(12.dp), fontSize = 13.sp, color = Slate
                    )
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.width(84.dp))
                            milestones.forEachIndexed { i, _ ->
                                Text(
                                    "${i + 1}", fontSize = 11.sp, color = Slate, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(26.dp)
                                )
                            }
                        }
                        teams.forEach { team ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    team.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(80.dp).clickable { onOpenTeam(team.id) }
                                )
                                Spacer(Modifier.width(4.dp))
                                milestones.forEach { m ->
                                    val color = when (milestoneStatus(m, team.id, data.submissions, today)) {
                                        MilestoneStatus.SUBMITTED -> Green
                                        MilestoneStatus.OVERDUE -> Red
                                        MilestoneStatus.DUE_SOON -> Amber
                                        MilestoneStatus.UPCOMING -> TrackBg
                                    }
                                    Box(
                                        Modifier.width(26.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Box(Modifier.size(16.dp).background(color, RoundedCornerShape(5.dp)))
                                    }
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(Green to "제출", Amber to "임박", Red to "지연", TrackBg to "예정").forEach { (c, l) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(9.dp).background(c, RoundedCornerShape(2.dp)))
                                    Spacer(Modifier.width(3.dp))
                                    Text(l, fontSize = 10.sp, color = Slate)
                                }
                            }
                        }
                    }
                }
            }

            // 마일스톤 목록 + 관리
            Text("마일스톤 (${milestones.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            milestones.forEachIndexed { i, m ->
                val submitted = teams.count { t -> data.submissions.any { it.milestoneId == m.id && it.teamId == t.id } }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(26.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${i + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(m.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(
                                    "마감 ${m.dueDate} (${dDayLabel(m.dueDate, today)}) · 제출 $submitted/${teams.size}팀",
                                    fontSize = 12.sp, color = Slate
                                )
                            }
                            IconButton(onClick = {
                                if (editMsId == m.id) editMsId = null
                                else { editMsId = m.id; emTitle = m.title; emDue = m.dueDate }
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "수정", tint = Slate, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { AppRepository.deleteMilestone(m.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Slate, modifier = Modifier.size(18.dp))
                            }
                        }
                        if (editMsId == m.id) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = emTitle, onValueChange = { emTitle = it },
                                label = { Text("마일스톤 제목") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = emDue, onValueChange = { emDue = it },
                                label = { Text("마감일 (yyyy-MM-dd)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                                isError = parseDateOrNull(emDue) == null
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        AppRepository.updateMilestone(m.copy(title = emTitle.trim(), dueDate = emDue.trim()))
                                        editMsId = null
                                    },
                                    enabled = emTitle.isNotBlank() && parseDateOrNull(emDue) != null,
                                    modifier = Modifier.weight(1f)
                                ) { Text("저장") }
                                OutlinedButton(onClick = { editMsId = null }, modifier = Modifier.weight(1f)) { Text("취소") }
                            }
                        }
                    }
                }
            }

            if (showAdd) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newTitle, onValueChange = { newTitle = it },
                            label = { Text("마일스톤 제목") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newDue, onValueChange = { newDue = it },
                            label = { Text("마감일 (yyyy-MM-dd)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                            isError = parseDateOrNull(newDue) == null
                        )
                        Button(
                            onClick = {
                                AppRepository.addMilestone(
                                    Milestone(AppRepository.newId(), courseId, newTitle.trim(), newDue.trim())
                                )
                                newTitle = ""; showAdd = false
                            },
                            enabled = newTitle.isNotBlank() && parseDateOrNull(newDue) != null,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("마일스톤 추가") }
                    }
                }
            } else {
                OutlinedButton(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("+ 마일스톤 추가")
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("과목 관리", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (editingCourse) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = cName, onValueChange = { cName = it },
                            label = { Text("과목명") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = cSem, onValueChange = { cSem = it },
                            label = { Text("학기") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            COURSE_TYPES.forEach { t ->
                                FilterChip(selected = cType == t, onClick = { cType = t }, label = { Text(t, fontSize = 12.sp) })
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    AppRepository.updateCourse(course.copy(name = cName.trim(), semester = cSem.trim(), type = cType))
                                    editingCourse = false
                                },
                                enabled = cName.isNotBlank(), modifier = Modifier.weight(1f)
                            ) { Text("저장") }
                            OutlinedButton(
                                onClick = { editingCourse = false; cName = course.name; cSem = course.semester; cType = course.type },
                                modifier = Modifier.weight(1f)
                            ) { Text("취소") }
                        }
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { editingCourse = true }, modifier = Modifier.weight(1f)) { Text("과목 정보 수정") }
                    OutlinedButton(
                        onClick = { confirmDelete = true }, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)
                    ) { Text("과목 삭제") }
                }
            }
            if (confirmDelete) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "이 과목을 삭제할까요? 마일스톤과 제출 기록도 함께 삭제됩니다.",
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { AppRepository.deleteCourse(courseId); onBack() },
                                colors = ButtonDefaults.buttonColors(containerColor = Red),
                                modifier = Modifier.weight(1f)
                            ) { Text("삭제") }
                            OutlinedButton(onClick = { confirmDelete = false }, modifier = Modifier.weight(1f)) { Text("취소") }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
