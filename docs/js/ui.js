// Toast System
export function showToast(message, type = 'success') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    let icon = type === 'success' ? '<i class="fa-solid fa-circle-check"></i>' : '<i class="fa-solid fa-circle-exclamation"></i>';
    
    toast.innerHTML = `${icon} <span>${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.classList.add('fade-out');
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

// Modal System
const globalModal = document.getElementById('global-modal');
const modalTitle = document.getElementById('modal-title');
const modalBody = document.getElementById('modal-body');
const btnCloseModal = document.getElementById('btn-close-modal');

btnCloseModal.addEventListener('click', closeModal);
globalModal.addEventListener('click', (e) => {
    if (e.target === globalModal) closeModal();
});

export function openModal(title, contentHtml, onMount = null) {
    modalTitle.innerText = title;
    modalBody.innerHTML = contentHtml;
    globalModal.classList.remove('hidden');
    
    if (onMount) onMount(modalBody);
}

export function closeModal() {
    globalModal.classList.add('hidden');
    setTimeout(() => modalBody.innerHTML = '', 300);
}

// Pagination Component
export function renderPagination(pageData, onPageChange) {
    if (!pageData) return '';
    
    const { number, totalPages } = pageData;
    let html = `<div class="pagination">
        <span>Página ${number + 1} de ${totalPages}</span>
        <div style="display:flex; gap:0.5rem;">
            <button class="btn btn-secondary" id="btn-prev-page" ${number === 0 ? 'disabled' : ''}>Anterior</button>
            <button class="btn btn-secondary" id="btn-next-page" ${number >= totalPages - 1 ? 'disabled' : ''}>Próxima</button>
        </div>
    </div>`;
    
    return html;
}

export function setupPaginationListeners(container, currentPage, onPageChange) {
    const prev = container.querySelector('#btn-prev-page');
    const next = container.querySelector('#btn-next-page');
    
    if(prev && !prev.disabled) prev.addEventListener('click', () => onPageChange(currentPage - 1));
    if(next && !next.disabled) next.addEventListener('click', () => onPageChange(currentPage + 1));
}

// View Switching
export const views = {};

export async function switchView(viewName) {
    const container = document.getElementById('view-container');
    const pageTitle = document.getElementById('page-title');
    
    // Update sidebar active state
    document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
    document.querySelector(`[data-view="${viewName}"]`)?.classList.add('active');

    // Title mapping
    const titles = {
        'dashboard': 'Dashboard Overview',
        'authors': 'Authors Management',
        'genres': 'Genres Management',
        'mangas': 'Mangas Catalog',
        'chapters': 'Chapters Database',
        'pages': 'Pages Viewer',
        'apikeys': 'API Keys & Settings'
    };
    
    pageTitle.innerText = titles[viewName] || viewName;

    // Load View
    if (views[viewName] && views[viewName].render) {
        container.innerHTML = '<div style="text-align:center; padding:2rem;"><i class="fa-solid fa-circle-notch fa-spin fa-2x" style="color:var(--accent)"></i></div>';
        try {
            await views[viewName].render(container);
        } catch (e) {
            console.error(e);
            container.innerHTML = `<div class="error-panel"><h3>Erro ao carregar View</h3><p>${e.message}</p></div>`;
        }
    } else {
        container.innerHTML = `<p>View ${viewName} not implemented yet.</p>`;
    }
}
