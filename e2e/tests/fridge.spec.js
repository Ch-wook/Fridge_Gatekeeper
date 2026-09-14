import { test, expect } from '@playwright/test'
import { randomUUID } from 'node:crypto'

const today = new Intl.DateTimeFormat('en-CA', {
  timeZone: 'Asia/Seoul', year: 'numeric', month: '2-digit', day: '2-digit',
}).format(new Date())
function dateAfter(days) {
  const date = new Date(`${today}T00:00:00Z`)
  date.setUTCDate(date.getUTCDate() + days)
  return date.toISOString().slice(0, 10)
}
async function signup(page) {
  const credentials = { email: `e2e-${randomUUID()}@example.test`, password: randomUUID() }
  await page.goto('/signup')
  await page.getByLabel('닉네임', { exact: true }).fill('검증 요리사')
  await page.getByLabel('이메일', { exact: true }).fill(credentials.email)
  await page.getByLabel('비밀번호', { exact: true }).fill(credentials.password)
  await page.getByRole('button', { name: '회원가입', exact: true }).click()
  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByRole('status')).toContainText('가입이 완료')
  await expect(page.getByLabel('이메일', { exact: true })).toHaveValue(credentials.email)
  await page.getByLabel('비밀번호', { exact: true }).fill(credentials.password)
  await page.getByRole('button', { name: '로그인', exact: true }).click()
  await expect(page.getByRole('heading', { level: 1 })).toContainText('검증 요리사님')
  return credentials
}
async function addIngredient(page, name, category, amount, unit, days) {
  await page.getByRole('button', { name: '＋ 식재료 추가', exact: true }).click()
  const dialog = page.getByRole('dialog', { name: '식재료 추가', exact: true })
  await dialog.getByLabel('식재료명', { exact: true }).fill(name)
  await dialog.getByRole('combobox', { name: '카테고리', exact: true }).selectOption(category)
  await dialog.getByLabel('수량', { exact: true }).fill(String(amount))
  await dialog.getByRole('combobox', { name: '단위', exact: true }).selectOption(unit)
  await dialog.getByLabel('구매 날짜', { exact: true }).fill(dateAfter(-7))
  await dialog.getByLabel('유통기한', { exact: true }).fill(dateAfter(days))
  await dialog.getByRole('button', { name: '식재료 저장', exact: true }).click()
  await expect(dialog).not.toBeVisible()
  await expect(page.getByRole('button', { name: `${name} 수정`, exact: true })).toBeVisible()
}
async function cleanup(page) {
  const csrfResponse = await page.request.get('/api/auth/csrf', { timeout: 5000 })
  if (!csrfResponse.ok()) return
  const csrf = await csrfResponse.json()
  const response = await page.request.get('/api/ingredients')
  if (!response.ok()) return
  for (const ingredient of await response.json()) {
    await page.request.delete(`/api/ingredients/${ingredient.id}`, { headers: { [csrf.headerName]: csrf.token } })
  }
  await page.request.post('/api/auth/logout', { headers: { [csrf.headerName]: csrf.token } })
}

