/* ============================================================
   config.js — mühitə görə dəyişən yeganə fayl.
   ============================================================ */

/* Deploy edərkən YALNIZ bu faylı əvəz etmək kifayətdir - qalan JS-ə
   toxunmaq lazım deyil.

   Lokalda: aşağıdakı dəyərlər olduğu kimi qalır.
   Produksiyada: https:// və wss:// olmalıdır, əks halda brauzer qarışıq
   məzmun (mixed content) səbəbindən sorğuları bloklayacaq.

   wsUrl birbaşa chat-service-ə gedir, gateway-ə YOX - gateway WebSocket
   "upgrade" əməliyyatını proxy edə bilmir. */
window.SIGNUM_CONFIG = {
  apiBase: 'http://localhost:8080',
  wsUrl:   'ws://localhost:8083/ws-tattoo'
};
