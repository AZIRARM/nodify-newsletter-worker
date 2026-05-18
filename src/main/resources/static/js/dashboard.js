const API_BASE = window.location.origin;

async function loadData() {
    await Promise.all([
        loadStats(),
        loadCampaigns(),
        loadRecentUsers()
    ]);
}

async function loadStats() {
    try {
        const res = await fetch(`${API_BASE}/api/stats`);
        const stats = await res.json();
        document.getElementById('totalUsers').textContent = stats.userCount || 0;
        document.getElementById('totalCampaigns').textContent = stats.campaignCount || 0;
        document.getElementById('totalSent').textContent = stats.sentCount || 0;
        const openRate = stats.sentCount > 0 ? Math.round((stats.openedCount / stats.sentCount) * 100) : 0;
        document.getElementById('totalOpened').textContent = `${openRate}%`;
    } catch (error) {
        console.error('Error loading stats:', error);
    }
}

async function loadCampaigns() {
    const grid = document.getElementById('campaignsGrid');
    grid.innerHTML = '<div class="loading"><i class="fas fa-spinner fa-spin"></i> Loading...</div>';
    try {
        const res = await fetch(`${API_BASE}/api/campaigns`);
        const campaigns = await res.json();

        const now = new Date();

        const filteredCampaigns = campaigns.filter(c => {
            if (c.endDate && new Date(c.endDate) < now) return false;
            return true;
        });

        filteredCampaigns.sort((a, b) => {
            const aStart = a.startDate ? new Date(a.startDate) : null;
            const bStart = b.startDate ? new Date(b.startDate) : null;
            const aIsActive = !aStart || aStart <= now;
            const bIsActive = !bStart || bStart <= now;
            if (aIsActive && !bIsActive) return -1;
            if (!aIsActive && bIsActive) return 1;
            return 0;
        });

        if (!filteredCampaigns || filteredCampaigns.length === 0) {
            grid.innerHTML = '<div class="empty"><i class="fas fa-inbox"></i> No campaigns yet</div>';
            return;
        }

        grid.innerHTML = filteredCampaigns.map(c => {
            const newsletterLink = c.newsletterId ? `/newsletter/${c.newsletterId}` : '#';
            const newsletterTitle = c.newsletterTitle || 'Newsletter';

            let scheduleInfo = '';
            if (c.scheduledStart) {
                scheduleInfo = `<div class="campaign-schedule">📅 Scheduled: ${new Date(c.scheduledStart).toLocaleString()}</div>`;
            }
            if (c.startDate) {
                scheduleInfo += `<div class="campaign-start">🚀 Start: ${new Date(c.startDate).toLocaleString()}</div>`;
            }
            if (c.endDate) {
                scheduleInfo += `<div class="campaign-end">🏁 End: ${new Date(c.endDate).toLocaleString()}</div>`;
            }
            if (c.campaignStatus === 'scheduled') {
                scheduleInfo = `<div class="campaign-waiting">⏰ Not started yet</div>` + scheduleInfo;
            }

            return `
            <div class="campaign-card">
                <div class="campaign-header">
                    <span class="campaign-name">📧 ${escapeHtml(c.name)}</span>
                    <span class="status status-${c.status.toLowerCase()}">${c.status}</span>
                </div>
                ${scheduleInfo}
                <div class="campaign-stats">
                    <div class="stat-item"><div class="stat-value">${c.sentCount || 0}</div><div class="stat-label">Sent</div></div>
                    <div class="stat-item"><div class="stat-value">${c.openedCount || 0}</div><div class="stat-label">Opened</div></div>
                    <div class="stat-item"><div class="stat-value">${c.openRate || 0}%</div><div class="stat-label">Rate</div></div>
                </div>
                <div class="campaign-actions">
                    <a href="/campaign/${c.id}" class="btn-view">📊 Details</a>
                    <a href="${newsletterLink}" class="btn-newsletter" target="_blank">📧 View Newsletter</a>
                    <button class="btn-delete" onclick="deleteCampaign(${c.id})">🗑 Delete</button>
                </div>
            </div>
        `}).join('');
    } catch (error) {
        console.error('Error loading campaigns:', error);
        grid.innerHTML = '<div class="empty"><i class="fas fa-exclamation-triangle"></i> Error loading campaigns</div>';
    }
}

async function loadRecentUsers() {
    const container = document.getElementById('usersGrid');
    if (!container) return;
    container.innerHTML = '<div class="loading"><i class="fas fa-spinner fa-spin"></i> Loading...</div>';
    try {
        const res = await fetch(`${API_BASE}/api/users/recent`);
        const users = await res.json();
        if (!users || users.length === 0) {
            container.innerHTML = '<div class="empty"><i class="fas fa-inbox"></i> No users yet</div>';
            return;
        }
        container.innerHTML = users.map(u => `
            <div class="user-card">
                <div class="user-avatar"><i class="fas fa-user-circle"></i></div>
                <div class="user-info">
                    <div class="user-name">${escapeHtml(u.firstName || '')} ${escapeHtml(u.lastName || '')}</div>
                    <div class="user-email">${escapeHtml(u.email)}</div>
                </div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error loading users:', error);
        container.innerHTML = '<div class="empty">Error loading users</div>';
    }
}

async function deleteCampaign(id) {
    if (confirm('Delete this campaign?')) {
        const response = await fetch(`/api/campaigns/${id}`, { method: 'DELETE' });
        if (response.ok) {
            loadCampaigns();
            loadStats();
        } else {
            alert('Error deleting campaign');
        }
    }
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

document.addEventListener('DOMContentLoaded', loadData);