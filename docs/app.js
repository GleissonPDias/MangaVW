const API_BASE_URL = 'https://mangavw.onrender.com';
let apiKey = localStorage.getItem('manga_api_key') || '';

// DOM Elements
const grid = document.getElementById('mangaGrid');
const inputApiKey = document.getElementById('apiKey');
const btnConfigure = document.getElementById('btnConfigure');
const btnSync = document.getElementById('btnSync');
const errorPanel = document.getElementById('errorPanel');
const errorMsg = document.getElementById('errorMsg');

// Initialize
if (apiKey) inputApiKey.value = apiKey;

// Event Listeners
btnConfigure.addEventListener('click', () => {
    apiKey = inputApiKey.value.trim();
    localStorage.setItem('manga_api_key', apiKey);
    
    // Add visual feedback
    const originalText = btnConfigure.innerText;
    btnConfigure.innerText = 'Saved!';
    btnConfigure.style.background = '#10b981'; // Green
    
    setTimeout(() => {
        btnConfigure.innerText = originalText;
        btnConfigure.style.background = ''; // Back to default
    }, 2000);

    fetchMangas(); // Refresh with new key if needed
});

btnSync.addEventListener('click', async () => {
    btnSync.innerText = "Syncing...";
    btnSync.disabled = true;
    
    try {
        const headers = {};
        if (apiKey) headers['X-API-Key'] = apiKey;
        
        // Simulating idempotency key required by the backend
        headers['Idempotency-Key'] = crypto.randomUUID();

        // Fazemos uma chamada ao nosso backend rodando localmente
        const response = await fetch(`${API_BASE_URL}/mangas/sync`, {
            method: 'POST',
            headers: headers
        });

        if (!response.ok) {
            throw new Error(`HTTP Error: ${response.status}`);
        }
        
        alert("Sync Completed Successfully!");
        fetchMangas();
    } catch (err) {
        showError(`Failed to POST /mangas/sync.\n\nDetalhes do Erro: ${err.message}\n\nSe o seu Spring Boot estiver rodando, este erro aconteceu porque a API bloqueou nossa requisição (CORS Blocked). Note que requisições POST disparam um PREFLIGHT OPTIONS que o backend rejeitou!`);
    } finally {
        btnSync.innerText = "Sync from MangaDex (POST)";
        btnSync.disabled = false;
    }
});

function showError(msg) {
    errorPanel.classList.remove('hidden');
    errorMsg.innerText = msg;
    grid.innerHTML = ''; // clear skeletons
}

function hideError() {
    errorPanel.classList.add('hidden');
}

async function fetchMangas() {
    hideError();
    grid.innerHTML = `
        <div class="glass-card skeleton"><div class="skeleton-img"></div><div class="skeleton-text"></div></div>
        <div class="glass-card skeleton"><div class="skeleton-img"></div><div class="skeleton-text"></div></div>
        <div class="glass-card skeleton"><div class="skeleton-img"></div><div class="skeleton-text"></div></div>
        <div class="glass-card skeleton"><div class="skeleton-img"></div><div class="skeleton-text"></div></div>
    `;

    try {
        const headers = {
            'X-API-Version': '2'
        };

        const response = await fetch(`${API_BASE_URL}/mangas`, {
            method: 'GET',
            headers: headers
        });

        if (!response.ok) {
            throw new Error(`HTTP Error: ${response.status}`);
        }

        const data = await response.json();
        renderMangas(data._embedded ? data._embedded.mangaList : []);
    } catch (err) {
        showError(`Failed to fetch mangas.\n\nDetalhes do Erro: ${err.message}\n\nIsto é o bloqueio do CORS em ação! O navegador (Frontend) não confia na origem (Backend porta 8080) porque o Backend ainda não configurou as políticas de permissão explícitas!`);
    }
}

function renderMangas(mangas) {
    grid.innerHTML = '';
    
    if (mangas.length === 0) {
        grid.innerHTML = `<p style="grid-column: 1/-1; text-align: center; color: var(--text-secondary)">No mangas found in database. Try syncing!</p>`;
        return;
    }

    mangas.forEach(manga => {
        const card = document.createElement('div');
        card.className = 'glass-card';
        
        const synopsis = manga.sinopsis ? (manga.sinopsis.length > 150 ? manga.sinopsis.substring(0, 150) + '...' : manga.sinopsis) : 'No synopsis available.';
        
        // 1. Extraindo a Capa (salva como a primeira página do primeiro capítulo)
        let coverUrl = 'https://via.placeholder.com/250x350/1e293b/94a3b8?text=Sem+Capa';
        if (manga.chapters && manga.chapters.length > 0 && 
            manga.chapters[0].pages && manga.chapters[0].pages.length > 0) {
            coverUrl = manga.chapters[0].pages[0].imageUrl;
        }

        // 2. Extraindo o Nome do Autor
        const authorName = manga.author ? manga.author.name : 'Autor Desconhecido';

        // 3. Contagem de Capítulos
        const chaptersCount = manga.chapters ? manga.chapters.length : 0;

        // 4. Extraindo Gêneros (limite de 2)
        let genresHtml = '';
        if (manga.genres && manga.genres.length > 0) {
            manga.genres.slice(0, 2).forEach(g => {
                genresHtml += `<span class="manga-tag">${g.name}</span>`;
            });
        }
        
        card.innerHTML = `
            <img src="${coverUrl}" alt="Capa de ${manga.title}" class="manga-cover" referrerpolicy="no-referrer" onerror="this.src='https://via.placeholder.com/250x350/1e293b/94a3b8?text=Sem+Capa'">
            <h3>${manga.title}</h3>
            <div class="manga-author">Por ${authorName}</div>
            
            <div style="display:flex; justify-content:space-between; align-items:center;">
                <span class="status-badge">${manga.status}</span>
                <span style="font-size: 0.85rem; color: var(--text-secondary); font-weight: 700;">${chaptersCount} Capítulos</span>
            </div>
            
            <div class="manga-tags">${genresHtml}</div>
            
            <p style="flex-grow: 1;">${synopsis}</p>
        `;
        grid.appendChild(card);
    });
}

// Initial load
fetchMangas();
