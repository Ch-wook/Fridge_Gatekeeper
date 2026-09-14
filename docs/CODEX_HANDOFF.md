# Codex 인수인계 — 냉장고 지킴이

> 작성: 2026-09-15. 구현 기준 커밋: `21bd1e19803a272460b4c9741559bd7f9e393244`.
> 이 문서는 이전 대화 없이 다음 Codex가 작업을 이어가기 위한 기록입니다. 코드와 환경이 이후 바뀌었다면 현재 코드·Git 상태·사용자의 최신 요청을 우선하세요.

## 1. 현재 상태와 먼저 읽을 문서

사용자는 중단된 냉장고 식재료 관리·레시피 서비스를 README 기준으로 완성해 달라고 요청했습니다. 요청에 적힌 `RENAMD.md`는 실제 저장소의 [README.md](../README.md)를 뜻하는 것으로 해석해 작업했습니다.

README의 MVP 기능 구현, 누락된 채팅 서버, 프론트엔드 보완, Windows 실행 환경, 자동·실서버 검증과 단계별 문서 정리를 완료했습니다. 구현 변경은 GitHub `main`에 푸시했습니다. 배포 서버 구축이나 실제 OpenAI 유료 호출까지 완료했다는 의미는 아닙니다.

| 항목 | 기준 |
| --- | --- |
| 저장소 | [Ch-wook/Fridge_Gatekeeper](https://github.com/Ch-wook/Fridge_Gatekeeper) |
| 작업 브랜치 | `main` |
| 최초 코드 | `9fe7f43` — `first commit` |
| 구현 완료 커밋 | [21bd1e1](https://github.com/Ch-wook/Fridge_Gatekeeper/commit/21bd1e19803a272460b4c9741559bd7f9e393244) — `feat: complete fridge management and recipe service` |
| 해당 커밋 변경 | 39개 파일, 1,930줄 추가·89줄 삭제 |
| 이 문서의 범위 | 위 구현 작업, 실행 환경, 시행착오, 검증 결과와 후속 작업 기준 |

읽는 순서는 이 문서 → [실행 안내](../README.md) → [API 계약](api.md) → 작업할 기능의 실제 코드입니다. [DB 설계](../database/README.md), STEP 1~11도 참고하세요. STEP 1 등 초기 문서는 당시 구현 기록이므로 현재 실행 방법은 README를 사용합니다.

## 2. 시작할 때의 실제 상태와 수행한 작업

처음부터 새로 만든 프로젝트가 아닙니다. 최초 커밋에 인증·재료 CRUD·유통기한·추천 알고리즘·레시피 seed·React 화면이 상당 부분 구현되어 있었습니다. 반면 README에 설명된 채팅 서버, STEP 8~11 문서, 브라우저 테스트가 빠져 있었습니다. README에 적힌 로컬 JDK·MySQL 준비 상태도 실제 PC와 달랐습니다.

### 백엔드

- `chat` 패키지의 `ChatController`, `ChatDtos`, `ChatService`, `OpenAiClient`를 추가했습니다.
- 키 없이 현재 냉장고 기반으로 응답하는 LOCAL 추천과, 키가 있으면 Responses API를 호출하는 경로를 연결했습니다.
- 질문마다 인증된 사용자의 최신 재고를 읽고, 입력 검증·사용자 격리·시간 제한·외부 장애 처리를 구현했습니다.
- `GlobalExceptionHandler`가 지원하지 않는 Content-Type·Accept 요청을 500으로 처리하던 문제를 415·406 처리로 수정했습니다.
- 기존 추천 알고리즘과 인증 구조는 유지했습니다. 실제 seed 16개와 상세 API, 동의어·단위 합산·인분·타인 재고 격리 등을 검증하는 통합 테스트를 보강했습니다.
- 채팅 관련 단위·HTTP·보안 통합 테스트 23개를 추가했습니다.

### 프론트엔드

- 가입 성공 후 `/login`으로 이동하며 이메일만 유지하고 비밀번호를 다시 입력하도록 정리했습니다. `/signup` 재진입 문제도 수정했습니다.
- 식재료 상태 필터가 URL 쿼리와 브라우저 뒤로가기에 동기화되도록 `useSearch`와 이동 처리를 보완했습니다.
- API 계층에 CSRF 응답 검증, 동시에 발생한 토큰 요청 공유, 403 후 한 번만 재시도, 잘못된 성공 JSON·네트워크 오류·60초 제한 처리를 보완했습니다.
- 이전 화면에서 취소한 요청이 새 화면의 데이터에 반영되지 않도록 조회 훅을 보완했습니다.
- Vite가 루트 `.env`의 `SERVER_PORT`와 `API_PROXY_TARGET`을 읽도록 했습니다.
- 모바일 채팅 입력창·로딩·포커스, 버튼 줄바꿈, 재료 목록의 날짜 가독성을 수정했습니다.
- 비포커스 상태의 본문 바로가기 링크가 긴 화면 캡처에 노출되던 문제를 수정하면서 키보드 포커스 접근은 유지했습니다.
- API 회귀 테스트 8개와 `npm test` 명령을 추가했습니다.

### 실행 환경·테스트·문서

- 시스템 Java를 바꾸지 않고 `.local/`에 JDK 21을 준비했습니다. 공식 배포본 체크섬을 확인했고 재설치용 `setup-java.ps1`을 추가했습니다.
- Docker가 없는 현재 PC에서는 설치된 MySQL 8.0 실행 파일을 사용해 프로젝트 전용 DB를 별도 포트·데이터 폴더에 구성했습니다.
- DB 시작·종료·연결 설정 복구·포트 변경·Windows 감시 프로세스·한글 경로 문제를 해결했습니다.
- 기본 빌드·검증을 한 번에 실행하는 `verify.ps1`을 추가했습니다.
- 기존 HTTP smoke 스크립트를 실제 MySQL 서버에서 실행했습니다.
- Playwright E2E 시나리오 2개를 작성하고 데스크톱·모바일 설정에서 총 4개로 실행했습니다.
- STEP 8~11을 작성하고 API 계약·README·기존 문서의 깨진 앵커를 정리했습니다.
- 이 인수인계 요청에서는 `AGENTS.md`와 이 문서를 추가하고 README에 발견 가능한 링크를 연결했습니다.

## 3. 구조와 수정할 파일 찾기

| 영역 | 위치와 주요 파일 | 역할 |
| --- | --- | --- |
| 서버 시작·의존성 | `backend/pom.xml`, `FridgeGatekeeperApplication.java` | Java 21, Spring Boot 4.1.1, Maven Wrapper |
| 인증 | `backend/src/main/java/com/fridgegatekeeper/user/` | 회원, 비밀번호 해시, 세션·CSRF, 로그인 사용자 식별 |
| 재료·대시보드 | `backend/src/main/java/com/fridgegatekeeper/ingredient/` | 사용자별 CRUD, 버전 충돌, 날짜 상태와 집계 |
| 추천 | `backend/src/main/java/com/fridgegatekeeper/recommendation/` | `RecommendationService`, `IngredientNames`, `Quantities`, 응답 DTO |
| 레시피 | `backend/src/main/java/com/fridgegatekeeper/recipe/` | 레시피·재료 Entity, 조회 API |
| 채팅 | `backend/src/main/java/com/fridgegatekeeper/chat/` | LOCAL 응답, OpenAI HTTP 호출 |
| 공통 | `backend/src/main/java/com/fridgegatekeeper/common/` | 오류 응답, Clock, `/api/health` |
| DB 생성·seed | `backend/src/main/resources/db/migration/` | `V1__initial_schema.sql`, `V2__seed_recipes.sql` |
| 서버 설정 | [application.yml](../backend/src/main/resources/application.yml) | DB·세션·한국 시간·OpenAI 설정 |
| 화면 진입 | [App.jsx](../frontend/src/App.jsx), `frontend/src/lib/router.js` | 세션 확인·로그아웃, 경로에 따른 화면 선택 |
| 화면 | `frontend/src/pages/` | Auth, Dashboard, Ingredients, Recipes, Chat |
| 공통 UI | `frontend/src/components/` | Dialog, IngredientEditor·Row, RecipeCard·Detail, 인분 선택 |
| 통신 | [api.js](../frontend/src/services/api.js), `frontend/src/hooks/useResource.js` | 쿠키·CSRF·오류·취소·재조회 |
| 날짜·표시 | `frontend/src/lib/format.js` | 한국 날짜, 단위·카테고리 등의 표시 |
| 스타일 | `frontend/src/styles/global.css` | 기존 디자인과 반응형 레이아웃 |
| 검증 | `backend/src/test/`, `frontend/tests/`, `e2e/`, `scripts/smoke-api.mjs` | 단위·통합·실서버·브라우저 테스트 |

프론트엔드는 JavaScript JSX이며 TypeScript나 React Router를 사용하지 않습니다. `router.js`가 History API와 `popstate`를 연결합니다. `WelcomePage.jsx`는 초기 단계 화면 기록이며 현재 인증 이후 서비스의 진입은 `App.jsx`입니다.

DB 테이블은 `users`, `ingredients`, `recipes`, `recipe_ingredients`입니다. JPA `ddl-auto: validate`와 Flyway를 함께 사용합니다. 이미 실행된 V1·V2를 변경하면 기존 DB와 체크섬이 충돌할 수 있으므로 DB 변경은 후속 마이그레이션으로 진행합니다.

## 4. 유지해야 하는 동작·계약

### 인증과 재고 소유권

- JWT 대신 서버의 HTTP 세션을 사용합니다. 세션 쿠키는 HttpOnly, SameSite=Lax이며 기본 만료는 8시간입니다.
- `GET /api/auth/csrf`가 반환하는 `token`과 `headerName`을 변경 요청에 사용합니다. 로그인 시 세션 ID와 CSRF 토큰이 교체됩니다.
- 가입은 자동 로그인이 아닙니다. 가입 후 로그인해야 합니다.
- 사용자 ID는 요청 본문에서 신뢰하지 않고 인증 세션에서 확인합니다. 타인의 재료 ID 조회·수정·삭제는 404로 처리합니다.
- 수정 요청에는 조회 당시 `version`이 필요합니다. 다른 화면에서 이미 수정한 재료는 409를 반환하며 최신 목록을 다시 읽게 합니다.
- 비밀번호·세션을 브라우저 localStorage에 저장하지 않습니다. 미리 정해진 일반 로그인 계정은 없으며 직접 가입합니다.

### 날짜와 추천 계산

- 서버는 주입된 `Clock`과 `Asia/Seoul` 기준 날짜를 사용합니다. 오늘 만료인 재료는 아직 만료가 아닙니다.
- `EXPIRED`: 오늘 이전. `SOON`: 오늘부터 3일 뒤까지. `SAFE`: 4일 이상 남음. 오늘까지의 수는 SOON 집계에도 포함됩니다.
- 만료된 재료는 추천 재고에서 제외합니다. 같은 이름으로 인식되는 유효 재고는 수량을 합산합니다.
- kg↔g, L↔ml 환산만 지원합니다. 팩↔g 등 알 수 없는 환산은 추측하지 않고 `unitMismatch`로 알립니다.
- `IngredientNames`에 정의된 일부 동의어를 지원하며 모든 자유 입력 이름을 이해하는 것은 아닙니다.
- 추천 정렬은 보유 재료 활용 종류 내림차순 → 임박 재료 활용 종류 내림차순 → 부족 종류 오름차순 → ID입니다. 임의 가중치 점수로 바꾸지 않았습니다.
- 일부 수량만 있어도 활용 재료로 계산하고 부족량은 별도로 표시합니다. `canCook`는 필요한 모든 수량이 준비되었는지 나타냅니다.
- 인분은 1 또는 2만 받습니다. 필요량은 비례 계산하고 영양값은 인분 선택과 무관하게 항상 1인분 기준 예시 추정값입니다.
- 현재 레시피는 직접 작성한 16개 seed입니다. 조리용 물은 기본 환경으로 보지만 소금·기름·양념을 자동 보유 처리하지 않습니다.
- 레시피 조회·채팅은 재고를 차감하지 않습니다. 식재료의 실제 변경은 CRUD 요청으로 수행합니다.

### 화면·API 연결

- 브라우저는 같은 출처의 `/api`로 요청하고 Vite가 백엔드로 전달합니다. 백엔드 URL을 화면 코드에 직접 넣지 않습니다.
- 프론트엔드 요청은 쿠키를 포함하고, 401이면 필요한 화면에서 로그인 복귀 이벤트를 발생시킵니다. 잘못된 로그인 자체는 세션 만료 안내로 오인하지 않습니다.
- CSRF 오류 재시도는 한 번으로 제한하며, 일반 네트워크 실패에 변경 요청을 무조건 반복하지 않습니다.
- 상태 필터 쿼리를 훅으로 구독하므로 같은 페이지에서 뒤로가기·앞으로가기가 동작해야 합니다.
- 레시피 상세의 인분 변경은 서버에서 수량을 다시 계산합니다. 네트워크 오류와 최신 요청 취소를 유지해야 합니다.
- Dialog는 브라우저 기본 `<dialog>`를 사용합니다. 키보드 포커스, Escape, 처리 중 닫기 제한을 고려합니다.
- 자세한 요청·응답 필드와 HTTP 상태는 [API 계약](api.md)을 기준으로 확인합니다.

## 5. 채팅과 OpenAI 구현

[STEP 10](step-10.md)에 파일별 설명과 계약이 있습니다. 실제 경로는 `/api/ai/status`, `/api/ai/chat`이고 Java 패키지명은 `chat`입니다.

`ChatService`는 매 질문마다 현재 사용자 재고와 추천을 읽습니다. API 키가 없으면 외부 요청 없이 LOCAL 답변을 만듭니다. 보유 재료를 활용하는 메뉴 최대 3개를 고르며, 이번 질문의 임박·간단·단백질 키워드에 제한적으로 반응합니다. LOCAL은 대화 문맥 전체, 임의 시간 조건, 알레르기·재료 제외 조건 등을 이해하는 AI가 아닙니다.

키가 있으면 `OpenAiClient`가 JDK HttpClient와 Jackson 3으로 Responses API를 호출합니다. `available`은 비어 있지 않은 키 설정 여부만 의미하며 유효성·잔액·접근 권한 검사가 아닙니다.

| 항목 | 현재 구현 |
| --- | --- |
| 현재 질문 | 공백만 허용하지 않음, 최대 2,000자 |
| 인분 | 1 또는 2 |
| 이전 대화 | 최대 10개, 역할 user/assistant, 각 내용 최대 2,000자 |
| 재고 전송 | 유통기한 순 최대 100건, 잘림 여부 별도 표시 |
| 추천 전송·응답 카드 | 서버가 계산한 최대 3개 |
| 기본 모델 | `OPENAI_MODEL`, 기본 `gpt-4.1-mini` |
| 외부 요청 | `/v1/responses`, `store: false`, `max_output_tokens: 1600` |
| 시간 제한 | 연결 10초, 요청 기본 35초, 프론트엔드 요청 60초 |
| 출력 처리 | 모든 assistant 메시지의 `output_text` 수집, 거절 텍스트 전달 |
| 실패 처리 | 503과 구분된 오류 코드, 성공한 AI 응답이나 자동 LOCAL 응답으로 위장하지 않음 |

회원 ID·이메일·비밀번호·재고 ID·구매 날짜는 재고 문맥에 붙여 보내지 않습니다. 사용자가 대화에 직접 쓴 내용은 질문·history에 포함됩니다. 외부 API 키는 서버에서만 읽습니다. `store: false`가 외부 제공자의 모든 로그·보관 정책을 없앤다는 뜻은 아닙니다.

채팅 내역은 현재 React 화면 상태에만 있으며 앱 DB에 저장하지 않습니다. 화면을 떠나거나 새로고침하면 지워집니다. 외부 호출 동안 DB 연결을 잡아두지 않도록 조회 트랜잭션과 AI 요청을 분리했습니다.

## 6. Windows 실행 환경과 주의할 상태

### 작업 당시 확인한 환경

- Windows PowerShell 5 계열, Node.js 24.15.0.
- 시스템 `java`는 17이었으므로 프로젝트 JDK 21.0.12.1을 `.local/jdk-21.0.12.1+1/`에 준비했습니다.
- `C:/Program Files/MySQL/MySQL Server 8.0/bin`의 MySQL 8.0.44를 사용했습니다. 기존 PC 서비스의 3306과 프로젝트의 3307은 다른 DB입니다.
- Docker 실행 도구가 없어 실제 Docker 기동 검증은 하지 않았습니다. 저장소의 Compose 구성은 MySQL 8.4입니다.
- Chrome 실행 파일은 `C:/Program Files/Google/Chrome/Application/chrome.exe`를 사용했습니다.

이 환경은 Git에 복제되지 않습니다. 기존 대화에서 서버가 실행 중이었다고 해도 지금 실행 중이라고 가정하지 마세요. 과거 PID로 프로세스를 종료하지 말고 현재 포트와 명령행을 확인하세요. 마지막 실서버 검증에서는 패키징된 JAR를 8080, Vite 개발 서버를 5173, 프로젝트 MySQL을 3307로 실행했습니다.

### 로컬 파일

| 위치 | 의미 |
| --- | --- |
| `.local/mysql/data/` | 실제 프로젝트 DB. 회원·재고 데이터 보존 대상 |
| `.local/mysql/my.ini` | 이 인스턴스의 설정, 포트·데이터 경로 |
| `.local/mysql/connection.json` | 선택된 MySQL 실행 파일·포트·JDBC URL |
| `.local/mysql/admin.cnf` | 이 DB의 관리자 접속 설정. 비밀 값이 있으므로 출력·커밋 금지 |
| `.local/mysql/initialize.sql` | 최초 시작용 SQL. 준비 성공 후 제거되는 파일 |
| `.local/mysql/server.log` | MySQL 진단 로그 |
| `.local/jdk-21*/` | 프로젝트 JDK, 실행·검증 스크립트가 선택 |
| `.local/verify-all.log`, `.local/backend.log`, `.local/frontend.log` | 작업 중 생성한 로그, 이후 존재·내용은 달라질 수 있음 |
| `.local/db-script-check-한글 경로/`, `.local/db-final-check-한글 경로/` | 한글·공백 경로 검증용 별도 환경. 검증 후 해당 DB는 종료했으며 실제 프로젝트 데이터 폴더와 구분할 것 |

`.local/`, `.env`, 설치 의존성, 빌드 결과와 브라우저 결과는 Git에서 제외합니다. 이 문서에 비밀 값이나 실제 사용자 데이터를 복사하지 않습니다.

### 실행 스크립트의 중요한 처리

- [setup-java.ps1](../scripts/setup-java.ps1): 공식 Temurin JDK 21 메타데이터와 SHA-256 확인, `.local/`에 설치. 시스템 Java 설정을 바꾸지 않습니다.
- [start-db.ps1](../scripts/start-db.ps1): 프로젝트 연결 설정이 있으면 우선 재사용합니다. 이후 기존 portable 시작 파일, Docker, 설치된 MySQL 실행 파일 순의 경로가 있습니다.
- [start-local-db.ps1](../scripts/start-local-db.ps1): 별도 데이터 폴더, 기본 3307, 준비 대기, 초기 사용자 생성과 연결 설정 복구를 수행합니다.
- [stop-local-db.ps1](../scripts/stop-local-db.ps1): 이 프로젝트 설정·실행 파일에 해당하는 프로세스에만 정상 종료 요청을 보냅니다. 포트는 현재 연결 설정에서 명시합니다.
- [start-backend.ps1](../scripts/start-backend.ps1): 로컬 JDK 선택, 로컬 DB 자동 선택, 허용된 `.env` 항목을 환경 변수로 설정한 뒤 Maven을 실행합니다.
- [start-frontend.ps1](../scripts/start-frontend.ps1): `npm.cmd ci` 후 Vite를 실행합니다.
- [verify.ps1](../scripts/verify.ps1): 로컬 JDK 선택, Maven verify, 프론트엔드 설치·테스트·lint·build를 순서대로 실행합니다.

Windows MySQL에는 동일한 `--defaults-file`을 가진 감시 프로세스와 자식 작업 프로세스가 함께 나타날 수 있습니다. 이것만으로 중복 실행이라고 판단하면 안 됩니다. 현재 스크립트는 실행 파일·설정 절대경로·부모 자식 관계로 구분하며 관련 없는 프로세스는 처리하지 않습니다.

PowerShell의 `$ErrorActionPreference='Stop'`은 네이티브 stderr를 조기에 예외로 바꿀 수 있어 준비 확인 부분만 별도로 처리했습니다. 한글 데이터 경로는 일부 MySQL Windows 빌드에서 `Illegal byte sequence`가 발생하므로 NTFS 짧은 경로를 사용합니다. 설정 경로 비교는 짧은 경로와 긴 경로를 정규화합니다. 짧은 경로가 없는 환경은 영문 경로나 Docker가 필요합니다.

`DB_URL`은 `.env.example`에서 주석입니다. 이를 임의로 3306에 고정하면 Windows의 3307 자동 선택을 덮어씁니다. 명시적 `.env` 설정이 우선하며 Maven/JAR 직접 실행은 루트 `.env`를 자동으로 읽지 않습니다. Vite는 루트 `.env`의 `SERVER_PORT` 또는 전체 `API_PROXY_TARGET` 주소를 사용합니다.

## 7. 재개·실행·검증 명령

먼저 현재 상태를 읽습니다. 서버를 다시 띄우기 전에 포트 사용자를 확인하세요.

```powershell
git status --short
git log -3 --oneline
Get-NetTCPConnection -State Listen -LocalPort 3307,5173,8080 -ErrorAction SilentlyContinue
```

프로젝트 루트에서 실행합니다. JDK 준비는 필요할 때만 실행하고 백엔드·프론트엔드는 각각 별도 터미널을 사용합니다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/setup-java.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/start-db.ps1
```

```powershell
# 백엔드 터미널
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/start-backend.ps1
```

```powershell
# 프론트엔드 터미널
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/start-frontend.ps1
```

웹은 `http://127.0.0.1:5173`, 서버 확인은 `http://127.0.0.1:8080/api/health`입니다. health 응답은 DB·OpenAI의 전체 연결 상태를 검사하는 진단은 아닙니다.

```powershell
# 기본 자동 테스트와 빌드. 실제 MySQL 없이 H2로 실행 가능
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/verify.ps1

# DB·백엔드·프론트엔드 실행 후
node scripts/smoke-api.mjs
cd e2e
npm.cmd ci
$env:BROWSER_EXECUTABLE = 'C:/Program Files/Google/Chrome/Application/chrome.exe'
npm.cmd test
```

설치된 Chrome을 쓰지 않을 경우 `BROWSER_EXECUTABLE` 대신 `npx.cmd playwright install chromium`으로 테스트 브라우저를 준비합니다. `e2e/playwright.config.js`는 서버를 자동 시작하지 않습니다.

직접 JAR를 실행하려면 JDK 21과 DB 환경 변수를 선택합니다. 로컬 설정이 있을 때의 예시는 다음과 같습니다.

```powershell
$env:JAVA_HOME = (Resolve-Path '.local/jdk-21.0.12.1+1').Path
$env:DB_URL = (Get-Content '.local/mysql/connection.json' -Raw -Encoding UTF8 | ConvertFrom-Json).url
& "$env:JAVA_HOME/bin/java.exe" -jar backend/target/fridge-gatekeeper-0.0.1-SNAPSHOT.jar
```

이 직접 실행 예시는 `.env`의 다른 항목을 읽지 않습니다. DB 인증이나 OpenAI 설정을 바꾼 환경에서는 필요한 환경 변수를 함께 설정하거나 기존 시작 스크립트를 사용하세요. 종료는 서버를 시작한 터미널에서 `Ctrl+C`, 프로젝트 DB는 다음 명령을 사용합니다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/stop-local-db.ps1
```

## 8. 검증 이력과 의미

최종 구현 검증은 2026-09-14~15에 수행했습니다. 아래는 과거 구현의 통과 기록이며 이 인수인계 문서 작성만을 위해 애플리케이션 전체 테스트를 다시 실행했다는 의미가 아닙니다.

| 검사 | 결과 | 재현 위치 |
| --- | --- | --- |
| 백엔드 Maven verify | 48개 통과, JAR 생성 | `backend/src/test/`, `scripts/verify.ps1` |
| 프론트 API 회귀 | 8개 통과 | `frontend/tests/api.test.js` |
| 프론트 lint·build | 통과 | `frontend/package.json` |
| 실제 MySQL HTTP | 통과 | `scripts/smoke-api.mjs` |
| 데스크톱 E2E | 2개 통과 | `e2e/tests/fridge.spec.js` |
| 모바일 E2E | 2개 통과 | 같은 시나리오, 390×844·터치 에뮬레이션 |
| DB 스크립트 | 반복 시작·종료, 설정 복구, 포트 변경·데이터 보존 통과 | 작업 중 실제 Windows 프로세스로 개별 검증 |
| 한글·공백 폴더 | 새 DB 초기화·반복 시작·정상 종료 통과 | 별도 3309 인스턴스에서 개별 검증 후 종료 |

백엔드 48개의 구성은 앱 시작 1, 회원·재료 통합 10, 추천 14, 채팅 서비스 7, OpenAI HTTP 13, 채팅 통합 3입니다. [STEP 11](step-11.md)에 테스트 파일 링크와 자세한 범위를 기록했습니다.

DB 스크립트 검증에서는 재료를 저장한 뒤 3307→3308→3307로 재시작하여 수량·단위와 회원·레시피 수가 보존됨을 확인했습니다. 실행 중 포트 변경은 거부하고, 연결 metadata가 없어진 상태에서 재실행하면 복구되는지도 확인했습니다. 이 개별 OS 검사는 `verify.ps1`이나 Playwright에 포함된 자동 시나리오는 아닙니다.

브라우저에서는 가입·로그인·재료 추가/수정/삭제·필터/뒤로가기·레시피 상세/환산/인분·LOCAL 채팅·재로그인 후 재고 유지·서버 오류 재시도·세션 만료를 확인했습니다. 가로 넘침과 JavaScript 오류도 검사했습니다. 최신 캡처에서 모바일 버튼 한 줄 표시, 재료 날짜 가독성, 비포커스 본문 바로가기 숨김을 육안 확인했습니다.

검증용 회원은 `smoke-`, `e2e-` 접두사의 고유 이메일이며 미리 만든 실사용 계정이 아닙니다. 테스트 재료는 정상 종료 시 정리하지만 회원은 남습니다. 강제 종료된 테스트의 재료가 남을 수도 있습니다. 기존 사용자 데이터를 광범위하게 지우는 방식으로 정리하지 마세요.

산출물은 `backend/target/surefire-reports/`, `frontend/dist/`, `e2e/test-results/`에 생성됩니다. 버전 관리에 포함하지 않으므로 다른 PC에 없을 수 있습니다.

## 9. 이미 해결한 문제를 다시 겪지 않기 위한 메모

- 처음 README에 적혀 있던 MySQL 8.4.11 portable 환경은 이 PC에 없었습니다. 현재 방식은 설치된 MySQL 8.0.44 + 독립 데이터 폴더입니다.
- 기본 Java 17로 Maven을 실행하면 Java 21 컴파일이 실패합니다. 로컬 JDK를 선택하는 스크립트 또는 명시적 `JAVA_HOME`을 사용하세요.
- 잘못된 Content-Type을 500으로 처리하던 오류는 회귀 테스트로 재현한 뒤 415 처리로 수정했습니다.
- E2E에서 `<label>`이 `<select>`를 감쌀 때 `getByLabel('카테고리', { exact: true })`는 옵션 텍스트까지 비교해 실패했습니다. 현재는 `getByRole('combobox', { name: '카테고리', exact: true })`를 사용합니다. React `useId`나 Dialog 제목 소실 문제는 아니었습니다.
- Playwright `filter({ has: ... })`의 내부 locator는 후보 요소를 기준으로 평가됩니다. 레시피 행 안에서 부모 dialog를 다시 찾는 locator를 넣지 않습니다.
- 테스트 정리 실패가 원래 테스트 오류를 가리지 않도록 E2E의 cleanup 예외를 별도로 처리했습니다.
- Windows MySQL의 감시·자식 프로세스는 정상 구동 형태일 수 있습니다. 한글 경로는 UTF-8 설정 파일만으로 해결되지 않아 native 파일 경로도 짧은 경로로 처리했습니다.
- 기존 `admin.cnf`의 최초 포트만 사용하면 포트 변경 후 종료가 실패합니다. 종료 요청은 현재 `connection.json`의 포트를 명시해야 합니다.
- 이전 작업 환경에서는 내장 브라우저 연결에 사용 가능한 브라우저가 없어서 저장소의 Playwright 테스트 실행기로 검증했습니다. 다음 세션의 브라우저 도구 가용성은 다시 확인하고 그 세션의 지침을 따르세요.
- 외부 `DEBUG` 환경 변수에 따라 로그가 많아질 수 있었습니다. 로그 내용과 실제 테스트 성공 여부를 구분하고 비밀 값을 진단 출력에 포함하지 마세요.

## 10. 완료된 MVP와 아직 검증하지 않은 범위

현재 요청했던 README 기준의 MVP에 알려진 필수 미구현 기능은 남기지 않았습니다. 다음 항목은 완료했다고 간주하지 마세요.

- **실제 OpenAI API 호출:** 키를 사용한 유료 연동 검증은 하지 않았습니다. localhost 모의 HTTP 서버와 LOCAL 모드를 검증했습니다. 실제 연동은 사용할 키·모델·한도 설정이 필요합니다.
- **Docker MySQL 8.4:** Compose 파일은 존재하지만 이번 PC에서 실행하지 않았습니다. 실제 검증 DB는 MySQL 8.0.44입니다.
- **운영 배포:** HTTPS, 리버스 프록시, 도메인, 운영 DB·백업, 배포 자동화는 구성하지 않았습니다. GitHub 푸시는 애플리케이션 배포가 아닙니다.
- **다른 브라우저·실물 모바일:** Chrome 계열의 데스크톱·모바일 에뮬레이션을 확인했습니다. Safari·Firefox·실물 기기 검증 결과는 없습니다.
- **레시피·영양 데이터 확장:** 현재 seed 16개와 예시 영양값입니다. 외부 공인 DB, 레시피 관리 화면, 더 많은 인분·동의어·단위는 별도 요구사항입니다.
- **추가 제품 기능:** 장보기 목록, 조리 후 재고 차감, 알림 발송, 저장된 대화·즐겨찾기, 비밀번호 재설정 등은 현재 MVP 범위에 포함하지 않았습니다. 필요하면 사용자의 다음 요청에 맞춰 설계하세요.

`LIVE_OPENAI=1`은 실서버 테스트에서 유료 채팅 호출을 허용하는 명시적 스위치입니다. 단지 전체 테스트를 돌린다는 이유로 설정하지 마세요. 앱의 OPENAI 상태가 true라고 해서 해당 호출이 이미 검증되었다고 보고하지 마세요.

## 11. 다음 Codex의 작업 시작 순서

1. 루트 [AGENTS.md](../AGENTS.md), 이 문서, 사용자의 최신 요청을 읽습니다.
2. `git status`, 현재 브랜치·커밋을 확인합니다. `21bd1e1`은 기준점이며 이후 변경을 덮어쓰지 않습니다.
3. 작업 영역의 실제 코드와 [API 계약](api.md)을 읽고, 환경·데이터·외부 설정에 의존하는 부분을 구분합니다.
4. 기존 구조를 유지해 필요한 변경을 구현합니다. 인증·소유권·한국 날짜·수량·단위 규칙이 영향을 받는지 확인합니다.
5. 변경에 맞는 자동·실서버 검증을 수행합니다. DB 변경 시 마이그레이션과 기존 데이터 보존을 확인합니다.
6. 변경 내용, 실행 방법, 검증 결과, 미검증 범위를 관련 문서에 갱신합니다. 인수인계 기준이 달라지면 이 문서의 기준 커밋·날짜·상태도 갱신합니다.
7. 커밋·푸시는 사용자가 요청한 범위로 진행합니다. 이 문서가 모든 미래 작업에 대한 무조건적인 푸시 허가를 뜻하지 않습니다.

다음 세션에 전달할 짧은 요청 예시:

> AGENTS.md와 docs/CODEX_HANDOFF.md를 먼저 읽고 현재 Git 상태를 확인해 주세요. 기존 냉장고 서비스 구현과 데이터 보존 규칙을 유지하면서, 이번에 요청하는 기능을 추가하고 관련 테스트·문서를 갱신해 주세요.
