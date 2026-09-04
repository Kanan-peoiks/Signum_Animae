/* ============================================================
   ui.js — kiçik köməkçilər: təhlükəsiz HTML, toast, modal,
   tarix/reytinq formatlaması.
   ============================================================ */

/* İstifadəçidən gələn hər mətn innerHTML-ə düşməzdən əvvəl
   buradan keçməlidir — əks halda XSS qapısı açıq qalır. */
function esc(value) {
  if (value === null || value === undefined) return '';
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

const $  = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

/* ---------- toast ---------- */
function toast(message, kind = '') {
  const box = document.createElement('div');
  box.className = 'toast ' + kind;
  box.textContent = message;
  $('#toasts').appendChild(box);
  setTimeout(() => {
    box.classList.add('out');
    setTimeout(() => box.remove(), 320);
  }, kind === 'err' ? 5200 : 3400);
}
const toastOk  = (m) => toast(m, 'ok');
/* lastAuthFailureAt api.js-də tərtib olunur - sessiya bitəndə paralel
   sorğuların hərəsi ayrıca "err" toast atmasın deyə qısa bir pəncərədə
   susduruluruq (qlobal signum:unauthorized handler öz mesajını birbaşa
   toast() ilə göstərir, bu sıxışdırmadan yan keçir). */
const toastErr = (m) => {
  if (Date.now() - lastAuthFailureAt < 1500) return;
  toast(m, 'err');
};

/* ---------- modal ---------- */
function openModal(title, bodyHtml, options = {}) {
  const root = $('#modalRoot');
  const overlay = document.createElement('div');
  overlay.className = 'overlay';
  overlay.innerHTML =
    '<div class="modal" role="dialog" aria-modal="true">' +
      '<h3 class="modal-title">' + esc(title) + '</h3>' +
      '<div class="modal-body">' + bodyHtml + '</div>' +
      '<div class="modal-actions">' +
        '<button class="btn btn-ghost" data-close>' + esc(options.cancelText || 'Bağla') + '</button>' +
        (options.okText ? '<button class="btn btn-primary" data-ok>' + esc(options.okText) + '</button>' : '') +
      '</div>' +
    '</div>';

  const close = () => { overlay.remove(); document.removeEventListener('keydown', onKey); };
  const onKey = (e) => { if (e.key === 'Escape') close(); };

  overlay.addEventListener('click', (e) => { if (e.target === overlay) close(); });
  $('[data-close]', overlay).addEventListener('click', close);
  document.addEventListener('keydown', onKey);

  const okBtn = $('[data-ok]', overlay);
  if (okBtn && options.onOk) {
    okBtn.addEventListener('click', () => options.onOk(overlay, close, okBtn));
  }

  root.appendChild(overlay);
  const firstInput = $('input,textarea,select', overlay);
  if (firstInput) firstInput.focus();

  return { overlay, close };
}

/* ---------- düymə üzərində yüklənmə ---------- */
function withBusy(button, label) {
  const original = button.innerHTML;
  button.disabled = true;
  button.innerHTML = '<span class="spinner"></span>' + (label ? ' ' + esc(label) : '');
  return () => { button.disabled = false; button.innerHTML = original; };
}

/* ---------- formatlama ---------- */
function initials(name) {
  if (!name) return '?';
  return name.trim().split(/\s+/).slice(0, 2)
    .map(w => w[0] ? w[0].toUpperCase() : '').join('');
}

function stars(avg) {
  const value = Math.round(Number(avg) || 0);
  let out = '';
  for (let i = 1; i <= 5; i++) {
    out += (i <= value) ? '<span>&#9733;</span>' : '<span class="off">&#9733;</span>';
  }
  return '<span class="stars">' + out + '</span>';
}

function ratingBlock(avg, count) {
  const a = Number(avg) || 0;
  const c = Number(count) || 0;
  if (!c) return '<span class="rating"><span class="rating-num" style="color:var(--ink-faint)">Hələ rəy yoxdur</span></span>';
  return '<span class="rating">' + stars(a) +
         '<span class="rating-num">' + a.toFixed(1) + ' (' + c + ')</span></span>';
}

/* Backend LocalDateTime-ı "2026-09-05T14:30:00" kimi qaytarır.
   toLocaleDateString('az-AZ') qısa ay üçün "M09" verir — oxunaqlı deyil,
   ona görə ay adlarını özümüz yazırıq. */
const AY = ['yan','fev','mar','apr','may','iyn','iyl','avq','sen','okt','noy','dek'];

function parseDate(iso) {
  if (!iso) return null;
  const d = new Date(iso);
  return isNaN(d) ? null : d;
}

const pad2 = (n) => String(n).padStart(2, '0');

function fmtDay(iso) {
  const d = parseDate(iso);
  if (!d) return iso ? String(iso) : '—';
  return pad2(d.getDate()) + ' ' + AY[d.getMonth()] + ' ' + d.getFullYear();
}

function fmtDateTime(iso) {
  const d = parseDate(iso);
  if (!d) return iso ? String(iso) : '—';
  return fmtDay(iso) + ', ' + pad2(d.getHours()) + ':' + pad2(d.getMinutes());
}

function fmtTime(iso) {
  const d = parseDate(iso);
  if (!d) return '';
  return pad2(d.getHours()) + ':' + pad2(d.getMinutes());
}

function fmtMoney(value) {
  if (value === null || value === undefined || value === '') return '—';
  return Number(value).toFixed(0) + ' AZN';
}

const STATUS_AZ = {
  PENDING:   'Gözləyir',
  CONFIRMED: 'Təsdiqlənib',
  COMPLETED: 'Tamamlanıb',
  CANCELLED: 'Ləğv edilib'
};
const statusBadge = (s) =>
  '<span class="badge ' + esc(s) + '">' + esc(STATUS_AZ[s] || s || '—') + '</span>';

/* ---------- ümumi bloklar ---------- */
const spinner = () => '<div class="spinner"></div>';

function emptyState(text, mark = '✵') {
  return '<div class="empty"><div class="empty-mark">' + mark + '</div><p>' + esc(text) + '</p></div>';
}

function pageHead(title, subtitle) {
  return '<div class="page-head"><h1 class="page-title">' + esc(title) + '</h1>' +
         (subtitle ? '<p class="page-sub">' + esc(subtitle) + '</p>' : '') + '</div>';
}

/* "Realism, Blackwork" → nişanlar. Backend-də styles sadə String-dir. */
function styleChips(styles) {
  if (!styles || !String(styles).trim()) return '';
  return '<div class="chips">' +
    String(styles).split(',')
      .map(s => s.trim()).filter(Boolean).slice(0, 5)
      .map(s => '<span class="chip">' + esc(s) + '</span>')
      .join('') + '</div>';
}
