let allUsers = [];

async function loadUsers() {
    const res = await fetch('/users/api/list');
    allUsers = await res.json();
    renderUsers(allUsers);
}

function renderUsers(users) {
    const tbody = document.getElementById('usersTableBody');
    if (!users || users.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="loading">No users found</td></tr>';
        return;
    }
    tbody.innerHTML = users.map(u => `
        <tr>
            <td>${u.id}</td>
            <td>${escapeHtml(u.email)}</td>
            <td>${escapeHtml(u.firstName || '')}</td>
            <td>${escapeHtml(u.lastName || '')}</td>
            <td>${escapeHtml(u.phone || '')}</td>
            <td>${escapeHtml(u.address || '')}</td>
            <td>${new Date(u.createdAt).toLocaleDateString()}</td>
            <td>
                <button class="btn-edit" onclick="editUser(${u.id})">Edit</button>
                <button class="btn-delete" onclick="deleteUser(${u.id})">Delete</button>
            </td>
        </tr>
    `).join('');
}

function searchUsers() {
    const search = document.getElementById('searchInput').value.toLowerCase();
    const filtered = allUsers.filter(u =>
        u.email.toLowerCase().includes(search) ||
        (u.firstName && u.firstName.toLowerCase().includes(search)) ||
        (u.lastName && u.lastName.toLowerCase().includes(search))
    );
    renderUsers(filtered);
}

function openUserModal() {
    document.getElementById('modalTitle').innerText = 'Add User';
    document.getElementById('userId').value = '';
    document.getElementById('userEmail').value = '';
    document.getElementById('userFirstName').value = '';
    document.getElementById('userLastName').value = '';
    document.getElementById('userPhone').value = '';
    document.getElementById('userAddress').value = '';
    document.getElementById('userModal').style.display = 'flex';
}

function editUser(id) {
    const user = allUsers.find(u => u.id === id);
    if (!user) return;
    document.getElementById('modalTitle').innerText = 'Edit User';
    document.getElementById('userId').value = user.id;
    document.getElementById('userEmail').value = user.email;
    document.getElementById('userFirstName').value = user.firstName || '';
    document.getElementById('userLastName').value = user.lastName || '';
    document.getElementById('userPhone').value = user.phone || '';
    document.getElementById('userAddress').value = user.address || '';
    document.getElementById('userModal').style.display = 'flex';
}

async function saveUser() {
    const id = document.getElementById('userId').value;
    const user = {
        email: document.getElementById('userEmail').value,
        firstName: document.getElementById('userFirstName').value,
        lastName: document.getElementById('userLastName').value,
        phone: document.getElementById('userPhone').value,
        address: document.getElementById('userAddress').value
    };

    let url = '/users/api/create';
    let method = 'POST';
    if (id) {
        url = `/users/api/${id}`;
        method = 'PUT';
    }

    const res = await fetch(url, {
        method: method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(user)
    });

    if (res.ok) {
        closeUserModal();
        loadUsers();
    } else {
        const error = await res.json();
        alert(error.error || 'Error saving user');
    }
}

async function deleteUser(id) {
    if (!confirm('Delete this user? This will also remove them from all campaigns.')) return;
    await fetch(`/users/api/${id}`, { method: 'DELETE' });
    loadUsers();
}

async function exportUsers() {
    const res = await fetch('/users/api/export');
    const users = await res.json();
    const blob = new Blob([JSON.stringify(users, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'users-export.json';
    a.click();
    URL.revokeObjectURL(url);
}

async function importUsers(input) {
    const file = input.files[0];
    if (!file) return;
    const text = await file.text();
    try {
        const users = JSON.parse(text);
        const res = await fetch('/users/api/import', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(users)
        });
        if (res.ok) {
            loadUsers();
            alert('Users imported successfully');
        }
    } catch (e) {
        alert('Invalid JSON file');
    }
    input.value = '';
}

function closeUserModal() {
    document.getElementById('userModal').style.display = 'none';
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

loadUsers();