import { apiFetch } from '../api.js';
import { showToast, openModal, closeModal, renderPagination, setupPaginationListeners } from '../ui.js';

let currentPage = 0;
let currentSearch = '';

export const MangasView = {
    async render(container) {
        container.innerHTML = `
            <div class="action-toolbar">
                <h2>Mangas</h2>
                <div class="search-box">
                    <input type="text" id="search-manga" class="glass-input" placeholder="Search by title..." value="${currentSearch}">
                    <button class="btn btn-primary" id="btn-search"><i class="fa-solid fa-magnifying-glass"></i></button>
                    <button class="btn btn-secondary" id="btn-sync-mangas" style="margin-left: 1rem;"><i class="fa-solid fa-rotate"></i> Sync MangaDex</button>
                    <button class="btn btn-primary" id="btn-add-manga" style="margin-left: 1rem;"><i class="fa-solid fa-plus"></i> New Manga</button>
                </div>
            </div>
            
            <div class="manga-grid" id="mangas-grid">
                <!-- Skeleton loader -->
                <div class="glass-card skeleton"><div class="skeleton-img"></div><div class="skeleton-text"></div></div>
                <div class="glass-card skeleton"><div class="skeleton-img"></div><div class="skeleton-text"></div></div>
            </div>
            <div id="pagination-container" style="margin-top: 2rem;"></div>
        `;

        document.getElementById('btn-add-manga').addEventListener('click', () => this.openForm());
        
        const btnSync = document.getElementById('btn-sync-mangas');
        btnSync.addEventListener('click', async () => {
            btnSync.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin"></i> Syncing...';
            btnSync.disabled = true;
            try {
                await apiFetch('/mangas/sync', { method: 'POST' });
                showToast('MangaDex Sync completed successfully!');
                currentPage = 0;
                this.loadData();
            } catch (err) {
                showToast(err.message, 'error');
            } finally {
                btnSync.innerHTML = '<i class="fa-solid fa-rotate"></i> Sync MangaDex';
                btnSync.disabled = false;
            }
        });
        
        const btnSearch = document.getElementById('btn-search');
        const inputSearch = document.getElementById('search-manga');
        
        btnSearch.addEventListener('click', () => {
            currentSearch = inputSearch.value.trim();
            currentPage = 0;
            this.loadData();
        });

        inputSearch.addEventListener('keypress', (e) => {
            if(e.key === 'Enter') btnSearch.click();
        });

        await this.loadData();
    },

    async loadData() {
        const grid = document.getElementById('mangas-grid');
        const pContainer = document.getElementById('pagination-container');
        if(!grid) return;

        try {
            let url = currentSearch ? `/mangas/search?title=${encodeURIComponent(currentSearch)}&page=${currentPage}` : `/mangas?page=${currentPage}`;
            const data = await apiFetch(url);
            
            const mangas = data._embedded ? data._embedded.mangaList : [];
            
            if(mangas.length === 0) {
                grid.innerHTML = `<p style="grid-column: 1/-1; text-align: center; color: var(--text-secondary)">No mangas found.</p>`;
                pContainer.innerHTML = '';
                return;
            }

            grid.innerHTML = mangas.map(manga => {
                const authorName = manga.author ? manga.author.name : 'Unknown Author';
                let coverUrl = 'https://via.placeholder.com/250x350/1e293b/94a3b8?text=No+Cover';
                if (manga.chapters && manga.chapters[0] && manga.chapters[0].pages && manga.chapters[0].pages[0]) {
                    coverUrl = manga.chapters[0].pages[0].imageUrl;
                }

                let genresHtml = (manga.genres || []).slice(0, 3).map(g => `<span class="manga-tag">${g.name}</span>`).join(' ');

                return `
                <div class="manga-card">
                    <img src="${coverUrl}" class="manga-cover" referrerpolicy="no-referrer" onerror="this.src='https://via.placeholder.com/250x350/1e293b/94a3b8?text=No+Cover'">
                    <h3>${manga.title}</h3>
                    <div class="manga-author">By ${authorName}</div>
                    <div style="display:flex; justify-content:space-between; align-items:center;">
                        <span class="status-badge">${manga.status}</span>
                        <span style="font-size: 0.85rem; color: var(--text-secondary); font-weight: 700;">${manga.chapters?.length || 0} Ch.</span>
                    </div>
                    <div class="manga-tags">${genresHtml}</div>
                    <p style="flex-grow:1;">${manga.sinopsis?.substring(0, 100) || ''}...</p>
                    <div class="manga-card-actions">
                        <button class="btn btn-secondary btn-edit" data-id="${manga.id}" style="flex:1"><i class="fa-solid fa-pen"></i> Edit</button>
                        <button class="btn btn-danger btn-delete" data-id="${manga.id}"><i class="fa-solid fa-trash"></i></button>
                    </div>
                </div>`;
            }).join('');

            pContainer.innerHTML = renderPagination(data.page);
            setupPaginationListeners(pContainer, currentPage, (newPage) => {
                currentPage = newPage;
                this.loadData();
            });

            document.querySelectorAll('.btn-edit').forEach(btn => btn.addEventListener('click', (e) => {
                const id = e.currentTarget.getAttribute('data-id');
                this.openForm(id);
            }));

            document.querySelectorAll('.btn-delete').forEach(btn => btn.addEventListener('click', (e) => {
                const id = e.currentTarget.getAttribute('data-id');
                this.deleteManga(id);
            }));

        } catch (err) {
            grid.innerHTML = `<p style="grid-column: 1/-1; text-align: center; color: var(--danger)">Error: ${err.message}</p>`;
            showToast(err.message, 'error');
        }
    },

    async openForm(id = null) {
        let manga = { title: '', sinopsis: '', status: 'EM_ANDAMENTO', author: null, details: null, genres: [] };
        let authors = [];
        let genres = [];
        
        try {
            const authorsData = await apiFetch(`/authors?size=100`);
            authors = authorsData._embedded ? authorsData._embedded.authorList : [];
            
            const genresData = await apiFetch(`/genres?size=100`);
            genres = genresData._embedded ? genresData._embedded.genreList : [];
            
            if (id) {
                manga = await apiFetch(`/mangas/${id}`);
            }
        } catch (err) {
            console.error(err);
            return showToast('Failed to load form requirements: ' + err.message, 'error');
        }

        const authorOptions = authors.map(a => `<option value="${a.id}" ${manga.author && manga.author.id === a.id ? 'selected' : ''}>${a.name}</option>`).join('');
        const genreOptions = genres.map(g => {
            const isSelected = (manga.genres || []).some(mg => mg.id === g.id);
            return `<div style="display:flex; align-items:center; gap:0.5rem;"><input type="checkbox" class="g-checkbox" value="${g.id}" ${isSelected ? 'checked' : ''}> <label>${g.name}</label></div>`;
        }).join('');

        const formHtml = `
            <div style="display:grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
                <div class="form-group" style="grid-column: 1/-1">
                    <label>Title</label>
                    <input type="text" id="f-title" class="glass-input" value="${manga.title}">
                </div>
                <div class="form-group">
                    <label>Status</label>
                    <select id="f-status" class="glass-select">
                        <option value="EM_ANDAMENTO" ${manga.status === 'EM_ANDAMENTO' ? 'selected' : ''}>Em Andamento</option>
                        <option value="FINALIZADO" ${manga.status === 'FINALIZADO' ? 'selected' : ''}>Finalizado</option>
                        <option value="CANCELADO" ${manga.status === 'CANCELADO' ? 'selected' : ''}>Cancelado</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Author</label>
                    <select id="f-author" class="glass-select">
                        <option value="">Select Author...</option>
                        ${authorOptions}
                    </select>
                </div>
                <div class="form-group">
                    <label>Publication Year</label>
                    <input type="number" id="f-year" class="glass-input" value="${manga.details ? manga.details.publicationYear : 2024}">
                </div>
                <div class="form-group">
                    <label>Licensed</label>
                    <select id="f-licensed" class="glass-select">
                        <option value="true" ${manga.details && manga.details.licensed ? 'selected' : ''}>Yes</option>
                        <option value="false" ${manga.details && !manga.details.licensed ? 'selected' : ''}>No</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>ISBN</label>
                    <input type="text" id="f-isbn" class="glass-input" value="${manga.details && manga.details.isbn !== 'N/A' ? manga.details.isbn : ''}" placeholder="ex: 978-85...">
                </div>
            </div>
            <div class="form-group">
                <label>Synopsis</label>
                <textarea id="f-synopsis" class="glass-input">${manga.sinopsis}</textarea>
            </div>
            <div class="form-group">
                <label>Genres</label>
                <div style="display:grid; grid-template-columns: 1fr 1fr 1fr; gap:0.5rem; max-height:150px; overflow-y:auto; padding:0.5rem; background:rgba(0,0,0,0.2); border-radius:8px;">
                    ${genreOptions}
                </div>
            </div>
            <div class="modal-actions">
                <button class="btn btn-secondary" onclick="document.getElementById('btn-close-modal').click()">Cancel</button>
                <button class="btn btn-primary" id="btn-save-manga">Save</button>
            </div>
        `;

        openModal(id ? 'Edit Manga' : 'New Manga', formHtml, (modalBody) => {
            modalBody.querySelector('#btn-save-manga').addEventListener('click', async () => {
                const title = document.getElementById('f-title').value.trim();
                const sinopsis = document.getElementById('f-synopsis').value.trim();
                const status = document.getElementById('f-status').value;
                const authorId = document.getElementById('f-author').value;
                const year = document.getElementById('f-year').value;
                const licensed = document.getElementById('f-licensed').value === 'true';
                const isbn = document.getElementById('f-isbn').value.trim();
                
                const selectedGenres = Array.from(modalBody.querySelectorAll('.g-checkbox:checked')).map(cb => ({ id: parseInt(cb.value) }));
                
                if(!title || !sinopsis || !authorId) return showToast('Title, Synopsis and Author are required', 'error');
                
                const payload = { 
                    title, 
                    sinopsis, 
                    status,
                    author: { id: parseInt(authorId) },
                    details: { publicationYear: parseInt(year) || 2024, licensed, isbn: isbn || "N/A" },
                    genres: selectedGenres
                };

                try {
                    if (id) {
                        await apiFetch(`/mangas/${id}`, {
                            method: 'PUT',
                            body: JSON.stringify(payload)
                        });
                        showToast('Manga updated!');
                    } else {
                        await apiFetch('/mangas', {
                            method: 'POST',
                            body: JSON.stringify(payload)
                        });
                        showToast('Manga created!');
                        currentPage = 0;
                    }
                    closeModal();
                    this.loadData();
                } catch (err) {
                    showToast(err.message, 'error');
                }
            });
        });
    },

    async deleteManga(id) {
        if(!confirm('Are you sure you want to delete this manga?')) return;
        try {
            await apiFetch(`/mangas/${id}`, { method: 'DELETE' });
            showToast('Manga deleted');
            this.loadData();
        } catch (err) {
            showToast(err.message, 'error');
        }
    }
};
