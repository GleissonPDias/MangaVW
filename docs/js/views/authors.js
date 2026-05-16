import { apiFetch } from '../api.js';
import { showToast, openModal, closeModal, renderPagination, setupPaginationListeners } from '../ui.js';

let currentPage = 0;
let currentSearch = '';

export const AuthorsView = {
    async render(container) {
        container.innerHTML = `
            <div class="action-toolbar">
                <h2>Authors</h2>
                <div class="search-box">
                    <input type="text" id="search-author" class="glass-input" placeholder="Search by name..." value="${currentSearch}">
                    <button class="btn btn-primary" id="btn-search"><i class="fa-solid fa-magnifying-glass"></i></button>
                    <button class="btn btn-primary" id="btn-add-author" style="margin-left: 1rem;"><i class="fa-solid fa-plus"></i> New Author</button>
                </div>
            </div>
            
            <div class="table-container">
                <table id="authors-table">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Name</th>
                            <th>Biography</th>
                            <th style="width: 150px;">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr><td colspan="4" style="text-align:center;">Loading...</td></tr>
                    </tbody>
                </table>
                <div id="pagination-container"></div>
            </div>
        `;

        document.getElementById('btn-add-author').addEventListener('click', () => this.openForm());
        
        const btnSearch = document.getElementById('btn-search');
        const inputSearch = document.getElementById('search-author');
        
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
        const tbody = document.querySelector('#authors-table tbody');
        const pContainer = document.getElementById('pagination-container');
        if(!tbody) return;

        try {
            let url = currentSearch ? `/authors/search?name=${encodeURIComponent(currentSearch)}&page=${currentPage}` : `/authors?page=${currentPage}`;
            const data = await apiFetch(url);
            
            const authors = data._embedded ? data._embedded.authorList : [];
            
            if(authors.length === 0) {
                tbody.innerHTML = `<tr><td colspan="4" style="text-align:center;">No authors found.</td></tr>`;
                pContainer.innerHTML = '';
                return;
            }

            tbody.innerHTML = authors.map(author => `
                <tr>
                    <td>${author.id}</td>
                    <td><strong>${author.name}</strong></td>
                    <td>${author.biography.length > 50 ? author.biography.substring(0, 50) + '...' : author.biography}</td>
                    <td class="actions-cell">
                        <button class="btn-icon btn-edit" data-id="${author.id}"><i class="fa-solid fa-pen"></i></button>
                        <button class="btn-icon btn-delete" data-id="${author.id}"><i class="fa-solid fa-trash" style="color:var(--danger)"></i></button>
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
                this.deleteAuthor(id);
            }));

        } catch (err) {
            tbody.innerHTML = `<tr><td colspan="4" style="text-align:center; color:var(--danger)">Error: ${err.message}</td></tr>`;
            showToast(err.message, 'error');
        }
    },

    async openForm(id = null) {
        let author = { name: '', biography: '' };
        
        if (id) {
            try {
                author = await apiFetch(`/authors/${id}`);
            } catch (err) {
                return showToast('Failed to load author details', 'error');
            }
        }

        const formHtml = `
            <div class="form-group">
                <label>Name</label>
                <input type="text" id="f-name" class="glass-input" value="${author.name}">
            </div>
            <div class="form-group">
                <label>Biography</label>
                <textarea id="f-bio" class="glass-input">${author.biography}</textarea>
            </div>
            <div class="modal-actions">
                <button class="btn btn-secondary" onclick="document.getElementById('btn-close-modal').click()">Cancel</button>
                <button class="btn btn-primary" id="btn-save-author">Save</button>
            </div>
        `;

        openModal(id ? 'Edit Author' : 'New Author', formHtml, (modalBody) => {
            modalBody.querySelector('#btn-save-author').addEventListener('click', async () => {
                const name = document.getElementById('f-name').value.trim();
                const biography = document.getElementById('f-bio').value.trim();
                
                if(!name || !biography) return showToast('Fill all fields', 'error');
                
                try {
                    if (id) {
                        await apiFetch(`/authors/${id}`, {
                            method: 'PUT',
                            body: JSON.stringify({ name, biography })
                        });
                        showToast('Author updated!');
                    } else {
                        await apiFetch('/authors', {
                            method: 'POST',
                            body: JSON.stringify({ name, biography })
                        });
                        showToast('Author created!');
                        currentPage = 0; // Reset pagination on create
                    }
                    closeModal();
                    this.loadData();
                } catch (err) {
                    showToast(err.message, 'error');
                }
            });
        });
    },

    async deleteAuthor(id) {
        if(!confirm('Are you sure you want to delete this author?')) return;
        try {
            await apiFetch(`/authors/${id}`, { method: 'DELETE' });
            showToast('Author deleted');
            this.loadData();
        } catch (err) {
            showToast(err.message, 'error');
        }
    }
};