test('회원가입부터 재료 관리, 인분별 레시피, 채팅과 재로그인까지', async ({ page }, testInfo) => {
  const pageErrors = []
  page.on('pageerror', error => pageErrors.push(error.message))
  const credentials = await signup(page)
  try {
    await page.getByRole('navigation', { name: '주 메뉴' }).getByRole('link', { name: '내 냉장고', exact: true }).click()
    await expect(page.getByRole('heading', { name: '냉장고의 첫 재료를 기록해 볼까요?' })).toBeVisible()
    await addIngredient(page, '계란', 'DAIRY', 6, 'PIECE', 0)
    await addIngredient(page, '돼지고기', 'MEAT', 1, 'KILOGRAM', 7)
    await addIngredient(page, '김치', 'VEGETABLE', 500, 'GRAM', 7)
    await addIngredient(page, '소금', 'SEASONING', 10, 'GRAM', -1)

    await page.getByRole('button', { name: '계란 수정', exact: true }).click()
    const editor = page.getByRole('dialog', { name: '식재료 수정', exact: true })
    await editor.getByLabel('수량', { exact: true }).fill('8')
    await editor.getByRole('button', { name: '수정 저장', exact: true }).click()
    await expect(editor).not.toBeVisible()
    await expect(page.locator('.ingredient-row').filter({ has: page.getByRole('button', { name: '계란 수정', exact: true }) })).toContainText('8개')

    await page.getByRole('button', { name: '오늘까지', exact: true }).click()
    await expect(page.getByText('전체 4개 중 1개', { exact: true })).toBeVisible()
    await page.getByRole('button', { name: '만료', exact: true }).click()
    await expect(page.getByRole('button', { name: '소금 수정', exact: true })).toBeVisible()
    await page.goBack()
    await expect(page.getByRole('button', { name: '오늘까지', exact: true })).toHaveAttribute('aria-pressed', 'true')
    await expect(page.getByRole('button', { name: '계란 수정', exact: true })).toBeVisible()
    await page.getByRole('button', { name: '전체', exact: true }).click()
    await page.getByLabel('식재료 검색', { exact: true }).fill('김치')
    await expect(page.getByText('전체 4개 중 1개', { exact: true })).toBeVisible()
    await page.getByLabel('식재료 검색', { exact: true }).fill('')

    await page.getByRole('navigation', { name: '주 메뉴' }).getByRole('link', { name: '오늘의 레시피', exact: true }).click()
    await expect(page.getByText('총 16개 메뉴 · 1인분 기준', { exact: true })).toBeVisible()
    await page.getByRole('button', { name: '김치찌개 레시피 보기', exact: true }).click()
    const recipe = page.getByRole('dialog', { name: '김치찌개', exact: true })
    const pork = recipe.getByRole('row').filter({ has: page.getByRole('rowheader', { name: '돼지고기', exact: true }) })
    await expect(pork.getByRole('cell').nth(0)).toHaveText('100g')
    await expect(pork.getByRole('cell').nth(1)).toHaveText('1,000g')
    await recipe.getByRole('button', { name: '2인분', exact: true }).click()
    await expect(pork.getByRole('cell').nth(0)).toHaveText('200g')
    await expect(recipe.getByRole('heading', { name: '차근차근 만드는 순서', exact: true })).toBeVisible()
    await recipe.getByRole('button', { name: '닫기', exact: true }).last().click()

    await page.getByRole('navigation', { name: '주 메뉴' }).getByRole('link', { name: 'AI 요리 도우미', exact: true }).click()
    const aiStatus = await (await page.request.get('/api/ai/status')).json()
    if (!aiStatus.available || process.env.LIVE_OPENAI === '1') {
      await page.getByLabel('요리 도우미에게 질문하기', { exact: true }).fill('오늘 냉장고 재료로 만들 요리를 추천해 줘')
      await page.getByRole('button', { name: '보내기', exact: true }).click()
      await expect(page.getByRole('log').getByText(aiStatus.available ? 'AI 답변' : '기본 추천', { exact: true })).toBeVisible({ timeout: 55_000 })
      await expect(page.getByRole('heading', { name: '대화에서 추천한 메뉴', exact: true })).toBeVisible()
      await page.screenshot({ path: testInfo.outputPath('chat.png'), fullPage: true })
    }
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)

    await page.getByRole('navigation', { name: '주 메뉴' }).getByRole('link', { name: '내 냉장고', exact: true }).click()
    await page.getByRole('button', { name: '소금 삭제', exact: true }).click()
    await page.getByRole('dialog', { name: '식재료 삭제', exact: true }).getByRole('button', { name: '삭제하기', exact: true }).click()
    await expect(page.getByText('전체 3개 중 3개', { exact: true })).toBeVisible()
    await page.reload()
    await expect(page.getByRole('button', { name: '계란 수정', exact: true })).toBeVisible()
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
    await page.screenshot({ path: testInfo.outputPath('ingredients.png'), fullPage: true })
    await page.getByRole('button', { name: '로그아웃', exact: true }).click()
    await expect(page.getByRole('button', { name: '로그인', exact: true })).toBeVisible()
    await page.getByLabel('이메일', { exact: true }).fill(credentials.email)
    await page.getByLabel('비밀번호', { exact: true }).fill(credentials.password)
    await page.getByRole('button', { name: '로그인', exact: true }).click()
    await expect(page.getByRole('heading', { level: 1 })).toContainText('검증 요리사님')
    await expect(page.getByRole('link').filter({ hasText: '전체 식재료' })).toContainText('3개')
    expect(pageErrors).toEqual([])
  } finally { await cleanup(page).catch(() => {}) }
})

test('재가입 화면 이동과 서버 오류 재시도, 만료된 세션 복구', async ({ page }) => {
  await signup(page)
  try {
    await page.route('**/api/ingredients?sort=expiration', route => route.fulfill({
      status: 503, contentType: 'application/json', body: JSON.stringify({ code: 'TEST_UNAVAILABLE', message: '검증용 일시 오류', fieldErrors: {} }),
    }))
    await page.getByRole('navigation', { name: '주 메뉴' }).getByRole('link', { name: '내 냉장고', exact: true }).click()
    await expect(page.getByRole('alert')).toContainText('검증용 일시 오류')
    await page.unroute('**/api/ingredients?sort=expiration')
    await page.getByRole('button', { name: '다시 시도', exact: true }).click()
    await expect(page.getByRole('heading', { name: '냉장고의 첫 재료를 기록해 볼까요?' })).toBeVisible()
    const csrf = await (await page.request.get('/api/auth/csrf')).json()
    await page.request.post('/api/auth/logout', { headers: { [csrf.headerName]: csrf.token } })
    await page.getByRole('button', { name: '새로고침', exact: true }).click()
    await expect(page.getByRole('button', { name: '로그인', exact: true })).toBeVisible()
    await expect(page.getByRole('status')).toContainText('로그인 시간이 만료')
    await page.getByRole('link', { name: '회원가입', exact: true }).click()
    await expect(page.getByLabel('닉네임', { exact: true })).toBeVisible()
  } finally { await cleanup(page).catch(() => {}) }
})
