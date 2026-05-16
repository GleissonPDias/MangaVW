import { getApiKey, setApiKey, getApiVersion, setApiVersion, apiFetch } from '../api.js';
import { showToast } from '../ui.js';

export const ApiKeysView = {
    render(container) {
        const currentKey = getApiKey();
        const currentVersion = getApiVersion();
        
        container.innerHTML = `
            <div class="action-toolbar">
                <h2>API Keys & Settings</h2>
            </div>
            
            <div class="glass-card" style="max-width: 600px; margin-bottom: 2rem;">
                <h3>Global API Settings</h3>
                <p style="margin-bottom: 1rem;">Set the key and version that this Dashboard will use to communicate with the Backend.</p>
                <div class="search-box" style="margin-bottom: 1rem;">
                    <input type="text" id="local-key" class="glass-input" value="${currentKey}" style="flex:1" placeholder="Enter X-API-Key">
                    <button class="btn btn-primary" id="btn-save-key">Save Key</button>
                    <button class="btn" style="background: rgba(255, 71, 87, 0.2); color: #ff4757; border: 1px solid rgba(255, 71, 87, 0.3);" id="btn-clear-key" title="Clear Key">
                        <i class="fa-solid fa-trash"></i> Clear
                    </button>
                </div>
                <div class="search-box">
                    <select id="local-version" class="glass-select" style="flex:1">
                        <option value="1" ${currentVersion === '1' ? 'selected' : ''}>Version 1 (Simplified)</option>
                        <option value="2" ${currentVersion === '2' ? 'selected' : ''}>Version 2 (Full)</option>
                    </select>
                    <button class="btn btn-primary" id="btn-save-version">Save Version</button>
                </div>
            </div>

            <div class="glass-card" style="max-width: 600px;">
                <h3>Generate New Key (Backend)</h3>
                <p style="margin-bottom: 1rem;">Create a new API Key in the database.</p>
                <div class="search-box">
                    <input type="text" id="new-key-client" class="glass-input" style="flex:1" placeholder="Client Name (e.g. MobileApp)">
                    <button class="btn btn-primary" id="btn-create-key">Generate Key</button>
                </div>
                <div id="new-key-result" style="margin-top: 1rem; color: var(--success); font-weight: bold; word-break: break-all;"></div>
            </div>
        `;

        document.getElementById('btn-save-key').addEventListener('click', () => {
            const val = document.getElementById('local-key').value.trim();
            setApiKey(val);
            showToast('API Key saved to LocalStorage!');
        });

        document.getElementById('btn-clear-key').addEventListener('click', () => {
            document.getElementById('local-key').value = '';
            setApiKey('');
            showToast('API Key cleared from LocalStorage!');
        });

        document.getElementById('btn-save-version').addEventListener('click', () => {
            const val = document.getElementById('local-version').value;
            setApiVersion(val);
            showToast('API Version updated! Changing version affects headers sent to backend.');
        });

        document.getElementById('btn-create-key').addEventListener('click', async () => {
            const clientName = document.getElementById('new-key-client').value.trim();
            if(!clientName) {
                showToast('Please enter a Client Name', 'error');
                return;
            }
            
            try {
                // skipApiKey: true evita enviar uma chave inválida que o usuário tenha salvo e que bloquearia esta requisição pública
                const res = await apiFetch(`/api-keys?clientName=${encodeURIComponent(clientName)}`, { 
                    method: 'POST',
                    skipApiKey: true 
                });
                document.getElementById('new-key-result').innerText = `Generated Key: ${res.key} (Copy this now!)`;
                showToast('API Key Generated on server!');
            } catch (err) {
                showToast(err.message, 'error');
            }
        });
    }
};
