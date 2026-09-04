/* ============================================================
   chat.js — canlı söhbət (STOMP over WebSocket) + söhbət səhifəsi.

   DİQQƏT: WebSocket birbaşa chat-service-ə (8083) qoşulur, gateway-ə
   (8080) yox. Səbəb: Spring Cloud Gateway Server MVC-nin HTTP proxy
   handler-i WebSocket protokol "upgrade"-ini yerinə yetirə bilmir.
   REST sorğular isə normal şəkildə gateway üzərindən gedir.
   ============================================================ */

const ChatModule = {
  client: null,
  connected: false,

  roomId: null,
  peerId: null,
  peerName: '',
  bookingId: null,

  subMessages: null,
  subTyping: null,

  seenIds: new Set(),      // eyni mesajın iki dəfə əlavə olunmasının qarşısını alır
  peerCache: new Map(),    // userId -> fullName
  bookingDateCache: new Map(),  // bookingId -> formatted date

  typingTimer: null,
  lastTypingSent: 0,
  presenceTimer: null,

  /* ---------- bağlantı ---------- */
  connect() {
    if (this.client || !Session.userId) return;
    if (typeof StompJs === 'undefined') {
      console.warn('STOMP kitabxanası yüklənməyib — söhbət yalnız REST rejimində işləyəcək.');
      return;
    }

    this.client = new StompJs.Client({
      brokerURL: WS_URL + '?userId=' + Session.userId,
      reconnectDelay: 6000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        this.connected = true;
        this.updateLiveDot();
        if (this.roomId) this.subscribeRoom(this.roomId);
      },
      onWebSocketClose: () => {
        this.connected = false;
        this.updateLiveDot();
      },
      onStompError: (frame) => {
        console.warn('STOMP xətası:', frame.headers && frame.headers.message);
      }
    });

    try { this.client.activate(); } catch (e) { console.warn('WebSocket aktivləşmədi:', e); }
  },

  disconnect() {
    this.unsubscribeRoom();
    clearInterval(this.presenceTimer);
    this.presenceTimer = null;
    if (this.client) { try { this.client.deactivate(); } catch (e) { /* susmaq */ } }
    this.client = null;
    this.connected = false;
    this.roomId = null;
  },

  subscribeRoom(roomId) {
    if (!this.client || !this.connected) return;
    this.unsubscribeRoom();

    this.subMessages = this.client.subscribe('/topic/rooms/' + roomId, (frame) => {
      let msg;
      try { msg = JSON.parse(frame.body); } catch (e) { return; }
      if (this.roomId !== roomId) return;
      this.appendMessage(msg);
    });

    this.subTyping = this.client.subscribe('/topic/rooms/' + roomId + '/typing', (frame) => {
      let evt;
      try { evt = JSON.parse(frame.body); } catch (e) { return; }
      if (this.roomId !== roomId) return;
      if (Number(evt.userId) === Number(Session.userId)) return;  // özümüzü göstərmirik
      this.showTyping();
    });
  },

  unsubscribeRoom() {
    if (this.subMessages) { try { this.subMessages.unsubscribe(); } catch (e) {} this.subMessages = null; }
    if (this.subTyping)   { try { this.subTyping.unsubscribe();   } catch (e) {} this.subTyping = null; }
  },

  /* ---------- otaq siyahısı səhifəsi ---------- */
  async renderPage(host) {
    host.innerHTML = pageHead('Söhbətlər', 'Hər söhbət bir sifarişə bağlıdır') + spinner();

    let rooms;
    try {
      rooms = Session.isArtist
        ? await Api.chat.roomsForArtist(Session.userId)
        : await Api.chat.roomsForCustomer(Session.userId);
    } catch (err) {
      host.innerHTML = pageHead('Söhbətlər') + emptyState(err.message, '!');
      return;
    }

    if (!rooms.length) {
      host.innerHTML = pageHead('Söhbətlər', 'Hər söhbət bir sifarişə bağlıdır') +
        emptyState('Hələ söhbət yoxdur. Sifarişlər səhifəsindən söhbət aça bilərsən.', '☾');
      return;
    }

    // hər otağın qarşı tərəfinin adını VƏ sifarişin tarixini çəkirik - "Sifariş #7"
    // kimi çılpaq DB id-si göstərmək əvəzinə tarix daha oxunaqlıdır.
    await Promise.all(rooms.map(r => Promise.all([
      this.resolvePeerName(this.peerOf(r)),
      this.resolveBookingDate(r.bookingId)
    ])));

    host.innerHTML = pageHead('Söhbətlər', 'Hər söhbət bir sifarişə bağlıdır') +
      '<div class="chat-wrap">' +
        '<div class="chat-list" id="chatList">' +
          rooms.map(r => {
            const pid = this.peerOf(r);
            const name = this.peerCache.get(pid) || ('İstifadəçi #' + pid);
            const dateLabel = this.bookingDateCache.get(r.bookingId) || '';
            return '<button class="chat-list-item" data-room="' + r.id + '" ' +
                   'data-peer="' + pid + '" data-booking="' + r.bookingId + '">' +
                     '<span class="avatar avatar-sm">' + esc(initials(name)) + '</span>' +
                     '<span><span class="cli-name">' + esc(name) + '</span><br>' +
                     '<span class="cli-sub">' + esc(dateLabel) + '</span></span>' +
                   '</button>';
          }).join('') +
        '</div>' +
        '<div class="chat-box" id="chatBox">' +
          emptyState('Sol tərəfdən bir söhbət seç.', '☾') +
        '</div>' +
      '</div>';

    $$('#chatList .chat-list-item').forEach(btn => {
      btn.addEventListener('click', () => {
        $$('#chatList .chat-list-item').forEach(b => b.classList.toggle('is-active', b === btn));
        this.openRoom(
          Number(btn.dataset.room),
          Number(btn.dataset.peer),
          Number(btn.dataset.booking)
        );
      });
    });

    // birbaşa müəyyən otağa keçid tələb olunubsa (sifarişlər səhifəsindən)
    if (App.pendingRoomId) {
      const target = $('#chatList .chat-list-item[data-room="' + App.pendingRoomId + '"]');
      App.pendingRoomId = null;
      if (target) target.click();
    }
  },

  peerOf(room) {
    return Session.isArtist ? room.customerId : room.artistId;
  },

  async resolvePeerName(userId) {
    if (this.peerCache.has(userId)) return this.peerCache.get(userId);
    try {
      const u = await Api.users.get(userId);
      this.peerCache.set(userId, u.fullName || ('İstifadəçi #' + userId));
    } catch (e) {
      this.peerCache.set(userId, 'İstifadəçi #' + userId);
    }
    return this.peerCache.get(userId);
  },

  /* Söhbət siyahısında "Sifariş #7" kimi çılpaq DB id-si göstərmək əvəzinə
     bronun tarixini göstəririk - bookingDateCache ilə eyni növ keşləmə. */
  async resolveBookingDate(bookingId) {
    if (this.bookingDateCache.has(bookingId)) return this.bookingDateCache.get(bookingId);
    try {
      const b = await Api.bookings.byId(bookingId);
      this.bookingDateCache.set(bookingId, fmtDay(b.bookingDate));
    } catch (e) {
      this.bookingDateCache.set(bookingId, '');
    }
    return this.bookingDateCache.get(bookingId);
  },

  /* ---------- konkret otağın açılması ---------- */
  async openRoom(roomId, peerId, bookingId) {
    this.roomId = roomId;
    this.peerId = peerId;
    this.bookingId = bookingId;
    this.seenIds.clear();

    // Ad və bronun cari statusunu paralel çəkirik - ləğv edilmiş bron üçün
    // söhbətə davam etmək olsun, amma yeni qiymət təklifi göndərmək OLMASIN
    // (əvəzinə müştəri yeni sifariş açmalıdır).
    const [peerName, booking] = await Promise.all([
      this.resolvePeerName(peerId),
      Api.bookings.byId(bookingId).catch(() => null)
    ]);
    this.peerName = peerName;
    const isCancelled = booking && booking.status === 'CANCELLED';

    const box = $('#chatBox');
    if (!box) return;

    box.innerHTML =
      '<div class="chat-head">' +
        '<span class="avatar avatar-sm">' + esc(initials(this.peerName)) + '</span>' +
        '<div class="chat-head-main">' +
          '<div class="chat-peer">' + esc(this.peerName) + '</div>' +
          '<span class="presence"><span class="dot" id="peerDot"></span>' +
            '<span id="peerState">yoxlanılır…</span></span>' +
        '</div>' +
        (Session.isArtist && !isCancelled
          ? '<button class="btn btn-brass btn-sm" id="offerBtn">Qiymət təklif et</button>'
          : '') +
      '</div>' +
      (isCancelled
        ? '<div class="chat-notice">Bu sifariş ləğv edilib — söhbətə davam edə bilərsən, ' +
          'amma yeni qiymət təklifi üçün müştəri yenidən sifariş açmalıdır.</div>'
        : '') +
      '<div class="chat-body" id="chatBody">' + spinner() + '</div>' +
      '<div class="typing" id="typingLine"></div>' +
      '<div class="chat-foot">' +
        '<input type="text" id="msgInput" placeholder="Mesaj yaz…" autocomplete="off" maxlength="1500">' +
        '<button class="btn btn-primary btn-sm" id="sendBtn">Göndər</button>' +
      '</div>';

    this.subscribeRoom(roomId);
    this.updateLiveDot();

    const offerBtn = $('#offerBtn');
    if (offerBtn) offerBtn.addEventListener('click', () => this.promptOffer());

    $('#sendBtn').addEventListener('click', () => this.sendText());
    const input = $('#msgInput');
    input.addEventListener('keydown', (e) => { if (e.key === 'Enter') this.sendText(); });
    input.addEventListener('input', () => this.sendTyping());

    // tarixçə
    const body = $('#chatBody');
    try {
      const history = await Api.chat.history(roomId);
      body.innerHTML = '';
      history.forEach(m => this.appendMessage(m, true));
      this.scrollDown();
      await Api.chat.markRead(roomId, Session.userId).catch(() => {});
      App.refreshChatBadge();
    } catch (err) {
      body.innerHTML = emptyState(err.message, '!');
    }

    // qarşı tərəfin onlayn vəziyyəti
    clearInterval(this.presenceTimer);
    this.checkPresence();
    this.presenceTimer = setInterval(() => this.checkPresence(), 20000);
  },

  async checkPresence() {
    if (!this.peerId) return;
    const dot = $('#peerDot'), label = $('#peerState');
    if (!dot || !label) { clearInterval(this.presenceTimer); return; }
    try {
      const res = await Api.chat.presence(this.peerId);
      dot.classList.toggle('on', !!res.online);
      label.textContent = res.online ? 'onlayn' : 'oflayn';
    } catch (e) {
      dot.classList.remove('on');
      label.textContent = 'naməlum';
    }
  },

  updateLiveDot() {
    const label = $('#peerState');
    if (label && !this.connected) {
      // WS qopubsa REST hələ də işləyir — istifadəçiyə bunu bildiririk
      label.textContent = 'canlı bağlantı yoxdur';
    }
  },

  /* ---------- mesaj çıxarma ---------- */
  appendMessage(msg, skipScroll) {
    const body = $('#chatBody');
    if (!body) return;
    if (msg.id && this.seenIds.has(msg.id)) return;   // dublikatı at
    if (msg.id) this.seenIds.add(msg.id);

    body.insertAdjacentHTML('beforeend', this.messageHtml(msg));
    if (Number(msg.senderId) !== Number(Session.userId)) App.refreshChatBadge();

    const node = body.lastElementChild;
    const accept = $('[data-accept]', node);
    const reject = $('[data-reject]', node);
    if (accept) accept.addEventListener('click', () => this.respondOffer(msg.id, true, node));
    if (reject) reject.addEventListener('click', () => this.respondOffer(msg.id, false, node));

    if (!skipScroll) this.scrollDown();
  },

  messageHtml(msg) {
    const mine = Number(msg.senderId) === Number(Session.userId);

    if (msg.messageType === 'SYSTEM') {
      return '<div class="msg sys"><div class="bubble">' + esc(msg.content) + '</div></div>';
    }

    if (msg.messageType === 'OFFER') {
      // Təklifə yalnız qarşı tərəf (müştəri) və yalnız o hələ PENDING ikən cavab verə bilər
      const canRespond = !mine && msg.offerStatus === 'PENDING';
      const stateText = { ACCEPTED: 'Qəbul edildi', REJECTED: 'Rədd edildi' }[msg.offerStatus];
      return '<div class="msg ' + (mine ? 'me' : '') + '" data-msg="' + esc(msg.id) + '">' +
        '<div class="offer">' +
          '<div class="offer-label">Qiymət təklifi</div>' +
          '<div class="offer-amount">' + esc(fmtMoney(msg.amount)) + '</div>' +
          (msg.content ? '<div class="offer-note">' + esc(msg.content) + '</div>' : '') +
          (canRespond
            ? '<div class="offer-actions">' +
                '<button class="btn btn-primary btn-sm" data-accept>Qəbul et</button>' +
                '<button class="btn btn-danger btn-sm" data-reject>Rədd et</button>' +
              '</div>'
            : (stateText
                ? '<div class="offer-state ' + esc(msg.offerStatus) + '">' + esc(stateText) + '</div>'
                : '<div class="offer-state">Cavab gözlənilir…</div>')) +
        '</div>' +
        '<div class="msg-time">' + esc(fmtTime(msg.createdAt)) + '</div>' +
      '</div>';
    }

    return '<div class="msg ' + (mine ? 'me' : '') + '">' +
      '<div class="bubble">' + esc(msg.content) + '</div>' +
      '<div class="msg-time">' + esc(fmtTime(msg.createdAt)) + '</div>' +
    '</div>';
  },

  scrollDown() {
    const body = $('#chatBody');
    if (body) body.scrollTop = body.scrollHeight;
  },

  /* ---------- göndərmə ---------- */
  async sendText() {
    const input = $('#msgInput');
    if (!input) return;
    const text = input.value.trim();
    if (!text || !this.roomId) return;
    input.value = '';

    const payload = { senderId: Session.userId, content: text, messageType: 'TEXT' };

    // WebSocket açıqdırsa oradan — cavab broadcast ilə geri qayıdacaq.
    if (this.connected && this.client) {
      this.client.publish({
        destination: '/app/rooms/' + this.roomId + '/send',
        body: JSON.stringify(payload)
      });
      return;
    }

    // Ehtiyat yol: REST. Backend burada da broadcast edir, dublikatı id ilə tuturuq.
    try {
      const saved = await Api.chat.send(this.roomId, payload);
      this.appendMessage(saved);
    } catch (err) {
      toastErr(err.message);
      input.value = text;
    }
  },

  sendTyping() {
    if (!this.connected || !this.client || !this.roomId) return;
    const now = Date.now();
    if (now - this.lastTypingSent < 1800) return;   // hər hərfdə deyil, seyrək göndəririk
    this.lastTypingSent = now;
    this.client.publish({
      destination: '/app/rooms/' + this.roomId + '/typing',
      body: JSON.stringify({ userId: Session.userId })
    });
  },

  showTyping() {
    const line = $('#typingLine');
    if (!line) return;
    line.textContent = this.peerName + ' yazır…';
    clearTimeout(this.typingTimer);
    this.typingTimer = setTimeout(() => { line.textContent = ''; }, 3000);
  },

  /* ---------- təkliflər ---------- */
  promptOffer() {
    openModal('Qiymət təklifi',
      '<label class="field"><span>Məbləğ (AZN)</span>' +
        '<input type="number" id="offerAmount" min="1" step="1" placeholder="180"></label>' +
      '<label class="field" style="margin-top:14px"><span>Qeyd (istəyə bağlı)</span>' +
        '<input type="text" id="offerNote" maxlength="200" placeholder="Bu qiymətə 3 saatlıq seans daxildir"></label>',
      {
        okText: 'Təklifi göndər',
        onOk: async (overlay, close, okBtn) => {
          const amount = Number($('#offerAmount', overlay).value);
          if (!amount || amount <= 0) { toastErr('Düzgün məbləğ daxil et.'); return; }
          const note = $('#offerNote', overlay).value.trim();
          const done = withBusy(okBtn, 'Göndərilir');
          try {
            const saved = await Api.chat.send(this.roomId, {
              senderId: Session.userId,
              content: note || 'Bu qiymətə razısınızmı?',
              messageType: 'OFFER',
              amount
            });
            this.appendMessage(saved);
            close();
            toastOk('Təklif göndərildi.');
          } catch (err) {
            toastErr(err.message);
            done();
          }
        }
      });
  },

  async respondOffer(messageId, accept, node) {
    const buttons = $$('button', node);
    buttons.forEach(b => b.disabled = true);
    try {
      const updated = await Api.chat.respondToOffer(this.roomId, messageId, Session.userId, accept);
      // Təklif kartını yenilənmiş vəziyyətlə əvəz edirik
      this.seenIds.delete(messageId);
      node.outerHTML = this.messageHtml(updated);
      this.seenIds.add(messageId);
      toastOk(accept ? 'Təklif qəbul edildi, sifarişin qiyməti yeniləndi.' : 'Təklif rədd edildi.');
      this.scrollDown();
    } catch (err) {
      toastErr(err.message);
      buttons.forEach(b => b.disabled = false);
    }
  }
};
