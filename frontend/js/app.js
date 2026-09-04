/* ============================================================
   app.js — tətbiqin qabığı, naviqasiya və bütün səhifələr.
   ============================================================ */

const NAV = {
  CUSTOMER: [
    { key: 'discover',  ico: '✦', label: 'Kəşf et' },
    { key: 'bookings',  ico: '❖', label: 'Sifarişlərim' },
    { key: 'chats',     ico: '☾', label: 'Söhbətlər' },
    { key: 'ai',        ico: '◈', label: 'AI Studiya' },
    { key: 'notifs',    ico: '✉', label: 'Bildirişlər' },
    { key: 'profile',   ico: '☍', label: 'Profilim' }
  ],
  ARTIST: [
    { key: 'orders',    ico: '❖', label: 'Sifarişlər' },
    { key: 'chats',     ico: '☾', label: 'Söhbətlər' },
    { key: 'reviews',   ico: '✧', label: 'Rəylərim' },
    { key: 'analytics', ico: '◎', label: 'Analitika' },
    { key: 'ai',        ico: '◈', label: 'AI Studiya' },
    { key: 'notifs',    ico: '✉', label: 'Bildirişlər' },
    { key: 'profile',   ico: '☍', label: 'Profilim' }
  ]
};

const App = {
  route: null,
  pendingRoomId: null,
  nameCache: ChatModule.peerCache,   // eyni keşi paylaşırıq

  chatBadgeTimer: null,

  start() {
    $('#appShell').classList.remove('is-hidden');
    $('#whoName').textContent = Session.data.fullName || Session.data.email || '';
    $('#whoRole').textContent = Session.isArtist ? 'Rəssam' : 'Müştəri';

    this.buildNav();
    ChatModule.connect();
    this.nav(Session.isArtist ? 'orders' : 'discover');
    this.refreshNotifBadge();
    this.refreshChatBadge();
    // WebSocket yalnız HAZIRDA açıq otağa abunədir - başqa otaqda gələn mesajı
    // görmək üçün nişanı seyrək (20s) təzələyirik, presence yoxlaması ilə eyni məntiq.
    clearInterval(this.chatBadgeTimer);
    this.chatBadgeTimer = setInterval(() => this.refreshChatBadge(), 20000);
  },

  buildNav() {
    const items = NAV[Session.isArtist ? 'ARTIST' : 'CUSTOMER'];
    $('#sideNav').innerHTML = items.map(i =>
      '<button class="nav-item" data-route="' + i.key + '">' +
        '<span class="nav-ico">' + i.ico + '</span>' + esc(i.label) +
        (i.key === 'notifs' ? '<span class="nav-badge is-hidden" id="notifBadge">0</span>' : '') +
        (i.key === 'chats'  ? '<span class="nav-badge is-hidden" id="chatBadge">0</span>'  : '') +
      '</button>'
    ).join('');

    $$('#sideNav .nav-item').forEach(btn => {
      btn.addEventListener('click', () => {
        $('#sideNav').classList.remove('open');
        this.nav(btn.dataset.route);
      });
    });
  },

  nav(route, param) {
    this.route = route;
    $$('#sideNav .nav-item').forEach(b =>
      b.classList.toggle('is-active', b.dataset.route === route));

    const host = $('#mainView');
    window.scrollTo(0, 0);

    const pages = {
      discover: () => this.pageDiscover(host),
      artist:   () => this.pageArtist(host, param),
      customer: () => this.pageCustomer(host, param),
      bookings: () => this.pageBookings(host),
      orders:   () => this.pageOrders(host),
      chats:    () => ChatModule.renderPage(host),
      reviews:  () => this.pageReviews(host),
      analytics: () => this.pageAnalytics(host),
      ai:       () => this.pageAi(host),
      notifs:   () => this.pageNotifs(host),
      profile:  () => this.pageProfile(host)
    };
    (pages[route] || pages.discover)();
  },

  /* Bron/söhbət siyahılarında yalnız id gəlir — ad üçün ayrıca sorğu lazımdır,
     ona görə nəticələri keşləyirik. */
  resolveName(userId) { return ChatModule.resolvePeerName(userId); },

  /* ============================================================
     KƏŞF ET (müştəri)
     ============================================================ */
  async pageDiscover(host) {
    host.innerHTML =
      pageHead('Ustanı tap', 'Şəhər, stil və reytinqə görə süz') +
      '<div class="filters">' +
        '<label class="field"><span>Şəhər</span><input type="text" id="fCity" placeholder="Bakı"></label>' +
        '<label class="field"><span>Stil</span><input type="text" id="fStyle" placeholder="Realism"></label>' +
        '<label class="field"><span>Minimum reytinq</span>' +
          '<select id="fRating">' +
            '<option value="">Fərqi yoxdur</option>' +
            '<option value="3">3+</option><option value="4">4+</option><option value="4.5">4.5+</option>' +
          '</select></label>' +
        '<label class="field"><span>Minimum təcrübə</span>' +
          '<select id="fExperience">' +
            '<option value="">Fərqi yoxdur</option>' +
            '<option value="1">1+ il</option><option value="3">3+ il</option>' +
            '<option value="5">5+ il</option><option value="10">10+ il</option>' +
          '</select></label>' +
        '<label class="field"><span>Sırala</span>' +
          '<select id="fSort">' +
            '<option value="">Fərqi yoxdur</option>' +
            '<option value="rating">Ən yüksək reytinq</option>' +
            '<option value="experience">Ən təcrübəli</option>' +
          '</select></label>' +
        '<button class="btn btn-primary" id="searchBtn">Axtar</button>' +
      '</div>' +
      '<div class="section-title">Populyar ustalar</div>' +
      '<div id="popularBox">' + spinner() + '</div>' +
      '<div class="section-title">Axtarış nəticələri</div>' +
      '<div id="resultsBox">' + spinner() + '</div>';

    $('#searchBtn').addEventListener('click', () => this.runSearch());
    ['fCity', 'fStyle'].forEach(id =>
      $('#' + id).addEventListener('keydown', e => { if (e.key === 'Enter') this.runSearch(); }));
    $('#fRating').addEventListener('change', () => this.runSearch());
    $('#fExperience').addEventListener('change', () => this.runSearch());
    $('#fSort').addEventListener('change', () => this.runSearch());

    // populyarlıq siyahısı Redis-dəki baxış sayğacından formalaşır
    try {
      const popular = await Api.artists.popular(8);
      $('#popularBox').innerHTML = popular.length
        ? '<div class="grid grid-artists">' + popular.map(a => this.artistCard(a)).join('') + '</div>'
        : emptyState('Hələ heç bir profilə baxılmayıb — bir usta profilinə gir, burada görünəcək.', '✦');
      this.bindArtistCards($('#popularBox'));
    } catch (err) {
      $('#popularBox').innerHTML = emptyState(err.message, '!');
    }

    this.runSearch();
  },

  async runSearch() {
    const box = $('#resultsBox');
    if (!box) return;
    box.innerHTML = spinner();
    try {
      const list = await Api.artists.search(
        $('#fCity').value.trim(), $('#fStyle').value.trim(), $('#fRating').value,
        $('#fExperience').value, $('#fSort').value);
      box.innerHTML = list.length
        ? '<div class="grid grid-artists">' + list.map(a => this.artistCard(a)).join('') + '</div>'
        : emptyState('Bu şərtlərə uyğun usta tapılmadı.', '✦');
      this.bindArtistCards(box);
    } catch (err) {
      box.innerHTML = emptyState(err.message, '!');
    }
  },

  artistCard(a) {
    return '<article class="artist-card" data-artist="' + esc(a.userId) + '">' +
      '<div class="a-head">' +
        '<span class="avatar">' + esc(initials(a.fullName)) + '</span>' +
        '<span><span class="a-name">' + esc(a.fullName || 'Ad yoxdur') + '</span><br>' +
        '<span class="a-city">' + esc(a.city || 'Şəhər göstərilməyib') + '</span></span>' +
      '</div>' +
      '<p class="a-bio">' + esc(a.bio || 'Bu usta hələ özü haqqında yazmayıb.') + '</p>' +
      (a.styles ? '<div style="margin-top:12px">' + styleChips(a.styles) + '</div>' : '') +
      '<div class="a-foot">' + ratingBlock(a.ratingAvg, a.ratingCount) +
        '<span class="a-city">' +
          (a.experienceYears ? esc(a.experienceYears) + ' il təcrübə' : '') + '</span>' +
      '</div>' +
    '</article>';
  },

  bindArtistCards(root) {
    $$('.artist-card', root).forEach(card =>
      card.addEventListener('click', () => this.nav('artist', Number(card.dataset.artist))));
  },

  /* ============================================================
     USTA PROFİLİ (müştəri baxışı)
     ============================================================ */
  async pageArtist(host, artistUserId) {
    host.innerHTML = spinner();
    let artist, reviews = [];
    try {
      // Bu sorğu həm də Redis-də baxış sayğacını artırır (populyarlıq üçün)
      artist = await Api.artists.byUserId(artistUserId);
      reviews = await Api.reviews.forArtist(artistUserId).catch(() => []);
    } catch (err) {
      host.innerHTML = pageHead('Usta') + emptyState(err.message, '!');
      return;
    }

    host.innerHTML =
      '<button class="btn btn-ghost btn-sm" id="backBtn" style="margin-bottom:20px">‹ Geri</button>' +
      '<div class="card card-pad">' +
        '<div style="display:flex;gap:20px;align-items:center;flex-wrap:wrap">' +
          '<span class="avatar avatar-lg">' + esc(initials(artist.fullName)) + '</span>' +
          '<div style="flex:1;min-width:200px">' +
            '<div class="page-title" style="font-size:25px">' + esc(artist.fullName || '—') + '</div>' +
            '<div class="a-city" style="margin-top:5px">' + esc(artist.city || 'Şəhər göstərilməyib') +
              (artist.experienceYears ? ' · ' + esc(artist.experienceYears) + ' il təcrübə' : '') + '</div>' +
            '<div style="margin-top:10px">' + ratingBlock(artist.ratingAvg, artist.ratingCount) + '</div>' +
          '</div>' +
          '<button class="btn btn-primary" id="bookBtn">Sifariş ver</button>' +
        '</div>' +
        (artist.bio
          ? '<p style="margin:22px 0 0;font-family:var(--f-serif);font-size:17px;' +
            'line-height:1.75;color:var(--ink-dim)">' + esc(artist.bio) + '</p>'
          : '') +
        (artist.styles ? '<div style="margin-top:18px">' + styleChips(artist.styles) + '</div>' : '') +
      '</div>' +
      '<div class="section-title">Rəylər (' + reviews.length + ')</div>' +
      '<div class="rows">' +
        (reviews.length
          ? reviews.map(r =>
              '<div class="row-card"><div class="row-main">' + stars(r.rating) +
                '<div style="margin-top:6px;color:var(--ink-dim);font-size:13.5px">' +
                  esc(r.comment || '(şərh yazılmayıb)') + '</div>' +
                '<div class="row-meta">' + esc(fmtDay(r.createdAt)) + '</div>' +
                (r.artistReply
                  ? '<div style="margin-top:10px;padding:10px 12px;background:var(--bg-soft,rgba(0,0,0,.03));' +
                      'border-radius:8px;font-size:13px">' +
                      '<b>Ustanın cavabı:</b> ' + esc(r.artistReply) +
                    '</div>'
                  : '') +
              '</div></div>').join('')
          : emptyState('Bu usta haqqında hələ rəy yoxdur.', '✧')) +
      '</div>';

    $('#backBtn').addEventListener('click', () => this.nav('discover'));
    $('#bookBtn').addEventListener('click', () => this.promptBooking(artist));
  },

  /* ---------- yeni sifariş ---------- */
  promptBooking(artist) {
    const soon = new Date(Date.now() + 86400000);
    const local = new Date(soon.getTime() - soon.getTimezoneOffset() * 60000)
                    .toISOString().slice(0, 16);

    openModal('Sifariş: ' + (artist.fullName || ''),
      '<label class="field"><span>Tarix və saat</span>' +
        '<input type="datetime-local" id="bDate" value="' + local + '"></label>' +
      '<label class="field" style="margin-top:14px"><span>Nə istəyirsən?</span>' +
        '<textarea id="bNotes" placeholder="Bilək üzərində kiçik minimalist ilan, qara-ağ…"></textarea></label>' +
      '<div class="field-row" style="margin-top:14px">' +
        '<label class="field"><span>Təxmini büdcə (AZN)</span>' +
          '<input type="number" id="bPrice" min="0" step="10" placeholder="150"></label>' +
        '<label class="field"><span>Eskiz linki</span>' +
          '<input type="text" id="bUrl" placeholder="https://…"></label>' +
      '</div>',
      {
        okText: 'Sifarişi göndər',
        onOk: async (ov, close, okBtn) => {
          const dateVal = $('#bDate', ov).value;
          if (!dateVal) { toastErr('Tarix seç.'); return; }
          const done = withBusy(okBtn, 'Göndərilir');
          try {
            await Api.bookings.create({
              customerId: Session.userId,
              artistId: artist.userId,
              // datetime-local "YYYY-MM-DDTHH:mm" verir, backend LocalDateTime saniyə gözləyir
              bookingDate: dateVal.length === 16 ? dateVal + ':00' : dateVal,
              notes: $('#bNotes', ov).value.trim(),
              tattooConceptUrl: $('#bUrl', ov).value.trim(),
              estimatedPrice: Number($('#bPrice', ov).value) || null
            });
            notifyQuietly(artist.userId, '', 'Yeni sifariş',
              (Session.data.fullName || 'Bir müştəri') + ' sizə sifariş göndərdi.');
            close();
            toastOk('Sifariş göndərildi.');
            this.nav('bookings');
          } catch (err) {
            toastErr(err.message);
            done();
          }
        }
      });
  },

  /* ============================================================
     MÜŞTƏRİ PROFİLİ (rəssam baxışı — sadəcə oxumaq üçün)
     ============================================================ */
  async pageCustomer(host, customerUserId) {
    host.innerHTML = spinner();
    let user, pastTattoos = [];
    try {
      user = await Api.users.get(customerUserId);
      // Artıq server özü qərar verir: qiymət/qeyd kimi məxfi sahələr heç
      // qayıtmır, usta adı isə çağıranın premium statusuna görə artıq server
      // tərəfində baş hərflərə endirilib ya da tam gəlir (bax
      // BookingService.getCompletedSummaryForCustomer) - burda əlavə maskalama
      // etməyə ehtiyac yoxdur, çünki əvvəlki versiya tam adı hər halda
      // brauzerə göndərib sonra gizlədirdi (yəni real qorunma yox idi).
      pastTattoos = await Api.bookings.completedSummary(customerUserId).catch(() => []);
      pastTattoos.sort((a, b) => (b.bookingDate || '').localeCompare(a.bookingDate || ''));
    } catch (err) {
      host.innerHTML = pageHead('Müştəri') + emptyState(err.message, '!');
      return;
    }

    const isPremiumViewer = !!(Session.data && Session.data.premium);
    const emailRow = user.email
      ? '<dt>E-poçt</dt><dd>' + esc(user.email) + '</dd>'
      : '';

    host.innerHTML =
      '<button class="btn btn-ghost btn-sm" id="backBtn" style="margin-bottom:20px">‹ Geri</button>' +
      '<div class="card card-pad">' +
        '<div style="display:flex;gap:20px;align-items:center;flex-wrap:wrap">' +
          '<span class="avatar avatar-lg">' + esc(initials(user.fullName)) + '</span>' +
          '<div style="flex:1;min-width:200px">' +
            '<div class="page-title" style="font-size:25px">' + esc(user.fullName || '—') + '</div>' +
            '<div class="a-city" style="margin-top:5px">' + esc(user.city || 'Şəhər göstərilməyib') + '</div>' +
          '</div>' +
        '</div>' +
        '<dl class="kv" style="margin-top:22px">' +
          emailRow +
          '<dt>Qeydiyyat</dt><dd>' + esc(fmtDay(user.createdAt)) + '</dd>' +
        '</dl>' +
      '</div>' +
      '<div class="section-title">Keçmiş tatuajlar (' + pastTattoos.length + ')</div>' +
      (!isPremiumViewer && pastTattoos.length
        ? '<p class="form-note" style="text-align:left;margin:-6px 0 14px">' +
            'Ustaların tam adını görmək üçün premium abunəçi olmalısan — hələlik ' +
            'yalnız baş hərfləri göstərilir. <a href="#" id="goPremium" ' +
            'style="color:var(--brass)">Profilimdən aç</a>.</p>'
        : '') +
      '<div class="rows">' +
        (pastTattoos.length
          ? pastTattoos.map(t => {
              return '<div class="row-card"><div class="row-main">' +
                '<div class="row-title">' + esc(t.artistName || '—') + '</div>' +
                (t.description ? '<div class="row-meta">' + esc(t.description) + '</div>' : '') +
                '<div class="row-meta">' + esc(fmtDay(t.bookingDate)) + '</div>' +
              '</div></div>';
            }).join('')
          : emptyState('Bu müştərinin hələ tamamlanmış sifarişi yoxdur.', '❖')) +
      '</div>';

    $('#backBtn').addEventListener('click', () => this.nav('orders'));
    const goPremium = $('#goPremium');
    if (goPremium) goPremium.addEventListener('click', (e) => { e.preventDefault(); this.nav('profile'); });
  },

  /* ============================================================
     SİFARİŞLƏRİM (müştəri)
     ============================================================ */
  async pageBookings(host) {
    host.innerHTML = pageHead('Sifarişlərim', 'Bütün sorğuların və vəziyyətləri') + spinner();
    let list;
    try {
      list = await Api.bookings.forCustomer(Session.userId);
    } catch (err) {
      host.innerHTML = pageHead('Sifarişlərim') + emptyState(err.message, '!');
      return;
    }
    if (!list.length) {
      host.innerHTML = pageHead('Sifarişlərim') +
        emptyState('Hələ sifariş yoxdur. "Kəşf et" bölməsindən usta seç.', '❖');
      return;
    }

    await Promise.all(list.map(b => this.resolveName(b.artistId)));
    list.sort((a, b) => b.id - a.id);

    host.innerHTML = pageHead('Sifarişlərim', 'Bütün sorğuların və vəziyyətləri') +
      '<div class="rows">' + list.map(b => {
        const name = this.nameCache.get(b.artistId) || ('Usta #' + b.artistId);
        return '<div class="row-card">' +
          '<span class="avatar avatar-sm">' + esc(initials(name)) + '</span>' +
          '<div class="row-main">' +
            '<div class="row-title">' + esc(name) + '</div>' +
            '<div class="row-meta">' + esc(fmtDateTime(b.bookingDate)) +
              ' · ' + esc(fmtMoney(b.estimatedPrice)) + '</div>' +
            (b.notes ? '<div class="row-meta">' + esc(b.notes) + '</div>' : '') +
          '</div>' +
          statusBadge(b.status) +
          '<div class="row-actions">' +
            '<button class="btn btn-ghost btn-sm" data-chat="' + b.id + '" data-artist="' +
              b.artistId + '">Söhbət</button>' +
            (b.status === 'COMPLETED'
              ? '<button class="btn btn-brass btn-sm" data-review="' + b.id + '">Rəy yaz</button>' : '') +
            (b.status === 'PENDING' || b.status === 'CONFIRMED'
              ? '<button class="btn btn-danger btn-sm" data-cancel="' + b.id + '">Ləğv et</button>' : '') +
          '</div>' +
        '</div>';
      }).join('') + '</div>';

    $$('[data-chat]', host).forEach(btn => btn.addEventListener('click', () =>
      this.openChatFor(Number(btn.dataset.chat), Session.userId, Number(btn.dataset.artist), btn)));
    $$('[data-review]', host).forEach(btn => btn.addEventListener('click', () =>
      this.promptReview(Number(btn.dataset.review))));
    $$('[data-cancel]', host).forEach(btn => btn.addEventListener('click', () =>
      this.changeStatus(Number(btn.dataset.cancel), 'CANCELLED', btn, 'bookings')));
  },

  /* ============================================================
     GƏLƏN SİFARİŞLƏR (rəssam)
     ============================================================ */
  async pageOrders(host) {
    host.innerHTML = pageHead('Gələn sifarişlər', 'Təsdiqlə, tamamla və ya ləğv et') + spinner();
    let list;
    try {
      list = await Api.bookings.forArtist(Session.userId);
    } catch (err) {
      host.innerHTML = pageHead('Gələn sifarişlər') + emptyState(err.message, '!');
      return;
    }
    if (!list.length) {
      host.innerHTML = pageHead('Gələn sifarişlər') +
        emptyState('Hələ sifariş gəlməyib. Profilini doldurmaq görünürlüyünü artırır.', '❖');
      return;
    }

    await Promise.all(list.map(b => this.resolveName(b.customerId)));
    list.sort((a, b) => b.id - a.id);

    host.innerHTML = pageHead('Gələn sifarişlər', 'Təsdiqlə, tamamla və ya ləğv et') +
      '<div class="rows">' + list.map(b => {
        const name = this.nameCache.get(b.customerId) || ('Müştəri #' + b.customerId);
        return '<div class="row-card">' +
          '<span class="avatar avatar-sm" data-viewcustomer="' + b.customerId +
            '" style="cursor:pointer">' + esc(initials(name)) + '</span>' +
          '<div class="row-main">' +
            '<div class="row-title" data-viewcustomer="' + b.customerId +
              '" style="cursor:pointer">' + esc(name) + '</div>' +
            '<div class="row-meta">' + esc(fmtDateTime(b.bookingDate)) +
              ' · ' + esc(fmtMoney(b.estimatedPrice)) + '</div>' +
            (b.notes ? '<div class="row-meta">' + esc(b.notes) + '</div>' : '') +
          '</div>' +
          statusBadge(b.status) +
          '<div class="row-actions">' +
            '<button class="btn btn-ghost btn-sm" data-viewprofile="' + b.customerId +
              '">Profilə bax</button>' +
            '<button class="btn btn-ghost btn-sm" data-chat="' + b.id + '" data-customer="' +
              b.customerId + '">Söhbət</button>' +
            (b.status === 'PENDING'
              ? '<button class="btn btn-primary btn-sm" data-set="CONFIRMED" data-id="' +
                b.id + '">Təsdiqlə</button>' : '') +
            (b.status === 'CONFIRMED'
              ? '<button class="btn btn-brass btn-sm" data-set="COMPLETED" data-id="' +
                b.id + '">Tamamla</button>' : '') +
            (b.status === 'PENDING' || b.status === 'CONFIRMED'
              ? '<button class="btn btn-danger btn-sm" data-set="CANCELLED" data-id="' +
                b.id + '">Ləğv et</button>' : '') +
          '</div>' +
        '</div>';
      }).join('') + '</div>';

    $$('[data-chat]', host).forEach(btn => btn.addEventListener('click', () =>
      this.openChatFor(Number(btn.dataset.chat), Number(btn.dataset.customer), Session.userId, btn)));
    $$('[data-set]', host).forEach(btn => btn.addEventListener('click', () =>
      this.changeStatus(Number(btn.dataset.id), btn.dataset.set, btn, 'orders')));
    $$('[data-viewprofile]', host).forEach(el => el.addEventListener('click', () =>
      this.nav('customer', Number(el.dataset.viewprofile))));
    $$('[data-viewcustomer]', host).forEach(el => el.addEventListener('click', () =>
      this.nav('customer', Number(el.dataset.viewcustomer))));
  },

  async changeStatus(bookingId, status, btn, backRoute) {
    const done = withBusy(btn, '');
    try {
      const updated = await Api.bookings.setStatus(bookingId, status);
      const peerId = Session.isArtist ? updated.customerId : updated.artistId;
      notifyQuietly(peerId, '', 'Sifariş vəziyyəti dəyişdi',
        esc(fmtDay(updated.bookingDate)) + ' tarixli sifariş → ' + (STATUS_AZ[status] || status));
      toastOk('Vəziyyət yeniləndi: ' + (STATUS_AZ[status] || status));
      this.nav(backRoute);
    } catch (err) {
      toastErr(err.message);
      done();
    }
  },

  /* Söhbət otağı bookingId üzrə idempotent yaradılır — hər iki tərəf
     eyni düyməni basanda eyni otağa düşür. */
  async openChatFor(bookingId, customerId, artistId, btn) {
    const done = withBusy(btn, '');
    try {
      const room = await Api.chat.getOrCreateRoom({ customerId, artistId, bookingId });
      this.pendingRoomId = room.id;
      this.nav('chats');
    } catch (err) {
      toastErr(err.message);
      done();
    }
  },

  /* ---------- rəy ---------- */
  promptReview(bookingId) {
    let picked = 5;
    const { overlay } = openModal('Rəy yaz',
      '<div class="field"><span>Qiymətləndirmə</span>' +
        '<div class="star-pick" id="starPick">' +
          [1,2,3,4,5].map(i => '<button type="button" class="on" data-v="' + i + '">&#9733;</button>').join('') +
        '</div></div>' +
      '<label class="field" style="margin-top:16px"><span>Şərh</span>' +
        '<textarea id="rComment" placeholder="Təcrübən barədə bir neçə söz…"></textarea></label>',
      {
        okText: 'Göndər',
        onOk: async (ov, close, okBtn) => {
          const done = withBusy(okBtn, 'Göndərilir');
          try {
            await Api.reviews.create({
              bookingId,
              customerId: Session.userId,
              rating: picked,
              comment: $('#rComment', ov).value.trim()
            });
            close();
            toastOk('Rəyin üçün təşəkkür!');
            this.nav('bookings');
          } catch (err) {
            toastErr(err.message);
            done();
          }
        }
      });

    $$('#starPick button', overlay).forEach(b =>
      b.addEventListener('click', () => {
        picked = Number(b.dataset.v);
        $$('#starPick button', overlay).forEach(x =>
          x.classList.toggle('on', Number(x.dataset.v) <= picked));
      }));
  },

  /* ============================================================
     RƏYLƏRİM (rəssam)
     ============================================================ */
  async pageReviews(host) {
    host.innerHTML = pageHead('Rəylərim', 'Müştərilərin nə dediyi') + spinner();
    let reviews, profile;
    try {
      reviews = await Api.reviews.forArtist(Session.userId);
      profile = await Api.artists.byUserId(Session.userId).catch(() => null);
    } catch (err) {
      host.innerHTML = pageHead('Rəylərim') + emptyState(err.message, '!');
      return;
    }

    host.innerHTML = pageHead('Rəylərim', 'Müştərilərin nə dediyi') +
      (profile
        ? '<div class="card card-pad" style="margin-bottom:24px;text-align:center">' +
            '<div style="font-family:var(--f-display);font-size:40px;color:var(--brass)">' +
              (Number(profile.ratingAvg) || 0).toFixed(1) + '</div>' +
            stars(profile.ratingAvg) +
            '<div class="row-meta" style="margin-top:8px">' +
              esc(profile.ratingCount || 0) + ' rəy əsasında</div>' +
          '</div>'
        : '') +
      '<div class="rows">' +
        (reviews.length
          ? reviews.map(r =>
              '<div class="row-card"><div class="row-main">' + stars(r.rating) +
                '<div style="margin-top:6px;color:var(--ink-dim);font-size:13.5px">' +
                  esc(r.comment || '(şərh yazılmayıb)') + '</div>' +
                '<div class="row-meta">' + esc(fmtDay(r.createdAt)) + '</div>' +
                (r.artistReply
                  ? '<div style="margin-top:10px;padding:10px 12px;background:var(--bg-soft,rgba(0,0,0,.03));' +
                      'border-radius:8px;font-size:13px">' +
                      '<b>Sənin cavabın:</b> ' + esc(r.artistReply) +
                    '</div>'
                  : '<button class="btn btn-ghost btn-sm reply-btn" data-id="' + r.id + '" style="margin-top:10px">Cavab yaz</button>') +
              '</div></div>').join('')
          : emptyState('Hələ rəy yoxdur. Tamamlanmış sifarişdən sonra müştəri rəy yaza bilər.', '✧')) +
      '</div>';

    $$('.reply-btn', host).forEach(btn => {
      btn.addEventListener('click', () => {
        const reviewId = Number(btn.dataset.id);
        openModal('Rəyə cavab yaz',
          '<label class="field"><span>Cavabın</span>' +
            '<textarea id="replyText" placeholder="Təşəkkür edirik, yenidən gözləyirik…"></textarea></label>',
          {
            okText: 'Göndər',
            onOk: async (ov, close, okBtn) => {
              const text = $('#replyText', ov).value.trim();
              if (!text) { toastErr('Cavab boş ola bilməz.'); return; }
              const done = withBusy(okBtn, 'Göndərilir');
              try {
                await Api.reviews.reply(reviewId, { artistId: Session.userId, reply: text });
                close();
                toastOk('Cavab əlavə edildi.');
                this.pageReviews(host);
              } catch (err) {
                toastErr(err.message);
                done();
              }
            }
          });
      });
    });
  },

  /* ============================================================
     ANALİTİKA (rəssam)
     ============================================================ */
  async pageAnalytics(host) {
    host.innerHTML = pageHead('Analitika', 'Sifarişlərin, qazancın və təkliflərin xülasəsi') + spinner();

    let bStats = null, oStats = null, views = 0;
    try {
      [bStats, oStats, views] = await Promise.all([
        Api.bookings.artistStats(Session.userId).catch(() => null),
        Api.chat.offerStats(Session.userId).catch(() => null),
        Api.artists.viewCount(Session.userId).catch(() => 0)
      ]);
    } catch (err) {
      host.innerHTML = pageHead('Analitika') + emptyState(err.message, '!');
      return;
    }

    const tile = (value, label) =>
      '<div class="card card-pad" style="text-align:center;flex:1;min-width:140px">' +
        '<div style="font-family:var(--f-display);font-size:32px;color:var(--brass)">' + esc(value) + '</div>' +
        '<div class="row-meta" style="margin-top:6px">' + esc(label) + '</div>' +
      '</div>';

    const acceptancePct = oStats && oStats.acceptanceRate != null
      ? Math.round(oStats.acceptanceRate * 100) + '%'
      : '—';

    host.innerHTML = pageHead('Analitika', 'Sifarişlərin, qazancın və təkliflərin xülasəsi') +
      '<div class="section-title" style="margin-top:0">Sifarişlər</div>' +
      '<div style="display:flex;gap:16px;flex-wrap:wrap;margin-bottom:24px">' +
        tile(bStats ? bStats.totalBookings : '—', 'Ümumi sifariş') +
        tile(bStats ? bStats.pendingBookings : '—', 'Gözləyən') +
        tile(bStats ? bStats.confirmedBookings : '—', 'Təsdiqlənmiş') +
        tile(bStats ? bStats.completedBookings : '—', 'Tamamlanmış') +
        tile(bStats ? bStats.cancelledBookings : '—', 'Ləğv edilmiş') +
      '</div>' +
      '<div class="section-title">Qazanc</div>' +
      '<div style="display:flex;gap:16px;flex-wrap:wrap;margin-bottom:24px">' +
        tile(bStats ? (Number(bStats.totalEarnings) || 0).toFixed(0) + ' AZN' : '—', 'Tamamlanmış sifarişlərdən') +
        tile(views, 'Profil baxışı') +
      '</div>' +
      '<div class="section-title">Qiymət təklifləri</div>' +
      '<div style="display:flex;gap:16px;flex-wrap:wrap">' +
        tile(oStats ? oStats.totalOffers : '—', 'Göndərilən təklif') +
        tile(oStats ? oStats.accepted : '—', 'Qəbul edilən') +
        tile(oStats ? oStats.rejected : '—', 'Rədd edilən') +
        tile(acceptancePct, 'Qəbul nisbəti') +
      '</div>';
  },

  /* ============================================================
     AI STUDİYA
     ============================================================ */
  pageAi(host) {
    host.innerHTML =
      pageHead('AI Studiya', 'Konsept məsləhəti al və ya eskizini analiz etdir') +
      '<div class="grid grid-2">' +
        '<div class="card card-pad">' +
          '<div class="section-title" style="margin-top:0">İdeya məsləhəti</div>' +
          '<label class="field"><span>Nə düşünürsən?</span>' +
            '<textarea id="aiPrompt" placeholder="Bilək üzərində kiçik minimalist ilan, qara-ağ…"></textarea></label>' +
          '<label class="field" style="margin-top:13px"><span>Stil (istəyə bağlı)</span>' +
            '<input type="text" id="aiStyle" placeholder="Minimalist"></label>' +
          '<button class="btn btn-primary btn-block" id="aiIdeaBtn">Məsləhət al</button>' +
          '<div id="aiIdeaOut"></div>' +
        '</div>' +
        '<div class="card card-pad">' +
          '<div class="section-title" style="margin-top:0">Eskiz analizi</div>' +
          '<div class="drop" id="aiDrop">Şəkli bura sürüşdür və ya klikləyib seç</div>' +
          '<input type="file" id="aiFile" accept="image/*" class="is-hidden">' +
          '<div id="aiPreview"></div>' +
          '<label class="field" style="margin-top:13px"><span>Sual (istəyə bağlı)</span>' +
            '<input type="text" id="aiImgPrompt" placeholder="Bu eskizi analiz et"></label>' +
          '<button class="btn btn-primary btn-block" id="aiImgBtn" disabled>Analiz et</button>' +
          '<div id="aiImgOut"></div>' +
        '</div>' +
      '</div>';

    $('#aiIdeaBtn').addEventListener('click', async (e) => {
      const prompt = $('#aiPrompt').value.trim();
      if (!prompt) { toastErr('Əvvəlcə ideyanı yaz.'); return; }
      const done = withBusy(e.currentTarget, 'Düşünür');
      $('#aiIdeaOut').innerHTML = '';
      try {
        const res = await Api.ai.generateIdea({
          userPrompt: prompt, preferredStyle: $('#aiStyle').value.trim()
        });
        $('#aiIdeaOut').innerHTML = '<div class="ai-out">' + esc(res.aiRecommendation) + '</div>';
      } catch (err) { toastErr(err.message); }
      finally { done(); }
    });

    let chosen = null;
    const drop = $('#aiDrop'), fileInput = $('#aiFile');

    const takeFile = (file) => {
      if (!file || !file.type.startsWith('image/')) { toastErr('Yalnız şəkil faylı seç.'); return; }
      chosen = file;
      drop.textContent = file.name;
      $('#aiImgBtn').disabled = false;
      const reader = new FileReader();
      reader.onload = () => {
        $('#aiPreview').innerHTML =
          '<img class="preview-img" alt="Seçilmiş eskiz" src="' + reader.result + '">';
      };
      reader.readAsDataURL(file);
    };

    drop.addEventListener('click', () => fileInput.click());
    fileInput.addEventListener('change', () => takeFile(fileInput.files[0]));
    drop.addEventListener('dragover', (e) => { e.preventDefault(); drop.classList.add('over'); });
    drop.addEventListener('dragleave', () => drop.classList.remove('over'));
    drop.addEventListener('drop', (e) => {
      e.preventDefault(); drop.classList.remove('over');
      takeFile(e.dataTransfer.files[0]);
    });

    $('#aiImgBtn').addEventListener('click', async (e) => {
      if (!chosen) return;
      const done = withBusy(e.currentTarget, 'Analiz edir');
      $('#aiImgOut').innerHTML = '';
      try {
        const res = await Api.ai.analyzeImage(chosen, $('#aiImgPrompt').value);
        $('#aiImgOut').innerHTML = '<div class="ai-out">' + esc(res.aiRecommendation) + '</div>';
      } catch (err) { toastErr(err.message); }
      finally { done(); }
    });
  },

  /* ============================================================
     BİLDİRİŞLƏR
     ============================================================ */
  async pageNotifs(host) {
    host.innerHTML = pageHead('Bildirişlər') + spinner();
    let list;
    try {
      list = await Api.notifications.forUser(Session.userId);
    } catch (err) {
      host.innerHTML = pageHead('Bildirişlər') + emptyState(err.message, '!');
      return;
    }
    if (!list.length) {
      host.innerHTML = pageHead('Bildirişlər') + emptyState('Bildiriş yoxdur.', '✉');
      this.refreshNotifBadge();
      return;
    }

    list.sort((a, b) => b.id - a.id);
    host.innerHTML = pageHead('Bildirişlər') +
      '<div class="rows">' + list.map(n => {
        const isRead = (n.read !== undefined) ? n.read : n.isRead;
        return '<div class="notif ' + (isRead ? '' : 'unread') +
          '" data-notif="' + n.id + '" style="cursor:pointer">' +
          '<div style="flex:1">' +
            '<div class="notif-title">' + esc(n.title) + '</div>' +
            '<div class="notif-msg">' + esc(n.message) + '</div>' +
            '<div class="notif-time">' + esc(fmtDateTime(n.createdAt)) + '</div>' +
          '</div>' +
          (isRead ? '' : '<button class="btn btn-ghost btn-sm" data-read="' + n.id + '">Oxundu</button>') +
        '</div>';
      }).join('') + '</div>';

    $$('[data-read]', host).forEach(btn => btn.addEventListener('click', async (e) => {
      e.stopPropagation();
      const done = withBusy(btn, '');
      try {
        await Api.notifications.markRead(Number(btn.dataset.read));
        this.nav('notifs');
      } catch (err) { toastErr(err.message); done(); }
    }));

    // Bildirişə tıklayanda ilgili bölməyə keçirik. Hazırda bütün bildirişlər
    // sifariş/söhbət mövzuludur (bax notifyQuietly çağırışlarına), ona görə
    // rola görə ən uyğun siyahıya yönləndiririk; "Oxundu" düyməsi öz klikini
    // yuxarı ötürmür (stopPropagation), ona görə iki iş toqquşmur.
    $$('[data-notif]', host).forEach(row => row.addEventListener('click', () => {
      Api.notifications.markRead(Number(row.dataset.notif)).catch(() => {});
      this.nav(Session.isArtist ? 'orders' : 'bookings');
    }));

    this.refreshNotifBadge();
  },

  async refreshNotifBadge() {
    const badge = $('#notifBadge');
    if (!badge || !Session.userId) return;
    try {
      const list = await Api.notifications.forUser(Session.userId);
      const unread = list.filter(n => !((n.read !== undefined) ? n.read : n.isRead)).length;
      badge.textContent = unread;
      badge.classList.toggle('is-hidden', unread === 0);
    } catch (e) {
      badge.classList.add('is-hidden');
    }
  },

  async refreshChatBadge() {
    const badge = $('#chatBadge');
    if (!badge || !Session.userId) return;
    try {
      const res = await Api.chat.unreadCount(Session.userId);
      const count = res && res.count ? res.count : 0;
      badge.textContent = count;
      badge.classList.toggle('is-hidden', count === 0);
    } catch (e) {
      badge.classList.add('is-hidden');
    }
  },

  /* ============================================================
     PROFİL
     ============================================================ */
  async pageProfile(host) {
    host.innerHTML = pageHead('Profilim') + spinner();

    let user, artist = null;
    try {
      user = await Api.users.get(Session.userId);
      if (Session.isArtist) artist = await Api.artists.byUserId(Session.userId).catch(() => null);
    } catch (err) {
      host.innerHTML = pageHead('Profilim') + emptyState(err.message, '!');
      return;
    }

    const years = (artist && artist.experienceYears !== null && artist.experienceYears !== undefined)
      ? artist.experienceYears : '';

    host.innerHTML = pageHead('Profilim', 'Məlumatlarını istənilən vaxt yenilə') +
      '<div class="grid grid-2">' +

        '<div class="card card-pad">' +
          '<div class="section-title" style="margin-top:0">Hesab</div>' +
          '<div style="display:flex;gap:16px;align-items:center;margin-bottom:20px">' +
            '<span class="avatar avatar-lg">' + esc(initials(user.fullName)) + '</span>' +
            '<div><div class="a-name">' + esc(user.fullName || '—') + '</div>' +
              '<div class="a-city">' + esc(user.email) + ' · ' +
                (Session.isArtist ? 'Rəssam' : 'Müştəri') + '</div></div>' +
          '</div>' +
          '<label class="field"><span>Ad, soyad</span>' +
            '<input type="text" id="pName" value="' + esc(user.fullName || '') + '"></label>' +
          '<label class="field" style="margin-top:13px"><span>Şəhər</span>' +
            '<input type="text" id="pCity" value="' + esc(user.city || '') + '"></label>' +
          '<label class="field" style="margin-top:13px"><span>Profil şəkli linki</span>' +
            '<input type="text" id="pAvatar" value="' + esc(user.profileImageUrl || '') +
              '" placeholder="https://…"></label>' +
          '<label class="field-check" style="margin-top:15px">' +
            '<input type="checkbox" id="pPremium"' + (user.premium ? ' checked' : '') + '>' +
            '<span>Premium abunəçi (demo) — başqasının profilində keçmiş sifarişlərin ' +
              'ustalarının tam adını yalnız premium istifadəçilər görə bilir</span>' +
          '</label>' +
          '<button class="btn btn-primary btn-block" style="margin-top:16px" id="saveUserBtn">Hesabı yenilə</button>' +
        '</div>' +

        (Session.isArtist
          ? '<div class="card card-pad">' +
              '<div class="section-title" style="margin-top:0">Usta profili</div>' +
              '<div style="margin-bottom:18px">' +
                ratingBlock(artist ? artist.ratingAvg : 0, artist ? artist.ratingCount : 0) + '</div>' +
              '<label class="field"><span>Haqqımda</span>' +
                '<textarea id="pBio" placeholder="Təcrübən, yanaşman, sevdiyin işlər…">' +
                  esc(artist ? (artist.bio || '') : '') + '</textarea></label>' +
              '<div class="field-row" style="margin-top:13px">' +
                '<label class="field"><span>Təcrübə (il)</span>' +
                  '<input type="number" id="pYears" min="0" max="70" value="' + esc(years) + '"></label>' +
                '<label class="field"><span>Stillər</span>' +
                  '<input type="text" id="pStyles" placeholder="Realism, Blackwork" value="' +
                    esc(artist ? (artist.styles || '') : '') + '"></label>' +
              '</div>' +
              '<p class="form-note" style="text-align:left;margin-top:10px">' +
                'Stilləri vergüllə ayır — axtarışda məhz bu sahə üzrə süzgəc işləyir.</p>' +
              '<button class="btn btn-primary btn-block" id="saveArtistBtn">Usta profilini yenilə</button>' +
            '</div>'
          : '<div class="card card-pad">' +
              '<div class="section-title" style="margin-top:0">Hesab haqqında</div>' +
              '<dl class="kv"><dt>E-poçt</dt><dd>' + esc(user.email) + '</dd></dl>' +
              '<dl class="kv"><dt>Rol</dt><dd>Müştəri</dd></dl>' +
              '<dl class="kv"><dt>Qeydiyyat</dt><dd>' + esc(fmtDay(user.createdAt)) + '</dd></dl>' +
              '<p class="form-note" style="text-align:left;margin-top:18px">' +
                'E-poçt və rol qeydiyyatdan sonra dəyişdirilmir.</p>' +
            '</div>') +

      '</div>';

    $('#saveUserBtn').addEventListener('click', async (e) => {
      const done = withBusy(e.currentTarget, 'Yadda saxlanır');
      try {
        const updated = await Api.users.update(Session.userId, {
          fullName: $('#pName').value.trim(),
          city: $('#pCity').value.trim(),
          profileImageUrl: $('#pAvatar').value.trim(),
          premium: $('#pPremium').checked
        });
        Session.patch({ fullName: updated.fullName, city: updated.city, premium: !!updated.premium });
        $('#whoName').textContent = updated.fullName;
        this.nameCache.set(Session.userId, updated.fullName);
        toastOk('Hesab yeniləndi.');
      } catch (err) { toastErr(err.message); }
      finally { done(); }
    });

    const saveArtist = $('#saveArtistBtn');
    if (saveArtist) {
      saveArtist.addEventListener('click', async (e) => {
        const done = withBusy(e.currentTarget, 'Yadda saxlanır');
        try {
          const y = $('#pYears').value;
          await Api.artists.updateProfile(Session.userId, {
            bio: $('#pBio').value.trim(),
            experienceYears: y === '' ? null : Number(y),
            styles: $('#pStyles').value.trim()
          });
          toastOk('Usta profili yeniləndi.');
        } catch (err) { toastErr(err.message); }
        finally { done(); }
      });
    }
  }
};

/* ============================================================
   BAŞLANĞIC
   ============================================================ */
document.addEventListener('DOMContentLoaded', () => {
  initAuthTabs();
  initAuthForms();

  $('#logoutBtn').addEventListener('click', logout);
  $('#navToggle').addEventListener('click', () => $('#sideNav').classList.toggle('open'));

  // Token bitəndə və ya icazə olmayanda avtomatik giriş ekranına qaytarırıq
  window.addEventListener('signum:unauthorized', () => {
    ChatModule.disconnect();
    $('#appShell').classList.add('is-hidden');
    showAuthScreen();
    // toastErr yox, birbaşa toast(): eyni anda uğursuz olan başqa sorğuların
    // öz toastErr çağırışları susdurulan pəncərədədir, amma bu KANONIK mesaj
    // hər zaman görünməlidir.
    toast('Sessiya bitib. Yenidən daxil ol.', 'err');
  });

  runSplash();
});
