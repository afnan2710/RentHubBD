document.addEventListener('DOMContentLoaded', function () {
    var categorySelect   = document.getElementById('category');
    var typeSelect       = document.getElementById('propertyType');
    var residentialOnly  = document.querySelectorAll('.residential-only');
    var rentInput        = document.getElementById('monthlyRent');
    var serviceInput     = document.getElementById('serviceCharge');
    var parkingInput     = document.getElementById('parkingFee');
    var utilityInput     = document.getElementById('utilityEstimate');
    var totalDisplay     = document.getElementById('totalCostDisplay');

    var allTypes = window.RENTHUB_PROPERTY_TYPES || [];

    /* ---------- Property type dropdown driven by category ---------- */
    function populateTypes(selectedValue) {
        typeSelect.innerHTML = '';
        var placeholder = document.createElement('option');
        placeholder.value = '';
        placeholder.textContent = categorySelect.value
            ? 'Select property type'
            : 'Select a category first';
        typeSelect.appendChild(placeholder);

        if (!categorySelect.value) {
            typeSelect.disabled = true;
            return;
        }

        allTypes
            .filter(function (t) { return t.category === categorySelect.value; })
            .forEach(function (t) {
                var opt = document.createElement('option');
                opt.value = t.value;
                opt.textContent = t.label;
                if (selectedValue && selectedValue === t.value) opt.selected = true;
                typeSelect.appendChild(opt);
            });

        typeSelect.disabled = false;
    }

    function toggleResidentialFields() {
        var isResidential = categorySelect.value === 'RESIDENTIAL';
        residentialOnly.forEach(function (el) {
            el.classList.toggle('is-hidden', !isResidential);
        });
    }

    if (categorySelect && typeSelect) {
        var initialType = typeSelect.getAttribute('data-initial-value') || '';
        populateTypes(initialType);
        toggleResidentialFields();

        categorySelect.addEventListener('change', function () {
            populateTypes(null);
            toggleResidentialFields();
        });
    }

    /* ---------- Live total cost ---------- */
    function recalcTotal() {
        if (!totalDisplay) return;
        var total = 0;
        [rentInput, serviceInput, parkingInput, utilityInput].forEach(function (input) {
            if (!input) return;
            var v = parseFloat(input.value);
            if (!isNaN(v) && v > 0) total += v;
        });
        totalDisplay.textContent = '\u09F3 ' + total.toLocaleString('en-US');
    }
    [rentInput, serviceInput, parkingInput, utilityInput].forEach(function (input) {
        if (input) input.addEventListener('input', recalcTotal);
    });
    recalcTotal();

    /* ---------- Photo previews ---------- */
    var photoInput = document.getElementById('photos');
    var previewGrid = document.getElementById('photoPreviewGrid');
    var photoCount  = document.getElementById('photoCount');

    if (photoInput && previewGrid) {
        photoInput.addEventListener('change', function () {
            previewGrid.innerHTML = '';
            var files = Array.from(photoInput.files || []);
            if (files.length === 0) {
                photoCount.textContent = 'No photos selected';
                return;
            }
            photoCount.textContent = files.length + ' photo' + (files.length === 1 ? '' : 's') + ' selected';

            files.slice(0, 8).forEach(function (file) {
                if (!file.type.startsWith('image/')) return;
                var reader = new FileReader();
                reader.onload = function (e) {
                    var div = document.createElement('div');
                    div.className = 'thumb';
                    var img = document.createElement('img');
                    img.src = e.target.result;
                    div.appendChild(img);
                    previewGrid.appendChild(div);
                };
                reader.readAsDataURL(file);
            });
        });
    }
});