const campaignId = parseInt(document.getElementById('campaignId').value);
let currentPage = 1;
const pageSize = 20;
let totalUsers = 0;

console.log('Campaign ID:', campaignId);

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
        document.getElementById('currentUsersBody').innerHTML = '<tr><td colspan="6" class="error">Error loading users</td></tr>';
    }
}

function renderCurrentUsers(users) {
    const tbody = document.getElementById('currentUsersBody');
    if (!users || users.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="empty">No users in this campaign</td></tr>';
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
        row.insertCell(4).innerHTML = u.opened ? '✓ Opened' : (u.sentAt ? 'Sent' : 'Pending');
        const actionCell = row.insertCell(5);
        const removeBtn = document.createElement('button');
        removeBtn.className = 'remove-btn';
        removeBtn.innerHTML = 'Remove';
        removeBtn.onclick = function () { removeUserFromCampaign(u.id); };
        actionCell.appendChild(removeBtn);
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

async function removeUserFromCampaign(userId) {
    if (!confirm('Remove this user from the campaign?')) return;
    try {
        await fetch(`/api/campaigns/${campaignId}/remove-user`, {
            method: 'DELETE',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId: userId })
        });
        loadCurrentUsers();
        loadAvailableUsers();
    } catch (error) {
        console.error('Error:', error);
    }
}

async function loadAvailableUsers() {
    try {
        const response = await fetch(`/api/campaigns/${campaignId}/available-users`);
        const users = await response.json();
        renderAvailableUsers(users);
    } catch (error) {
        console.error('Error:', error);
    }
}

function renderAvailableUsers(users) {
    const tbody = document.getElementById('availableUsersBody');
    if (!users || users.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="empty">No available users</td></tr>';
        return;
    }
    tbody.innerHTML = '';
    users.forEach(u => {
        const row = tbody.insertRow();
        row.insertCell(0).innerHTML = escapeHtml(u.email);
        row.insertCell(1).innerHTML = escapeHtml((u.firstName || '') + ' ' + (u.lastName || ''));
        row.insertCell(2).innerHTML = escapeHtml(u.phone || '');
        row.insertCell(3).innerHTML = escapeHtml(u.address || '');
        const actionCell = row.insertCell(4);
        const addBtn = document.createElement('button');
        addBtn.className = 'add-btn';
        addBtn.innerHTML = 'Add';
        addBtn.onclick = function () { addUserToCampaign(u.id); };
        actionCell.appendChild(addBtn);
    });
}

async function addUserToCampaign(userId) {
    try {
        await fetch(`/api/campaigns/${campaignId}/add-user`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId: userId })
        });
        loadCurrentUsers();
        loadAvailableUsers();
    } catch (error) {
        console.error('Error:', error);
    }
}

async function createAndAddUser() {
    const data = {
        email: document.getElementById('newEmail').value,
        firstName: document.getElementById('newFirstName').value,
        lastName: document.getElementById('newLastName').value,
        phone: document.getElementById('newPhone').value,
        address: document.getElementById('newAddress').value
    };
    if (!data.email) {
        alert('Email is required');
        return;
    }
    try {
        const res = await fetch('/api/users', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        const result = await res.json();
        await addUserToCampaign(result.id);
        document.getElementById('newEmail').value = '';
        document.getElementById('newFirstName').value = '';
        document.getElementById('newLastName').value = '';
        document.getElementById('newPhone').value = '';
        document.getElementById('newAddress').value = '';
        loadAvailableUsers();
    } catch (error) {
        console.error('Error:', error);
    }
}

async function importUsers() {
    const jsonText = document.getElementById('jsonImport').value;
    try {
        const users = JSON.parse(jsonText);
        await fetch('/api/users/import', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(users)
        });
        loadAvailableUsers();
        alert('Users imported successfully');
        document.getElementById('jsonImport').value = '';
    } catch (e) {
        alert('Invalid JSON format');
    }
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
    } else if (tabName === 'available') {
        document.getElementById('availablePanel').classList.add('active');
        tabs[1].classList.add('active');
        loadAvailableUsers();
    } else if (tabName === 'add') {
        document.getElementById('addPanel').classList.add('active');
        tabs[2].classList.add('active');
    } else if (tabName === 'import') {
        document.getElementById('importPanel').classList.add('active');
        tabs[3].classList.add('active');
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

// Initialisation
document.addEventListener('DOMContentLoaded', function () {
    console.log('DOM loaded, campaignId:', campaignId);
    loadCurrentUsers();
});