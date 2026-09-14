# 냉장고 지킴이 · Fridge Gatekeeper

보유 식재료를 등록하면 냉장고 상태에 맞춰 요리를 먼저 추천하는 반응형 웹 서비스입니다.
Java 21 / Spring Boot 4.1.1 / Spring Data JPA / MySQL 8 / React 19 / Vite 8을 사용합니다.
Docker 환경은 MySQL 8.4, 현재 Windows 로컬 환경은 설치된 MySQL 8.0.44를 사용합니다.

## 주요 기능

- 회원가입·로그인·로그아웃, 사용자별 냉장고 분리, 비밀번호 BCrypt 해시, 세션·CSRF 보호
- 식재료 추가·수정·삭제, 수량·단위·구매일·유통기한·보관 위치 관리, 정렬·검색·필터
- 오늘 만료·3일 이내 임박·만료된 재료 집계 및 상태 색상 표시
- 레시피 16개, 보유 재료 활용 수 → 임박 재료 활용 수 → 부족 재료 수 순으로 추천
- 필요·보유·부족 수량, 조리 순서·시간·난이도, 1·2인분 조절, 1인분당 영양 추정값
- 현재 냉장고를 기반으로 한 OpenAI 채팅과 API 키 없이 사용할 수 있는 기본 추천

## 빠른 실행 — 현재 Windows 작업 폴더

JDK 21과 Node.js 24 LTS를 사용합니다. Maven은 Wrapper가 내려받습니다.
명령은 모두 프로젝트 루트에서 실행합니다.

현재 작업 폴더에는 프로젝트 전용 JDK 21이 `.local/`에 준비되어 있으며 실행 스크립트가 자동으로 선택합니다.
다른 Windows PC에서 JDK 21이 없다면 다음 명령으로 공식 Eclipse Temurin 배포본을 내려받습니다.
다운로드의 SHA-256을 확인하며 시스템 Java 설정은 변경하지 않습니다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/setup-java.ps1
```

**1. MySQL 시작**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/start-db.ps1
```

현재 PC에서는 설치된 MySQL 실행 파일로 **프로젝트 전용 DB를 127.0.0.1:3307**에서 실행합니다.
데이터는 `.local/mysql/data/`에 저장하고 기존 MySQL 서비스와 데이터는 별도로 유지합니다.
재실행 시 기존 프로젝트 DB를 사용합니다. 프로젝트 DB 설정이 없는 PC에서는 Docker가 있으면 Compose를,
Docker가 없으면 설치된 MySQL 8.4 또는 8.0 실행 파일을 사용합니다.
개발 DB 기본값은 `fridge_gatekeeper`, 사용자 `fridge_app`, 비밀번호 `fridge_dev_password`입니다.

MySQL 설치 위치 또는 로컬 포트를 직접 지정할 수도 있습니다. 포트를 바꾸려면 먼저 프로젝트 DB를 종료하세요.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/start-local-db.ps1 -MySqlBin "C:/Program Files/MySQL/MySQL Server 8.0/bin" -Port 3307
```

**2. 터미널 1 — 백엔드**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/start-backend.ps1
```

