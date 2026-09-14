# STEP 11 — 빌드와 동작 검증

## 구현 목적

회원가입부터 식재료 관리, 레시피 추천과 채팅까지 연결되는지 확인하고, 같은 검증을 다시 실행할 수 있도록 자동 테스트와 실행 스크립트를 제공합니다.

## 검증 결과

2026-09-14~15 Windows 환경에서 최종 확인했습니다. JDK 21.0.12.1, Node.js 24.15.0, MySQL 8.0.44와 Playwright 1.63.0을 사용했습니다.

| 검사 | 결과 | 검증 범위 |
| --- | --- | --- |
| Maven `verify` | 48개 통과, JAR 생성 | 인증·재고·추천·채팅·앱 시작 |
| 프론트엔드 `npm test` | 8개 통과 | CSRF 공유·재시도, 세션 만료, 응답·네트워크 오류, 시간 제한 |
| 프론트엔드 `npm run lint` | 통과 | JavaScript·React 규칙 |
| 프론트엔드 `npm run build` | 통과 | Vite 배포용 정적 파일 생성 |
| 실제 MySQL HTTP 검증 | 통과 | 회원·세션·CSRF·CRUD·사용자 격리·한국 날짜·단위·인분·LOCAL 채팅 |
| 브라우저 E2E | 4개 통과 | 데스크톱 1440×1000, 모바일 화면 390×844에서 각각 2개 시나리오 |
| Windows DB 스크립트 | 통과 | 반복 시작·종료, 연결 설정 복구, 실행 중 포트 변경 거부, 포트 변경 후 종료, 데이터 보존 |

백엔드 자동 테스트는 격리된 H2 MySQL 모드에서 Flyway 마이그레이션과 JPA를 실행합니다. 실제 MySQL 검증은 별도로 실행한 프로젝트 전용 DB에서 수행했습니다. 모바일은 Chromium의 화면 크기·터치 에뮬레이션이며 실제 휴대폰이나 Safari 검증은 아닙니다.

생성된 JAR를 직접 실행한 서버에서도 HTTP·브라우저 테스트가 통과했습니다. DB 포트를 3307→3308→3307로 바꾸어 재시작한 뒤 저장한 식재료와 회원·레시피 수가 동일함을 확인했습니다. Windows MySQL의 감시·작업 프로세스를 구분하여 중복 실행 오판을 수정했습니다. 한글·공백 폴더에서는 NTFS 짧은 경로를 사용하며, 짧은 경로를 사용할 수 없는 경우 명확한 오류를 표시합니다.

### 백엔드 테스트

- [앱 시작](../backend/src/test/java/com/fridgegatekeeper/FridgeGatekeeperApplicationTests.java): 1개
- [회원·식재료 통합](../backend/src/test/java/com/fridgegatekeeper/user/AuthIngredientIntegrationTest.java): 10개. 세션·CSRF, 회원 중복, 비밀번호 해시, 입력 검증, 사용자별 재고 격리, 수정 버전 충돌, 실제 레시피 16개·상세·인분 계산과 HTTP 오류 상태를 확인합니다.
- [추천 계산](../backend/src/test/java/com/fridgegatekeeper/recommendation/RecommendationServiceTest.java): 14개. 한국 날짜 경계, 만료 재료 제외, 재고 합산, 동의어, 단위 환산·불일치, 수량 부족과 추천 순서를 확인합니다.
- [채팅 서비스](../backend/src/test/java/com/fridgegatekeeper/chat/ChatServiceTest.java): 7개
- [OpenAI HTTP 클라이언트](../backend/src/test/java/com/fridgegatekeeper/chat/OpenAiClientTest.java): 13개
- [채팅 통합](../backend/src/test/java/com/fridgegatekeeper/chat/ChatIntegrationTest.java): 3개

OpenAI는 localhost의 모의 HTTP 서버와 모의 시간 초과를 사용해 요청 형식, 여러 출력 항목, 오류 상태·응답 해석을 검증했습니다. **실제 OpenAI 키로 유료 API를 호출하지 않았습니다.** 키가 없는 LOCAL 모드는 실제 MySQL·브라우저에서도 확인했습니다.

### 프론트엔드와 브라우저 테스트

[API 회귀 테스트](../frontend/tests/api.test.js)는 실제 화면과 서버 사이에서 잘못된 성공 응답이나 끝없는 재시도가 발생하지 않도록 검증합니다.

[브라우저 시나리오](../e2e/tests/fridge.spec.js)는 다음 흐름을 데스크톱과 모바일 설정에서 반복합니다.

1. 회원가입 후 로그인 화면 이동, 이메일 유지, 비밀번호 재입력 로그인
2. 빈 냉장고 확인, 재료 4종 추가, 수량 수정
3. 오늘·만료 필터, 뒤로가기와 URL 동기화, 이름 검색
4. 레시피 16개 확인, 김치찌개 상세의 1kg→1,000g 보유량 및 1·2인분 필요량 확인
5. LOCAL 채팅 답변과 추천 카드 확인
6. 재료 삭제, 새로고침·로그아웃·재로그인 후 저장된 재고 확인
7. 화면 가로 넘침과 브라우저 JavaScript 오류 확인
8. 일시적 API 실패 후 재시도, 서버에서 종료한 세션의 로그인 복귀와 회원가입 화면 재진입

실제 회원·재고·레시피·채팅 API를 사용하며, 오류 복구 시나리오에서만 식재료 조회의 503을 주입합니다. 성공 화면을 캡처하여 모바일 입력창, 버튼, 재료 목록 배치도 확인합니다.

## 다시 실행하는 방법

프로젝트 루트에서 실행합니다. 기본 빌드·테스트는 MySQL 서버 없이 실행할 수 있습니다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/verify.ps1
```

실제 MySQL과 브라우저 검증은 [README](../README.md)의 DB·백엔드·프론트엔드를 먼저 실행합니다.

```powershell
node scripts/smoke-api.mjs
cd e2e
npm.cmd ci
npx.cmd playwright install chromium
npm.cmd test
```

이미 설치된 Chrome을 사용하면 설치 명령 대신 다음 환경 변수를 지정할 수 있습니다.

```powershell
$env:BROWSER_EXECUTABLE = 'C:/Program Files/Google/Chrome/Application/chrome.exe'
npm.cmd test
```

`APP_BASE_URL`은 브라우저 테스트 대상(기본 `http://127.0.0.1:5173`), `API_BASE_URL`은 HTTP 테스트 대상(기본 `http://127.0.0.1:8080`)을 변경합니다. OpenAI 키가 설정된 서버의 유료 채팅 호출은 기본적으로 생략하며 명시적으로 `LIVE_OPENAI=1`을 설정한 경우만 수행합니다.

테스트는 고유한 테스트 회원을 생성하고 생성한 식재료를 정리합니다. 테스트 회원은 남습니다. 중간에 실행을 강제 종료하면 테스트 재료도 남을 수 있으므로 개발 DB에서 실행하세요.

## 결과 파일

- 백엔드 JAR: `backend/target/fridge-gatekeeper-0.0.1-SNAPSHOT.jar`
- 백엔드 테스트 보고서: `backend/target/surefire-reports/`
- 프론트엔드 정적 파일: `frontend/dist/`
- 브라우저 화면 캡처·실패 추적: `e2e/test-results/`

빌드·테스트 산출물과 로컬 DB·JDK는 Git에 포함하지 않습니다. DB 운영 설정, HTTPS·리버스 프록시 배포, 실제 OpenAI 연결은 사용하는 환경에 맞춰 설정해야 합니다.
