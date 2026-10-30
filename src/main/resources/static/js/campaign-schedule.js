let currentScheduleCampaignId = null;

function openScheduleModal(campaignId, startDate, endDate, scheduledStart, retryInterval, active) {
    if (!campaignId || campaignId === 'null' || campaignId === 'undefined') {
        alert(t('common.error','Error: Invalid campaign ID'));
        return;
    }

    currentScheduleCampaignId = parseInt(campaignId, 10);

    const startDateInput = document.getElementById('scheduleStartDate');
    const endDateInput = document.getElementById('scheduleEndDate');
    const scheduledStartInput = document.getElementById('scheduleScheduledStart');
    const retryIntervalInput = document.getElementById('scheduleRetryInterval');
    const activeCheckbox = document.getElementById('scheduleActive');

    if (startDateInput) startDateInput.value = startDate || '';
    if (endDateInput) endDateInput.value = endDate || '';
    if (scheduledStartInput) scheduledStartInput.value = scheduledStart || '';
    if (retryIntervalInput) retryIntervalInput.value = retryInterval || 0;
    if (activeCheckbox) activeCheckbox.checked = active === true;

    const modal = document.getElementById('scheduleModal');
    if (modal) modal.style.display = 'flex';
}

function closeScheduleModal() {
    const modal = document.getElementById('scheduleModal');
    if (modal) modal.style.display = 'none';
    currentScheduleCampaignId = null;
}

async function saveSchedule() {
    if (!currentScheduleCampaignId) {
        alert(t('common.error','Error: No campaign selected'));
        return;
    }

    const data = {
        startDate: document.getElementById('scheduleStartDate').value,
        endDate: document.getElementById('scheduleEndDate').value,
        scheduledStart: document.getElementById('scheduleScheduledStart').value,
        retryIntervalMinutes: document.getElementById('scheduleRetryInterval').value,
        active: document.getElementById('scheduleActive').checked
    };

    try {
        const response = await fetch(`/api/campaigns/${currentScheduleCampaignId}/schedule`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams(data),
            redirect: 'manual'
        });

        // Le contrôleur renvoie une redirection 302 vers /detail
        if (response.ok || response.type === 'opaqueredirect' || response.status === 0 || response.status === 302) {
            closeScheduleModal();
            window.location.href = `/api/campaigns/${currentScheduleCampaignId}/detail`;
        } else {
            alert(t('schedule.error','Error saving schedule'));
        }
    } catch (error) {
        // fetch avec redirect: 'manual' peut lever une erreur selon le navigateur — on considère que c'est OK
        closeScheduleModal();
        window.location.href = `/api/campaigns/${currentScheduleCampaignId}/detail`;
    }
}

document.addEventListener('DOMContentLoaded', function () {
    const form = document.getElementById('scheduleForm');
    if (form) {
        form.addEventListener('submit', function (e) {
            e.preventDefault();
            saveSchedule();
        });
    }
});