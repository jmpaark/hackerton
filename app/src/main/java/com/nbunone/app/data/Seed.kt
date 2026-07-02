package com.nbunone.app.data

import java.time.LocalDate

object Seed {

    fun build(): AppData {
        // 날짜는 항상 '오늘' 기준 상대값 → 잔디·스트릭·건강도가 언제 시연해도 살아있음
        fun d(daysAgo: Long): String = LocalDate.now().minusDays(daysAgo).toString()

        // 과목 (미니 LMS)
        val cSw = Course("c-sweng", "소프트웨어공학", "2026-1학기", TYPE_MIDFINAL, "중간·기말 프로젝트로 성적 평가")
        val cCap = Course("c-capstone", "캡스톤디자인", "2026-1학기", TYPE_CAPSTONE, "졸업작품 프로젝트")

        val milestones = listOf(
            // 소프트웨어공학 (중간·기말)
            Milestone("ms-1", cSw.id, "팀 구성·주제 확정", d(20)),
            Milestone("ms-2", cSw.id, "제안서 제출", d(10)),
            Milestone("ms-3", cSw.id, "중간 발표 (중간고사 대체)", LocalDate.now().plusDays(4).toString()),
            Milestone("ms-4", cSw.id, "최종 발표 (기말고사 대체)", LocalDate.now().plusDays(25).toString()),
            Milestone("ms-5", cSw.id, "최종 보고서 제출", LocalDate.now().plusDays(28).toString()),
            // 캡스톤디자인 (졸업작품)
            Milestone("ms-6", cCap.id, "주제 확정", d(35)),
            Milestone("ms-7", cCap.id, "수행 계획서 제출", d(20)),
            Milestone("ms-8", cCap.id, "중간 발표", LocalDate.now().plusDays(2).toString()),
            Milestone("ms-9", cCap.id, "최종 발표", LocalDate.now().plusDays(40).toString())
        )

        // 팀 1: 정상 + 무임승차 1명 (민준)
        val soi = Member("m-soi", "소이", "2021001", "팀장", "일정 관리, 기획, 발표자료 총괄")
        val jimin = Member("m-jimin", "지민", "2021002", "개발", "앱 개발 및 데이터 설계")
        val sihun = Member("m-sihun", "시훈", "2021003", "시연", "데모 영상 제작, QA, 발표")
        val minjun = Member("m-minjun", "민준", "2021004", "자료조사", "선행 사례 조사")
        val t1 = Team(
            "t-nbun", "N분의1", "팀플 기여도 증명 플랫폼", "무임승차 없는 공정한 팀플을 위한 앱",
            listOf(soi, jimin, sihun, minjun),
            githubUrl = "https://github.com/jmpaark/hackerton",
            courseId = cSw.id
        )

        // 팀 2: 기록·평가 불일치 시연용 (도윤: 기록 많음, 평가 낮음)
        val haeun = Member("m-haeun", "하은", "2020011", "팀장", "설계 및 백엔드")
        val doyun = Member("m-doyun", "도윤", "2020012", "프론트엔드", "화면 개발")
        val seoyeon = Member("m-seoyeon", "서연", "2020013", "디자인", "UI/UX 디자인, 사용자 테스트")
        val t2 = Team(
            "t-capstone", "캡스톤A", "AI 학습 도우미", "졸업작품 프로젝트", listOf(haeun, doyun, seoyeon),
            courseId = cCap.id
        )

        val submissions = listOf(
            Submission("sub-1", "ms-1", t1.id, soi.id, d(20), "주제: 팀플 기여도 증명 플랫폼"),
            Submission("sub-2", "ms-2", t1.id, soi.id, d(10), "제안서 v1 제출", "제안서_N분의1.pdf", ""),
            Submission("sub-3", "ms-6", t2.id, haeun.id, d(35), "주제: AI 학습 도우미"),
            Submission("sub-4", "ms-7", t2.id, haeun.id, d(19), "계획서 제출 (하루 지연)", "수행계획서_캡스톤A.pdf", "")
        )

        var seq = 0
        fun id() = "log-${seq++}"

        val logs = mutableListOf<ActivityLog>()
        fun log(team: String, m: Member, daysAgo: Long, cat: String, content: String, h: Float, mood: String = "") {
            logs += ActivityLog(id(), team, m.id, d(daysAgo), cat, content, h, mood)
        }

        // ---- 팀 1 로그 ----
        log(t1.id, soi, 10, "회의", "킥오프 회의 진행, 역할 분배 확정", 2f, "🙂")
        log(t1.id, soi, 9, "문서작성", "기획서 초안 작성", 3f)
        log(t1.id, soi, 7, "회의", "중간 점검 회의 주재, 일정 조율", 1.5f, "😐")
        log(t1.id, soi, 5, "발표준비", "발표 자료 5장 구성안 작성", 2.5f, "🙂")
        log(t1.id, soi, 2, "문서작성", "README 및 제출 문서 정리", 2f, "😄")
        log(t1.id, soi, 1, "회의", "최종 점검 회의, 시연 리허설 조율", 1f, "😄")
        log(t1.id, jimin, 10, "회의", "킥오프 회의 참석, 기술 스택 제안", 2f, "🙂")
        log(t1.id, jimin, 8, "개발", "데이터 모델 및 저장소 구현", 4f, "😐")
        log(t1.id, jimin, 6, "개발", "활동 로그/동료평가 화면 개발", 5f, "😫")
        log(t1.id, jimin, 4, "개발", "교수님 대시보드 차트 구현", 4.5f, "🙂")
        log(t1.id, jimin, 1, "개발", "AI 리포트 연동 및 버그 수정", 3.5f, "😄")
        log(t1.id, sihun, 10, "회의", "킥오프 회의 참석", 2f)
        log(t1.id, sihun, 6, "자료조사", "유사 서비스 벤치마킹 정리", 2f, "🙂")
        log(t1.id, sihun, 3, "발표준비", "데모 시나리오 작성 및 리허설", 3f, "😐")
        log(t1.id, sihun, 1, "발표준비", "시연 영상 촬영 및 편집", 4f, "😄")
        log(t1.id, minjun, 9, "자료조사", "관련 논문 링크 공유", 1f, "😐")

        // ---- 팀 2 로그 (도윤: 기록은 많지만 실속 없음, 분위기 저조) ----
        log(t2.id, haeun, 12, "개발", "DB 설계 및 API 서버 구축", 5f)
        log(t2.id, haeun, 8, "개발", "학습 데이터 파이프라인 구현", 4f, "😐")
        log(t2.id, haeun, 4, "회의", "전체 회의 진행", 1.5f, "😕")
        log(t2.id, doyun, 11, "개발", "프로젝트 세팅", 3f)
        log(t2.id, doyun, 9, "개발", "로그인 화면 작업", 4f, "😕")
        log(t2.id, doyun, 7, "개발", "메인 화면 작업", 4f, "😐")
        log(t2.id, doyun, 5, "개발", "화면 다듬기", 3.5f, "😕")
        log(t2.id, doyun, 3, "개발", "화면 다듬기 계속", 3.5f, "😫")
        log(t2.id, seoyeon, 10, "문서작성", "디자인 시안 2종 제작", 3f, "🙂")
        log(t2.id, seoyeon, 6, "자료조사", "사용자 인터뷰 3건 진행 및 정리", 3f, "🙂")

        var eseq = 0
        fun eid() = "ev-${eseq++}"
        val evals = mutableListOf<PeerEval>()
        fun eval(team: String, from: Member, to: Member, c: Int, r: Int, col: Int, com: Int, comment: String = "") {
            evals += PeerEval(eid(), team, from.id, to.id, c, r, col, com, comment)
        }

        // ---- 팀 1 동료평가 ----
        eval(t1.id, soi, jimin, 5, 5, 4, 4, "개발 전부를 책임져줌. 최고의 팀원")
        eval(t1.id, soi, sihun, 4, 4, 5, 5, "데모 준비를 완벽하게 해줌")
        eval(t1.id, soi, minjun, 2, 1, 2, 2, "회의에 거의 안 나오고 연락도 잘 안 됨")
        eval(t1.id, jimin, soi, 5, 5, 5, 5, "일정 관리와 기획을 다 해줌")
        eval(t1.id, jimin, sihun, 4, 5, 4, 4, "리허설 준비 꼼꼼했음")
        eval(t1.id, jimin, minjun, 1, 2, 2, 1, "링크 하나 공유하고 끝. 참여 의지가 없음")
        eval(t1.id, sihun, soi, 5, 5, 4, 5, "팀장 역할을 확실히 해줌")
        eval(t1.id, sihun, jimin, 5, 5, 4, 4, "밤새워서 개발해줌")
        eval(t1.id, sihun, minjun, 2, 2, 1, 2, "맡은 조사도 결국 소이가 다시 함")

        // ---- 팀 2 동료평가 (도윤 불일치) ----
        eval(t2.id, haeun, doyun, 2, 3, 3, 2, "기록은 많은데 실제 완성된 화면이 거의 없음")
        eval(t2.id, haeun, seoyeon, 5, 4, 5, 5, "인터뷰까지 직접 진행. 기대 이상")
        eval(t2.id, doyun, haeun, 5, 5, 4, 4)
        eval(t2.id, doyun, seoyeon, 4, 4, 4, 4)
        eval(t2.id, seoyeon, haeun, 5, 5, 5, 4, "사실상 팀을 캐리함")
        eval(t2.id, seoyeon, doyun, 2, 2, 3, 3, "작업 시간 대비 결과물이 안 보임")

        val artifacts = listOf(
            Artifact("art-1", t1.id, soi.id, "발표자료_N분의1_v2.pptx", d(2), ""),
            Artifact("art-2", t1.id, sihun.id, "시연영상_최종.mp4", d(1), ""),
            Artifact("art-3", t1.id, jimin.id, "app-debug.apk", d(1), "")
        )

        val surveys = listOf(
            Survey(
                "sv-1", t1.id, soi.id,
                "팀플에서 무임승차를 경험한 적 있나요?",
                listOf("있다", "없다"), listOf(87, 13), d(3)
            ),
            Survey(
                "sv-2", t1.id, sihun.id,
                "팀플 기여도가 성적에 반영되어야 한다고 생각하나요?",
                listOf("매우 그렇다", "그렇다", "보통", "아니다"), listOf(52, 31, 12, 5), d(2)
            )
        )

        return AppData(
            teams = listOf(t1, t2), logs = logs, evals = evals,
            artifacts = artifacts, surveys = surveys,
            courses = listOf(cSw, cCap), milestones = milestones, submissions = submissions
        )
    }
}
