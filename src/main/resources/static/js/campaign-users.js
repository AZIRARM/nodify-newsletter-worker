const campaignId = parseInt(document.getElementById('campaignId').value);
let currentPage = 1;
const pageSize = 20;
let totalUsers = 0;

// Fallback i18n : si une fonction globale t() existe (injectée par Thymeleaf), on l'utilise,
// sinon on retourne la valeur par défaut.
window.t = window.t || function (key, fallback) {
    if (window.i18nMessages && window.i18nMessages[key]) {
        return window.i18nMessages[key];
    }
    return fallback !== undefined ? fallback : key;
};

async function loadCurrentUsers() {
    try {
        const response = await fetch(`/api/campaigns/${campaignId}/users?page=${currentPage}&size=${pageSize}`);
        if (!response.ok) throw new Error('API error');
        const data = await response.json();
        totalUsers = data.total;
        renderCurrentUsers(data.users);
        renderPagination();
    } catch (error) {
        console.error('Error:', error);
        document.getElementById('currentUsersBody').innerHTML =
            `<tr><td colspan="6" class="error">${t('common.error','Error loading users')}</td></tr>`;
    }
}

function renderCurrentUsers(users) {
    const tbody = document.getElementById('currentUsersBody');
    if (!users || users.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" class="empty">${t('campaign.no_current','No users in this campaign')}</td></tr>`;
        return;
    }
    tbody.innerHTML = '';
    users.forEach(u => {
        const row = tbody.insertRow();
        row.insertCell(0).innerHTML = escapeHtml(u.email);
        row.insertCell(1).innerHTML = escapeHtml((u.firstName || '') + ' ' + (u.lastName || ''));
        row.insertCell(2).innerHTML = escapeHtml(u.phone || '');
        row.insertCell(3).innerHTML = escapeHtml(u.address || '');
        row.insertCell(4).className = u.opened ? 'status-open' : 'status-not-open';
        row.insertCell(4).innerHTML = u.opened
            ? `✓ ${t('campaign.status_open','Opened')}`
            : (u.sentAt ? t('campaign.sent','Sent') : t('status.pending','Pending'));
        row.insertCell(5).innerHTML = '-';
    });
}

function renderPagination() {
    const totalPages = Math.ceil(totalUsers / pageSize);
    const container = document.getElementById('currentPagination');
    if (!container) return;
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
    loadCurrentUsers();
}

function showTab(tabName) {
    const panels = document.querySelectorAll('.panel');
    const tabs = document.querySelectorAll('.tab');
    panels.forEach(panel => panel.classList.remove('active'));
    tabs.forEach(tab => tab.classList.remove('active'));

    if (tabName === 'current') {
        document.getElementById('currentPanel').classList.add('active');
        tabs[0].classList.add('active');
        loadCurrentUsers();
    } else {
        // Onglets non supportés par le backend (available/add/import)
        const panel = document.getElementById(tabName + 'Panel');
        if (panel) {
            panel.classList.add('active');
            const tabIndex = ['current','available','add','import'].indexOf(tabName);
            if (tabIndex >= 0 && tabs[tabIndex]) tabs[tabIndex].classList.add('active');
        }
    }
}

function escapeHtml(text) {
    if (!text) return '-';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function closeModal() {
    document.getElementById('userModal').style.display = 'none';
}

document.addEventListener('DOMContentLoaded', function () {
    loadCurrentUsers();
});