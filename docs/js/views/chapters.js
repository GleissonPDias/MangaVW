import { apiFetch } from '../api.js';
import { showToast, openModal, closeModal, renderPagination, setupPaginationListeners } from '../ui.js';

let currentPage = 0;
let currentSearch = '';

export const ChaptersView = {
    async render(container) {
        container.innerHTML = `
            <div class="action-toolbar">
                <h2>Chapters</h2>
                <div class="search-box">
                    <input type="text" id="search-chapter" class="glass-input" placeholder="Search by language..." value="${currentSearch}">
                    <button class="btn btn-primary" id="btn-search"><i class="fa-solid fa-magnifying-glass"></i></button>
                    <button class="btn btn-primary" id="btn-add-chapter" style="margin-left: 1rem;"><i class="fa-solid fa-plus"></i> New Chapter</button>
                </div>
            </div>
            
            <div class="table-container">
                <table id="chapters-table">
                    <thead>
                        <tr>
                            <th style="width: 80px;">ID</th>
                            <th>Number</th>
                            <th>Language</th>
                            <th>Manga</th>
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

        document.getElementById('btn-add-chapter').addEventListener('click', () => this.openForm());
        
        const btnSearch = document.getElementById('btn-search');
        const inputSearch = document.getElementById('search-chapter');
        
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
        const tbody = document.querySelector('#chapters-table tbody');
        const pContainer = document.getElementById('pagination-container');
        if(!tbody) return;

        try {
            let url = currentSearch ? `/chapters/search?language=${encodeURIComponent(currentSearch)}&page=${currentPage}` : `/chapters?page=${currentPage}`;
            const data = await apiFetch(url);
            
            const chapters = data._embedded ? data._embedded.chapterList : [];
            
            if(chapters.length === 0) {
                tbody.innerHTML = `<tr><td colspan="5" style="text-align:center;">No chapters found.</td></tr>`;
                pContainer.innerHTML = '';
                return;
            }

            tbody.innerHTML = chapters.map(ch => `
                <tr>
                    <td>${ch.id}</td>
                    <td><span class="badge active">Ch. ${ch.chapterNumber}</span></td>
                    <td>${ch.language}</td>
                    <td>${ch.manga ? ch.manga.title : 'N/A'}</td>
                    <td class="actions-cell">
                        <button class="btn-icon btn-edit" data-id="${ch.id}"><i class="fa-solid fa-pen"></i></button>
                        <button class="btn-icon btn-delete" data-id="${ch.id}"><i class="fa-solid fa-trash" style="color:var(--danger)"></i></button>
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
                this.deleteChapter(id);
            }));

        } catch (err) {
            tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; color:var(--danger)">Error: ${err.message}</td></tr>`;
            showToast(err.message, 'error');
        }
    },

    async openForm(id = null) {
        let chapter = { chapterNumber: '', language: 'PT-BR', manga: null };
        let mangas = [];
        
        try {
            // Fetch mangas for the dropdown
            const mangasData = await apiFetch(`/mangas?size=100`);
            mangas = mangasData._embedded ? mangasData._embedded.mangaList : [];
            
            if (id) {
                chapter = await apiFetch(`/chapters/${id}`);
            }
        } catch (err) {
            return showToast('Failed to load data for form', 'error');
        }

        const mangaOptions = mangas.map(m => `<option value="${m.id}" ${chapter.manga && chapter.manga.id === m.id ? 'selected' : ''}>${m.title}</option>`).join('');

        const formHtml = `
            <div class="form-group">
                <label>Chapter Number</label>
                <input type="number" step="0.1" id="f-num" class="glass-input" value="${chapter.chapterNumber}">
            </div>
            <div class="form-group">
                <label>Language</label>
                <input type="text" id="f-lang" class="glass-input" value="${chapter.language}">
            </div>
            <div class="form-group">
                <label>Manga</label>
                <select id="f-manga" class="glass-select">
                    <option value="">Select Manga...</option>
                    ${mangaOptions}
                </select>
            </div>
            <div class="modal-actions">
                <button class="btn btn-secondary" onclick="document.getElementById('btn-close-modal').click()">Cancel</button>
                <button class="btn btn-primary" id="btn-save-chapter">Save</button>
            </div>
        `;

        openModal(id ? 'Edit Chapter' : 'New Chapter', formHtml, (modalBody) => {
            modalBody.querySelector('#btn-save-chapter').addEventListener('click', async () => {
                const chapterNumber = document.getElementById('f-num').value;
                const language = document.getElementById('f-lang').value.trim();
                const mangaId = document.getElementById('f-manga').value;
                
                if(!chapterNumber || !language || !mangaId) return showToast('Fill all fields', 'error');
                
                const payload = { 
                    chapterNumber: parseFloat(chapterNumber), 
                    language, 
                    manga: { id: parseInt(mangaId) } 
                };

                try {
                    if (id) {
                        await apiFetch(`/chapters/${id}`, {
                            method: 'PUT',
                            body: JSON.stringify(payload)
                        });
                        showToast('Chapter updated!');
                    } else {
                        await apiFetch('/chapters', {
                            method: 'POST',
                            body: JSON.stringify(payload)
                        });
                        showToast('Chapter created!');
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

    async deleteChapter(id) {
        if(!confirm('Are you sure you want to delete this chapter?')) return;
        try {
            await apiFetch(`/chapters/${id}`, { method: 'DELETE' });
            showToast('Chapter deleted');
            this.loadData();
        } catch (err) {
            showToast(err.message, 'error');
        }
    }
};
