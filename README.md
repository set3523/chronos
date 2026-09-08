# ⏳ Chronos (크로노스)

> **Chronos**는 Kotlin과 Firebase를 기반으로 개발된 안드로이드 시간 관리 및 일정/할 일(To-Do) 애플리케이션입니다.  
> 직관적인 일정 등록, 뽀모도로 타이머를 통한 몰입 환경, 그리고 시각화된 시간 활용 통계 리포트를 바탕으로 효율적인 시간 관리를 지원합니다.

---

## 📌 주요 기능 (Key Features)

### 1. 📅 일정 및 할 일(To-Do) 관리
* **캘린더 & 타임라인 뷰:** 월별, 일별 일정을 한눈에 파악할 수 있는 직관적인 UI 제공
* **카테고리 및 색상 태그:** 업무, 공부, 개인 등 카테고리별 색상 구분 및 중요도 설정
* **반복 일정 자동화:** 매일, 매주, 매월 등 반복되는 작업 및 루틴 설정 지원

### 2. ⏱️ 시간 추적 및 뽀모도로 타이머
* **뽀모도로(Pomodoro) 타이머:** 25분 집중, 5분 휴식 세션을 통한 집중력 향상 Mode 지원
* **실시간 스톱워치:** 특정 과제 및 작업에 실제 소요된 시간을 측정하고 기록
* **백그라운드 타이머:** 앱이 백그라운드 상태이거나 화면이 꺼져도 정확한 시간 측정 및 알림 제공

### 3. 📊 시간 활용 통계 및 분석 리포트
* **주간/월간 리포트:** 어디에 시간을 많이 사용했는지 한눈에 파악할 수 있는 차트 및 그래픽 통계
* **목표 달성률:** 설정한 목표 시간 대비 실제 집중한 시간의 비율 계산

### 4. 🔥 Firebase 기반 실시간 데이터 연동
* **Firebase Authentication:** 구글 계정 및 이메일을 통한 간편한 로그인/회원가입
* **Cloud Firestore 실시간 동기화:** 여러 기기 간 실시간 일정 백업 및 클라우드 데이터 동기화
* **Firebase Cloud Messaging (FCM):** 일정 시작 전 및 목표 달성 푸시 알림 제공

---

## 🛠 기술 스택 (Tech Stack)

| 구분 | 기술 / 라이브러리 |
| :--- | :--- |
| **Language** | Kotlin |
| **Architecture** | MVVM (Model-View-ViewModel) |
| **Async Processing** | Kotlin Coroutines, Flow |
| **Android Jetpack** | ViewModel, LiveData / StateFlow, ViewBinding, Navigation |
| **Backend Services** | Firebase Authentication, Cloud Firestore, FCM |
| **Build Tool** | Gradle (Kotlin DSL - `build.gradle.kts`) |
| **Target Platform** | Android (Min SDK 24 / Target SDK 34) |

---

## 📁 프로젝트 구조 (Project Structure)

```text
chronos/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/chronos/
│   │   │   │   ├── data/          # Firebase, Repository, Local Data Source
│   │   │   │   ├── domain/        # UseCases, Data Models
│   │   │   │   └── ui/            # Activities, Fragments, ViewModels, Adapters
│   │   │   └── res/               # Layouts, Drawables, Values
│   ├── release/
│   │   └── app-release.apk        # 릴리즈 빌드 APK
│   ├── build.gradle.kts           # App 모듈 빌드 스크립트
│   └── google-services.json       # Firebase 설정 파일
└── build.gradle.kts               # Root 빌드 스크립트
