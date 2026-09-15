/* ============================================================
   app.js — tətbiqin qabığı, naviqasiya və bütün səhifələr.
   ============================================================ */

const NAV = {
  ADMIN: [
    { key: 'admin',   ico: '⚑', label: 'Admin paneli' },
    { key: 'profile', ico: '⍍', label: 'Profilim' }
  ],
  CUSTOMER: [
    { key: 'discover',  ico: '✦', label: 'Kəşf et' },
    { key: 'bookings',  ico: '❖', label: 'Sifarişlərim' },
    { key: 'following', ico: '♡', label: 'İzlədiklərim' },
    { key: 'chats',     ico: '☾', label: 'Söhbətlər' },
    { key: 'ai',        ico: '◈', label: 'AI Studiya' },
    { key: 'notifs',    ico: '✉', label: 'Bildirişlər' },
    { key: 'profile',   ico: '☍', label: 'Profilim' }
  ],
  ARTIST: [
    { key: 'orders',    ico: '❖', label: 'Sifarişlər' },
    { key: 'chats',     ico: '☾', label: 'Söhbətlər' },
    { key: 'reviews',   ico: '✧', label: 'Rəylərim' },
    { key: 'availability', ico: '◷', label: 'Təqvim' },
    { key: 'analytics', ico: '◎', label: 'Analitika' },
    { key: 'ai',        ico: '◈', label: 'AI Studiya' },
    { key: 'notifs',    ico: '✉', label: 'Bildirişlər' },
    { key: 'profile',   ico: '☍', label: 'Profilim' }
  ]
};

