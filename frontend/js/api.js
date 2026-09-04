/* ============================================================
   api.js — backend ilə bütün əlaqə burada cəmlənib.

   VACİB: bütün REST sorğular gateway (8080) üzərindən gedir.
   YALNIZ WebSocket birbaşa chat-service-ə (8083) qoşulur, çünki
   Spring Cloud Gateway Server MVC-nin HTTP proxy handler-i
   WebSocket "upgrade" əməliyyatını dəstəkləmir.
   ============================================================ */

const API_BASE = 'http://localhost:8080';
const WS_URL   = 'ws://localhost:8083/ws-tattoo';

/* ---------- sessiya yaddaşı ---------- */
const SESSION_KEY = 'signum.session';

const Session = {
  data: null,

  load() {
    try {
      const raw = localStorage.getItem(SESSION_KEY);
      if (raw) this.data = JSON.parse(raw);
    } catch (e) {
      // Şəxsi rejim / bloklanmış storage — yaddaşsız davam edirik.
      this.data = null;
    }
    return this.data;
  },

  save(auth) {
    this.data = auth;
    try { localStorage.setItem(SESSION_KEY, JSON.stringify(auth)); } catch (e) { /* susmaq */ }
  },

  patch(partial) {
    if (!this.data) return;
    this.data = { ...this.data, ...partial };
    try { localStorage.setItem(SESSION_KEY, JSON.stringify(this.data)); } catch (e) { /* susmaq */ }
  },

  clear() {
    this.data = null;
    try { localStorage.removeItem(SESSION_KEY); } catch (e) { /* susmaq */ }
  },

  get token()    { return this.data ? this.data.token  : null; },
  get userId()   { return this.data ? this.data.userId : null; },
  get role()     { return this.data ? this.data.role   : null; },
  get isArtist() { return this.role === 'ARTIST'; }
};

/* ---------- aşağı səviyyəli sorğu ---------- */
class ApiError extends Error {
  constructor(message, status) { super(message); this.status = status; }
}

/* Sessiya bitəndə (401/403) çox vaxt bir neçə sorğu paralel işləyir və
   HAMISI eyni anda 401 qaytarır - hər biri öz catch-ində toastErr çağırsa,
   ekranda üst-üstə bir neçə "sessiya bitib" bildirişi yığılır. Bunun
   qarşısını almaq üçün: son auth-xətasının vaxtını qeyd edirik, toastErr
   (ui.js) isə bu qısa pəncərədə yeni "err" toast göstərmir - yalnız
   signum:unauthorized qlobal handler-in öz mesajı görünür. */
let lastAuthFailureAt = 0;

async function request(method, path, body, opts = {}) {
  const headers = {};
  if (Session.token) headers['Authorization'] = 'Bearer ' + Session.token;

  let payload;
  if (body instanceof FormData) {
    // Content-Type-ı brauzer özü qoyur (boundary ilə birlikdə) — əl ilə qoysaq sınar.
    payload = body;
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    payload = JSON.stringify(body);
  }

  let res;
  try {
    res = await fetch(API_BASE + path, { method, headers, body: payload });
  } catch (e) {
    throw new ApiError('Serverə qoşulmaq olmadı. Servislərin işlədiyini yoxla.', 0);
  }

  if (res.status === 401 || res.status === 403) {
    if (!opts.silentAuth) {
      Session.clear();
      lastAuthFailureAt = Date.now();
      window.dispatchEvent(new CustomEvent('signum:unauthorized'));
    }
    throw new ApiError('Sessiya bitib və ya icazə yoxdur. Yenidən daxil ol.', res.status);
  }

  const text = await res.text();
  let data = null;
  if (text) { try { data = JSON.parse(text); } catch (e) { data = text; } }

  if (!res.ok) {
    const msg = (data && data.message) ? data.message : ('Xəta baş verdi (' + res.status + ')');
    throw new ApiError(msg, res.status);
  }
  return data;
}

const GET   = (p, o)    => request('GET', p, undefined, o);
const POST  = (p, b, o) => request('POST', p, b, o);
const PATCH = (p, b, o) => request('PATCH', p, b, o);

/* ============================================================
   API — servis-servis qruplaşdırılmış
   ============================================================ */
