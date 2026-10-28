document.addEventListener('click', (event) => {
  const button = event.target.closest('[data-share-plan]');
  if (!button) return;
  event.preventDefault();
  const url = button.dataset.shareUrl;
  if (!url) return;
  const absoluteUrl = new URL(url, window.location.origin).href;
  const title = button.dataset.shareTitle || document.title;
  const text = button.dataset.shareText || '';
  if (navigator.share) {
    navigator.share({ title, text, url: absoluteUrl }).catch((error) => {
      if (error.name !== 'AbortError') copyLink(absoluteUrl, button);
    });
    return;
  }
  copyLink(absoluteUrl, button);
});

async function copyLink(url, button) {
  try {
    await navigator.clipboard.writeText(url);
  } catch {
    const input = document.createElement('input');
    input.value = url;
    input.style.position = 'fixed';
    input.style.opacity = '0';
    document.body.appendChild(input);
    input.select();
    document.execCommand('copy');
    input.remove();
  }
  const original = button.textContent;
  button.textContent = 'Link copiado';
  setTimeout(() => {
    button.textContent = original;
  }, 2000);
}
