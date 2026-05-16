import { apiFetch } from '../api.js';
import { showToast, openModal, closeModal, renderPagination, setupPaginationListeners } from '../ui.js';

let currentPage = 0;
let currentSearch = '';

export const GenresView = {
    async render(container) {
        container.innerHTML = `
            <div class="action-toolbar">
                <h2>Genres</h2>
                <div class="search-box">
                    <input type="text" id="search-genre" class="glass-input" placeholder="Search by name..." value="${currentSearch}">
                    <button class="btn btn-primary" id="btn-search"><i class="fa-solid fa-magnifying-glass"></i></button>
                    <button class="btn btn-primary" id="btn-add-genre" style="margin-left: 1rem;"><i class="fa-solid fa-plus"></i> New Genre</button>
                </div>
            </div>
            
            <div class="table-container">
                <table id="genres-table">
                    <thead>
                        <tr>
                            <th style="width: 80px;">ID</th>
                            <th>Name</th>
                            <th style="width: 150px;">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr><td colspan="3" style="text-align:center;">Loading...</td></tr>
                    </tbody>
                </table>
                <div id="pagination-container"></div>
            </div>
        `;

        document.getElementById('btn-add-genre').addEventListener('click', () => this.openForm());
        
        const btnSearch = document.getElementById('btn-search');
        const inputSearch = document.getElementById('search-genre');
        
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
        const tbody = document.querySelector('#genres-table tbody');
        const pContainer = document.getElementById('pagination-container');
        if(!tbody) return;

        try {
            let url = currentSearch ? `/genres/search?name=${encodeURIComponent(currentSearch)}&page=${currentPage}` : `/genres?page=${currentPage}`;
            const data = await apiFetch(url);
            
            const genres = data._embedded ? data._embedded.genreList : [];
            
            if(genres.length === 0) {
                tbody.innerHTML = `<tr><td colspan="3" style="text-align:center;">No genres found.</td></tr>`;
                pContainer.innerHTML = '';
                return;
            }

            tbody.innerHTML = genres.map(genre => `
                <tr>
                    <td>${genre.id}</td>
                    <td><span class="manga-tag">${genre.name}</span></td>
                    <td class="actions-cell">
                        <button class="btn-icon btn-edit" data-id="${genre.id}"><i class="fa-solid fa-pen"></i></button>
                        <button class="btn-icon btn-delete" data-id="${genre.id}"><i class="fa-solid fa-trash" style="color:var(--danger)"></i></button>
                    </td>
                </tr>
            `).join('');

            pContainer.innerHTML = renderPagination(data.page);
            setupPaginationListeners(pContainer, currentPage, (newPage) => {
                currentPage = newPage;
                this.loadData();
            });

            // Bind Actions
            document.querySelectorAll('.btn-edit').forEach(btn => btn.addEventListener('click', (e) => {
                const id = e.currentTarget.getAttribute('data-id');
                this.openForm(id);
            }));

            document.querySelectorAll('.btn-delete').forEach(btn => btn.addEventListener('click', (e) => {
                const id = e.currentTarget.getAttribute('data-id');
                this.deleteGenre(id);
            }));

        } catch (err) {
            tbody.innerHTML = `<tr><td colspan="3" style="text-align:center; color:var(--danger)">Error: ${err.message}</td></tr>`;
            showToast(err.message, 'error');
        }
    },

    async openForm(id = null) {
        let genre = { name: '' };
        
        if (id) {
            try {
                genre = await apiFetch(`/genres/${id}`);
            } catch (err) {
                return showToast('Failed to load genre', 'error');
            }
        }

        const formHtml = `
            <div class="form-group">
                <label>Name</label>
                <input type="text" id="f-name" class="glass-input" value="${genre.name}">
            </div>
            <div class="modal-actions">
                <button class="btn btn-secondary" onclick="document.getElementById('btn-close-modal').click()">Cancel</button>
                <button class="btn btn-primary" id="btn-save-genre">Save</button>
            </div>
        `;

        openModal(id ? 'Edit Genre' : 'New Genre', formHtml, (modalBody) => {
            modalBody.querySelector('#btn-save-genre').addEventListener('click', async () => {
                const name = document.getElementById('f-name').value.trim();
                
                if(!name) return showToast('Fill all fields', 'error');
                
                try {
                    if (id) {
                        await apiFetch(`/genres/${id}`, {
                            method: 'PUT',
                            body: JSON.stringify({ name })
                        });
                        showToast('Genre updated!');
                    } else {
                        await apiFetch('/genres', {
                            method: 'POST',
                            body: JSON.stringify({ name })
                        });
                        showToast('Genre created!');
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

    async deleteGenre(id) {
        if(!confirm('Are you sure you want to delete this genre?')) return;
        try {
            await apiFetch(`/genres/${id}`, { method: 'DELETE' });
            showToast('Genre deleted');
            this.loadData();
        } catch (err) {
            showToast(err.message, 'error');
        }
    }
};
