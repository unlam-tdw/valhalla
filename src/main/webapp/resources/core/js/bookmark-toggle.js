/**
 * [MARK] Toggle de "Guardados" para las vistas server-rendered.
 *
 * El fragmento components/bookmark-button.html sigue siendo un form POST: sin JavaScript el
 * navegador guarda o quita y recarga. Este modulo intercepta ese submit y usa el endpoint REST
 * (POST/DELETE /api/plans/{id}/bookmarks), que responde 204: no hay recarga y el icono, el
 * aria-pressed y las clases se actualizan en el lugar.
 *
 * Tambien pregunta la confirmacion al quitar. Los forms de Guardados del perfil
 * (data-bookmark-remove="true") no se interceptan: siguen siendo POST nativos, pero comparten la
 * misma confirmacion para que el usuario vea un solo dialogo.
 */
import { ApiError, apiFetch } from './lib/api.js';

const CONFIRM_REMOVE = '¿Quitar este plan de Guardados?';

function labelFor(bookmarked) {
  return bookmarked ? 'Quitar de Guardados' : 'Guardar en Guardados';
}

/** Pinta el estado final en el boton y deja el form coherente si el JS se desactiva despues. */
function paint(button, bookmarked) {
  const planId = button.dataset.planId;
  const label = labelFor(bookmarked);

  button.dataset.bookmarked = String(bookmarked);
  button.setAttribute('aria-pressed', String(bookmarked));
  button.setAttribute('aria-label', label);
  button.setAttribute('title', label);
  button.classList.toggle('text-primary', bookmarked);
  button.classList.toggle('text-gray-600', !bookmarked);
  button.classList.toggle('hover:text-primary', !bookmarked);

  button
    .querySelector('[data-bookmark-icon="filled"]')
    ?.classList.toggle('hidden', !bookmarked);
  button
    .querySelector('[data-bookmark-icon="outline"]')
    ?.classList.toggle('hidden', bookmarked);

  const srOnly = button.querySelector('.sr-only');
  if (srOnly) srOnly.textContent = label;

  const form = button.closest('form');
  if (!form || !planId) return;
  form.action = bookmarked
    ? `/bookmarks/${planId}/remove`
    : `/bookmarks/${planId}`;
  if (bookmarked) form.setAttribute('data-bookmark-remove', 'true');
  else form.removeAttribute('data-bookmark-remove');
}

async function toggle(button) {
  const planId = button.dataset.planId;
  if (!planId) return;

  const removing = button.dataset.bookmarked === 'true';
  try {
    const response = await apiFetch(`/api/plans/${planId}/bookmarks`, {
      method: removing ? 'DELETE' : 'POST'
    });
    if (response.status !== 204) {
      throw new ApiError(`La operación falló (${response.status}).`, response.status);
    }
    paint(button, !removing);
  } catch (error) {
    console.error('No se pudo actualizar Guardados:', error);
  }
}

function onSubmit(event) {
  const form = event.target;
  if (!(form instanceof HTMLFormElement)) return;

  const button = form.querySelector('[data-bookmark-toggle]');
  if (!button) {
    // El "Quitar" de la card del perfil sigue siendo un POST nativo; solo se confirma.
    if (form.getAttribute('data-bookmark-remove') === 'true' && !window.confirm(CONFIRM_REMOVE)) {
      event.preventDefault();
    }
    return;
  }

  event.preventDefault();
  if (button.disabled) return;

  const removing = button.dataset.bookmarked === 'true';
  if (removing && !window.confirm(CONFIRM_REMOVE)) return;

  toggle(button);
}

document.addEventListener('submit', onSubmit);
