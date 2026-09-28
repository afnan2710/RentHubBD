(function () {
    'use strict';

    var root = document.querySelector('.cb-root');
    if (!root) return;

    var toggle = document.getElementById('cbToggle');
    var closeBtn = document.getElementById('cbClose');
    var window_ = document.getElementById('cbWindow');
    var messages = document.getElementById('cbMessages');
    var form = document.getElementById('cbForm');
    var input = document.getElementById('cbInput');

    function setOpen(open) {
        root.classList.toggle('is-open', open);
        window_.setAttribute('aria-hidden', open ? 'false' : 'true');
        if (open) {
            setTimeout(function () { input.focus(); }, 200);
        }
    }

    toggle.addEventListener('click', function () {
        setOpen(!root.classList.contains('is-open'));
    });
    closeBtn.addEventListener('click', function () { setOpen(false); });

    function scrollDown() {
        messages.scrollTop = messages.scrollHeight;
    }

    function appendUser(text) {
        var row = document.createElement('div');
        row.className = 'cb-msg cb-msg-user';
        var bubble = document.createElement('div');
        bubble.className = 'cb-bubble';
        bubble.textContent = text;
        row.appendChild(bubble);
        messages.appendChild(row);
        scrollDown();
    }

    function appendTyping() {
        var row = document.createElement('div');
        row.className = 'cb-msg cb-msg-bot';
        row.id = 'cbTyping';
        var box = document.createElement('div');
        box.className = 'cb-typing';
        box.innerHTML = '<span></span><span></span><span></span>';
        row.appendChild(box);
        messages.appendChild(row);
        scrollDown();
    }

    function removeTyping() {
        var el = document.getElementById('cbTyping');
        if (el) el.remove();
    }

    function escapeHtml(s) {
        if (s == null) return '';
        return String(s)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function formatRent(v) {
        if (v == null) return '৳ —';
        return '৳ ' + Math.round(v).toLocaleString('en-IN');
    }

    function buildListingCard(l) {
        var photo = l.photo
            ? '<img class="cb-listing-photo" src="/uploads/' + escapeHtml(l.photo) + '" alt="">'
            : '';

        var meta = [];
        if (l.bedrooms != null) meta.push(l.bedrooms + ' beds');
        if (l.bathrooms != null) meta.push(l.bathrooms + ' baths');
        if (l.sizeSqft != null) meta.push(Math.round(l.sizeSqft).toLocaleString('en-IN') + ' sqft');
        meta.push(escapeHtml(l.area || '') + ', ' + escapeHtml(l.city || ''));

        var facilities = '';
        if (l.facilities && l.facilities.length) {
            facilities = '<div class="cb-listing-facilities">';
            l.facilities.slice(0, 5).forEach(function (f) {
                facilities += '<span class="cb-facility-chip">' + escapeHtml(f) + '</span>';
            });
            facilities += '</div>';
        }

        return ''
            + '<a class="cb-listing-card" href="' + escapeHtml(l.url) + '">'
            +   photo
            +   '<div class="cb-listing-body">'
            +     '<p class="cb-listing-title">' + escapeHtml(l.title) + '</p>'
            +     '<p class="cb-listing-price">' + formatRent(l.rent) + '<small>/month</small></p>'
            +     '<p class="cb-listing-meta">' + meta.filter(Boolean).join(' · ') + '</p>'
            +     facilities
            +     '<span class="cb-listing-link">View details &rarr;</span>'
            +   '</div>'
            + '</a>';
    }

    function appendBot(response) {
        var row = document.createElement('div');
        row.className = 'cb-msg cb-msg-bot';

        var wrap = document.createElement('div');
        wrap.className = 'cb-bubble';

        var text = document.createElement('div');
        text.textContent = response.reply || '';
        wrap.appendChild(text);

        if (response.listings && response.listings.length) {
            var list = document.createElement('div');
            list.className = 'cb-msg-listings';
            response.listings.forEach(function (l) {
                var card = document.createElement('div');
                card.innerHTML = buildListingCard(l);
                list.appendChild(card.firstChild);
            });
            wrap.appendChild(list);
        }

        row.appendChild(wrap);
        messages.appendChild(row);
        scrollDown();
    }

    var busy = false;

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        if (busy) return;

        var text = input.value.trim();
        if (!text) return;

        appendUser(text);
        input.value = '';
        busy = true;
        form.querySelector('.cb-send').disabled = true;
        appendTyping();

        fetch('/api/chatbot/message', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            credentials: 'same-origin',
            body: JSON.stringify({ message: text })
        })
        .then(function (r) {
            if (!r.ok) throw new Error('http ' + r.status);
            return r.json();
        })
        .then(function (data) {
            removeTyping();
            appendBot(data);
        })
        .catch(function (err) {
            removeTyping();
            appendBot({
                reply: 'Sorry, I couldn\'t reach the assistant just now. Please try again in a moment.'
            });
            console.error(err);
        })
        .finally(function () {
            busy = false;
            form.querySelector('.cb-send').disabled = false;
            input.focus();
        });
    });
})();