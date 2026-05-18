let allNewsletters = [];

async function loadNewsletters() {
    const grid = document.getElementById('newslettersGrid');
    grid.innerHTML = '<div class="loading"><i class="fas fa-spinner fa-spin"></i> Loading...</div>';

    try {
        const response = await fetch('/newsletters/api/list');
        allNewsletters = await response.json();
        renderNewsletters(allNewsletters);
    } catch (error) {
        console.error('Error loading newsletters:', error);
        grid.innerHTML = '<div class="empty"><i class="fas fa-exclamation-triangle"></i> Error loading newsletters</div>';
    }
}

function renderNewsletters(newsletters) {
    const grid = document.getElementById('newslettersGrid');
    if (!newsletters || newsletters.length === 0) {
        grid.innerHTML = '<div class="empty"><i class="fas fa-inbox"></i> No newsletters found</div>';
        return;
    }
    grid.innerHTML = newsletters.map(n => {
        let campaignsInfo = '';
        if (n.campaignCount > 0) {
            campaignsInfo = `<div class="campaigns-used"><i class="fas fa-link"></i> Used in ${n.campaignCount} campaign(s) - Cannot delete</div>`;
        }

        return `
    <div class="newsletter-card">
        <div class="newsletter-header">
            <div>
                <div class="newsletter-title">📰 ${escapeHtml(n.title)}</div>
                <div class="newsletter-code">${escapeHtml(n.code)}</div>
            </div>
        </div>
        <div class="newsletter-info">
            <div class="newsletter-subject"><i class="fas fa-tag"></i> ${escapeHtml(n.subject || 'No subject')}</div>
            <div class="newsletter-dates">
                <i class="fas fa-calendar-plus"></i> Created: ${formatDate(n.createdAt)}
            </div>
        </div>
        ${campaignsInfo}
        <div class="newsletter-actions">
            <a href="/newsletter/${n.id}" class="btn-view" target="_blank"><i class="fas fa-eye"></i> View</a>
            <a href="/newsletters/${n.id}/manage-users" class="btn-users"><i class="fas fa-users"></i> Users</a>
            <button class="btn-delete" onclick="deleteNewsletter(${n.id})" ${n.campaignCount > 0 ? 'disabled' : ''}>
                <i class="fas fa-trash"></i> Delete
            </button>
        </div>
    </div>
    `;
    }).join('');
}

function searchNewsletters() {
    const searchTerm = document.getElementById('searchInput').value.toLowerCase();
    if (!searchTerm) {
        renderNewsletters(allNewsletters);
        return;
    }
    const filtered = allNewsletters.filter(n =>
        n.title.toLowerCase().includes(searchTerm) ||
        n.code.toLowerCase().includes(searchTerm)
    );
    renderNewsletters(filtered);
}

async function deleteNewsletter(id) {
    const newsletter = allNewsletters.find(n => n.id === id);
    if (newsletter.campaignCount > 0) {
        alert(`Cannot delete: This newsletter is used in ${newsletter.campaignCount} campaign(s)`);
        return;
    }

    if (!confirm('Delete this newsletter?')) return;

    try {
        const response = await fetch(`/newsletters/api/${id}`, { method: 'DELETE' });
        const result = await response.json();
        if (result.success) {
            loadNewsletters();
        } else {
            alert(result.error);
        }
    } catch (error) {
        console.error('Error deleting newsletter:', error);
        alert('Error deleting newsletter');
    }
}

function openNewsletterModal() {
    document.getElementById('modalTitle').innerHTML = '<i class="fas fa-newspaper"></i> Create Newsletter';
    document.getElementById('newsletterId').value = '';
    document.getElementById('newsletterCode').value = '';
    document.getElementById('newsletterTitle').value = '';
    document.getElementById('newsletterSubject').value = '';
    document.getElementById('newsletterContent').value = '';
    document.getElementById('newsletterModal').style.display = 'flex';
}

function closeNewsletterModal() {
    document.getElementById('newsletterModal').style.display = 'none';
}

document.getElementById('newsletterForm').addEventListener('submit', async function (e) {
    e.preventDefault();

    const data = {
        code: document.getElementById('newsletterCode').value,
        title: document.getElementById('newsletterTitle').value,
        subject: document.getElementById('newsletterSubject').value,
        contentHtml: document.getElementById('newsletterContent').value
    };

    if (!data.code || !data.title) {
        alert('Code and Title are required');
        return;
    }

    try {
        const response = await fetch('/api/newsletters', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });

        if (response.ok) {
            closeNewsletterModal();
            loadNewsletters();
        } else {
            alert('Error creating newsletter');
        }
    } catch (error) {
        console.error('Error:', error);
        alert('Error creating newsletter');
    }
});

function formatDate(date) {
    if (!date) return '-';
    return new Date(date).toLocaleDateString();
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function refreshNewsletters() {
    loadNewsletters();
}

loadNewsletters();