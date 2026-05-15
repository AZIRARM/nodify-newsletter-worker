let currentScheduleCampaignId = null;

function openScheduleModal(campaignId, startDate, endDate, scheduledStart, retryInterval, active) {
    if (!campaignId || campaignId === 'null' || campaignId === 'undefined') {
        alert('Error: Invalid campaign ID');
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
        alert('Error: No campaign selected');
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
        const response = await fetch(`/campaign/${currentScheduleCampaignId}/schedule`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams(data)
        });

        if (response.ok) {
            closeScheduleModal();
            location.reload();
        } else {
            alert('Error saving schedule');
        }
    } catch (error) {
        console.error('Error:', error);
        alert('Error saving schedule');
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