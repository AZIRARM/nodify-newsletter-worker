const campaignId = window.location.pathname.split('/')[2];
let currentPage = 1;
const pageSize = 20;
let totalUsers = 0;

console.log('Campaign ID:', campaignId);

async function loadCampaignStats() {
    const response = await fetch(`/api/campaigns/${campaignId}/stats`);
    const stats = await response.json();
    document.getElementById('totalSent').textContent = stats.sentCount || 0;
    document.getElementById('totalOpened').textContent = stats.openedCount || 0;
    const openRate = stats.sentCount > 0 ? Math.round((stats.openedCount / stats.sentCount) * 100) : 0;
    document.getElementById('openRate').textContent = openRate + '%';
}

async function loadUsers() {
    const response = await fetch(`/api/campaigns/${campaignId}/users?page=${currentPage}&size=${pageSize}`);
    const data = await response.json();
    totalUsers = data.total;
    renderUsers(data.users);
    renderPagination();
}

function renderUsers(users) {
    const tbody = document.getElementById('usersTableBody');
    if (!users || users.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="empty">No users found</td></tr>';
        return;
    }
    tbody.innerHTML = '';
    users.forEach(user => {
        const row = tbody.insertRow();
        row.insertCell(0).innerHTML = escapeHtml(user.email);
        row.insertCell(1).innerHTML = escapeHtml((user.firstName || '') + ' ' + (user.lastName || ''));
        row.insertCell(2).innerHTML = user.sentAt ? new Date(user.sentAt).toLocaleString() : '-';
        row.insertCell(3).innerHTML = user.openedAt ? new Date(user.openedAt).toLocaleString() : '-';
        row.insertCell(4).innerHTML = user.opened ? '<span class="status-open">✓ Opened</span>' : '<span class="status-not-open">✗ Not opened</span>';
    });
}

function renderPagination() {
    const totalPages = Math.ceil(totalUsers / pageSize);
    const container = document.getElementById('pagination');
    if (totalPages <= 1) {
        container.innerHTML = '';
        return;
    }
    let html = '';
    for (let i = 1; i <= totalPages; i++) {
        html += `<button class="page-btn ${i === currentPage ? 'active' : ''}" onclick="goToPage(${i})">${i}</button>`;
    }
    container.innerHTML = html;
}

function goToPage(page) {
    currentPage = page;
    loadUsers();
}

function retryCampaign() {
    if (!confirm('Retry sending to users who have not received the email?')) return;
    fetch(`/api/campaigns/${campaignId}/retry`, { method: 'POST' })
        .then(response => {
            if (response.ok) {
                alert('Retry started');
                loadCampaignStats();
                loadUsers();
            } else {
                alert('Error starting retry');
            }
        })
        .catch(error => console.error('Error:', error));
}

function formatDate(date) {
    return date ? new Date(date).toLocaleString() : '-';
}

function escapeHtml(text) {
    if (!text) return '-';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

async function loadCampaignData() {
    const response = await fetch(`/api/campaigns/${campaignId}`);
    const data = await response.json();

    window.campaignData = {
        id: data.id,
        startDate: data.startDate || '',
        endDate: data.endDate || '',
        scheduledStart: data.scheduledStart || '',
        retryInterval: data.retryIntervalMinutes || 0,
        active: data.active
    };
}

function openScheduleModalFromDetail() {
    if (window.campaignData) {
        openScheduleModal(
            window.campaignData.id,
            window.campaignData.startDate,
            window.campaignData.endDate,
            window.campaignData.scheduledStart,
            window.campaignData.retryInterval,
            window.campaignData.active
        );
    }
}

loadCampaignStats();
loadUsers();
loadCampaignData();