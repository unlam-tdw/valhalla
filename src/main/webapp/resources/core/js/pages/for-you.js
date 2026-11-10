/**
 * [PAGE] "Cargar mas" del feed /for-you: pide el endpoint parcial con la pagina siguiente y
 * agrega las cards al grid existente, sin recargar la pagina ni perder los filtros.
 *
 * El link del template ya es un fallback real a la pagina completa; este modulo solo intercepta
 * el click cuando hay JavaScript. El fragmento y la pagina completa comparten el mismo markup
 * (components/feed-cards), asi no hay dos versiones de la card.
 */
import { fetchFragment } from '../lib/api.js';

const button = document.querySelector('[data-load-more]');
const grid = document.querySelector('[data-feed-grid]');

if (button && grid) {
  const more = document.querySelector('[data-feed-more]');
  const error = document.querySelector('#feed-load-more-error');
  let loading = false;

  const setLoading = (value) => {
    loading = value;
    button.setAttribute('aria-disabled', String(value));
    button.style.pointerEvents = value ? 'none' : '';
    button.textContent = value ? 'Cargando…' : 'Cargar más';
    grid.setAttribute('aria-busy', String(value));
  };

  const showError = () => {
    if (!error) return;
    error.textContent = 'No se pudieron cargar más planes. Probá de nuevo.';
    error.hidden = false;
  };

  button.addEventListener('click', async (event) => {
    // Deja pasar ctrl/cmd/shift-click: abrir el fallback en otra pestana sigue siendo util.
    if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey || event.button !== 0) {
      return;
    }
    event.preventDefault();
    if (loading) return;

    const url = button.dataset.loadMoreUrl;
    if (!url) return;

    setLoading(true);
    if (error) {
      error.hidden = true;
      error.textContent = '';
    }
    try {
      const html = await fetchFragment(url);
      const fragment = new DOMParser().parseFromString(html, 'text/html');
      const page = fragment.querySelector('[data-feed-page]');
      if (!page) throw new Error('El feed no devolvió una página válida.');

      page.querySelectorAll('article').forEach((card) => {
        grid.append(document.importNode(card, true));
      });

      if (page.dataset.hasMore === 'true') {
        button.href = page.dataset.nextPageUrl;
        button.dataset.loadMoreUrl = page.dataset.nextPartialUrl;
      } else if (more) {
        more.remove();
      }
    } catch {
      showError();
    } finally {
      setLoading(false);
    }
  });
}
