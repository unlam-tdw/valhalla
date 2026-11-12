/** Collapse budget notes only when their rendered text exceeds one line. */
export function initBudgetDescription() {
    const container = document.querySelector('[data-budget-description]');
    if (!container) return;
    const notes = container.querySelector('p');
    const button = container.querySelector('button');
    let expanded = false;

    function measure() {
        notes.classList.add('plan-budget-notes--collapsed');
        const overflowing = notes.scrollHeight > notes.clientHeight + 1;
        button.hidden = !overflowing;
        if (!overflowing) expanded = false;
        notes.classList.toggle('plan-budget-notes--collapsed', !expanded);
        button.setAttribute('aria-expanded', String(expanded));
        button.textContent = expanded ? 'Ver menos' : 'Ver más';
    }

    button.addEventListener('click', () => {
        expanded = !expanded;
        notes.classList.toggle('plan-budget-notes--collapsed', !expanded);
        button.setAttribute('aria-expanded', String(expanded));
        button.textContent = expanded ? 'Ver menos' : 'Ver más';
    });

    // Observe width only: expanding the text also changes the container's height.
    let width;
    new ResizeObserver(([entry]) => {
        if (entry.contentRect.width === width) return;
        width = entry.contentRect.width;
        measure();
    }).observe(container);
    document.fonts.ready.then(measure);
    measure();
}
