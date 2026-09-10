# STEP 4 — 식재료 CRUD

## 1. 구현 목적

각 사용자가 자신의 식재료를 등록·수정·삭제하고 정렬된 목록을 조회합니다. 식재료명, 카테고리, 수량과 단위, 구매 날짜, 유통기한, 보관 위치를 함께 저장합니다.

## 2. 생성 파일

- [IngredientRepository.java](../backend/src/main/java/com/fridgegatekeeper/ingredient/IngredientRepository.java)
- [IngredientRequest.java](../backend/src/main/java/com/fridgegatekeeper/ingredient/IngredientRequest.java)
- [IngredientResponse.java](../backend/src/main/java/com/fridgegatekeeper/ingredient/IngredientResponse.java)
- [IngredientService.java](../backend/src/main/java/com/fridgegatekeeper/ingredient/IngredientService.java)
- [IngredientController.java](../backend/src/main/java/com/fridgegatekeeper/ingredient/IngredientController.java)

## 3. 전체 코드

위 링크에서 전체 구현을 볼 수 있습니다. 데이터 저장 구조는 STEP 2의 `Ingredient` Entity를 사용하고, 상태 계산은 STEP 5의 `ExpiryStatus`를 사용합니다.

## 4. 코드 설명

Controller가 HTTP 입력을 받고 DTO 검증 후 Service를 호출합니다. Service는 업무 규칙을 검사하고 Repository를 통해 DB에 접근합니다. 모든 식재료 단건 조회에는 `id`와 로그인한 `userId`를 동시에 사용합니다. 다른 사용자의 식재료를 수정·삭제하려 해도 404를 반환합니다.

수량은 양수이며 정수 9자리, 소수 3자리까지 허용합니다. 구매 날짜는 오늘 이후일 수 없고 유통기한보다 뒤일 수 없습니다. 이미 만료된 식재료도 과거 구매 날짜와 함께 등록할 수 있습니다. 이름이 같은 재료도 구매일과 유통기한이 다른 경우 별도 항목으로 관리할 수 있습니다.

수정할 때는 조회한 `version`을 함께 보냅니다. 다른 화면에서 먼저 수정했으면 409를 반환합니다. JPA의 `@Version`은 거의 동시에 발생한 수정도 검사합니다.

| API | 기능 |
| --- | --- |
| `GET /api/ingredients?sort=expiration` | 유통기한순 목록 |
| `GET /api/ingredients?sort=category` | 카테고리별 목록 |
| `GET /api/ingredients?sort=storage` | 보관 위치별 목록 |
| `GET /api/ingredients/{id}` | 단건 조회 |
| `POST /api/ingredients` | 등록, 201 |
| `PUT /api/ingredients/{id}` | 수정, 200 |
| `DELETE /api/ingredients/{id}` | 삭제, 204 |

```json
{
  "name": "계란",
  "category": "DAIRY",
  "quantity": 6,
  "unit": "PIECE",
  "purchaseDate": "2026-09-07",
  "expirationDate": "2026-09-10",
  "storageType": "FRIDGE"
}
```

수정 요청에는 같은 필드와 최신 `version`을 포함합니다. 모든 쓰기 요청에는 STEP 3에서 받은 CSRF 헤더와 로그인 세션이 필요합니다.

## 5. 실행 방법

MySQL과 백엔드를 README대로 실행하고 로그인합니다. STEP 8~9의 냉장고 화면에서 등록·편집·삭제와 정렬을 확인합니다. 위 예시 날짜는 실행하는 날짜에 맞게 바꿉니다. 자동 검증은 `backend`에서 `./mvnw.cmd test`를 실행합니다.