const Api = {

  /* ---- auth-service ---- */
  auth: {
    register: (payload) => POST('/api/v1/auth/register', payload),
    login:    (payload) => POST('/api/v1/auth/login', payload)
  },

  /* ---- auth-service: rəssam profilləri ---- */
  artists: {
    search(city, style, minRating, minExperience, sortBy) {
      const q = new URLSearchParams();
      if (city)         q.set('city', city);
      if (style)        q.set('style', style);
      if (minRating)    q.set('minRating', minRating);
      if (minExperience) q.set('minExperience', minExperience);
      if (sortBy)       q.set('sortBy', sortBy);
      const qs = q.toString();
      return GET('/api/v1/artists/public/search' + (qs ? '?' + qs : ''));
    },
    popular: (limit = 8) => GET('/api/v1/artists/public/popular?limit=' + limit),
    // {userId} — rəssamın USER id-si (bütün sistemdə "artistId" elə budur)
    byUserId: (userId) => GET('/api/v1/artists/public/' + userId),
    updateProfile: (userId, payload) => PATCH('/api/v1/artists/' + userId, payload),
    viewCount: (userId) => GET('/api/v1/artists/' + userId + '/views')
  },

  /* ---- auth-service: istifadəçi hesabı ---- */
  users: {
    get:    (id) => GET('/api/v1/users/' + id),
    update: (id, payload) => PATCH('/api/v1/users/' + id, payload)
  },

  /* ---- booking-service ---- */
  bookings: {
    create:      (payload) => POST('/api/v1/bookings', payload),
    byId:        (id) => GET('/api/v1/bookings/' + id),
    forCustomer: (customerId) => GET('/api/v1/bookings/customer/' + customerId),
    forArtist:   (artistId) => GET('/api/v1/bookings/artist/' + artistId),
    setStatus:   (id, status) => PATCH('/api/v1/bookings/' + id + '/status', { status }),
    /* Başqasının profilindəki "keçmiş tatuajlar" siyahısı - server artıq qiymət/qeyd
       kimi məxfi sahələri kəsir və usta adını premium-a görə özü maskalayır (bax
       booking-service BookingService.getCompletedSummaryForCustomer). */
    completedSummary: (customerId) => GET('/api/v1/bookings/customer/' + customerId + '/completed-summary'),
    artistStats: (artistId) => GET('/api/v1/bookings/artist/' + artistId + '/stats')
  },

  /* ---- booking-service: rəylər ---- */
  reviews: {
    create:    (payload) => POST('/api/v1/reviews', payload),
    forArtist: (artistId) => GET('/api/v1/reviews/artist/' + artistId),
    reply:     (reviewId, payload) => PATCH('/api/v1/reviews/' + reviewId + '/reply', payload)
  },

  /* ---- chat-service ---- */
  chat: {
    // bookingId üzrə idempotent: eyni bron üçün həmişə eyni otağı qaytarır
    getOrCreateRoom: (payload) => POST('/api/v1/chat/rooms', payload),
    room:            (roomId) => GET('/api/v1/chat/rooms/' + roomId),
    roomsForCustomer:(customerId) => GET('/api/v1/chat/rooms/customer/' + customerId),
    roomsForArtist:  (artistId) => GET('/api/v1/chat/rooms/artist/' + artistId),
    history:         (roomId) => GET('/api/v1/chat/rooms/' + roomId + '/messages'),
    send:            (roomId, payload) => POST('/api/v1/chat/rooms/' + roomId + '/messages', payload),
    markRead:        (roomId, userId) => PATCH('/api/v1/chat/rooms/' + roomId + '/messages/read?userId=' + userId),
    respondToOffer:  (roomId, messageId, userId, accept) =>
                      PATCH('/api/v1/chat/rooms/' + roomId + '/messages/' + messageId + '/offer', { userId, accept }),
    presence:        (userId) => GET('/api/v1/chat/presence/' + userId),
    unreadCount:     (userId) => GET('/api/v1/chat/unread-count/' + userId),
    offerStats:      (artistId) => GET('/api/v1/chat/rooms/artist/' + artistId + '/offer-stats')
  },

  /* ---- ai-service ---- */
  ai: {
    generateIdea: (payload) => POST('/api/v1/ai/generate-idea', payload),
    analyzeImage(file, prompt) {
      const fd = new FormData();
      fd.append('image', file);
      if (prompt && prompt.trim()) fd.append('prompt', prompt.trim());
      return POST('/api/v1/ai/analyze-image', fd);
    }
  },

  /* ---- notification-service ---- */
  notifications: {
    send:      (payload) => POST('/api/v1/notifications/send', payload),
    forUser:   (userId) => GET('/api/v1/notifications/user/' + userId),
    markRead:  (id) => PATCH('/api/v1/notifications/' + id + '/read')
  }
};

/* Bildiriş göndərmək arxa planda baş verir — uğursuz olsa əsas
   əməliyyatı pozmamalıdır, ona görə səhv udulur. E-poçt ünvanını server özü
   həll edir (bax notifyQuietly-nin daxilindəki qeydə); notification-service
   tərəfində real SMTP (MAIL_USERNAME/MAIL_PASSWORD env dəyişənləri)
   qurulmayıbsa e-poçt göndərilməsi öz-özünə səssizcə uğursuz olur, amma
   in-app bildiriş hər zaman yaranır. */
async function notifyQuietly(userId, userEmail, title, message) {
  // E-poçt ünvanını artıq özümüz çəkmirik: notification-service bunu server
  // tərəfində, auth-service-dən, öz daxili kanalı ilə həll edir (bax
  // NotificationService.sendNotification). Əvvəllər bunu Api.users.get(userId)
  // ilə brauzerdə çəkirdik ki, başqasının e-poçtunu ifşa edirdi (server indi
  // özününkü olmayan profil üçün e-poçtu heç göndərmir də) - userEmail arqumenti
  // geriyə uyğunluq üçün saxlanılıb, amma artıq istifadə olunmur.
  try {
    await Api.notifications.send({ userId, title, message, sendEmail: true });
  } catch (e) { /* susmaq */ }
}
