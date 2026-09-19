# SIGNUM ANIMAE — deploy

Hədəf quruluş: **Azure Container Apps** (6 servis + Redis), **bir PostgreSQL Flexible
Server** (içində 4 baza), **Static Web Apps** (frontend), email üçün **Resend** (SMTP, `kananpeoiks.me` domeni).

Ardıcıllıq vacibdir: əvvəlcə hər şeyi lokalda konteyner kimi işə sal. Azure-da xəta
axtarmaq həm yavaş, həm bahalıdır.

---

## 1. Lokal sınaq (Azure-dan ƏVVƏL)

```bash
cp .env.example .env      # dəyərləri yoxla, JWT_SECRET doldur
docker compose up --build
```

Qalxdıqdan sonra:

| Ünvan | Nə |
|---|---|
| http://localhost:5500 | Tətbiq |
| http://localhost:8025 | Gedən məktublar (mailpit tutur, internetə çıxmır) |

Yoxlanılmalı: qeydiyyat → giriş → usta axtarışı → sifariş → **söhbət** (WebSocket) →
şifrə sıfırlama (məktub mailpit-də görünməlidir).

Söhbət işləmirsə, `frontend/js/config.js` içindəki `wsUrl` dəyərinə bax — WebSocket
gateway-dən KEÇMİR, birbaşa chat-service-ə gedir.

```bash
docker compose down       # dayandır
docker compose down -v    # bazaları da sil
```

---

## 2. Azure — bir dəfəlik hazırlıq

```bash
az login
az group create --name signum-rg --location westeurope

# Şəkillər üçün registry
az acr create --resource-group signum-rg --name signumacr --sku Basic
az acr login --name signumacr
```

### PostgreSQL

```bash
az postgres flexible-server create \
  --resource-group signum-rg --name signum-pg \
  --tier Burstable --sku-name Standard_B1ms \
  --admin-user signumadmin --admin-password '<GÜCLÜ-ŞİFRƏ>' \
  --public-access 0.0.0.0 --version 16
```

Dörd bazanı yarat:

```bash
for db in authservice bookingservice chatservice notificationservice; do
  az postgres flexible-server db create \
    --resource-group signum-rg --server-name signum-pg \
    --database-name signum_animae_$db
done
```

### Container Apps mühiti

```bash
az containerapp env create \
  --resource-group signum-rg --name signum-env --location westeurope
```

---

## 3. Şəkilləri yığ və göndər

```bash
for s in gateway-service auth-service booking-service chat-service notification-service ai-service; do
  docker build -t signumacr.azurecr.io/$s:v1 ./$s
  docker push signumacr.azurecr.io/$s:v1
done
```

---

## 4. Servisləri qaldır

**Ən vacib qayda:** yalnız **gateway** və **chat-service** xaricə açıq olmalıdır.
Qalan dördü `--ingress internal` ilə qalır. Səbəb: bütün servislər `X-User-Id`
başlığına etibar edir — xaricdən çatan servis o başlığı özü yaza bilər.

chat-service istisnadır, çünki gateway WebSocket "upgrade" əməliyyatını proxy edə
bilmir. Onun REST tərəfini `JwtHeaderFilter` qoruyur — tokeni özü yoxlayır.

### Daxili servislər (auth, booking, notification, ai)

```bash
az containerapp create \
  --resource-group signum-rg --name auth-service \
  --environment signum-env \
  --image signumacr.azurecr.io/auth-service:v1 \
  --target-port 8081 --ingress internal \
  --min-replicas 0 --max-replicas 2 \
  --env-vars \
    DB_HOST=signum-pg.postgres.database.azure.com \
    DB_NAME=signum_animae_authservice \
    Username=signumadmin Password=secretref:db-password \
    REDIS_HOST=redis \
    JWT_SECRET=secretref:jwt-secret \
    INTERNAL_SERVICE_TOKEN=secretref:internal-token \
    NOTIFICATION_SERVICE_URL=http://notification-service \
    FRONTEND_URL=https://signumanimae.kananpeoiks.me
```

