# SIGNUM ANIMAE — Frontend

Saf HTML/CSS/JavaScript. Build, npm və ya framework tələb olunmur.

## Fayl strukturu

```
frontend/
├── index.html        açılış ekranı + giriş/qeydiyyat + tətbiq qabığı
├── css/style.css     bütün dizayn (tünd qara + tüstülü bənövşəyi palitra)
└── js/
    ├── api.js        backend ilə bütün əlaqə + sessiya/token idarəsi
    ├── ui.js         köməkçilər (təhlükəsiz HTML, toast, modal, tarix formatı)
    ├── auth.js       açılış animasiyası, giriş və qeydiyyat
    ├── chat.js       canlı söhbət (STOMP/WebSocket) + söhbət səhifəsi
    └── app.js        naviqasiya və bütün səhifələr
```

## İşə salmaq

Əvvəlcə bütün 6 backend servisi (gateway 8080, auth 8081, chat 8083, ai 8084,
notification 8085, booking 8089) və PostgreSQL + Redis işə salınmalıdır.

Sonra `frontend` qovluğunda sadə bir static server qaldır:

```bash
cd frontend
python -m http.server 5500
```

və brauzerdə `http://localhost:5500` aç.

> `index.html`-i birbaşa ikiqat kliklə açmaq (`file://`) tövsiyə olunmur —
> bu halda brauzer sorğuları "null" origin ilə göndərir və CORS problemi çıxa
> bilər. IntelliJ-də faylın üzərinə sağ klik → *Open in Browser* də işləyir.

## Portlar haradadır

`js/api.js` faylının ilk sətirlərində:

```js
const API_BASE = 'http://localhost:8080';   // gateway — bütün REST sorğular
const WS_URL   = 'ws://localhost:8083/ws-tattoo';  // chat-service — birbaşa
```

**WebSocket niyə birbaşa chat-service-ə gedir?** Spring Cloud Gateway Server
MVC-nin HTTP proxy handler-i WebSocket protokol "upgrade"-ini yerinə yetirə
bilmir, ona görə socket 8083-ə birbaşa qoşulur. Bütün REST sorğular isə normal
şəkildə gateway (8080) üzərindən keçir və JWT orada yoxlanılır.

## Rollar üzrə səhifələr

**Müştəri:** Kəşf et (axtarış + populyar ustalar) · Sifarişlərim · Söhbətlər ·
AI Studiya · Bildirişlər · Profilim

**Rəssam:** Gələn sifarişlər · Söhbətlər · Rəylərim · AI Studiya · Bildirişlər ·
Profilim (bio, təcrübə, stillər)

## Bilinən məhdudiyyətlər

- **Bildirişlər hazırda frontend-dən yaradılır.** Sifariş verildikdə və status
  dəyişdikdə `notification-service`-ə sorğu göndərilir. Düzgün memarlıqda bunu
  backend-in özü (booking-service) etməlidir — hazırda orada belə bir bağlantı
  yoxdur, ona görə funksiya görünsün deyə müvəqqəti olaraq frontend-ə qoyulub.
- **Söhbətdə istifadəçi kimliyi `?userId=` ilə ötürülür.** chat-service-in öz
  təhlükəsizlik qatı yoxdur; socket birbaşa qoşulduğu üçün gateway-in JWT
  yoxlamasından keçmir. Layihənin miqyasına uyğun şüurlu sadələşdirmədir.
- WebSocket qopanda söhbət avtomatik REST rejiminə keçir (mesaj gedir, amma
  canlı push olmur; səhifə yenilənəndə görünür).
