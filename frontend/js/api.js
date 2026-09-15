/* ============================================================
   api.js — backend ilə bütün əlaqə burada cəmlənib.
   ============================================================ */

const API_BASE = 'http://localhost:8080';
const PAGE_SIZE = 12;
const CHAT_PAGE_SIZE = 30;
const WS_URL   = 'ws://localhost:8083/ws-tattoo';

const SESSION_KEY = 'signum.session';

const Session = {
  data: null,

  load() {
    try {
      const raw = localStorage.getItem(SESSION_KEY);
      if (raw) this.data = JSON.parse(raw);
    } catch (e) {
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
  get isArtist() { return this.role === 'ARTIST'; },
  get isAdmin()  { return this.role === 'ADMIN'; }
};

class ApiError extends Error {
  constructor(message, status) { super(message); this.status = status; }
}

let lastAuthFailureAt = 0;

async function request(method, path, body, opts = {}) {
  const headers = {};
  if (Session.token) headers['Authorization'] = 'Bearer ' + Session.token;

  let payload;
  if (body instanceof FormData) {
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
    const authText = await res.text();
    let authData = null;
    if (authText) { try { authData = JSON.parse(authText); } catch (e) { authData = authText; } }
    const serverMessage = (authData && authData.message) ? authData.message : null;

    if (opts.silentAuth) {
      throw new ApiError(serverMessage || 'Giriş alınmadı.', res.status);
    }

    if (res.status === 403) {
      throw new ApiError(serverMessage || 'Bu əməliyyat üçün icazən yoxdur.', 403);
    }

    Session.clear();
    lastAuthFailureAt = Date.now();
    window.dispatchEvent(new CustomEvent('signum:unauthorized'));
    throw new ApiError(serverMessage || 'Sessiya bitib. Yenidən daxil ol.', 401);
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

const GET    = (p, o)    => request('GET', p, undefined, o);
const POST   = (p, b, o) => request('POST', p, b, o);
const PATCH  = (p, b, o) => request('PATCH', p, b, o);
const DELETE = (p, o)    => request('DELETE', p, undefined, o);

/* ============================================================
   API — servis-servis qruplaşdırılmış
   ============================================================ */
const Api = {

  auth: {
    register: (payload) => POST('/api/v1/auth/register', payload, { silentAuth: true }),
    login:    (payload) => POST('/api/v1/auth/login', payload, { silentAuth: true }),
    forgotPassword:   (email) => POST('/api/v1/auth/forgot-password', { email }),
    resetPassword:    (token, newPassword) => POST('/api/v1/auth/reset-password', { token, newPassword }),
    verifyEmail:      (token) => GET('/api/v1/auth/verify-email?token=' + encodeURIComponent(token)),
    sendVerification: (email) => POST('/api/v1/auth/send-verification', { email })
  },

  artists: {
    search(city, style, minRating, minExperience, sortBy, page = 0, size = PAGE_SIZE) {
      const q = new URLSearchParams();
      if (city)         q.set('city', city);
      if (style)        q.set('style', style);
      if (minRating)    q.set('minRating', minRating);
      if (minExperience) q.set('minExperience', minExperience);
      if (sortBy)       q.set('sortBy', sortBy);
      q.set('page', page);
      q.set('size', size);
      return GET('/api/v1/artists/public/search?' + q.toString());
    },
    popular: (limit = 8) => GET('/api/v1/artists/public/popular?limit=' + limit),
    byUserId: (userId) => GET('/api/v1/artists/public/' + userId),
    updateProfile: (userId, payload) => PATCH('/api/v1/artists/' + userId, payload),
    viewCount: (userId) => GET('/api/v1/artists/' + userId + '/views')
  },

  users: {
    get:    (id) => GET('/api/v1/users/' + id),
    update: (id, payload) => PATCH('/api/v1/users/' + id, payload)
  },

  bookings: {
    create:      (payload) => POST('/api/v1/bookings', payload),
    byId:        (id) => GET('/api/v1/bookings/' + id),
    forCustomer: (customerId, page = 0, size = PAGE_SIZE) =>
                 GET('/api/v1/bookings/customer/' + customerId + '?page=' + page + '&size=' + size),
    forArtist:   (artistId, page = 0, size = PAGE_SIZE) =>
                 GET('/api/v1/bookings/artist/' + artistId + '?page=' + page + '&size=' + size),
    setStatus:   (id, status) => PATCH('/api/v1/bookings/' + id + '/status', { status }),
    completedSummary: (customerId) => GET('/api/v1/bookings/customer/' + customerId + '/completed-summary'),
    artistStats: (artistId) => GET('/api/v1/bookings/artist/' + artistId + '/stats')
  },

  availability: {
    add:          (payload) => POST('/api/v1/availability', payload),
    forArtist:    (artistId) => GET('/api/v1/availability/artist/' + artistId),
    publicSlots:  (artistId) => GET('/api/v1/availability/artist/' + artistId + '/public'),
    setBooked:    (id, booked) => PATCH('/api/v1/availability/' + id + '/booked?booked=' + booked),
    remove:       (id) => DELETE('/api/v1/availability/' + id)
  },

  reviews: {
    create:    (payload) => POST('/api/v1/reviews', payload),
    forArtist: (artistId, page = 0, size = PAGE_SIZE) =>
               GET('/api/v1/reviews/artist/' + artistId + '?page=' + page + '&size=' + size),
    reply:     (reviewId, payload) => PATCH('/api/v1/reviews/' + reviewId + '/reply', payload)
  },

  chat: {
    getOrCreateRoom: (payload) => POST('/api/v1/chat/rooms', payload),
    room:            (roomId) => GET('/api/v1/chat/rooms/' + roomId),
    roomsForCustomer:(customerId) => GET('/api/v1/chat/rooms/customer/' + customerId),
    roomsForArtist:  (artistId) => GET('/api/v1/chat/rooms/artist/' + artistId),
    history:         (roomId, page = 0, size = CHAT_PAGE_SIZE) =>
                     GET('/api/v1/chat/rooms/' + roomId + '/messages?page=' + page + '&size=' + size),
    send:            (roomId, payload) => POST('/api/v1/chat/rooms/' + roomId + '/messages', payload),
    markRead:        (roomId) => PATCH('/api/v1/chat/rooms/' + roomId + '/messages/read'),
    respondToOffer:  (roomId, messageId, userId, accept) =>
                      PATCH('/api/v1/chat/rooms/' + roomId + '/messages/' + messageId + '/offer', { userId, accept }),
    presence:        (userId) => GET('/api/v1/chat/presence/' + userId),
    unreadCount:     (userId) => GET('/api/v1/chat/unread-count/' + userId),
    offerStats:      (artistId) => GET('/api/v1/chat/rooms/artist/' + artistId + '/offer-stats')
  },

  ai: {
    generateIdea: (payload) => POST('/api/v1/ai/generate-idea', payload),
    analyzeImage(file, prompt) {
      const fd = new FormData();
      fd.append('image', file);
      if (prompt && prompt.trim()) fd.append('prompt', prompt.trim());
      return POST('/api/v1/ai/analyze-image', fd);
    }
  },

  aiIdeas: {
    save:        (payload) => POST('/api/v1/ai-ideas', payload),
    forCustomer: (customerId) => GET('/api/v1/ai-ideas/customer/' + customerId),
    link:        (id, bookingId) => PATCH('/api/v1/ai-ideas/' + id + '/link?bookingId=' + bookingId),
    remove:      (id) => DELETE('/api/v1/ai-ideas/' + id)
  },

  follows: {
    add:         (artistId) => POST('/api/v1/follows', { artistId }),
    remove:      (artistId) => DELETE('/api/v1/follows?artistId=' + artistId),
    forCustomer: (customerId) => GET('/api/v1/follows/customer/' + customerId),
    count:       (artistId) => GET('/api/v1/follows/artist/' + artistId + '/count'),
    isFollowing: (artistId) => GET('/api/v1/follows/exists?artistId=' + artistId)
  },

  admin: {
    users:         (page = 0, size = PAGE_SIZE) =>
                   GET('/api/v1/admin/users?page=' + page + '&size=' + size),
    setBanned:     (userId, banned) => PATCH('/api/v1/admin/users/' + userId + '/ban?banned=' + banned),
    reviews:       (page = 0, size = PAGE_SIZE) =>
                   GET('/api/v1/admin/reviews?page=' + page + '&size=' + size),
    deleteReview:  (id) => DELETE('/api/v1/admin/reviews/' + id),
    userStats:     () => GET('/api/v1/admin/stats/users'),
    bookingStats:  () => GET('/api/v1/admin/stats/bookings')
  },

  notifications: {
    forUser:   (userId, page = 0, size = PAGE_SIZE) =>
               GET('/api/v1/notifications/user/' + userId + '?page=' + page + '&size=' + size),
    unreadCount: (userId) => GET('/api/v1/notifications/user/' + userId + '/unread-count'),
    markRead:  (id) => PATCH('/api/v1/notifications/' + id + '/read')
  }
};
