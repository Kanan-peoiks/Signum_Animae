/* ============================================================
   config.js — mühitə görə dəyişən yeganə fayl.
   ============================================================ */

/* Deploy edərkən YALNIZ bu faylı əvəz etmək kifayətdir.

   https və wss məcburidir - brauzer HTTPS səhifədən http/ws sorğusunu
   bloklayır (mixed content).

   wsUrl birbaşa chat-service-ə gedir, gateway-ə YOX - gateway WebSocket
   "upgrade" əməliyyatını proxy edə bilmir. */
window.SIGNUM_CONFIG = {
  apiBase: 'https://gateway-service.blackpond-2fcca8ba.polandcentral.azurecontainerapps.io',
  wsUrl:   'wss://chat-service.blackpond-2fcca8ba.polandcentral.azurecontainerapps.io/ws-tattoo'
};
