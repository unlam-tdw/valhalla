(function () {
  const region = document.createElement('div');
  region.className = 'planit-toasts';
  region.setAttribute('aria-live', 'polite');
  region.setAttribute('aria-relevant', 'additions');
  document.body.appendChild(region);

  function show(message) {
    if (!message) return;
    const toast = document.createElement('div');
    toast.className = 'planit-toast';
    toast.setAttribute('role', 'status');
    const text = document.createElement('span');
    text.textContent = message;
    const close = document.createElement('button');
    close.type = 'button';
    close.textContent = '\u00d7';
    close.setAttribute('aria-label', 'Cerrar aviso');
    toast.append(text, close);
    region.appendChild(toast);
    let timer;
    const dismiss = () => { clearTimeout(timer); toast.remove(); };
    const start = () => { clearTimeout(timer); timer = setTimeout(dismiss, 6000); };
    close.addEventListener('click', dismiss);
    toast.addEventListener('mouseenter', () => clearTimeout(timer));
    toast.addEventListener('mouseleave', start);
    toast.addEventListener('focusin', () => clearTimeout(timer));
    toast.addEventListener('focusout', start);
    start();
  }
  document.querySelectorAll('[data-toast]').forEach(node => {
    show(node.textContent.trim());
    node.remove();
  });
  const url = new URL(window.location.href);
  const messages = {
    passwordChanged: 'Contraseña actualizada.',
    saved: 'Configuración guardada.'
  };
  let consumed = false;
  Object.entries(messages).forEach(([key, message]) => {
    if (url.searchParams.get(key) === 'true') {
      show(message);
      url.searchParams.delete(key);
      consumed = true;
    }
  });
  if (consumed) history.replaceState(history.state, '', url.pathname + url.search + url.hash);
})();
