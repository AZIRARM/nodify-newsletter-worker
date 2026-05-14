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
    document.getElementById('openRate').textContent = (stats.openRate || 0) + '%';
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
    tbody.innerHTML = users.map(user => `
        <tr>
            <td>${escapeHtml(user.email)}</td>
            <td>${escapeHtml((user.firstName || '') + ' ' + (user.lastName || ''))}</td>
            <td>${formatDate(user.sentAt)}</td>
            <td>${formatDate(user.openedAt)}</td>
            <td class="${user.opened ? 'status-open' : 'status-not-open'}">${user.opened ? '✓ Opened' : '✗ Not opened'}</td>
        </tr>
    `).join('');
}

function renderPagination() {
    const totalPages = Math.ceil(totalUsers / pageSize);
    const container = document.getElementById('pagination');
    if (totalPages <= 1) { container.innerHTML = ''; return; }
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
    if (!confirm('Retry sending to non-opened users?')) return;
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

loadCampaignStats();
loadUsers();