Qalan üçü üçün eyni şablon — yalnız `--name`, `--image`, `--target-port`,
`DB_NAME` və servis URL-ləri dəyişir. Daxili ünvanlar `http://<app-adı>` şəklindədir
(port yazılmır).

### Redis

```bash
az containerapp create \
  --resource-group signum-rg --name redis --environment signum-env \
  --image redis:7-alpine --target-port 6379 --ingress internal \
  --min-replicas 1 --max-replicas 1
```

Managed Azure Cache for Redis ala bilərsən, amma lazım deyil: buradakı iki istifadə
(usta baxış sayğacı, onlayn statusu) "fail-open"dur — Redis düşsə sistem işləməyə
davam edir.

### Xaricə açıq ikisi

```bash
az containerapp create ... --name gateway-service --target-port 8080 --ingress external
az containerapp create ... --name chat-service    --target-port 8083 --ingress external
```

Gateway-in env dəyişənləri:

```
JWT_SECRET=secretref:jwt-secret
CORS_ALLOWED_ORIGINS=https://signumanimae.kananpeoiks.me
AUTH_SERVICE_URL=http://auth-service
BOOKING_SERVICE_URL=http://booking-service
CHAT_SERVICE_URL=http://chat-service
AI_SERVICE_URL=http://ai-service
NOTIFICATION_SERVICE_URL=http://notification-service
```

---

## 5. Frontend

`frontend/js/config.js` faylını produksiya dəyərləri ilə yenilə:

```js
window.SIGNUM_CONFIG = {
  apiBase: 'https://gateway-service.<...>.azurecontainerapps.io',
  wsUrl:   'wss://chat-service.<...>.azurecontainerapps.io/ws-tattoo'
};
```

`https` və `wss` **məcburidir** — brauzer HTTPS səhifədən `http`/`ws` sorğusunu
bloklayır (mixed content).

Sonra Static Web Apps-a yüklə (GitHub Actions ilə avtomatik bağlanır):

```bash
az staticwebapp create \
  --resource-group signum-rg --name signum-animae \
  --source https://github.com/Kanan-peoiks/Signum_Animae \
  --branch master --app-location "frontend" --login-with-github
```

---

## 6. Deploy-dan sonra ilk yoxlamalar

1. **Qeydiyyat + giriş** — işləmirsə əvvəlcə gateway-in loguna bax
2. **Söhbət** — WebSocket qoşulmursa `wsUrl`-də `wss://` olduğunu yoxla
3. **Şifrə sıfırlama** — məktub gəlmirsə notification-service loguna və Resend
   panelindəki "Emails" siyahısına bax. `MAIL_FROM` Resend-də təsdiqlənmiş domendə
   olmalıdır (`noreply@kananpeoiks.me`)
4. **CORS** — brauzer konsolunda CORS xətası varsa `CORS_ALLOWED_ORIGINS` dəqiq
   frontend domenini göstərməlidir (`*` qoyma)

---

## Təhlükəsizlik — deploy zamanı unutma

| | |
|---|---|
| `JWT_SECRET` | Produksiya üçün **YENİSİNİ** yarat. Lokal dəyər aylarla açıq mətndə olub |
| `INTERNAL_SERVICE_TOKEN` | Eyni — yeni və uzun dəyər |
| Baza şifrəsi | Container Apps secret kimi saxla, env-ə açıq yazma |
| Resend API açarı | Eyni şəkildə secret (`resend-key`) |
| `.env` | Git-ə getmir, elə də qalsın |

Bütün sirləri belə yarat:

```bash
az containerapp secret set --resource-group signum-rg --name <app> \
  --secrets jwt-secret=<dəyər> internal-token=<dəyər> db-password=<dəyər>
```

---

## Təxmini aylıq xərc

| | |
|---|---|
| Container Apps (7 konteyner, sıfıra miqyaslanır) | pulsuz kvota daxilində ≈ $0–5 |
| PostgreSQL Flexible B1ms | ≈ $13–18 (yeni hesabda ilk 12 ay pulsuz ola bilər) |
| Static Web Apps | $0 |
| Resend (pulsuz plan, 3000 məktub/ay) | $0 |

Dəqiq rəqəm region və məzənnəyə görə dəyişir — Azure Pricing Calculator-da təsdiqlə.
