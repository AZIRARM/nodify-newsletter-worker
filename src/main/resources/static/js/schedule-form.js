document.addEventListener('DOMContentLoaded', function () {
    const form = document.querySelector('form');
    const retryInput = document.querySelector('input[name="retryIntervalMinutes"]');

    if (retryInput) {
        retryInput.addEventListener('change', function () {
            if (this.value < 0) this.value = 0;
        });
    }

    const scheduledStartInput = document.querySelector('input[name="scheduledStart"]');
    if (scheduledStartInput && !scheduledStartInput.value) {
        const now = new Date();
        now.setMinutes(now.getMinutes() + 30);
        const formatted = now.toISOString().slice(0, 16);
        scheduledStartInput.min = formatted;
    }
});