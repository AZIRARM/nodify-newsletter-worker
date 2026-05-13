const API_BASE = window.location.origin;

async function loadData() {
    await Promise.all([
        loadStats(),
        loadCampaigns()
    ]);
}

async function loadStats() {
    try {
        const res = await fetch(`${API_BASE}/api/stats`);
        const stats = await res.json();

        document.getElementById('totalUsers').textContent = stats.userCount || 0;
        document.getElementById('totalCampaigns').textContent = stats.campaignCount || 0;
        document.getElementById('totalSent').textContent = stats.sentCount || 0;

        const openRate = stats.totalSent > 0 ? Math.round((stats.openedCount / stats.totalSent) * 100) : 0;
        document.getElementById('totalOpened').textContent = `${openRate}%`;
    } catch (error) {
        console.error('Error loading stats:', error);
    }
}

async function loadCampaigns() {
    const grid = document.getElementById('campaignsGrid');
    grid.innerHTML = '<div class="loading"><i class="fas fa-spinner fa-spin"></i> Loading campaigns...</div>';

    try {
        const res = await fetch(`${API_BASE}/api/campaigns`);
        const campaigns = await res.json();

        if (!campaigns || campaigns.length === 0) {
            grid.innerHTML = '<div class="empty"><i class="fas fa-inbox"></i> No campaigns yet. Send a webhook to create one.</div>';
            return;
        }

        grid.innerHTML = campaigns.map(campaign => `
            <div class="campaign-card">
                <div class="campaign-header">
                    <span class="campaign-name">📧 ${escapeHtml(campaign.name)}</span>
                    <span class="status status-${campaign.status.toLowerCase()}">${campaign.status}</span>
                </div>
                <div class="campaign-stats">
                    <div class="stat-item">
                        <div class="stat-value">${campaign.sentCount || 0}</div>
                        <div class="stat-label">Sent</div>
                    </div>
                    <div class="stat-item">
                        <div class="stat-value">${campaign.openedCount || 0}</div>
                        <div class="stat-label">Opened</div>
                    </div>
                    <div class="stat-item">
                        <div class="stat-value">${campaign.openRate || 0}%</div>
                        <div class="stat-label">Open Rate</div>
                    </div>
                </div>
                <div class="campaign-actions">
                    <a href="/campaign/${campaign.id}" class="btn-view">📊 Details</a>
                    <button class="btn-delete" onclick="deleteCampaign(${campaign.id})">🗑 Delete</button>
                </div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error loading campaigns:', error);
        grid.innerHTML = '<div class="empty">Error loading campaigns</div>';
    }
}

async function deleteCampaign(id) {
    if (confirm('Delete this campaign? This action cannot be undone.')) {
        try {
            await fetch(`${API_BASE}/api/campaigns/${id}`, { method: 'DELETE' });
            loadCampaigns();
            loadStats();
        } catch (error) {
            console.error('Error deleting campaign:', error);
            alert('Error deleting campaign');
        }
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

document.addEventListener('DOMContentLoaded', loadData);