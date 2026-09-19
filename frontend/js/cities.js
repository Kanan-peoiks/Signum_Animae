/* ============================================================
   cities.js — Azərbaycanın şəhər və rayon mərkəzləri.
   Axtarış filtrində açılan siyahı (dropdown) bu siyahıdan qurulur.
   Əlifba sırası Azərbaycan əlifbasına görədir.
   ============================================================ */

const AZ_CITIES = [
  'Ağcabədi', 'Ağdam', 'Ağdaş', 'Ağstafa', 'Ağsu', 'Astara',
  'Babək', 'Bakı', 'Balakən', 'Bərdə', 'Beyləqan', 'Biləsuvar',
  'Cəbrayıl', 'Cəlilabad', 'Culfa',
  'Daşkəsən',
  'Füzuli',
  'Gədəbəy', 'Gəncə', 'Goranboy', 'Göyçay', 'Göygöl',
  'Hacıqabul',
  'Xaçmaz', 'Xankəndi', 'Xızı', 'Xocalı', 'Xocavənd',
  'İmişli', 'İsmayıllı',
  'Kəlbəcər', 'Kəngərli', 'Kürdəmir',
  'Qax', 'Qazax', 'Qəbələ', 'Qobustan', 'Quba', 'Qubadlı', 'Qusar',
  'Laçın', 'Lerik', 'Lənkəran',
  'Masallı', 'Mingəçevir',
  'Naftalan', 'Naxçıvan', 'Neftçala',
  'Oğuz', 'Ordubad',
  'Saatlı', 'Sabirabad', 'Salyan', 'Samux', 'Sədərək', 'Siyəzən', 'Sumqayıt',
  'Şabran', 'Şahbuz', 'Şamaxı', 'Şəki', 'Şəmkir', 'Şərur', 'Şirvan', 'Şuşa',
  'Tərtər', 'Tovuz',
  'Ucar',
  'Yardımlı', 'Yevlax',
  'Zaqatala', 'Zəngilan', 'Zərdab'
];

/* Şəhər seçimi üçün <select>. selected siyahıda yoxdursa (məsələn, usta
   profilində əl ilə yazılmış köhnə dəyər), o dəyər də əlavə olunur ki,
   seçim itməsin. */
function citySelect(id, selected, emptyLabel = 'Bütün şəhərlər') {
  const value = (selected || '').trim();
  const list = (value && !AZ_CITIES.includes(value)) ? [value].concat(AZ_CITIES) : AZ_CITIES;
  return '<select id="' + id + '">' +
    '<option value="">' + esc(emptyLabel) + '</option>' +
    list.map(c =>
      '<option value="' + esc(c) + '"' + (c === value ? ' selected' : '') + '>' + esc(c) + '</option>'
    ).join('') +
    '</select>';
}
