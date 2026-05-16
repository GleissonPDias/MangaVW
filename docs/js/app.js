import { switchView, views } from './ui.js';

// Import Views
import { DashboardView } from './views/dashboard.js';
import { AuthorsView } from './views/authors.js';
import { GenresView } from './views/genres.js';
import { MangasView } from './views/mangas.js';
import { MangaDetailsView } from './views/mangadetails.js';
import { ChaptersView } from './views/chapters.js';
import { PagesView } from './views/pages.js';
import { ApiKeysView } from './views/apikeys.js';

// Register Views
views['dashboard'] = DashboardView;
views['authors'] = AuthorsView;
views['genres'] = GenresView;
views['mangas'] = MangasView;
views['mangadetails'] = MangaDetailsView;
views['chapters'] = ChaptersView;
views['pages'] = PagesView;
views['apikeys'] = ApiKeysView;

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    
    // Setup Navigation
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const view = e.currentTarget.getAttribute('data-view');
            window.location.hash = view;
        });
    });

    // Handle Hash Change
    window.addEventListener('hashchange', handleRoute);
    
    // Initial Route
    handleRoute();
});

function handleRoute() {
    let hash = window.location.hash.replace('#', '');
    if (!hash || !views[hash]) {
        hash = 'dashboard';
        window.location.hash = hash;
    }
    switchView(hash);
}
