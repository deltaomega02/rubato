# Rubato — AI 여행 경로 추천 앱

**2024 LINC 3.0 캡스톤디자인 경진대회 대상** (연성대학교)

사용자의 취향 태그(미식, 출사, 가족여행 등)를 기반으로 AI가 여행 일정을 생성하고,
지도 위에 동선을 시각화해 주는 Android 앱. 5인 팀 졸업작품 (팀장: 박시우 — 기획·서버·AI 로직).

## 주요 기능
- **태그 기반 코스 생성** — 여행지·기간·테마를 선택하면 AI가 동선을 고려한 일정 생성
- **경로 시각화** — Naver Map 위에 일정 순서대로 마커·동선 표시
- **능동형 인터뷰 챗봇** — 계획이 없는 사용자에게 AI가 먼저 질문해 취향 파악
- **하이브리드 AI 구조** — 일정의 논리 구성은 GPT-4o, 실시간 정보(영업시간 등) 검색은 Gemini가 담당

## 스택
Android(Java, Retrofit) · PHP · MySQL · OpenAI GPT-4o · Google Gemini · Naver Map API

## 데모
전체 시연 영상과 기능별 데모 클립, 설치용 APK는 [Releases v1.0](https://github.com/deltaomega02/rubato/releases/tag/v1.0)에서 받을 수 있다.

## 구조
- `Rubato_Application/` — Android 앱
- `Rubato_Server/` — PHP API 서버
- `docs/` — 기능명세서, 화면설계서, ERD, 발표자료 등 캡스톤 산출문서

> API 키는 모두 placeholder(`YOUR_..._KEY`)로 대체되어 있습니다. 실행하려면 본인 키를 설정하세요.
