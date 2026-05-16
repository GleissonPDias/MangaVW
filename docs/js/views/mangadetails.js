import { apiFetch } from '../api.js';
import { showToast, openModal, closeModal, renderPagination, setupPaginationListeners } from '../ui.js';

let currentPage = 0;

export const MangaDetailsView = {
    async render(container) {
        container.innerHTML = `
            <div class="action-toolbar">
                <h2>Manga Details</h2>
                <div class="search-box">
                    <select id="filter-licensed" class="glass-select" style="margin-right: 1rem;">
                        <option value="">All (Licensed & Unlicensed)</option>
                        <option value="true">Licensed Only</option>
                        <option value="false">Unlicensed Only</option>
                    </select>
                    <button class="btn btn-primary" id="btn-add-details"><i class="fa-solid fa-plus"></i> New Details</button>
                </div>
            </div>
            
            <div class="table-container">
                <table id="details-table">
                    <thead>
                        <tr>
                            <th style="width: 80px;">ID</th>
                            <th>ISBN</th>
                            <th>Year</th>
                            <th>Licensed</th>
                            <th>Linked Manga</th>
                            <th style="width: 150px;">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr><td colspan="6" style="text-align:center;">Loading...</td></tr>
                    </tbody>
                </table>
                <div id="pagination-container"></div>
            </div>
        `;

        document.getElementById('btn-add-details').addEventListener('click', () => this.openForm());
        
        document.getElementById('filter-licensed').addEventListener('change', () => {
            currentPage = 0;
            this.loadData();
        });

        await this.loadData();
    },

    async loadData() {
        const tbody = document.querySelector('#details-table tbody');
        const pContainer = document.getElementById('pagination-container');
        if(!tbody) return;

        try {
            const filterVal = document.getElementById('filter-licensed') ? document.getElementById('filter-licensed').value : '';
            let url = `/manga-details?page=${currentPage}`;
            if (filterVal !== '') {
                url += `&licensed=${filterVal}`;
            }

            const data = await apiFetch(url);
            
            const detailsList = data._embedded ? data._embedded.mangaDetailsList : [];
            
            if(detailsList.length === 0) {
                tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;">No details found.</td></tr>`;
                pContainer.innerHTML = '';
                return;
            }

            tbody.innerHTML = detailsList.map(d => `
                <tr>
                    <td>${d.id}</td>
                    <td>${d.isbn || 'N/A'}</td>
                    <td>${d.publicationYear}</td>
                    <td><span class="badge ${d.licensed ? 'active' : 'inactive'}">${d.licensed ? 'Yes' : 'No'}</span></td>
                    <td>${d.manga ? d.manga.title : 'None'}</td>
                    <td class="actions-cell">
                        <button class="btn-icon btn-edit" data-id="${d.id}"><i class="fa-solid fa-pen"></i></button>
                        <button class="btn-icon btn-delete" data-id="${d.id}"><i class="fa-solid fa-trash" style="color:var(--danger)"></i></button>
                    </td>
                </tr>
            `).join('');

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
                this.deleteDetails(id);
            }));

        } catch (err) {
            tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:var(--danger)">Error: ${err.message}</td></tr>`;
            showToast(err.message, 'error');
        }
    },

    async openForm(id = null) {
        let details = { isbn: '', publicationYear: 2024, licensed: false };
        
        if (id) {
            try {
                details = await apiFetch(`/manga-details/${id}`);
            } catch (err) {
                return showToast('Failed to load details', 'error');
            }
        }

        const formHtml = `
            <div class="form-group">
                <label>ISBN</label>
                <input type="text" id="f-isbn" class="glass-input" value="${details.isbn || ''}">
            </div>
            <div class="form-group">
                <label>Publication Year</label>
                <input type="number" id="f-year" class="glass-input" value="${details.publicationYear}">
            </div>
            <div class="form-group">
                <label>Licensed</label>
                <select id="f-licensed" class="glass-select">
                    <option value="true" ${details.licensed ? 'selected' : ''}>Yes</option>
                    <option value="false" ${!details.licensed ? 'selected' : ''}>No</option>
                </select>
            </div>
            <div class="modal-actions">
                <button class="btn btn-secondary" onclick="document.getElementById('btn-close-modal').click()">Cancel</button>
                <button class="btn btn-primary" id="btn-save-details">Save</button>
            </div>
        `;

        openModal(id ? 'Edit Manga Details' : 'New Manga Details', formHtml, (modalBody) => {
            modalBody.querySelector('#btn-save-details').addEventListener('click', async () => {
                const isbn = document.getElementById('f-isbn').value.trim();
                const year = document.getElementById('f-year').value;
                const licensed = document.getElementById('f-licensed').value === 'true';
                
                const payload = { 
                    isbn: isbn || "N/A", 
                    publicationYear: parseInt(year) || 2024, 
                    licensed 
                };

                try {
                    if (id) {
                        await apiFetch(`/manga-details/${id}`, {
                            method: 'PUT',
                            body: JSON.stringify(payload)
                        });
                        showToast('Details updated!');
                    } else {
                        await apiFetch('/manga-details', {
                            method: 'POST',
                            body: JSON.stringify(payload)
                        });
                        showToast('Details created!');
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

    async deleteDetails(id) {
        if(!confirm('Are you sure you want to delete these details?')) return;
        try {
            await apiFetch(`/manga-details/${id}`, { method: 'DELETE' });
            showToast('Details deleted');
            this.loadData();
        } catch (err) {
            showToast(err.message, 'error');
        }
    }
};
