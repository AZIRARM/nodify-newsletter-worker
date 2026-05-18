const newsletterId = parseInt(document.getElementById('newsletterId').value);
let currentPage = 1;
const pageSize = 20;
let totalUsers = 0;

console.log('Newsletter ID:', newsletterId);

async function loadCurrentUsers() {
    try {
        const response = await fetch(`/newsletters/${newsletterId}/subscribers?page=${currentPage}&size=${pageSize}`);
        if (!response.ok) throw new Error('API error');
        const data = await response.json();
        totalUsers = data.total;
        renderCurrentUsers(data.users);
        renderPagination();
    } catch (error) {
        console.error('Error:', error);
        document.getElementById('currentUsersBody').innerHTML = '<table><td colspan="5" class="error">Error loading subscribers</td></tr>';
    }
}

function renderCurrentUsers(users) {
    const tbody = document.getElementById('currentUsersBody');
    if (!users || users.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="empty">No subscribers in this newsletter</td></tr>';
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
        const removeBtn = document.createElement('button');
        removeBtn.className = 'remove-btn';
        removeBtn.innerHTML = 'Remove';
        removeBtn.onclick = function () { removeUserFromNewsletter(u.id); };
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

async function loadAvailableUsers() {
    try {
        const response = await fetch(`/newsletters/${newsletterId}/available-users`);
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
        addBtn.onclick = function () { addUserToNewsletter(u.id); };
        actionCell.appendChild(addBtn);
    });
}

async function addUserToNewsletter(userId) {
    try {
        await fetch(`/newsletters/${newsletterId}/add-user`, {
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

async function removeUserFromNewsletter(userId) {
    if (!confirm('Remove this user from the newsletter?')) return;
    try {
        await fetch(`/newsletters/${newsletterId}/remove-user`, {
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
        const res = await fetch('/users/api/create', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        const result = await res.json();
        await addUserToNewsletter(result.id);
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

        for (const user of users) {
            const createRes = await fetch('/users/api/create', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    email: user.email,
                    firstName: user.firstName,
                    lastName: user.lastName,
                    phone: user.phone || '',
                    address: user.address || ''
                })
            });

            if (createRes.ok) {
                const newUser = await createRes.json();
                await addUserToNewsletter(newUser.id);
            } else {
                const error = await createRes.json();
                console.error('Error creating user:', error);
                if (error.error === 'Email already exists') {
                    const existingUsersRes = await fetch('/users/api/list');
                    const existingUsers = await existingUsersRes.json();
                    const existingUser = existingUsers.find(u => u.email === user.email);
                    if (existingUser) {
                        await addUserToNewsletter(existingUser.id);
                    }
                }
            }
        }

        loadCurrentUsers();
        loadAvailableUsers();
        alert('Users imported successfully');
        document.getElementById('jsonImport').value = '';
    } catch (e) {
        console.error('Error:', e);
        alert('Invalid JSON format or import error');
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

document.addEventListener('DOMContentLoaded', function () {
    console.log('DOM loaded, newsletterId:', newsletterId);
    loadCurrentUsers();
});