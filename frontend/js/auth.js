/* ============================================================
   auth.js — açılış animasiyası, giriş və qeydiyyat ekranı.
   ============================================================ */

/* ---------- açılış yazısının hərf-hərf canlanması ---------- */
function buildSplashWord() {
  const word = 'SIGNUM ANIMAE';
  const host = $('#splashWord');
  let delay = 0.72;

  host.innerHTML = Array.from(word).map(ch => {
    if (ch === ' ') return '<span class="sp"></span>';
    const span = '<span class="ch" style="animation-delay:' + delay.toFixed(2) + 's">' + ch + '</span>';
    delay += 0.062;
    return span;
  }).join('');
}

function runSplash() {
  buildSplashWord();
  setTimeout(() => {
    $('#splash').remove();
    if (Session.load() && Session.token) {
      App.start();
    } else {
      showAuthScreen();
    }
  }, 3950);
}

function showAuthScreen() {
  $('#appShell').classList.add('is-hidden');
  const screen = $('#authScreen');
  screen.classList.remove('is-hidden');
  // animasiyanı yenidən oynatmaq üçün reflow
  screen.style.animation = 'none';
  void screen.offsetWidth;
  screen.style.animation = '';
}

/* ---------- tablar ---------- */
function initAuthTabs() {
  $$('.tab').forEach(tab => {
    tab.addEventListener('click', () => {
      const name = tab.dataset.tab;
      $$('.tab').forEach(t => t.classList.toggle('is-active', t === tab));
      $('#tabInk').classList.toggle('right', name === 'register');
      $('#loginForm').classList.toggle('is-hidden', name !== 'login');
      $('#registerForm').classList.toggle('is-hidden', name !== 'register');
    });
  });
}

/* ---------- formlar ---------- */
function initAuthForms() {

  $('#loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const form = e.target;
    const btn = $('button[type=submit]', form);
    const done = withBusy(btn, 'Yoxlanılır');
    try {
      const auth = await Api.auth.login({
        email:    form.email.value.trim(),
        password: form.password.value
      });
      await completeLogin(auth);
    } catch (err) {
      toastErr(err.message);
      done();
    }
  });

  $('#registerForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const form = e.target;
    const btn = $('button[type=submit]', form);
    const done = withBusy(btn, 'Yaradılır');
    try {
      const auth = await Api.auth.register({
        email:    form.email.value.trim(),
        password: form.password.value,
        fullName: form.fullName.value.trim(),
        city:     form.city.value.trim(),
        role:     form.role.value
      });
      toastOk('Xoş gəldin, ' + form.fullName.value.trim() + '!');
      await completeLogin(auth);
    } catch (err) {
      toastErr(err.message);
      done();
    }
  });
}

/* Giriş/qeydiyyat cavabında fullName gəlmir — onu ayrıca çəkirik ki,
   yuxarı paneldə istifadəçinin adı görünsün. */
async function completeLogin(auth) {
  Session.save(auth);
  try {
    const profile = await Api.users.get(auth.userId);
    Session.patch({ fullName: profile.fullName, city: profile.city });
  } catch (e) {
    Session.patch({ fullName: auth.email });
  }
  $('#authScreen').classList.add('is-hidden');
  App.start();
}

function logout() {
  ChatModule.disconnect();
  Session.clear();
  $('#appShell').classList.add('is-hidden');
  showAuthScreen();
  toast('Çıxış edildi.');
}
