import { apiFetch } from '../api.js';
import { showToast } from '../ui.js';

export const DashboardView = {
    async render(container) {
        // Render shell
        container.innerHTML = `
            <div class="action-toolbar">
                <h2>Welcome to MangaVW</h2>
                <button class="btn btn-primary" id="btn-sync">
                    <i class="fa-solid fa-rotate"></i> Sync MangaDex
                </button>
            </div>
            
            <div class="dashboard-grid">
                <div class="stat-card">
                    <div class="stat-icon"><i class="fa-solid fa-book-open"></i></div>
                    <div class="stat-info">
                        <h3 id="stat-mangas">-</h3>
                        <p>Total Mangas</p>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon"><i class="fa-solid fa-list-ol"></i></div>
                    <div class="stat-info">
                        <h3 id="stat-chapters">-</h3>
                        <p>Total Chapters</p>
                    </div>
                </div>
            </div>
            
            <div class="glass-card" style="margin-top: 2rem;">
                <h3>System Status</h3>
                <p>Welcome to the MangaVW Premium Dashboard. Use the sidebar to manage all entities. Make sure you have configured your API Key in the settings.</p>
            </div>
        `;

        // Bind events
        document.getElementById('btn-sync').addEventListener('click', async (e) => {
            const btn = e.currentTarget;
            btn.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin"></i> Syncing...';
            btn.disabled = true;
            try {
                await apiFetch('/mangas/sync', { method: 'POST' });
                showToast('MangaDex Sync completed successfully!');
                this.loadStats(); // Reload stats
            } catch (err) {
                showToast(err.message, 'error');
            } finally {
                btn.innerHTML = '<i class="fa-solid fa-rotate"></i> Sync MangaDex';
                btn.disabled = false;
            }
        });

        this.loadStats();
    },

    async loadStats() {
        try {
            const mangasRes = await apiFetch('/mangas?size=1');
            document.getElementById('stat-mangas').innerText = mangasRes.page?.totalElements || 0;
            
            const chaptersRes = await apiFetch('/chapters?size=1');
            document.getElementById('stat-chapters').innerText = chaptersRes.page?.totalElements || 0;
        } catch(err) {
            console.error("Stats load failed", err);
        }
    }
};
