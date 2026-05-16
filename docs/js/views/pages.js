import { apiFetch } from '../api.js';
import { showToast, openModal, closeModal, renderPagination, setupPaginationListeners } from '../ui.js';

let currentPage = 0;
let currentSearch = '';

export const PagesView = {
    async render(container) {
        container.innerHTML = `
            <div class="action-toolbar">
                <h2>Pages</h2>
                <div class="search-box">
                    <input type="text" id="search-page" class="glass-input" placeholder="Search by Image URL..." value="${currentSearch}">
                    <button class="btn btn-primary" id="btn-search"><i class="fa-solid fa-magnifying-glass"></i></button>
                    <button class="btn btn-primary" id="btn-add-page" style="margin-left: 1rem;"><i class="fa-solid fa-plus"></i> New Page</button>
                </div>
            </div>
            
            <div class="table-container">
                <table id="pages-table">
                    <thead>
                        <tr>
                            <th style="width: 80px;">ID</th>
                            <th>Page No.</th>
                            <th>Image</th>
                            <th>Chapter</th>
                            <th style="width: 150px;">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr><td colspan="5" style="text-align:center;">Loading...</td></tr>
                    </tbody>
                </table>
                <div id="pagination-container"></div>
            </div>
        `;

        document.getElementById('btn-add-page').addEventListener('click', () => this.openForm());
        
        const btnSearch = document.getElementById('btn-search');
        const inputSearch = document.getElementById('search-page');
        
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
        const tbody = document.querySelector('#pages-table tbody');
        const pContainer = document.getElementById('pagination-container');
        if(!tbody) return;

        try {
            let url = currentSearch ? `/pages/search?imageUrl=${encodeURIComponent(currentSearch)}&page=${currentPage}` : `/pages?page=${currentPage}`;
            const data = await apiFetch(url);
            
            const pages = data._embedded ? data._embedded.pageList : [];
            
            if(pages.length === 0) {
                tbody.innerHTML = `<tr><td colspan="5" style="text-align:center;">No pages found.</td></tr>`;
                pContainer.innerHTML = '';
                return;
            }

            tbody.innerHTML = pages.map(p => `
                <tr>
                    <td>${p.id}</td>
                    <td>${p.pageNumber}</td>
                    <td>
                        <img src="${p.imageUrl}" alt="Page" style="height:50px; border-radius:4px; object-fit:cover; background:var(--glass-bg);" referrerpolicy="no-referrer" onerror="this.style.display='none'">
                    </td>
                    <td>${p.chapter ? 'Ch. ' + p.chapter.chapterNumber + ' (' + (p.chapter.manga ? p.chapter.manga.title : '') + ')' : 'N/A'}</td>
                    <td class="actions-cell">
                        <button class="btn-icon btn-edit" data-id="${p.id}"><i class="fa-solid fa-pen"></i></button>
                        <button class="btn-icon btn-delete" data-id="${p.id}"><i class="fa-solid fa-trash" style="color:var(--danger)"></i></button>
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
                this.deletePage(id);
            }));

        } catch (err) {
            tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; color:var(--danger)">Error: ${err.message}</td></tr>`;
            showToast(err.message, 'error');
        }
    },

    async openForm(id = null) {
        let page = { pageNumber: '', imageUrl: '', chapter: null };
        let chapters = [];
        
        try {
            const chData = await apiFetch(`/chapters?size=100`);
            chapters = chData._embedded ? chData._embedded.chapterList : [];
            
            if (id) {
                page = await apiFetch(`/pages/${id}`);
            }
        } catch (err) {
            return showToast('Failed to load data for form', 'error');
        }

        const chOptions = chapters.map(c => `<option value="${c.id}" ${page.chapter && page.chapter.id === c.id ? 'selected' : ''}>Ch. ${c.chapterNumber} - ${c.manga ? c.manga.title : ''}</option>`).join('');

        const formHtml = `
            <div class="form-group">
                <label>Page Number</label>
                <input type="number" id="f-num" class="glass-input" value="${page.pageNumber}">
            </div>
            <div class="form-group">
                <label>Image URL</label>
                <input type="text" id="f-url" class="glass-input" value="${page.imageUrl}">
            </div>
            <div class="form-group">
                <label>Chapter</label>
                <select id="f-chapter" class="glass-select">
                    <option value="">Select Chapter...</option>
                    ${chOptions}
                </select>
            </div>
            <div class="modal-actions">
                <button class="btn btn-secondary" onclick="document.getElementById('btn-close-modal').click()">Cancel</button>
                <button class="btn btn-primary" id="btn-save-page">Save</button>
            </div>
        `;

        openModal(id ? 'Edit Page' : 'New Page', formHtml, (modalBody) => {
            modalBody.querySelector('#btn-save-page').addEventListener('click', async () => {
                const pageNumber = document.getElementById('f-num').value;
                const imageUrl = document.getElementById('f-url').value.trim();
                const chapterId = document.getElementById('f-chapter').value;
                
                if(!pageNumber || !imageUrl || !chapterId) return showToast('Fill all fields', 'error');
                
                const payload = { 
                    pageNumber: parseInt(pageNumber), 
                    imageUrl, 
                    chapter: { id: parseInt(chapterId) } 
                };

                try {
                    if (id) {
                        await apiFetch(`/pages/${id}`, {
                            method: 'PUT',
                            body: JSON.stringify(payload)
                        });
                        showToast('Page updated!');
                    } else {
                        await apiFetch('/pages', {
                            method: 'POST',
                            body: JSON.stringify(payload)
                        });
                        showToast('Page created!');
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

    async deletePage(id) {
        if(!confirm('Are you sure you want to delete this page?')) return;
        try {
            await apiFetch(`/pages/${id}`, { method: 'DELETE' });
            showToast('Page deleted');
            this.loadData();
        } catch (err) {
            showToast(err.message, 'error');
        }
    }
};
