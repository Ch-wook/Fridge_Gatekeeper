import { useState } from 'react'
import { api } from '../services/api.js'
import { categories, locations, todayInSeoul, units } from '../lib/format.js'
import { Dialog, ErrorBox, SelectOptions } from './UI.jsx'

export default function IngredientEditor({ ingredient, onClose, onSaved, onReload }) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const [purchaseDate, setPurchaseDate] = useState(ingredient?.purchaseDate || todayInSeoul())
  async function submit(event) {
    event.preventDefault()
    setError(null)
    const form = Object.fromEntries(new FormData(event.currentTarget))
    if (!form.name.trim()) { setError(new Error('식재료명을 입력해 주세요.')); return }
    if (form.expirationDate < form.purchaseDate) { setError(new Error('유통기한은 구매 날짜와 같거나 이후여야 해요.')); return }
    // 수정 시 처음 조회한 version을 함께 보내 다른 탭의 수정 사항을 덮어쓰지 않습니다.
    const body = { ...form, name: form.name.trim(), quantity: Number(form.quantity), ...(ingredient ? { version: ingredient.version } : {}) }
    setBusy(true)
    try {
      await api(ingredient ? `/ingredients/${ingredient.id}` : '/ingredients', { method: ingredient ? 'PUT' : 'POST', body })
      onSaved(ingredient ? '식재료를 수정했어요.' : '냉장고에 식재료를 추가했어요.')
    } catch (failure) { setError(failure) } finally { setBusy(false) }
  }
  return <Dialog title={ingredient ? '식재료 수정' : '식재료 추가'} onClose={onClose} busy={busy}>
    <form onSubmit={submit}><div className="dialog-body stack"><p className="muted">재료의 양과 날짜를 기록하면 더 알맞은 메뉴를 추천해요. 모든 항목을 입력해 주세요.</p>
      {error && <ErrorBox error={error} />}
      {error?.status === 409 && <button type="button" className="button secondary" onClick={onReload}>최신 목록 다시 불러오기</button>}
      <div className="form-grid">
        <label className="field full-width">식재료명<input name="name" defaultValue={ingredient?.name || ''} placeholder="예: 계란, 대파, 두부" maxLength={80} required disabled={busy} /></label>
        <label className="field">카테고리<select name="category" defaultValue={ingredient?.category || 'VEGETABLE'} disabled={busy} required><SelectOptions values={categories} /></select></label>
        <label className="field">보관 위치<select name="storageType" defaultValue={ingredient?.storageType || 'FRIDGE'} disabled={busy} required><SelectOptions values={locations} /></select></label>
        <label className="field">수량<input name="quantity" type="number" min="0.001" max="999999999" step="0.001" defaultValue={ingredient?.quantity || 1} required disabled={busy} /></label>
        <label className="field">단위<select name="unit" defaultValue={ingredient?.unit || 'PIECE'} disabled={busy} required><SelectOptions values={units} /></select></label>
        <label className="field">구매 날짜<input name="purchaseDate" type="date" value={purchaseDate} max={todayInSeoul()} onChange={(event) => setPurchaseDate(event.target.value)} required disabled={busy} /></label>
        <label className="field">유통기한<input name="expirationDate" type="date" defaultValue={ingredient?.expirationDate || todayInSeoul()} min={purchaseDate} required disabled={busy} /></label>
      </div><p className="helper">오늘부터 3일 이내는 ‘임박’, 오늘 이전은 ‘만료’로 표시해요. 날짜 기준은 한국 시간이에요.</p>
    </div><div className="dialog-actions"><button type="button" className="button secondary" onClick={onClose} disabled={busy}>취소</button><button className="button" disabled={busy}>{busy ? '저장 중…' : ingredient ? '수정 저장' : '식재료 저장'}</button></div></form>
  </Dialog>
}
