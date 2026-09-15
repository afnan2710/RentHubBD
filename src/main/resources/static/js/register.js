document.addEventListener('DOMContentLoaded', function () {
    var photoInput = document.getElementById('profilePhoto');
    var photoPreview = document.getElementById('photoPreview');

    if (photoInput && photoPreview) {
        photoInput.addEventListener('change', function () {
            var file = photoInput.files && photoInput.files[0];
            if (!file) return;
            var reader = new FileReader();
            reader.onload = function (e) {
                photoPreview.innerHTML = '<img src="' + e.target.result + '" alt="Profile preview">';
                photoPreview.classList.add('has-photo');
            };
            reader.readAsDataURL(file);
        });
    }

    var password = document.getElementById('password');
    var confirmPassword = document.getElementById('confirmPassword');
    var strengthLabel = document.getElementById('strengthLabel');
    var matchLabel = document.getElementById('matchLabel');
    var segments = [
        document.getElementById('strengthSeg1'),
        document.getElementById('strengthSeg2'),
        document.getElementById('strengthSeg3'),
        document.getElementById('strengthSeg4')
    ];
    var colors = ['#C0392B', '#C9A227', '#1D7A5F', '#14213D'];
    var labels = ['Weak', 'Fair', 'Good', 'Strong'];

    function getStrength(value) {
        var score = 0;
        if (value.length >= 8) score++;
        if (/[a-z]/.test(value) && /[A-Z]/.test(value)) score++;
        if (/\d/.test(value)) score++;
        if (/[^A-Za-z0-9]/.test(value)) score++;
        return score;
    }

    function updateStrength() {
        if (!password) return;
        var value = password.value;
        var score = value.length === 0 ? 0 : getStrength(value);
        segments.forEach(function (seg, index) {
            if (!seg) return;
            seg.style.background = index < score ? colors[Math.max(score - 1, 0)] : '#DDE3E0';
        });
        if (strengthLabel) {
            strengthLabel.textContent = value.length === 0
                ? 'Use 8+ characters with a mix of letters, numbers and symbols'
                : labels[Math.max(score - 1, 0)] + ' password';
        }
    }

    function updateMatch() {
        if (!confirmPassword || !matchLabel || !password) return;
        if (confirmPassword.value.length === 0) {
            matchLabel.textContent = '';
            matchLabel.className = 'hint';
            return;
        }
        if (confirmPassword.value === password.value) {
            matchLabel.textContent = 'Passwords match';
            matchLabel.className = 'hint match-ok';
        } else {
            matchLabel.textContent = 'Passwords do not match';
            matchLabel.className = 'hint match-fail';
        }
    }

    if (password) {
        password.addEventListener('input', function () {
            updateStrength();
            updateMatch();
        });
        password.addEventListener('keyup', function () {
            updateStrength();
            updateMatch();
        });
    }
    if (confirmPassword) {
        confirmPassword.addEventListener('input', updateMatch);
        confirmPassword.addEventListener('keyup', updateMatch);
    }
});