document.addEventListener('DOMContentLoaded', function () {
    var container = document.getElementById('chatMessages');
    var form = document.getElementById('chatForm');
    var input = document.getElementById('chatInput');
    if (!container || !form || !input) return;

    var conversationId = container.getAttribute('data-conversation-id');
    var lastId = 0;

    Array.from(container.querySelectorAll('.msg-bubble')).forEach(function (b) {
        var raw = b.getAttribute('data-msg-id');
        if (raw) {
            var v = parseInt(raw, 10);
            if (!isNaN(v) && v > lastId) lastId = v;
        }
    });

    container.scrollTop = container.scrollHeight;

    function appendMessage(msg) {
        var row = document.createElement('div');
        row.className = 'msg ' + (msg.mine ? 'msg-mine' : 'msg-theirs');

        var bubble = document.createElement('div');
        bubble.className = 'msg-bubble';
        bubble.setAttribute('data-msg-id', msg.id);

        var content = document.createElement('p');
        content.className = 'msg-content';
        content.textContent = msg.content;

        var time = document.createElement('span');
        time.className = 'msg-time';
        time.textContent = formatTime(msg.sentAt);

        bubble.appendChild(content);
        bubble.appendChild(time);
        row.appendChild(bubble);
        container.appendChild(row);
        container.scrollTop = container.scrollHeight;
    }

    function formatTime(value) {
        if (!value) return '';
        var d;
        if (Array.isArray(value)) {
            d = new Date(value[0], value[1] - 1, value[2],
                    value[3] || 0, value[4] || 0, value[5] || 0);
        } else {
            d = new Date(value);
        }
        if (isNaN(d.getTime())) return '';
        var h = String(d.getHours()).padStart(2, '0');
        var m = String(d.getMinutes()).padStart(2, '0');
        return h + ':' + m;
    }

    function poll() {
        fetch('/api/chat/' + conversationId + '/messages?afterId=' + lastId, {
            headers: { 'Accept': 'application/json' },
            credentials: 'same-origin'
        })
        .then(function (r) {
            if (!r.ok) throw new Error('poll ' + r.status);
            return r.json();
        })
        .then(function (messages) {
            if (!Array.isArray(messages) || messages.length === 0) return;
            messages.forEach(function (m) {
                appendMessage(m);
                if (m.id > lastId) lastId = m.id;
            });
        })
        .catch(function () { });
    }

    setInterval(poll, 2500);

    input.addEventListener('input', function () {
        input.style.height = 'auto';
        input.style.height = Math.min(input.scrollHeight, 120) + 'px';
    });

    input.addEventListener('keydown', function (e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            form.dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
        }
    });

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        e.stopPropagation();
        var text = input.value.trim();
        if (!text) return;

        input.value = '';
        input.style.height = 'auto';

        fetch('/api/chat/' + conversationId + '/send', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            credentials: 'same-origin',
            body: JSON.stringify({ content: text })
        })
        .then(function (r) {
            if (!r.ok) return r.text().then(function (t) {
                throw new Error('send ' + r.status + ' ' + t);
            });
            return r.json();
        })
        .then(function (msg) {
            appendMessage(msg);
            if (msg.id > lastId) lastId = msg.id;
        })
        .catch(function (err) {
            console.error(err);
            alert('Could not send message. Check the browser console for details.');
        });
    });
});