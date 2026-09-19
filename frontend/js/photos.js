/* ============================================================
   photos.js — daxili səhifələrdə hansı fotonun harada görünəcəyi.
   Bir şəkli dəyişmək və ya yeni səhifəyə banner vermək üçün yalnız
   bu fayla toxunmaq kifayətdir; stilləri theme.css-dədir.
   ============================================================ */

const PHOTO_DIR = 'assets/photos/';

/* Səhifə başlığının arxasındakı foto (route → foto).
   y — fotonun hansı hissəsinin görünəcəyi (background-position-y).
   slim — alçaq banner: Söhbətlərdə söhbət sahəsinin hündürlüyü ekrana
   görə hesablanır, hündür banner mesaj yazma yerini aşağı itələyərdi. */
const PAGE_PHOTOS = {
  discover:     { src: 'shop-smith-street.webp', y: '4%'  },
  following:    { src: 'shop-boston.webp',       y: '30%' },
  bookings:     { src: 'studio-interior.webp',   y: '45%' },
  orders:       { src: 'studio-interior.webp',   y: '45%' },
  profile:      { src: 'shop-club-tattoo.webp',  y: '35%' },
  chats:        { src: 'neon-inked.webp',        y: '47%', slim: true },
  notifs:       { src: 'tools-tray.webp',        y: '50%' },
  availability: { src: 'studio-letters.webp',    y: '28%' },
  reviews:      { src: 'process-closeup.webp',   y: '27%' },
  analytics:    { src: 'artist-portrait.webp',   y: '22%' }
};

// AI Studiyadakı "İlham üçün" zolağı
const INSPO_PHOTOS = ['work-viking.webp', 'work-reaper.webp', 'work-buddha.webp', 'work-thorns.webp'];

// Usta profilinin örtüyü — ustanın id-sinə görə seçilir: hər usta üçün sabit,
// amma ustadan-ustaya fərqli
const ARTIST_COVERS = [
  { src: 'studio-letters.webp',  y: '30%' },
  { src: 'process-closeup.webp', y: '27%' },
  { src: 'studio-interior.webp', y: '45%' }
];

// Boş siyahılarda ikon əvəzinə göstərilən yumru foto
const EMPTY_PHOTO = 'machine.webp';

/* CSS dəyişəni üçün stil sətri. Ünvan mütləqdir: CSS dəyişənindəki nisbi
   url() bəzi brauzerlərdə theme.css-in qovluğuna görə həll olunur və
   şəkil tapılmır. */
function photoStyle(photo) {
  const url = new URL(PHOTO_DIR + photo.src, document.baseURI).href;
  return '--photo:url(\'' + url + '\');--photo-y:' + photo.y;
}

function inspoStrip() {
  return '<div class="inspo" aria-hidden="true">' +
      '<div class="inspo-label">İlham üçün</div>' +
      '<div class="inspo-row">' +
        INSPO_PHOTOS.map(f => '<img src="' + PHOTO_DIR + f + '" alt="" loading="lazy">').join('') +
      '</div>' +
    '</div>';
}

function artistCover(artistUserId) {
  const cover = ARTIST_COVERS[Math.abs(Number(artistUserId) || 0) % ARTIST_COVERS.length];
  return '<div class="artist-cover" aria-hidden="true" style="' + photoStyle(cover) + '"></div>';
}