[상태 확인](http://127.0.0.1:8080/api/health)에서 `{"status":"UP","service":"fridge-gatekeeper"}`가 표시됩니다.
첫 실행에 Flyway가 테이블과 레시피를 생성합니다. 재시작해도 회원·재고는 유지됩니다.

**3. 터미널 2 — 프론트엔드**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/start-frontend.ps1
```

[http://127.0.0.1:5173](http://127.0.0.1:5173)에서 회원가입 후 식재료를 등록하세요.
이메일·비밀번호·닉네임은 직접 설정하며 미리 만들어진 로그인 계정은 없습니다.
각 서버는 해당 터미널의 `Ctrl+C`로 종료합니다.

로컬 MySQL 종료는 다음 명령을 사용합니다. 데이터는 보존됩니다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/stop-local-db.ps1
```

## 다른 PC 또는 macOS/Linux

`.local/`은 Git에 포함하지 않는 현재 PC 전용 실행 환경입니다. 다른 PC에서는 Docker Desktop 또는 MySQL 8이 필요합니다.
아래 명령은 Docker DB와 개발 서버를 각각 실행합니다. 별도로 설치한 MySQL을 사용할 경우 DB·사용자 생성과 접속 설정을 먼저 준비하세요.

```bash
# 프로젝트 루트: 최초 한 번 복사하고 값을 편집합니다.
cp .env.example .env
docker compose up -d --wait mysql

# 터미널 1 (기본 DB 설정 그대로 실행할 때)
cd backend
sh ./mvnw spring-boot:run

# 터미널 2: 프로젝트 루트에서 시작
cd frontend
npm ci
npm run dev
```

Windows에서 직접 실행한다면 백엔드 명령은 `.\mvnw.cmd spring-boot:run`입니다.
Docker DB 종료는 `docker compose down`을 사용합니다. DB를 보존하려면 `-v`를 붙이지 않습니다.

## 환경 변수와 OpenAI

루트 `.env.example`을 `.env`로 복사한 뒤 필요한 값을 편집합니다. 실제 키는 프론트엔드에 넣지 않습니다.

| 변수 | 용도 |
| --- | --- |
| DB_URL / DB_USERNAME / DB_PASSWORD | Spring Boot의 MySQL 접속 정보 |
| MYSQL_PORT / MYSQL_PASSWORD / MYSQL_ROOT_PASSWORD | Docker Compose의 MySQL 최초 설정 |
| OPENAI_API_KEY | 서버에서만 읽는 OpenAI API 키 |
| OPENAI_MODEL | 기본 `gpt-4.1-mini`, 사용 가능한 모델로 변경 가능 |
| SERVER_PORT | 백엔드 포트, 기본 8080. Vite도 루트 `.env`의 값을 사용 |
| API_PROXY_TARGET | 별도 주소의 백엔드를 사용할 때 Vite 프록시 전체 주소 지정 |
| SESSION_COOKIE_SECURE | HTTPS 환경에서는 true |

`scripts/start-backend.ps1`은 `.env`를 읽어 서버 환경 변수로 전달합니다.
`DB_URL`이 없으면 `.local/mysql/connection.json`의 프로젝트 DB를 자동 선택하며, 해당 파일도 없으면 기본 3306을 사용합니다.
`.env.example`의 `DB_URL`은 주석 상태이므로 그대로 복사해도 로컬 DB 자동 선택이 유지됩니다.
직접 지정한 `DB_URL`은 자동 선택보다 우선합니다. 과거 설정에 3306이 남아 있다면 프로젝트 DB용으로 3307로 바꾸거나 해당 줄을 주석 처리하세요.
Maven이나 JAR를 직접 실행할 때 Spring Boot는 루트 `.env`를 자동으로 읽지 않으므로 환경 변수를 별도로 설정하세요.
Docker 포트를 바꾸면 `DB_URL`도 함께 바꾸고, 비밀번호를 바꾸면 `MYSQL_PASSWORD`와 `DB_PASSWORD`를 맞춥니다.
이미 만들어진 Docker 볼륨의 비밀번호는 환경 변수 변경만으로 바뀌지 않습니다.

키가 없으면 채팅은 **기본 추천(LOCAL)**으로 동작합니다. 임박 재료, 간단한 요리, 단백질 조건을 제한적으로 반영합니다.
키가 설정되면 서버에서 Responses API를 호출합니다. 화면의 AI 사용 가능 표시는 키 설정 여부이며 실제 연결 성공을 보장하는 상태 검사는 아닙니다.
연동에는 [OpenAI 공식 텍스트 생성 문서](https://developers.openai.com/api/docs/guides/text)를 참고했습니다.
AI에는 현재 사용자의 식재료 최대 100건, 추천 최대 3개, 현재 질문과 최근 대화 최대 10개를 보냅니다.
회원 ID·이메일·비밀번호는 보내지 않으며 `store: false`를 요청합니다. 대화는 앱 DB에 저장하지 않습니다.
AI 오류는 사용자에게 알리고, 실패한 응답을 성공한 AI 추천으로 표시하지 않습니다.

## 추천 기준과 데이터

유통기한은 한국 시간의 날짜를 기준으로 계산합니다. 오늘까지는 만료가 아니며 오늘~3일 뒤까지 임박입니다.
만료된 재료는 추천 계산에서 제외합니다. 날짜 기준의 안전 표시는 실제 식품 상태를 보증하지 않습니다.
동일 식재료의 사용 가능한 수량을 합산하며 kg↔g, L↔ml을 변환합니다.
팩↔g처럼 임의 환산이 필요한 단위는 부족 재료에 표시하여 확인할 수 있게 합니다.
달걀↔계란 등 일부 동의어를 지원하며 자유 입력한 모든 식재료명에 대한 인식은 보장하지 않습니다.

레시피와 조리 순서는 MVP용으로 직접 작성했습니다. 영양값은 **1인분당 예시 추정값**으로 공인 영양 DB 자료가 아닙니다.
조리용 물은 기본 환경으로 간주하며, 소금·기름·양념은 재고에 있어야 보유 재료로 계산합니다.

## 구조와 단계별 코드 설명

```text
backend/     REST API, 인증, JPA, Flyway, 추천, OpenAI, 테스트
frontend/    React 화면, 공통 컴포넌트, API 호출, CSS
database/    테이블 설계
docs/        단계별 목적·전체 코드 링크·설명·실행 방법, API 계약
scripts/     Windows 실행 스크립트
compose.yaml MySQL 개발 환경
```

[DB 설계](database/README.md) · [REST API 계약](docs/api.md)

| 단계 | 개발 안내 |
| --- | --- |
| 1 | [프로젝트 구조](docs/step-01.md) |
| 2 | [DB 설계·Entity](docs/step-02.md) |
| 3 | [회원 기능](docs/step-03.md) |
| 4 | [식재료 CRUD](docs/step-04.md) |
| 5 | [유통기한 관리](docs/step-05.md) |
| 6 | [레시피 데이터](docs/step-06.md) |
| 7 | [추천 알고리즘](docs/step-07.md) |
| 8 | [React 화면](docs/step-08.md) |
| 9 | [화면·API 연결](docs/step-09.md) |
| 10 | [OpenAI 연동](docs/step-10.md) |
| 11 | [테스트·검증 결과](docs/step-11.md) |

## 빌드와 테스트

현재 Windows 폴더에서는 로컬 JDK 선택까지 포함한 검증 스크립트를 사용합니다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/verify.ps1
```

수동 검증은 JDK 21이 선택된 터미널에서 `backend`의 `mvnw.cmd --batch-mode --no-transfer-progress verify`,
`frontend`의 `npm ci`, `npm test`, `npm run lint`, `npm run build`를 실행합니다.
macOS/Linux에서는 `sh ./mvnw --batch-mode --no-transfer-progress verify`를 사용합니다.

자동 테스트는 격리된 H2 MySQL 모드를 사용합니다. 실제 MySQL·브라우저 검증 결과는 STEP 11 문서에 구분해 기록합니다.
백엔드 결과물은 `backend/target/fridge-gatekeeper-0.0.1-SNAPSHOT.jar`, 프론트엔드는 `frontend/dist/`입니다.
개발에서는 Vite의 `/api` 프록시로 연결하며, 배포 시에도 웹과 `/api`를 같은 출처에서 제공하도록 리버스 프록시를 설정합니다.

실행 중인 서버의 HTTP 통합 검증과 데스크톱·모바일 브라우저 검증은 다음과 같습니다.

```powershell
# 프로젝트 루트, DB/백엔드/프론트엔드 실행 후
node scripts/smoke-api.mjs
cd e2e
npm.cmd ci
npx.cmd playwright install chromium
npm.cmd test
```

이미 설치된 Chrome을 쓰려면 브라우저 설치 대신 `$env:BROWSER_EXECUTABLE = 'C:/Program Files/Google/Chrome/Application/chrome.exe'`를 지정합니다.
테스트는 고유한 `smoke-` 또는 `e2e-` 이메일로 회원을 만들고, 정상 종료 시 테스트 재료를 정리합니다. 테스트 회원은 DB에 남습니다.
실제 OpenAI 유료 호출은 기본적으로 생략하며, `LIVE_OPENAI=1`을 명시한 경우에만 실행합니다.
실패 화면·추적 파일과 성공 화면 캡처는 `e2e/test-results/`에 저장되며 Git에서는 제외됩니다.

## 문제 해결

- 3306 포트 충돌: 기존 DB 연결 정보를 설정하거나 Docker 포트와 DB_URL을 함께 변경하세요.
- DB 연결 오류: MySQL을 먼저 실행하고 사용자·비밀번호·DB 이름을 확인하세요.
- 8080 포트 충돌: 이전 백엔드를 종료하거나 루트 `.env`의 `SERVER_PORT`를 변경하고 양쪽 서버를 재시작하세요.
- Java 버전 오류: `scripts/setup-java.ps1`로 JDK 21을 준비하고 제공한 실행·검증 스크립트를 사용하세요.
- 로컬 MySQL 시작 오류: `.local/mysql/server.log`를 확인하세요. `.local/mysql/data`는 실제 데이터이므로 임의로 삭제하지 마세요.
- 한글 경로의 MySQL: 실행 스크립트가 NTFS 짧은 경로를 사용합니다. 짧은 경로를 사용할 수 없다는 오류가 나면 영문 경로 또는 Docker를 사용하세요.
- npm.ps1 실행 제한: `npm.cmd`를 사용하거나 제공한 실행 스크립트를 사용하세요.
- 로그인 세션 만료: 다시 로그인하세요. 브라우저 쿠키를 허용하고 웹은 항상 같은 호스트로 접속하세요.
- OPENAI_API_KEY 변경: 백엔드를 재시작하세요. 유효한 키·모델 접근 권한이 있어야 실제 AI 응답을 받을 수 있습니다.
