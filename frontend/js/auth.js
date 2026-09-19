/* ============================================================
   auth.js — açılış animasiyası, giriş və qeydiyyat ekranı.
   ============================================================ */

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
    if (handleTokenLink()) return;
    if (Session.load() && Session.token) {
      App.start();
    } else {
      showAuthScreen();
    }
  }, 3950);
}

function handleTokenLink() {
  const params = new URLSearchParams(location.search);
  const mode  = params.get('mode');
  const token = params.get('token');
  if (!token || (mode !== 'reset' && mode !== 'verify')) return false;

  history.replaceState(null, '', location.pathname);

  if (mode === 'verify') {
    if (Session.load() && Session.token) App.start();
    else showAuthScreen();
    verifyEmailFromLink(token);
  } else {
    showAuthScreen();
    openResetPasswordModal(token);
  }
  return true;
}

async function verifyEmailFromLink(token) {
  try {
    const res = await Api.auth.verifyEmail(token);
    toastOk((res && res.message) || 'Email ünvanı təsdiqləndi.');
  } catch (err) {
    toastErr(err.message);
  }
}

function openResetPasswordModal(token) {
  openModal('Yeni şifrə təyin et',
    '<label class="field"><span>Yeni şifrə</span>' +
      '<input type="password" id="rpPassword" placeholder="••••••••"></label>' +
    '<label class="field" style="margin-top:13px"><span>Yeni şifrə (təkrar)</span>' +
      '<input type="password" id="rpPassword2" placeholder="••••••••"></label>' +
    '<p class="form-note" style="text-align:left;margin-top:10px">Ən azı 6 simvol.</p>',
    {
      okText: 'Şifrəni yenilə',
      onOk: async (overlay, close, okBtn) => {
        const pass  = $('#rpPassword', overlay).value;
        const pass2 = $('#rpPassword2', overlay).value;
        if (pass.length < 6)  { toastErr('Şifrə ən azı 6 simvol olmalıdır.'); return; }
        if (pass !== pass2)   { toastErr('Şifrələr uyğun gəlmir.'); return; }

        const done = withBusy(okBtn, 'Yenilənir');
        try {
          const res = await Api.auth.resetPassword(token, pass);
          close();
          toastOk((res && res.message) || 'Şifrə yeniləndi.');
        } catch (err) {
          toastErr(err.message);
          done();
        }
      }
    });
}

function openForgotPasswordModal() {
  openModal('Şifrəni unutdun?',
    '<p class="form-note" style="text-align:left;margin-bottom:12px">' +
      'Qeydiyyatda olan email ünvanını yaz — bərpa linkini göndərəcəyik.</p>' +
    '<label class="field"><span>E-poçt</span>' +
      '<input type="email" id="fpEmail" placeholder="ad@nümunə.com"></label>',
    {
      okText: 'Link göndər',
      onOk: async (overlay, close, okBtn) => {
        const email = $('#fpEmail', overlay).value.trim();
        if (!email) { toastErr('Email ünvanını yaz.'); return; }

        const done = withBusy(okBtn, 'Göndərilir');
        try {
          const res = await Api.auth.forgotPassword(email);
          close();
          toastOk((res && res.message) ||
            'Əgər bu email sistemdə qeydiyyatdadırsa, bərpa linki göndərildi.');
        } catch (err) {
          toastErr(err.message);
          done();
        }
      }
    });
}

function showAuthScreen() {
  $('#appShell').classList.add('is-hidden');
  const screen = $('#authScreen');
  screen.classList.remove('is-hidden');
  screen.style.animation = 'none';
  void screen.offsetWidth;
  screen.style.animation = '';
}

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

  $('#toRegisterLink').addEventListener('click', (e) => {
    e.preventDefault();
    $('.tab[data-tab="register"]').click();
  });
}

function initAuthForms() {

  $('#forgotLink').addEventListener('click', (e) => {
    e.preventDefault();
    openForgotPasswordModal();
  });

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
