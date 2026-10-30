const API_BASE = window.location.origin;

let allCampaigns = [];

async function loadData() {
    await Promise.all([
        loadNewslettersForSelect(),
        loadCampaigns()
    ]);
}

async function loadNewslettersForSelect() {
    const select = document.getElementById('newsletterId');
    if (!select) return;
    try {
        const res = await fetch(`${API_BASE}/api/newsletters/list`);
        const newsletters = await res.json();
        select.innerHTML = `<option value="">${t('campaigns.newsletter_placeholder','-- Select Newsletter --')}</option>`;
        newsletters.forEach(n => {
            const opt = document.createElement('option');
            opt.value = n.id;
            opt.textContent = `${n.title} (${n.code})`;
            select.appendChild(opt);
        });
    } catch (error) {
        console.error('Error loading newsletters:', error);
    }
}

async function loadCampaigns() {
    const grid = document.getElementById('campaignsGrid');
    grid.innerHTML = `<div class="loading"><i class="fas fa-spinner fa-spin"></i> ${t('common.loading','Loading...')}</div>`;
    try {
        const res = await fetch(`${API_BASE}/api/campaigns/list`);
        allCampaigns = await res.json();
        renderCampaigns();
    } catch (error) {
        console.error('Error loading campaigns:', error);
        grid.innerHTML = `<div class="empty"><i class="fas fa-exclamation-triangle"></i> ${t('common.error','Error loading campaigns')}</div>`;
    }
}

function applyFilters() {
    renderCampaigns();
}

function renderCampaigns() {
    const grid = document.getElementById('campaignsGrid');
    const now = new Date();

    const statusFilter = document.getElementById('statusFilter')?.value || 'ALL';
    const campaignStatusFilter = document.getElementById('campaignStatusFilter')?.value || 'ALL';
    const searchTerm = (document.getElementById('searchInput')?.value || '').toLowerCase();

    let filtered = allCampaigns.filter(c => {
        // filtre expiration
        if (c.endDate && new Date(c.endDate) < now) return false;
        // filtre statut technique
        if (statusFilter !== 'ALL' && c.status !== statusFilter) return false;
        // filtre statut campagne calculé
        if (campaignStatusFilter !== 'ALL' && c.campaignStatus !== campaignStatusFilter) return false;
        // filtre nom
        if (searchTerm && !c.name.toLowerCase().includes(searchTerm)) return false;
        return true;
    });

    filtered.sort((a, b) => {
        const aStart = a.startDate ? new Date(a.startDate) : null;
        const bStart = b.startDate ? new Date(b.startDate) : null;
        const aIsActive = !aStart || aStart <= now;
        const bIsActive = !bStart || bStart <= now;
        if (aIsActive && !bIsActive) return -1;
        if (!aIsActive && bIsActive) return 1;
        return 0;
    });

    if (filtered.length === 0) {
        grid.innerHTML = `<div class="empty"><i class="fas fa-inbox"></i> ${t('dashboard.empty_campaigns','No campaigns yet')}</div>`;
        return;
    }

    grid.innerHTML = filtered.map(c => {
        const newsletterLink = c.newsletterId ? `/api/campaigns/newsletter/${c.newsletterId}` : '#';

        let scheduleInfo = '';
        if (c.scheduledStart) {
            scheduleInfo = `<div class="campaign-schedule">📅 ${t('dashboard.scheduled','Scheduled')}: ${new Date(c.scheduledStart).toLocaleString()}</div>`;
        }
        if (c.startDate) {
            scheduleInfo += `<div class="campaign-start">🚀 ${t('dashboard.start','Start')}: ${new Date(c.startDate).toLocaleString()}</div>`;
        }
        if (c.endDate) {
            scheduleInfo += `<div class="campaign-end">🏁 ${t('dashboard.end','End')}: ${new Date(c.endDate).toLocaleString()}</div>`;
        }
        if (c.campaignStatus === 'scheduled') {
            scheduleInfo = `<div class="campaign-waiting">⏰ ${t('campaigns.not_started','Not started yet')}</div>` + scheduleInfo;
        }

        return `
        <div class="campaign-card">
            <div class="campaign-header">
                <span class="campaign-name">📧 ${escapeHtml(c.name)}</span>
                <span class="status status-${c.status.toLowerCase()}">${t('status.' + c.status.toLowerCase(), c.status)}</span>
            </div>
            ${scheduleInfo}
            <div class="campaign-stats">
                <div class="stat-item"><div class="stat-value">${c.sentCount || 0}</div><div class="stat-label">${t('campaign.sent','Sent')}</div></div>
                <div class="stat-item"><div class="stat-value">${c.openedCount || 0}</div><div class="stat-label">${t('campaign.opened','Opened')}</div></div>
                <div class="stat-item"><div class="stat-value">${c.openRate || 0}%</div><div class="stat-label">${t('campaign.rate','Rate')}</div></div>
            </div>
            <div class="campaign-actions">
                <a href="/api/campaigns/${c.id}/detail" class="btn-view">📊 ${t('common.details','Details')}</a>
                <a href="${newsletterLink}" class="btn-newsletter" target="_blank">📧 ${t('campaign.view_newsletter','View Newsletter')}</a>
                <button class="btn-delete" onclick="deleteCampaign(${c.id})">🗑 ${t('common.delete','Delete')}</button>
            </div>
        </div>
    `}).join('');
}

async function deleteCampaign(id) {
    if (confirm(t('newsletters.delete_confirm','Delete this campaign?'))) {
        const response = await fetch(`/api/campaigns/${id}`, { method: 'DELETE' });
        if (response.ok) {
            loadCampaigns();
        } else {
            alert(t('common.error','Error deleting campaign'));
        }
    }
}

function openNewCampaignModal() {
    document.getElementById('campaignName').value = '';
    document.getElementById('newsletterId').value = '';
    document.getElementById('newCampaignStartDate').value = '';
    document.getElementById('newCampaignEndDate').value = '';
    document.getElementById('newCampaignScheduledStart').value = '';
    document.getElementById('newCampaignModal').style.display = 'flex';
}

function closeNewCampaignModal() {
    document.getElementById('newCampaignModal').style.display = 'none';
}

async function createCampaign() {
    const name = document.getElementById('campaignName').value.trim();
    const newsletterId = document.getElementById('newsletterId').value;
    const startDate = document.getElementById('newCampaignStartDate').value;
    const endDate = document.getElementById('newCampaignEndDate').value;
    const scheduledStart = document.getElementById('newCampaignScheduledStart').value;

    if (!name || !newsletterId) {
        alert(t('campaigns.create_error','Please fill required fields'));
        return;
    }

    const payload = {
        name,
        newsletterId,
        startDate: startDate || null,
        endDate: endDate || null,
        scheduledStart: scheduledStart || null
    };

    try {
        const res = await fetch(`${API_BASE}/api/campaigns`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (res.ok) {
            closeNewCampaignModal();
            loadCampaigns();
        } else {
            alert(t('common.error','Error creating campaign'));
        }
    } catch (error) {
        console.error('Error:', error);
        alert(t('common.error','Error creating campaign'));
    }
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

document.addEventListener('DOMContentLoaded', loadData);