/**
 * El color de cada categoría, en un solo lugar. Los tokens están declarados una sola vez en
 * brand.css (layouts/base :: head) y se leen del :root computado: ningún hex vive acá, así que
 * el mapa de /explore y el del importador comparten la misma fuente.
 */

/* Tokens de categoría: la tabla enum -> token, sin colores propios. */
export const CATEGORY_TOKEN_BY_ENUM = {
  'RESTAURANT': '--category-restaurant',
  'BAR': '--category-bar',
  'CAFE': '--category-cafe',
  'MUSEUM': '--category-museum',
  'PARK': '--category-park',
  'SHOPPING': '--category-shopping',
  'NIGHTLIFE': '--category-nightlife',
  'CULTURE': '--category-culture',
  'SPORT': '--category-sport',
  'OTHER': '--category-other'
};
/* Respaldo para una categoría que no está en la tabla. */
export const OTHER_COLOR_TOKEN = '--category-other';

/** El color de la categoría, ya resuelto por el browser: sirve para un style o para Leaflet. */
export const categoryColor = (category) => {
  const rootStyle = getComputedStyle(document.documentElement);
  const token = CATEGORY_TOKEN_BY_ENUM[category] || OTHER_COLOR_TOKEN;
  // trim() importa: getPropertyValue devuelve el valor crudo del token, con espacios alrededor
  // en varios navegadores. El fallback también sale de un token, para que ningún hex quede
  // literal acá.
  return rootStyle.getPropertyValue(token).trim()
    || rootStyle.getPropertyValue(OTHER_COLOR_TOKEN).trim();
};
