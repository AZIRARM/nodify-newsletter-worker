let allNewsletters = [];

async function loadNewsletters() {
    const grid = document.getElementById('newslettersGrid');
    grid.innerHTML = `<div class="loading"><i class="fas fa-spinner fa-spin"></i> ${t('common.loading','Loading...')}</div>`;

    try {
        const response = await fetch('/newsletters/api/list');
        allNewsletters = await response.json();
        renderNewsletters(allNewsletters);
    } catch (error) {
        console.error('Error loading newsletters:', error);
        grid.innerHTML = `<div class="empty"><i class="fas fa-exclamation-triangle"></i> ${t('newsletters.error','Error loading newsletters')}</div>`;
    }
}

function renderNewsletters(newsletters) {
    const grid = document.getElementById('newslettersGrid');
    if (!newsletters || newsletters.length === 0) {
        grid.innerHTML = `<div class="empty"><i class="fas fa-inbox"></i> ${t('newsletters.no_newsletters','No newsletters found')}</div>`;
        return;
    }
    grid.innerHTML = newsletters.map(n => {
        let campaignsInfo = '';
        if (n.campaignCount > 0) {
            campaignsInfo = `<div class="campaigns-used"><i class="fas fa-link"></i> ${t('newsletters.used_in','Used in')} ${n.campaignCount} ${t('newsletters.campaigns','campaign(s)')} - ${t('newsletters.cannot_delete','Cannot delete')}</div>`;
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
            <div class="newsletter-subject"><i class="fas fa-tag"></i> ${escapeHtml(n.subject || t('newsletter.no_content','No subject'))}</div>
            <div class="newsletter-dates">
                <i class="fas fa-calendar-plus"></i> ${t('newsletters.created_at','Created')}: ${formatDate(n.createdAt)}
            </div>
        </div>
        ${campaignsInfo}
        <div class="newsletter-actions">
            <a href="/newsletter/${n.id}" class="btn-view"><i class="fas fa-eye"></i> ${t('common.details','View')}</a>
            <a href="/newsletters/${n.id}/manage-users" class="btn-users"><i class="fas fa-users"></i> ${t('newsletter.users','Users')}</a>
            <button class="btn-delete" onclick="deleteNewsletter(${n.id})" ${n.campaignCount > 0 ? 'disabled' : ''}>
                <i class="fas fa-trash"></i> ${t('common.delete','Delete')}
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
        alert(`${t('newsletters.cannot_delete','Cannot delete')}: ${t('newsletters.used_in','Used in')} ${newsletter.campaignCount} ${t('newsletters.campaigns','campaign(s)')}`);
        return;
    }

    if (!confirm(t('newsletters.delete_confirm','Delete this newsletter?'))) return;

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
        alert(t('newsletters.delete_error','Error deleting newsletter'));
    }
}

function openNewsletterModal() {
    document.getElementById('modalTitle').innerHTML = `<i class="fas fa-newspaper"></i> ${t('newsletters.create','Create Newsletter')}`;
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
        alert(`${t('newsletters.code_required','Code is required')} / ${t('newsletters.title_required','Title is required')}`);
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
            alert(t('newsletters.save_error','Error creating newsletter'));
        }
    } catch (error) {
        console.error('Error:', error);
        alert(t('newsletters.save_error','Error creating newsletter'));
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