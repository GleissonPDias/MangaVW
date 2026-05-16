export const API_BASE_URL = 'https://mangavw.onrender.com';

export function updateApiStatus(isOnline) {
    const statusEl = document.getElementById('api-status');
    const pulseEl = document.getElementById('api-pulse');
    const textEl = document.getElementById('api-text');
    if (!statusEl) return;
    
    if (isOnline) {
        textEl.innerText = 'API Connected';
        textEl.style.color = '';
        pulseEl.style.background = 'var(--success)';
        pulseEl.style.boxShadow = '0 0 0 0 rgba(46, 213, 115, 0.7)';
        pulseEl.style.animation = 'pulse 2s infinite';
    } else {
        textEl.innerText = 'API Offline';
        textEl.style.color = 'var(--danger)';
        pulseEl.style.background = 'var(--danger)';
        pulseEl.style.boxShadow = 'none';
        pulseEl.style.animation = 'none';
    }
}

// Retorna a API Key salva no LocalStorage
export function getApiKey() {
    return localStorage.getItem('manga_api_key') || '';
}

export function setApiKey(key) {
    localStorage.setItem('manga_api_key', key);
}

// Retorna a versão da API salva (1 ou 2)
export function getApiVersion() {
    return localStorage.getItem('manga_api_version') || '2';
}

export function setApiVersion(version) {
    localStorage.setItem('manga_api_version', version);
}

// Wrapper para o fetch que já inclui X-API-Key e X-API-Version
export async function apiFetch(endpoint, options = {}) {
    const url = `${API_BASE_URL}${endpoint}`;
    
    const headers = {
        'Content-Type': 'application/json',
        'X-API-Version': getApiVersion(),
        ...options.headers
    };

    const apiKey = getApiKey();
    if (apiKey && !options.skipApiKey) {
        headers['X-API-Key'] = apiKey;
    }

    if (options.method && options.method !== 'GET') {
        // Envia idempotency key para POST, PUT, DELETE
        headers['Idempotency-Key'] = crypto.randomUUID();
    }

    const config = {
        ...options,
        headers
    };

    try {
        const response = await fetch(url, config);
        
        // Se conseguimos uma resposta do servidor (mesmo que seja erro 400/500), a API está online!
        updateApiStatus(true);

        // Se for 204 No Content, retorna true (sucesso, sem corpo)
        if (response.status === 204) return true;

        const data = await response.json().catch(() => ({}));

        if (!response.ok) {
            let errorMsg = `HTTP ${response.status}: ${data.message || data.error || 'Erro desconhecido'}`;
            
            if (response.status === 429) {
                const retryAfter = response.headers.get('Retry-After');
                if (retryAfter) {
                    errorMsg += ` (Aguarde ${retryAfter} segundos)`;
                }
            }
            
            throw new Error(errorMsg);
        }

        return data;
    } catch (err) {
        // Se o erro NÃO for um erro HTTP gerado manualmente por nós acima,
        // significa que o fetch falhou miseravelmente (ex: Connection Refused, servidor offline)
        if (!err.message.startsWith('HTTP ')) {
            updateApiStatus(false);
        }
        throw err;
    }
}
