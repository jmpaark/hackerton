package com.nbunone.app.data

object Seed {

    fun build(): AppData {
        // 팀 1: 정상 + 무임승차 1명 (민준)
        val soi = Member("m-soi", "소이", "2021001", "팀장", "일정 관리, 기획, 발표자료 총괄")
        val jimin = Member("m-jimin", "지민", "2021002", "개발", "앱 개발 및 데이터 설계")
        val sihun = Member("m-sihun", "시훈", "2021003", "시연", "데모 영상 제작, QA, 발표")
        val minjun = Member("m-minjun", "민준", "2021004", "자료조사", "선행 사례 조사")
        val t1 = Team(
            "t-nbun", "N분의1", "팀플 기여도 증명 플랫폼", "무임승차 없는 공정한 팀플을 위한 앱",
            listOf(soi, jimin, sihun, minjun),
            githubUrl = "https://github.com/anthropics/anthropic-sdk-python"
        )

        // 팀 2: 기록·평가 불일치 시연용 (도윤: 기록 많음, 평가 낮음)
        val haeun = Member("m-haeun", "하은", "2020011", "팀장", "설계 및 백엔드")
        val doyun = Member("m-doyun", "도윤", "2020012", "프론트엔드", "화면 개발")
        val seoyeon = Member("m-seoyeon", "서연", "2020013", "디자인", "UI/UX 디자인, 사용자 테스트")
        val t2 = Team("t-capstone", "캡스톤A", "AI 학습 도우미", "졸업작품 프로젝트", listOf(haeun, doyun, seoyeon))

        var seq = 0
        fun id() = "log-${seq++}"

        val logs = mutableListOf<ActivityLog>()
        fun log(team: String, m: Member, date: String, cat: String, content: String, h: Float) {
            logs += ActivityLog(id(), team, m.id, date, cat, content, h)
        }

        // ---- 팀 1 로그 ----
        log(t1.id, soi, "2026-06-22", "회의", "킥오프 회의 진행, 역할 분배 확정", 2f)
        log(t1.id, soi, "2026-06-23", "문서작성", "기획서 초안 작성", 3f)
        log(t1.id, soi, "2026-06-25", "회의", "중간 점검 회의 주재, 일정 조율", 1.5f)
        log(t1.id, soi, "2026-06-27", "발표준비", "발표 자료 5장 구성안 작성", 2.5f)
        log(t1.id, soi, "2026-06-30", "문서작성", "README 및 제출 문서 정리", 2f)
        log(t1.id, jimin, "2026-06-22", "회의", "킥오프 회의 참석, 기술 스택 제안", 2f)
        log(t1.id, jimin, "2026-06-24", "개발", "데이터 모델 및 저장소 구현", 4f)
        log(t1.id, jimin, "2026-06-26", "개발", "활동 로그/동료평가 화면 개발", 5f)
        log(t1.id, jimin, "2026-06-28", "개발", "교수님 대시보드 차트 구현", 4.5f)
        log(t1.id, jimin, "2026-07-01", "개발", "AI 리포트 연동 및 버그 수정", 3.5f)
        log(t1.id, sihun, "2026-06-22", "회의", "킥오프 회의 참석", 2f)
        log(t1.id, sihun, "2026-06-26", "자료조사", "유사 서비스 벤치마킹 정리", 2f)
        log(t1.id, sihun, "2026-06-29", "발표준비", "데모 시나리오 작성 및 리허설", 3f)
        log(t1.id, sihun, "2026-07-01", "발표준비", "시연 영상 촬영 및 편집", 4f)
        log(t1.id, minjun, "2026-06-23", "자료조사", "관련 논문 링크 공유", 1f)

        // ---- 팀 2 로그 (도윤: 기록은 많지만 실속 없음) ----
        log(t2.id, haeun, "2026-06-20", "개발", "DB 설계 및 API 서버 구축", 5f)
        log(t2.id, haeun, "2026-06-24", "개발", "학습 데이터 파이프라인 구현", 4f)
        log(t2.id, haeun, "2026-06-28", "회의", "전체 회의 진행", 1.5f)
        log(t2.id, doyun, "2026-06-21", "개발", "프로젝트 세팅", 3f)
        log(t2.id, doyun, "2026-06-23", "개발", "로그인 화면 작업", 4f)
        log(t2.id, doyun, "2026-06-25", "개발", "메인 화면 작업", 4f)
        log(t2.id, doyun, "2026-06-27", "개발", "화면 다듬기", 3.5f)
        log(t2.id, doyun, "2026-06-29", "개발", "화면 다듬기 계속", 3.5f)
        log(t2.id, seoyeon, "2026-06-22", "문서작성", "디자인 시안 2종 제작", 3f)
        log(t2.id, seoyeon, "2026-06-26", "자료조사", "사용자 인터뷰 3건 진행 및 정리", 3f)

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
            Artifact("art-1", t1.id, soi.id, "발표자료_N분의1_v2.pptx", "2026-06-30", ""),
            Artifact("art-2", t1.id, sihun.id, "시연영상_최종.mp4", "2026-07-01", ""),
            Artifact("art-3", t1.id, jimin.id, "app-debug.apk", "2026-07-01", "")
        )

        return AppData(teams = listOf(t1, t2), logs = logs, evals = evals, artifacts = artifacts)
    }
}