const App = {
  route: null,
  pendingRoomId: null,

  /* İzlənilən ustaların userId-ləri. Usta kartları hər biri üçün ayrıca sorğu
     atmasın deyə bir dəfə yükləyib keshləyirik; hər izlə/çıx əməliyyatında yenilənir. */
  followingIds: new Set(),
  followingLoaded: false,
  nameCache: ChatModule.peerCache,   // eyni keşi paylaşırıq

  chatBadgeTimer: null,

  start() {
    $('#appShell').classList.remove('is-hidden');
    $('#whoName').textContent = Session.data.fullName || Session.data.email || '';
    $('#whoRole').textContent = Session.isAdmin ? 'Admin' : (Session.isArtist ? 'Rəssam' : 'Müştəri');

    this.followingIds = new Set();
    this.followingLoaded = false;

    this.buildNav();
    ChatModule.connect();
    this.nav(Session.isAdmin ? 'admin' : (Session.isArtist ? 'orders' : 'discover'));
    this.refreshNotifBadge();
    this.refreshChatBadge();
    // WebSocket yalnız HAZIRDA açıq otağa abunədir - başqa otaqda gələn mesajı
    // görmək üçün nişanı seyrək (20s) təzələyirik, presence yoxlaması ilə eyni məntiq.
    clearInterval(this.chatBadgeTimer);
    this.chatBadgeTimer = setInterval(() => this.refreshChatBadge(), 20000);
  },

  buildNav() {
    const items = NAV[Session.isAdmin ? 'ADMIN' : (Session.isArtist ? 'ARTIST' : 'CUSTOMER')];
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
      following: () => this.pageFollowing(host),
      orders:   () => this.pageOrders(host),
      chats:    () => ChatModule.renderPage(host),
      reviews:  () => this.pageReviews(host),
      availability: () => this.pageAvailability(host),
      analytics: () => this.pageAnalytics(host),
      ai:       () => this.pageAi(host),
      admin:    () => this.pageAdmin(host),
      notifs:   () => this.pageNotifs(host),
      profile:  () => this.pageProfile(host)
    };
    (pages[route] || pages.discover)();
  },

  /* Bron/söhbət siyahılarında yalnız id gəlir — ad üçün ayrıca sorğu lazımdır,
     ona görə nəticələri keşləyirik. */
  resolveName(userId) { return ChatModule.resolvePeerName(userId); },

  /* ============================================================
     USTA İZLƏMƏ (favoritlər)
     ============================================================ */

  /** İzləmə yalnız müştəriyə aiddir - usta özünü, admin isə heç kəsi izləmir. */
  canFollow() { return !Session.isArtist && !Session.isAdmin && Session.userId != null; },

  /** Keshi bir dəfə doldurur. Sınarsa boş qalır: izləmə nişanı görünməsə də,
      səhifənin qalanı normal işləməlidir. */
  async ensureFollowing() {
    if (!this.canFollow() || this.followingLoaded) return;
    try {
      const list = await Api.follows.forCustomer(Session.userId);
      this.followingIds = new Set((list || []).map(a => Number(a.userId)));
    } catch (err) {
      this.followingIds = new Set();
    }
    this.followingLoaded = true;
  },

  followButton(artistUserId, isFollowing, extraStyle) {
    return '<button class="btn btn-sm ' + (isFollowing ? 'btn-ghost' : 'btn-primary') + ' follow-btn" ' +
      'data-artist="' + esc(artistUserId) + '" data-following="' + (isFollowing ? 'true' : 'false') + '"' +
      (extraStyle ? ' style="' + extraStyle + '"' : '') + '>' +
      (isFollowing ? 'İzləyirsən ✓' : 'İzlə') + '</button>';
  },

  async toggleFollow(btn) {
    const artistId = Number(btn.dataset.artist);
    const wasFollowing = btn.dataset.following === 'true';
    const done = withBusy(btn, wasFollowing ? 'Çıxarılır' : 'İzlənilir');
    try {
      if (wasFollowing) {
        await Api.follows.remove(Session.userId, artistId);
        this.followingIds.delete(artistId);
      } else {
        await Api.follows.add(Session.userId, artistId);
        this.followingIds.add(artistId);
      }
      done();
      btn.dataset.following = wasFollowing ? 'false' : 'true';
      btn.className = btn.className.replace(wasFollowing ? 'btn-ghost' : 'btn-primary',
                                            wasFollowing ? 'btn-primary' : 'btn-ghost');
      btn.innerHTML = wasFollowing ? 'İzlə' : 'İzləyirsən ✓';
      toastOk(wasFollowing ? 'İzləmədən çıxarıldı.' : 'Usta izlənilənlərə əlavə edildi.');
    } catch (err) {
      done();
      toastErr(err.message);
    }
  },

  bindFollowButtons(root) {
    $$('.follow-btn', root).forEach(btn => {
      if (btn.dataset.bound) return;
      btn.dataset.bound = '1';
      btn.addEventListener('click', (e) => {
        // Usta kartının özünün "profilə keç" handler-i var - düymə onu işə salmamalıdır.
        e.stopPropagation();
        this.toggleFollow(btn);
      });
    });
  },

  /* İzlənilən ustaların siyahısı */
  async pageFollowing(host) {
    host.innerHTML = pageHead('İzlədiklərim', 'Bəyəndiyin ustalar bir yerdə') + spinner();

    let list;
    try {
      list = await Api.follows.forCustomer(Session.userId);
    } catch (err) {
      host.innerHTML = pageHead('İzlədiklərim') + emptyState(err.message, '!');
      return;
    }

    // Siyahının özü ən dəqiq mənbədir - eyni zamanda keshi də burada yeniləyirik.
    this.followingIds = new Set((list || []).map(a => Number(a.userId)));
    this.followingLoaded = true;

    host.innerHTML = pageHead('İzlədiklərim', 'Bəyəndiyin ustalar bir yerdə') +
      (list.length
        ? '<div class="grid grid-artists">' + list.map(a => this.artistCard(a)).join('') + '</div>'
        : emptyState('Hələ heç bir ustanı izləmirsən — "Kəşf et" bölməsindən başla.', '♡'));

    this.bindArtistCards(host);
  },

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

    // Kartların "İzlə" düyməsi keshə baxdığı üçün rəndləmədən əvvəl doldurulmalıdır.
    await this.ensureFollowing();

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

  /** @param page 0-dan başlayan səhifə nömrəsi.
   *  @param append true olanda mövcud kartların üstünə əlavə edir ("Daha çox yüklə"),
   *                false olanda siyahını sıfırdan qurur (yeni axtarış). */
  async runSearch(page = 0, append = false) {
    const box = $('#resultsBox');
    if (!box) return;
    if (!append) box.innerHTML = spinner();

    let res;
    try {
      res = await Api.artists.search(
        $('#fCity').value.trim(), $('#fStyle').value.trim(), $('#fRating').value,
        $('#fExperience').value, $('#fSort').value, page);
    } catch (err) {
      if (append) {
        toastErr(err.message);
        const btn = $('#moreArtistsBtn', box);
        if (btn) { btn.disabled = false; btn.textContent = 'Daha çox yüklə'; }
      } else {
        box.innerHTML = emptyState(err.message, '!');
      }
      return;
    }

    const list = res.content || [];
    const cards = list.map(a => this.artistCard(a)).join('');

    if (append) {
      const grid = $('.grid-artists', box);
      if (grid) grid.insertAdjacentHTML('beforeend', cards);
    } else if (list.length) {
      box.innerHTML =
        '<div class="row-meta" style="margin-bottom:12px">Tapıldı: ' + esc(res.totalElements) + '</div>' +
        '<div class="grid grid-artists">' + cards + '</div>' +
        '<div id="artistsMoreWrap"></div>';
    } else {
      box.innerHTML = emptyState('Bu şərtlərə uyğun usta tapılmadı.', '✦');
    }

    const wrap = $('#artistsMoreWrap', box);
    if (wrap) {
      wrap.innerHTML = res.last ? '' : this.loadMoreButton('moreArtistsBtn');
      const btn = $('#moreArtistsBtn', box);
      if (btn) {
        btn.addEventListener('click', () => {
          btn.disabled = true;
          btn.textContent = 'Yüklənir…';
          this.runSearch(res.number + 1, true);
        });
      }
    }

    this.bindArtistCards(box);
  },

  /** Səhifələnmiş siyahıların altındakı ortaq "daha çox" düyməsi. */
  loadMoreButton(id) {
    return '<div style="text-align:center;margin-top:18px">' +
      '<button class="btn btn-ghost" id="' + id + '">Daha çox yüklə</button></div>';
  },

  _pagedSeq: 0,

  /**
   * Səhifələnmiş siyahını verilmiş qaba yığır və altına "Daha çox yüklə" qoyur.
   * Backend-in bütün səhifələnmiş endpoint-ləri eyni formada cavab verir
   * ({ content, totalElements, totalPages, number, size, last }), ona görə bu köməkçi
   * hamsı üçün işləyir.
   *
   * @param o.box      DOM qabı (siyahı tam bu qabın içində qurulur)
   * @param o.fetch    (page) => Promise<PageResponse>
   * @param o.render   (element) => HTML sətri
   * @param o.empty    siyahı boş olanda göstərilən HTML (emptyState(...))
   * @param o.listClass siyahı qabının class-ı (default 'rows')
   * @param o.onPage   (res, box, yeniElementlər) - handler bağlamaq üçün, hər
   *                   səhifədən sonra çağrılır
   */
  async pagedList(o) {
    const box = o.box;
    if (!box) return;
    const id = 'pl' + (++this._pagedSeq);
    box.innerHTML = spinner();

    const load = async (page, append) => {
      let res;
      try {
        res = await o.fetch(page);
      } catch (err) {
        if (append) {
          toastErr(err.message);
          const b = document.getElementById(id + '-btn');
          if (b) { b.disabled = false; b.textContent = 'Daha çox yüklə'; }
        } else {
          box.innerHTML = emptyState(err.message, '!');
        }
        return;
      }

      const items = res.content || [];
      const html = items.map(o.render).join('');

      if (append) {
        const list = document.getElementById(id);
        if (list) list.insertAdjacentHTML('beforeend', html);
      } else if (items.length) {
        box.innerHTML =
          '<div class="' + (o.listClass || 'rows') + '" id="' + id + '">' + html + '</div>' +
          '<div id="' + id + '-more"></div>';
      } else {
        box.innerHTML = o.empty || emptyState('Siyahı boşdur.');
      }

      const moreWrap = document.getElementById(id + '-more');
      if (moreWrap) {
        moreWrap.innerHTML = res.last ? '' : this.loadMoreButton(id + '-btn');
        const btn = document.getElementById(id + '-btn');
        if (btn) {
          btn.addEventListener('click', () => {
            btn.disabled = true;
            btn.textContent = 'Yüklənir…';
            load(res.number + 1, true);
          });
        }
      }

      if (o.onPage) o.onPage(res, box);
    };

    await load(0, false);
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
      (this.canFollow()
        ? this.followButton(a.userId, this.followingIds.has(Number(a.userId)), 'margin-top:12px;width:100%')
        : '') +
    '</article>';
  },

  bindArtistCards(root) {
    // "Daha çox yüklə"dən sonra root-da həm köhnə, həm yeni kartlar olur -
    // artıq bağlanmış karta ikinci dəfə handler qoymaq iki dəfə naviqasiya deməkdir.
    $$('.artist-card', root).forEach(card => {
      if (card.dataset.bound) return;
      card.dataset.bound = '1';
      card.addEventListener('click', () => this.nav('artist', Number(card.dataset.artist)));
    });
    this.bindFollowButtons(root);
  },

  /* ============================================================
     USTA PROFİLİ (müştəri baxışı)
     ============================================================ */
  async pageArtist(host, artistUserId) {
    host.innerHTML = spinner();
    const canFollow = this.canFollow();
    let artist, slots = [], followers = 0, isFollowing = false;
    try {
      // Bu sorğu həm də Redis-də baxış sayğacını artırır (populyarlıq üçün)
      artist = await Api.artists.byUserId(artistUserId);
      slots = await Api.availability.publicSlots(artistUserId).catch(() => []);
      // İzləyici sayı və öz vəziyyətimə əlavə məlumatdır - sınarsa səhifə yenə açılmalıdır.
      [followers, isFollowing] = await Promise.all([
        Api.follows.count(artistUserId).catch(() => 0),
        canFollow ? Api.follows.isFollowing(Session.userId, artistUserId).catch(() => false) : false
      ]);
      if (canFollow) {
        if (isFollowing) this.followingIds.add(Number(artistUserId));
        else this.followingIds.delete(Number(artistUserId));
      }
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
              (artist.experienceYears ? ' · ' + esc(artist.experienceYears) + ' il təcrübə' : '') +
              ' · ' + esc(followers) + ' izləyici</div>' +
            '<div style="margin-top:10px">' + ratingBlock(artist.ratingAvg, artist.ratingCount) + '</div>' +
          '</div>' +
          '<div style="display:flex;gap:10px;flex-wrap:wrap">' +
            (canFollow ? this.followButton(artistUserId, isFollowing) : '') +
            '<button class="btn btn-primary" id="bookBtn">Sifariş ver</button>' +
          '</div>' +
        '</div>' +
        (artist.bio
          ? '<p style="margin:22px 0 0;font-family:var(--f-serif);font-size:17px;' +
            'line-height:1.75;color:var(--ink-dim)">' + esc(artist.bio) + '</p>'
          : '') +
        (artist.styles ? '<div style="margin-top:18px">' + styleChips(artist.styles) + '</div>' : '') +
      '</div>' +
      (slots.length
        ? '<div class="section-title">Uyğun vaxtlar</div>' +
          '<div class="rows" style="margin-bottom:24px">' +
            slots.map(s =>
              '<div class="row-card"><div class="row-main">' +
                '<div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px">' +
                  '<div>' + fmtDateTime(s.slotStart) + ' → ' + fmtTime(s.slotEnd) + '</div>' +
                  '<button class="btn btn-ghost btn-sm pick-slot" data-start="' + esc(s.slotStart) + '">Bu vaxtı seç</button>' +
                '</div>' +
              '</div></div>').join('') +
          '</div>'
        : '') +
      '<div class="section-title">Rəylər <span id="artistReviewCount"></span></div>' +
      '<div id="artistReviewsBox"></div>';

    this.pagedList({
      box: $('#artistReviewsBox', host),
      fetch: (page) => Api.reviews.forArtist(artistUserId, page),
      empty: emptyState('Bu usta haqqında hələ rəy yoxdur.', '✧'),
      render: (r) =>
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
        '</div></div>',
      onPage: (res) => {
        const counter = $('#artistReviewCount', host);
        if (counter) counter.textContent = '(' + res.totalElements + ')';
      }
    });

    $('#backBtn').addEventListener('click', () => this.nav('discover'));
    $('#bookBtn').addEventListener('click', () => this.promptBooking(artist));
    this.bindFollowButtons(host);
    $$('.pick-slot', host).forEach(btn => {
      btn.addEventListener('click', () => this.promptBooking(artist, btn.dataset.start));
    });
  },

  /* ---------- yeni sifariş ---------- */
  promptBooking(artist, presetDate) {
    const base = presetDate ? new Date(presetDate) : new Date(Date.now() + 86400000);
    const local = new Date(base.getTime() - base.getTimezoneOffset() * 60000)
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
      // qayıtmır (bax BookingService.getCompletedSummaryForCustomer).
      pastTattoos = await Api.bookings.completedSummary(customerUserId).catch(() => []);
      pastTattoos.sort((a, b) => (b.bookingDate || '').localeCompare(a.bookingDate || ''));
    } catch (err) {
      host.innerHTML = pageHead('Müştəri') + emptyState(err.message, '!');
      return;
    }

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
  },

  /* ============================================================
     SİFARİŞLƏRİM (müştəri)
     ============================================================ */
  async pageBookings(host) {
    host.innerHTML = pageHead('Sifarişlərim', 'Bütün sorğuların və vəziyyətləri') +
      '<div id="bookingsBox"></div>';

    await this.pagedList({
      box: $('#bookingsBox', host),
      // Ad ancaq id ilə gəlir - rəndləmədən ƏVVƏL həll edilməlidir, ona görə burada.
      // Sıralama artıq backend-dədir (ən yeni əvvəldə).
      fetch: async (page) => {
        const res = await Api.bookings.forCustomer(Session.userId, page);
        await Promise.all((res.content || []).map(b => this.resolveName(b.artistId)));
        return res;
      },
      empty: emptyState('Hələ sifariş yoxdur. "Kəşf et" bölməsindən usta seç.', '❖'),
      render: (b) => {
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
      },
      // "Daha çox yüklə" yeni sətirlər gətirir - onların da düymələri bağlanmalıdır.
      onPage: () => {
        this.bindOnce('[data-chat]', host, (btn) =>
          this.openChatFor(Number(btn.dataset.chat), Session.userId, Number(btn.dataset.artist), btn));
        this.bindOnce('[data-review]', host, (btn) => this.promptReview(Number(btn.dataset.review)));
        this.bindOnce('[data-cancel]', host, (btn) =>
          this.changeStatus(Number(btn.dataset.cancel), 'CANCELLED', btn, 'bookings'));
      }
    });
  },

  /** Verilmiş seçiciyə uyğun elementlərə bir dəfə klik handler-i qoşur.
   *  Səhifələnmiş siyahılarda köhnə sətirlər yerində qaldığı üçün təkrar bağlama
   *  eyni əməliyyatın iki dəfə işləməsi demək olardı. */
  bindOnce(selector, root, handler) {
    $$(selector, root).forEach(el => {
      if (el.dataset.bound) return;
      el.dataset.bound = '1';
      el.addEventListener('click', (e) => handler(el, e));
    });
  },

  /* ============================================================
     GƏLƏN SİFARİŞLƏR (rəssam)
     ============================================================ */
  async pageOrders(host) {
    host.innerHTML = pageHead('Gələn sifarişlər', 'Təsdiqlə, tamamla və ya ləğv et') +
      '<div id="ordersBox"></div>';

    await this.pagedList({
      box: $('#ordersBox', host),
      fetch: async (page) => {
        const res = await Api.bookings.forArtist(Session.userId, page);
        await Promise.all((res.content || []).map(b => this.resolveName(b.customerId)));
        return res;
      },
      empty: emptyState('Hələ sifariş gəlməyib. Profilini doldurmaq görünürlüyünü artırır.', '❖'),
      render: (b) => {
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
      },
      onPage: () => {
        this.bindOnce('[data-chat]', host, (btn) =>
          this.openChatFor(Number(btn.dataset.chat), Number(btn.dataset.customer), Session.userId, btn));
        this.bindOnce('[data-set]', host, (btn) =>
          this.changeStatus(Number(btn.dataset.id), btn.dataset.set, btn, 'orders'));
        this.bindOnce('[data-viewprofile]', host, (el) =>
          this.nav('customer', Number(el.dataset.viewprofile)));
        this.bindOnce('[data-viewcustomer]', host, (el) =>
          this.nav('customer', Number(el.dataset.viewcustomer)));
      }
    });
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
    const profile = await Api.artists.byUserId(Session.userId).catch(() => null);

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
      '<div id="myReviewsBox"></div>';

    await this.pagedList({
      box: $('#myReviewsBox', host),
      fetch: (page) => Api.reviews.forArtist(Session.userId, page),
      empty: emptyState('Hələ rəy yoxdur. Tamamlanmış sifarişdən sonra müştəri rəy yaza bilər.', '✧'),
      render: (r) =>
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
        '</div></div>',
      // Hər yeni səhifədən sonra təzə "Cavab yaz" düymələri gəlir - onları da bağlamaq lazımdır.
      onPage: () => this.bindReviewReplyButtons(host)
    });
  },

  bindReviewReplyButtons(host) {
    $$('.reply-btn', host).forEach(btn => {
      if (btn.dataset.bound) return;
      btn.dataset.bound = '1';
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
     TƏQVİM (rəssam) - uğun və dolu pəncərələri idarə etmək
     ============================================================ */
  async pageAvailability(host) {
    host.innerHTML = pageHead('Təqvim', 'Uğun vaxt pəncərələrini əlavə et və idarə et') + spinner();
    let slots;
    try {
      slots = await Api.availability.forArtist(Session.userId);
    } catch (err) {
      host.innerHTML = pageHead('Təqvim') + emptyState(err.message, '!');
      return;
    }

    const startDefault = new Date(Date.now() + 86400000);
    startDefault.setMinutes(0, 0, 0);
    const endDefault = new Date(startDefault.getTime() + 3600000);
    const toLocal = (d) => new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);

    host.innerHTML = pageHead('Təqvim', 'Uğun vaxt pəncərələrini əlavə et və idarə et') +
      '<div class="card card-pad" style="margin-bottom:24px">' +
        '<div class="field-row">' +
          '<label class="field"><span>Başlanğıc</span>' +
            '<input type="datetime-local" id="slotStart" value="' + toLocal(startDefault) + '"></label>' +
          '<label class="field"><span>Bitmə</span>' +
            '<input type="datetime-local" id="slotEnd" value="' + toLocal(endDefault) + '"></label>' +
        '</div>' +
        '<button class="btn btn-primary btn-sm" id="addSlotBtn" style="margin-top:14px">Pəncərə əlavə et</button>' +
      '</div>' +
      '<div class="section-title" style="margin-top:0">Pəncərələr (' + slots.length + ')</div>' +
      '<div class="rows">' +
        (slots.length
          ? slots.map(s =>
              '<div class="row-card"><div class="row-main">' +
                '<div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px">' +
                  '<div>' + fmtDateTime(s.slotStart) + ' → ' + fmtTime(s.slotEnd) + '</div>' +
                  '<span style="font-size:12px;padding:3px 10px;border-radius:20px;' +
                    (s.booked
                      ? 'background:rgba(200,60,60,.12);color:#a83232'
                      : 'background:rgba(60,160,90,.12);color:#2f8f52') + '">' +
                    (s.booked ? 'Dolu' : 'Boş') +
                  '</span>' +
                '</div>' +
                '<div style="margin-top:10px;display:flex;gap:8px;flex-wrap:wrap">' +
                  '<button class="btn btn-ghost btn-sm toggle-slot" data-id="' + s.id + '" data-booked="' + s.booked + '">' +
                    (s.booked ? 'Boş kimi işarələ' : 'Dolu kimi işarələ') +
                  '</button>' +
                  '<button class="btn btn-ghost btn-sm delete-slot" data-id="' + s.id + '">Sil</button>' +
                '</div>' +
              '</div></div>').join('')
          : emptyState('Hələ uğunluq pəncərəsi əlavə etməmisən.', '◷')) +
      '</div>';

    $('#addSlotBtn').addEventListener('click', async () => {
      const startVal = $('#slotStart').value;
      const endVal = $('#slotEnd').value;
      if (!startVal || !endVal) { toastErr('Başlanğıc və bitmə vaxtını seç.'); return; }
      const done = withBusy($('#addSlotBtn'), 'Əlavə edilir');
      try {
        await Api.availability.add({
          artistId: Session.userId,
          slotStart: startVal.length === 16 ? startVal + ':00' : startVal,
          slotEnd: endVal.length === 16 ? endVal + ':00' : endVal
        });
        toastOk('Pəncərə əlavə edildi.');
        this.pageAvailability(host);
      } catch (err) {
        toastErr(err.message);
        done();
      }
    });

    $$('.toggle-slot', host).forEach(btn => {
      btn.addEventListener('click', async () => {
        const id = Number(btn.dataset.id);
        const newBooked = btn.dataset.booked !== 'true';
        try {
          await Api.availability.setBooked(id, Session.userId, newBooked);
          this.pageAvailability(host);
        } catch (err) {
          toastErr(err.message);
        }
      });
    });

    $$('.delete-slot', host).forEach(btn => {
      btn.addEventListener('click', async () => {
        const id = Number(btn.dataset.id);
        try {
          await Api.availability.remove(id, Session.userId);
          toastOk('Pəncərə silindi.');
          this.pageAvailability(host);
        } catch (err) {
          toastErr(err.message);
        }
      });
    });
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
      '</div>' +
      '<div class="section-title">Saxlanmış ideyalar</div>' +
      '<div id="aiHistoryBox">' + spinner() + '</div>';

    let lastIdea = null;   // { prompt, style, aiRecommendation } - son "Məsləhət al" nəticəsi
    let lastImage = null;  // { prompt, aiRecommendation } - son "Analiz et" nəticəsi

    $('#aiIdeaBtn').addEventListener('click', async (e) => {
      const prompt = $('#aiPrompt').value.trim();
      if (!prompt) { toastErr('Əvvəlcə ideyanı yaz.'); return; }
      const style = $('#aiStyle').value.trim();
      const done = withBusy(e.currentTarget, 'Düşünür');
      $('#aiIdeaOut').innerHTML = '';
      try {
        const res = await Api.ai.generateIdea({ userPrompt: prompt, preferredStyle: style });
        lastIdea = { prompt, style, aiRecommendation: res.aiRecommendation };
        $('#aiIdeaOut').innerHTML = '<div class="ai-out">' + esc(res.aiRecommendation) + '</div>' +
          '<button class="btn btn-ghost btn-sm" id="aiSaveIdeaBtn" style="margin-top:10px">Saxla</button>';
        $('#aiSaveIdeaBtn').addEventListener('click', (ev) =>
          this.saveAiIdea(lastIdea.prompt, lastIdea.style, lastIdea.aiRecommendation, ev.currentTarget, host));
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
        const imgPrompt = $('#aiImgPrompt').value.trim();
        const res = await Api.ai.analyzeImage(chosen, imgPrompt);
        lastImage = { prompt: imgPrompt || 'Yüklənmiş eskizin analizi', aiRecommendation: res.aiRecommendation };
        $('#aiImgOut').innerHTML = '<div class="ai-out">' + esc(res.aiRecommendation) + '</div>' +
          '<button class="btn btn-ghost btn-sm" id="aiSaveImgBtn" style="margin-top:10px">Saxla</button>';
        $('#aiSaveImgBtn').addEventListener('click', (ev) =>
          this.saveAiIdea(lastImage.prompt, '', lastImage.aiRecommendation, ev.currentTarget, host));
      } catch (err) { toastErr(err.message); }
      finally { done(); }
    });

    this.loadAiHistory(host);
  },

  /* ---------- AI Studiya tarixçəsi ---------- */
  async saveAiIdea(promptText, styleText, recommendation, btn, host) {
    const done = withBusy(btn, 'Saxlanır');
    try {
      await Api.aiIdeas.save({
        customerId: Session.userId,
        prompt: promptText,
        style: styleText || null,
        aiRecommendation: recommendation
      });
      toastOk('Saxlanıldı.');
      this.loadAiHistory(host);
    } catch (err) { toastErr(err.message); }
    finally { done(); }
  },

  async loadAiHistory(host) {
    const box = $('#aiHistoryBox', host);
    if (!box) return;
    box.innerHTML = spinner();
    let ideas;
    try {
      ideas = await Api.aiIdeas.forCustomer(Session.userId);
    } catch (err) {
      box.innerHTML = emptyState(err.message, '!');
      return;
    }

    box.innerHTML = ideas.length
      ? '<div class="rows">' + ideas.map(idea => {
          const snippet = idea.aiRecommendation.length > 220
            ? idea.aiRecommendation.slice(0, 220) + '…' : idea.aiRecommendation;
          return '<div class="row-card"><div class="row-main">' +
            '<div class="row-title">' + esc(idea.prompt) +
              (idea.style ? ' · ' + esc(idea.style) : '') + '</div>' +
            '<div class="row-meta" style="white-space:pre-wrap">' + esc(snippet) + '</div>' +
            '<div class="row-meta">' + esc(fmtDay(idea.createdAt)) +
              (idea.bookingId ? ' · Bağlı sifariş: #' + esc(idea.bookingId) : '') + '</div>' +
            '<div style="margin-top:8px;display:flex;gap:8px;flex-wrap:wrap">' +
              (!idea.bookingId
                ? '<button class="btn btn-ghost btn-sm link-idea" data-id="' + idea.id + '">Sifarişə bağla</button>'
                : '') +
              '<button class="btn btn-ghost btn-sm delete-idea" data-id="' + idea.id + '">Sil</button>' +
            '</div>' +
          '</div></div>';
        }).join('') + '</div>'
      : emptyState('Hələ saxlanmış ideya yoxdur — yuxarıda bir məsləhət al və ya eskiz analiz etdir, bəyəndiyini saxla.', '◈');

    $$('.delete-idea', box).forEach(b => b.addEventListener('click', async () => {
      try {
        await Api.aiIdeas.remove(Number(b.dataset.id), Session.userId);
        toastOk('Silindi.');
        this.loadAiHistory(host);
      } catch (err) { toastErr(err.message); }
    }));

    $$('.link-idea', box).forEach(b => b.addEventListener('click', () => this.promptLinkIdea(Number(b.dataset.id), host)));
  },

  async promptLinkIdea(ideaId, host) {
    let bookings;
    try {
      // Bu bir açılan siyahıdır, "daha çox" düyməsi yoxdur - ona görə bir dəfəyə
      // daha böyük səhifə çəkirik (backend-in yuxarı həddi 100-dür).
      const res = Session.isArtist
        ? await Api.bookings.forArtist(Session.userId, 0, 100)
        : await Api.bookings.forCustomer(Session.userId, 0, 100);
      bookings = res.content || [];
    } catch (err) { toastErr(err.message); return; }

    if (!bookings.length) { toastErr('Hələ heç bir sifarişin yoxdur.'); return; }

    openModal('Sifarişə bağla',
      '<label class="field"><span>Hansı sifarişə bağlansın?</span>' +
        '<select id="linkBookingSelect">' +
          bookings.map(b =>
            '<option value="' + b.id + '">#' + b.id + ' — ' + esc(fmtDay(b.bookingDate)) +
              ' (' + esc(STATUS_AZ[b.status] || b.status) + ')</option>').join('') +
        '</select></label>',
      {
        okText: 'Bağla',
        onOk: async (ov, close, okBtn) => {
          const bookingId = Number($('#linkBookingSelect', ov).value);
          const done = withBusy(okBtn, 'Bağlanır');
          try {
            await Api.aiIdeas.link(ideaId, Session.userId, bookingId);
            close();
            toastOk('Sifarişə bağlandı.');
            this.loadAiHistory(host);
          } catch (err) {
            toastErr(err.message);
            done();
          }
        }
      });
  },

  /* ============================================================
     BİLDİRİŞLƏR
     ============================================================ */
  async pageNotifs(host) {
    host.innerHTML = pageHead('Bildirişlər') + '<div id="notifsBox"></div>';

    await this.pagedList({
      box: $('#notifsBox', host),
      fetch: (page) => Api.notifications.forUser(Session.userId, page),
      empty: emptyState('Bildiriş yoxdur.', '✉'),
      render: (n) => {
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
      },
      onPage: () => {
        this.bindOnce('[data-read]', host, (btn, e) => {
          // Düymə sətrin içindədir - klik yuxarı ötürsə həm də başqa səhifəyə keçərdik.
          e.stopPropagation();
          const done = withBusy(btn, '');
          Api.notifications.markRead(Number(btn.dataset.read))
            .then(() => this.nav('notifs'))
            .catch(err => { toastErr(err.message); done(); });
        });

        // Bildirişə klikləyəndə aid olduğu bölməyə keçirik. Hazırda bütün bildirişlər
        // sifariş/söhbət mövzuludur (bax notifyQuietly çağırışlarına), ona görə
        // rola görə ən uyğun siyahıya yönləndiririk.
        this.bindOnce('[data-notif]', host, (row) => {
          Api.notifications.markRead(Number(row.dataset.notif)).catch(() => {});
          this.nav(Session.isArtist ? 'orders' : 'bookings');
        });

        this.refreshNotifBadge();
      }
    });
  },

  async refreshNotifBadge() {
    const badge = $('#notifBadge');
    if (!badge || !Session.userId) return;
    try {
      const res = await Api.notifications.unreadCount(Session.userId);
      const unread = (res && res.count) ? res.count : 0;
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
                (Session.isAdmin ? 'Admin' : (Session.isArtist ? 'Rəssam' : 'Müştəri')) + '</div>' +
              // emailVerified null ola bilər (köhnə hesablar) - o zaman da "təsdiqlənməyib"
              // görünür, amma bu heç nəyi bloklamır, sadəcə nişandır.
              '<div style="margin-top:6px">' +
                (user.emailVerified
                  ? '<span class="badge COMPLETED">Email təsdiqlənib ✓</span>'
                  : '<span class="badge PENDING">Email təsdiqlənməyib</span>' +
                    '<button class="btn btn-ghost btn-sm" id="verifyEmailBtn" style="margin-left:8px">' +
                      'Təsdiq linki göndər</button>') +
              '</div>' +
            '</div>' +
          '</div>' +
          '<label class="field"><span>Ad, soyad</span>' +
            '<input type="text" id="pName" value="' + esc(user.fullName || '') + '"></label>' +
          '<label class="field" style="margin-top:13px"><span>Şəhər</span>' +
            '<input type="text" id="pCity" value="' + esc(user.city || '') + '"></label>' +
          '<label class="field" style="margin-top:13px"><span>Profil şəkli linki</span>' +
            '<input type="text" id="pAvatar" value="' + esc(user.profileImageUrl || '') +
              '" placeholder="https://…"></label>' +
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

    const verifyBtn = $('#verifyEmailBtn');
    if (verifyBtn) {
      verifyBtn.addEventListener('click', async () => {
        const done = withBusy(verifyBtn, 'Göndərilir');
        try {
          const res = await Api.auth.sendVerification(user.email);
          toastOk((res && res.message) || 'Təsdiq linki göndərildi.');
        } catch (err) {
          toastErr(err.message);
        }
        done();
      });
    }

    $('#saveUserBtn').addEventListener('click', async (e) => {
      const done = withBusy(e.currentTarget, 'Yadda saxlanır');
      try {
        const updated = await Api.users.update(Session.userId, {
          fullName: $('#pName').value.trim(),
          city: $('#pCity').value.trim(),
          profileImageUrl: $('#pAvatar').value.trim()
        });
        Session.patch({ fullName: updated.fullName, city: updated.city });
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
  },

  /* ============================================================
     ADMİN PANELİ (moderasiya)
     ============================================================ */
  async pageAdmin(host) {
    host.innerHTML = pageHead('Admin paneli', 'İstifadəçilər və rəylərin moderasiyası') + spinner();

    // Statistika əlavədir: bir servis əlçatmaz olsa qalan panel yenə işləməlidir.
    // Siyahıların özü aşağıda pagedList ilə ayrıca yüklənir.
    const [uStats, bStats] = await Promise.all([
      Api.admin.userStats().catch(() => null),
      Api.admin.bookingStats().catch(() => null)
    ]);

    const ROLE_AZ = { CUSTOMER: 'Müştəri', ARTIST: 'Rəssam', ADMIN: 'Admin' };

    const tile = (value, label) =>
      '<div class="card card-pad" style="text-align:center;flex:1;min-width:130px">' +
        '<div style="font-family:var(--f-display);font-size:28px;color:var(--brass)">' + esc(value) + '</div>' +
        '<div class="row-meta" style="margin-top:6px">' + esc(label) + '</div>' +
      '</div>';

    const statRow = (tiles) =>
      '<div style="display:flex;gap:16px;flex-wrap:wrap;margin-bottom:20px">' + tiles.join('') + '</div>';

    const topCities = (uStats && uStats.topCities && uStats.topCities.length)
      ? '<div class="section-title">Ən çox istifadəçisi olan şəhərlər</div>' +
        statRow(uStats.topCities.map(c => tile(c.count, c.city)))
      : '';

    const statsBlock = (!uStats && !bStats)
      ? '<div class="section-title" style="margin-top:0">Platforma statistikası</div>' +
        emptyState('Statistikanı yükləmək mümkün olmadı.', '!')
      : '<div class="section-title" style="margin-top:0">İstifadəçilər</div>' +
        statRow([
          tile(uStats ? uStats.totalUsers   : '—', 'Ümumi istifadəçi'),
          tile(uStats ? uStats.customers    : '—', 'Müştəri'),
          tile(uStats ? uStats.artists      : '—', 'Rəssam'),
          tile(uStats ? uStats.admins       : '—', 'Admin'),
          tile(uStats ? uStats.bannedUsers  : '—', 'Bloklanmış'),
          tile(uStats ? uStats.newLast7Days : '—', 'Son 7 gündə yeni'),
          tile(uStats ? uStats.newLast30Days: '—', 'Son 30 gündə yeni')
        ]) +
        topCities +
        '<div class="section-title">Sifarişlər və rəylər</div>' +
        statRow([
          tile(bStats ? bStats.totalBookings     : '—', 'Ümumi sifariş'),
          tile(bStats ? bStats.pendingBookings   : '—', 'Gözləyən'),
          tile(bStats ? bStats.confirmedBookings : '—', 'Təsdiqlənmiş'),
          tile(bStats ? bStats.completedBookings : '—', 'Tamamlanmış'),
          tile(bStats ? bStats.cancelledBookings : '—', 'Ləğv edilmiş'),
          tile(bStats ? bStats.newLast7Days      : '—', 'Son 7 gündə yeni'),
          tile(bStats ? bStats.newLast30Days     : '—', 'Son 30 gündə yeni')
        ]) +
        statRow([
          tile(bStats ? bStats.totalReviews : '—', 'Ümumi rəy'),
          tile(bStats && bStats.averageRating ? bStats.averageRating.toFixed(1) : '—', 'Orta reytinq'),
          tile(bStats ? (Number(bStats.totalRevenue) || 0).toFixed(0) + ' AZN' : '—', 'Təxmini dövriyyə')
        ]);

    host.innerHTML = pageHead('Admin paneli', 'İstifadəçilər və rəylərin moderasiyası') +
      statsBlock +
      '<div class="section-title">İstifadəçi siyahısı <span id="adminUserCount"></span></div>' +
      '<div id="adminUsersBox"></div>' +
      '<div class="section-title">Rəylər <span id="adminReviewTotal"></span></div>' +
      '<div id="adminReviewsBox"></div>';

    this.pagedList({
      box: $('#adminUsersBox', host),
      fetch: (page) => Api.admin.users(page),
      empty: emptyState('Heç bir istifadəçi tapılmadı.', '✵'),
      render: (u) =>
        '<div class="row-card"><div class="row-main">' +
          '<b>' + esc(u.fullName || '(adsız)') + '</b>' +
          (u.banned ? ' <span class="badge CANCELLED">Bloklanıb</span>' : '') +
          (u.emailVerified ? ' <span class="badge COMPLETED">Email ✓</span>' : '') +
          '<div class="row-meta">' + esc(u.email) + ' · ' + esc(ROLE_AZ[u.role] || u.role) +
            (u.city ? ' · ' + esc(u.city) : '') + '</div>' +
          '<div class="row-meta">Qeydiyyat: ' + esc(fmtDay(u.createdAt)) + '</div>' +
          (u.role !== 'ADMIN'
            ? '<button class="btn btn-sm ' + (u.banned ? 'btn-ghost' : 'btn-danger') + ' ban-btn" ' +
                'data-id="' + u.id + '" data-banned="' + (u.banned ? 'true' : 'false') + '" style="margin-top:10px">' +
                (u.banned ? 'Blokdan çıxar' : 'Blokla') + '</button>'
            : '') +
        '</div></div>',
      onPage: (res) => {
        const counter = $('#adminUserCount', host);
        if (counter) counter.textContent = '(' + res.totalElements + ')';
        this.bindOnce('.ban-btn', host, async (btn) => {
          const userId = Number(btn.dataset.id);
          const nextBanned = btn.dataset.banned !== 'true';
          const done = withBusy(btn, nextBanned ? 'Bloklanır' : 'Açılır');
          try {
            await Api.admin.setBanned(userId, nextBanned);
            toastOk(nextBanned ? 'İstifadəçi bloklandı.' : 'İstifadəçi blokdan çıxarıldı.');
            this.pageAdmin(host);
          } catch (err) {
            toastErr(err.message);
            done();
          }
        });
      }
    });

    this.pagedList({
      box: $('#adminReviewsBox', host),
      fetch: (page) => Api.admin.reviews(page),
      empty: emptyState('Heç bir rəy yoxdur.', '✧'),
      render: (r) =>
        '<div class="row-card"><div class="row-main">' + stars(r.rating) +
          '<div style="margin-top:6px;color:var(--ink-dim);font-size:13.5px">' +
            esc(r.comment || '(şərh yazılmayıb)') + '</div>' +
          '<div class="row-meta">Müştəri #' + esc(r.customerId) + ' → Rəssam #' + esc(r.artistId) +
            ' · ' + esc(fmtDay(r.createdAt)) + '</div>' +
          (r.artistReply
            ? '<div style="margin-top:10px;padding:10px 12px;background:var(--bg-soft,rgba(0,0,0,.03));' +
                'border-radius:8px;font-size:13px"><b>Ustanın cavabı:</b> ' + esc(r.artistReply) + '</div>'
            : '') +
          '<button class="btn btn-ghost btn-sm delete-review-btn" data-id="' + r.id + '" style="margin-top:10px">Sil</button>' +
        '</div></div>',
      onPage: (res) => {
        const counter = $('#adminReviewTotal', host);
        if (counter) counter.textContent = '(' + res.totalElements + ')';
        this.bindOnce('.delete-review-btn', host, async (btn) => {
          const reviewId = Number(btn.dataset.id);
          const done = withBusy(btn, 'Silinir');
          try {
            await Api.admin.deleteReview(reviewId);
            toastOk('Rəy silindi.');
            this.pageAdmin(host);
          } catch (err) {
            toastErr(err.message);
            done();
          }
        });
      }
    });
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
