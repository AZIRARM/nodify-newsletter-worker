let allCampaigns = [];

async function loadCampaigns() {
    const response = await fetch('/api/campaigns');
    allCampaigns = await response.json();
    applyFilters();
}

function applyFilters() {
    const status = document.getElementById('statusFilter').value;
    const search = document.getElementById('searchInput').value.toLowerCase();
    let filtered = allCampaigns;
    if (status !== 'ALL') filtered = filtered.filter(c => c.status === status);
    if (search) filtered = filtered.filter(c => c.name.toLowerCase().includes(search));
    renderCampaigns(filtered);
}

function renderCampaigns(campaigns) {
    const grid = document.getElementById('campaignsGrid');
    if (!campaigns || campaigns.length === 0) {
        grid.innerHTML = '<div class="empty"><i class="fas fa-inbox"></i> <span th:text="#{campaigns.no_campaigns}">No campaigns found</span></div>';
        return;
    }
    grid.innerHTML = campaigns.map(c => `
        <div class="campaign-card">
            <div class="campaign-header">
                <span class="campaign-name">📧 ${escapeHtml(c.name)}</span>
                <span class="status status-${c.status.toLowerCase()}">${c.status}</span>
            </div>
            <div class="campaign-stats">
                <div class="stat-item"><div class="stat-value">${c.sentCount || 0}</div><div class="stat-label">Sent</div></div>
                <div class="stat-item"><div class="stat-value">${c.openedCount || 0}</div><div class="stat-label">Opened</div></div>
                <div class="stat-item"><div class="stat-value">${c.openRate || 0}%</div><div class="stat-label">Rate</div></div>
            </div>
            <div class="campaign-actions">
                <a href="/campaign/${c.id}" class="btn-view">📊 Details</a>
                <a href="/campaign/${c.id}/schedule" class="btn-view">⏰ Schedule</a>
                <button class="btn-delete" onclick="deleteCampaign(${c.id})">🗑 Delete</button>
            </div>
        </div>
    `).join('');
}

async function deleteCampaign(id) {
    if (confirm('Delete this campaign?')) {
        await fetch(`/api/campaigns/${id}`, { method: 'DELETE' });
        loadCampaigns();
    }
}

async function loadNewsletters() {
    const response = await fetch('/api/newsletters');
    const newsletters = await response.json();
    const select = document.getElementById('newsletterId');
    select.innerHTML = '<option value="">-- Select Newsletter --</option>' +
        newsletters.map(n => `<option value="${n.id}">${n.title}</option>`).join('');
}

async function createCampaign() {
    const data = {
        name: document.getElementById('campaignName').value,
        folder: document.getElementById('campaignFolder').value,
        newsletterId: document.getElementById('newsletterId').value,
        scheduledStart: document.getElementById('scheduledStart').value
    };
    if (!data.name || !data.folder || !data.newsletterId) {
        alert('Please fill all fields');
        return;
    }
    await fetch('/api/campaigns', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    });
    closeNewCampaignModal();
    loadCampaigns();
}

function openNewCampaignModal() {
    document.getElementById('newCampaignModal').style.display = 'flex';
    loadNewsletters();
}

function closeNewCampaignModal() {
    document.getElementById('newCampaignModal').style.display = 'none';
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

loadCampaigns();