# REST API 계약

모든 경로는 /api 아래이며 JSON을 사용합니다. 사용자 ID는 요청 본문에서 받지 않고 인증 세션에서 확인합니다.
세션 쿠키는 HttpOnly입니다. POST/PUT/DELETE 요청은 먼저 GET /api/auth/csrf의 token을 headerName 헤더로 전달합니다.
로그인·로그아웃 후 토큰을 다시 발급받습니다. 프론트엔드 개발 서버의 /api 프록시를 통해 같은 출처로 요청합니다.

| 메서드 | 경로 | 요청 / 응답 |
| --- | --- | --- |
| GET | /auth/csrf | {token,headerName} |
| POST | /auth/register | {email,password,nickname} → 201 {id,email,nickname} |
| POST | /auth/login | {email,password} → {id,email,nickname} |
| GET | /auth/me | {id,email,nickname} 또는 401 |
| POST | /auth/logout | 204 |
| GET | /ingredients?sort=expiration | 식재료 배열. sort: expiration/category/storage |
| POST | /ingredients | 식재료 생성 → 201 식재료 |
| GET/PUT/DELETE | /ingredients/{id} | 조회/수정/삭제. 타인 소유 ID는 404 |
| GET | /dashboard | {total,safeCount,soonCount,expiredCount,todayCount,expiringIngredients,expiredIngredients,today} |
| GET | /recipes/recommendations?servings=1 | 추천 배열. 인분은 1 또는 2 |
| GET | /recipes/{id}?servings=2 | 추천과 같은 형식의 상세 |
| GET | /ai/status | {available} |
| POST | /ai/chat | {message,servings,history:[{role,content}]} → {reply,source,recommendedRecipes} |

식재료 요청: name,category,quantity,unit,purchaseDate,expirationDate,storageType. 수정에는 version 필수.
응답에는 id,version,status,daysUntilExpiration이 추가됩니다. 날짜는 YYYY-MM-DD입니다.
status: SAFE/SOON/EXPIRED, 단위 및 카테고리는 [DB 설계](../database/README.md)를 참고하세요.

추천: id,name,description,cookingTime,difficulty,servings,requiredIngredients,availableIngredients,missingIngredients,urgentIngredients,matchedCount,missingCount,canCook,steps,nutrition,reason.
재료 상세: name,requiredQuantity,availableQuantity,missingQuantity,unit,urgent,unitMismatch.
영양: calories,protein,carbs,fat,perServing=true,estimated=true. 모든 영양값은 1인분당 추정치입니다.
source는 OPENAI 또는 LOCAL이며 LOCAL은 API 키가 없을 때의 규칙 기반 추천입니다.

오류 응답: {code,message,fieldErrors}. 검증400, 인증401, CSRF403, 없음404, 중복·수정충돌409, AI 서비스 장애503을 구분합니다.
