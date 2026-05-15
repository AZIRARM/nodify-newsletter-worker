let allCampaigns = [];

async function loadCampaigns() {
    try {
        const response = await fetch('/api/campaigns');
        if (!response.ok) throw new Error('Failed to fetch campaigns');
        allCampaigns = await response.json();
        console.log('Campaigns loaded:', allCampaigns);
        applyFilters();
    } catch (error) {
        console.error('Error loading campaigns:', error);
        document.getElementById('campaignsGrid').innerHTML = '<div class="empty"><i class="fas fa-exclamation-triangle"></i> Error loading campaigns</div>';
    }
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
        grid.innerHTML = '<div class="empty"><i class="fas fa-inbox"></i> No campaigns found</div>';
        return;
    }

    grid.innerHTML = campaigns.map(c => {
        const campaignId = c.id;
        const startDate = c.startDate || '';
        const endDate = c.endDate || '';
        const scheduledStart = c.scheduledStart || '';
        const retryInterval = c.retryIntervalMinutes || 0;
        const active = c.active === true;

        return `
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
                <a href="/campaign/${campaignId}" class="btn-view">Details</a>
                <button class="btn-schedule" onclick="openScheduleModal(${campaignId}, '${startDate}', '${endDate}', '${scheduledStart}', ${retryInterval}, ${active})">Schedule</button>
                <button class="btn-delete" onclick="deleteCampaign(${campaignId})">Delete</button>
            </div>
        </div>
    `}).join('');
}

async function deleteCampaign(id) {
    if (!confirm('Delete this campaign?')) return;
    try {
        await fetch(`/api/campaigns/${id}`, { method: 'DELETE' });
        loadCampaigns();
    } catch (error) {
        console.error('Error deleting campaign:', error);
        alert('Error deleting campaign');
    }
}

async function openNewCampaignModal() {
    await loadNewsletters();
    document.getElementById('campaignName').value = '';
    document.getElementById('newCampaignStartDate').value = '';
    document.getElementById('newCampaignEndDate').value = '';
    document.getElementById('newCampaignScheduledStart').value = '';
    document.getElementById('newCampaignModal').style.display = 'flex';
}

function closeNewCampaignModal() {
    document.getElementById('newCampaignModal').style.display = 'none';
}

async function loadNewsletters() {
    const response = await fetch('/api/newsletters');
    const newsletters = await response.json();
    const select = document.getElementById('newsletterId');
    select.innerHTML = '<option value="">-- Select Newsletter --</option>' +
        newsletters.map(n => `<option value="${n.id}">${n.title || n.newsletterCode}</option>`).join('');
}

async function createCampaign() {
    const data = {
        name: document.getElementById('campaignName').value,
        newsletterId: document.getElementById('newsletterId').value,
        startDate: document.getElementById('newCampaignStartDate').value,
        endDate: document.getElementById('newCampaignEndDate').value,
        scheduledStart: document.getElementById('newCampaignScheduledStart').value
    };

    if (!data.name || !data.newsletterId) {
        alert('Please fill required fields');
        return;
    }

    try {
        await fetch('/api/campaigns', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        closeNewCampaignModal();
        loadCampaigns();
    } catch (error) {
        console.error('Error creating campaign:', error);
        alert('Error creating campaign');
    }
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Initialisation
document.addEventListener('DOMContentLoaded', function () {
    loadCampaigns();

    document.getElementById('searchInput').addEventListener('keyup', applyFilters);
    document.getElementById('statusFilter').addEventListener('change', applyFilters);
});