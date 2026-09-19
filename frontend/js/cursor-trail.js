/* ============================================================
   cursor-trail.js — siçanın arxasınca gedən elastik mürəkkəb xətti.
   Nöqtələr zəncir kimi bir-birinin arxasınca yay (spring) qanunu ilə
   çəkilir; xətt quyruğa doğru nazikləşir. Siçan dayananda zəncir
   uca yığılıb yox olur və animasiya dövrü dayanır (boş yerə CPU yemir).
   Yalnız açılış, giriş və qeydiyyat ekranında işləyir — tətbiqin içində
   (usta axtarışı, söhbət, profil) iz çəkilmir və canvas gizlənir.
   Toxunma ekranlarında və "hərəkəti azalt" seçimində işə düşmür.
   ============================================================ */
(function () {
  if (!window.matchMedia('(pointer: fine)').matches) return;
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;

  const POINTS   = 36;    // zəncirdəki nöqtə sayı — iz uzunluğu
  const WIDTH    = 0.32;  // baş hissənin qalınlığı (nöqtə başına)
  const SPRING   = 0.42;
  const FRICTION = 0.5;
  // Ağ + mix-blend-mode:difference (theme.css) - açıq fonda qara, fotoda ağ görünür
  const COLOR    = '#fff';

  const canvas = document.createElement('canvas');
  canvas.className = 'cursor-trail';
  canvas.setAttribute('aria-hidden', 'true');
  document.body.appendChild(canvas);
  const ctx = canvas.getContext('2d');

  const pointer = { x: -100, y: -100 };
  let trail = [];
  let running = false;
  let visible = false;

  function resize() {
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    canvas.width  = Math.round(window.innerWidth  * dpr);
    canvas.height = Math.round(window.innerHeight * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  }

  function reset(x, y) {
    trail = Array.from({ length: POINTS }, () => ({ x, y, dx: 0, dy: 0 }));
  }

  /* Yalnız giriş ekranında: tətbiq qabığı (#appShell) görünəndə iz yoxdur.
     Açılış ekranı da qabıq gizli olduğu üçün buraya düşür. */
  function onAuthScreen() {
    const shell = document.getElementById('appShell');
    return !shell || shell.classList.contains('is-hidden');
  }

  function start() {
    if (!running) { running = true; requestAnimationFrame(frame); }
  }

  function frame() {
    let spread = 0;
    for (let i = 0; i < trail.length; i++) {
      const p = trail[i];
      const lead = i === 0 ? pointer : trail[i - 1];
      const k = i === 0 ? SPRING * 0.4 : SPRING;
      p.dx = (p.dx + (lead.x - p.x) * k) * FRICTION;
      p.dy = (p.dy + (lead.y - p.y) * k) * FRICTION;
      p.x += p.dx;
      p.y += p.dy;
      spread += Math.abs(p.x - pointer.x) + Math.abs(p.y - pointer.y);
    }

    ctx.clearRect(0, 0, window.innerWidth, window.innerHeight);

    // Zəncir uca yığılıbsa (siçan dayanıb) - heç nə çəkmə və dövrü saxla
    if (!visible || !onAuthScreen() || spread < POINTS * 0.6) { running = false; return; }

    ctx.strokeStyle = COLOR;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.beginPath();
    ctx.moveTo(trail[0].x, trail[0].y);
    for (let i = 1; i < trail.length - 1; i++) {
      const mx = (trail[i].x + trail[i + 1].x) / 2;
      const my = (trail[i].y + trail[i + 1].y) / 2;
      ctx.quadraticCurveTo(trail[i].x, trail[i].y, mx, my);
      ctx.lineWidth = WIDTH * (POINTS - i);
      ctx.stroke();
      ctx.beginPath();
      ctx.moveTo(mx, my);
    }
    requestAnimationFrame(frame);
  }

  window.addEventListener('mousemove', (e) => {
    if (!onAuthScreen()) return;
    pointer.x = e.clientX;
    pointer.y = e.clientY;
    if (!visible) { visible = true; reset(pointer.x, pointer.y); }
    start();
  }, { passive: true });

  document.addEventListener('mouseleave', () => { visible = false; });
  window.addEventListener('blur', () => { visible = false; });
  window.addEventListener('resize', resize);

  /* Giriş anında ekranda qalmış izi sil: animasiya dövrü elə həmin an
     dayanmış ola bilər və son kadr tətbiqin üstündə donub qalardı. */
  const shell = document.getElementById('appShell');
  if (shell) {
    new MutationObserver(() => {
      if (!onAuthScreen()) {
        visible = false;
        ctx.clearRect(0, 0, window.innerWidth, window.innerHeight);
      }
    }).observe(shell, { attributes: true, attributeFilter: ['class'] });
  }

  resize();
  reset(pointer.x, pointer.y);
})();
