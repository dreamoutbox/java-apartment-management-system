/**
 * Apartment Hub — Table Utilities + Modal Helpers
 * Adds a live search bar and sortable column headers to every <table> on the page.
 *
 * Modal open/close is handled via event delegation off data-modal-target /
 * data-modal-action attributes — no th:onclick / inline handlers required in templates.
 *
 *   <button data-modal-target="modal-user-123">Update</button>
 *   <button data-modal-target="modal-user-123" data-modal-action="close">✕ Close</button>
 */

(function () {
    'use strict';

    // --- Modal delegation ---
    document.addEventListener('click', function (e) {
        var trigger = e.target.closest('[data-modal-target]');
        if (!trigger) return;

        var modal = document.getElementById(trigger.dataset.modalTarget);
        if (!modal) return;

        modal.style.display = trigger.dataset.modalAction === 'close' ? 'none' : 'flex';
    });

    // Optional: click on overlay background (not the modal box) closes it
    document.addEventListener('click', function (e) {
        if (e.target.classList && e.target.classList.contains('ams-modal-overlay')) {
            e.target.style.display = 'none';
        }
    });

    // --- Table search + sort ---
    function initTables() {
        document.querySelectorAll('table').forEach(function (table) {
            var tbody = table.querySelector('tbody');
            if (!tbody) return;

            // --- Search Bar ---
            if (!table.previousElementSibling || !table.previousElementSibling.classList.contains('ams-search-box')) {
                var searchWrap = document.createElement('div');
                searchWrap.className = 'ams-search-box';
                searchWrap.style.marginBottom = '10px';

                var searchInput = document.createElement('input');
                searchInput.type = 'text';
                searchInput.className = 'form-control';
                searchInput.placeholder = '🔍 Search in table...';
                searchInput.style.maxWidth = '280px';
                searchInput.style.padding = '6px 12px';
                searchInput.style.fontSize = '13px';

                searchInput.addEventListener('keyup', function () {
                    var query = this.value.toLowerCase();
                    tbody.querySelectorAll('tr').forEach(function (row) {
                        var text = row.innerText.toLowerCase();
                        row.style.display = text.includes(query) ? '' : 'none';
                    });
                });

                searchWrap.appendChild(searchInput);
                table.parentNode.insertBefore(searchWrap, table);
            }

            // --- Sortable Headers ---
            var sortState = { col: -1, asc: true };
            table.querySelectorAll('th').forEach(function (th, colIndex) {
                if (!th.querySelector('.sort-indicator')) {
                    var indicator = document.createElement('span');
                    indicator.className = 'sort-indicator';
                    indicator.style.cssText = 'margin-left:4px;opacity:0.4;font-size:10px;';
                    indicator.textContent = '⇅';
                    th.appendChild(indicator);
                }
                th.style.cursor = 'pointer';
                th.title = 'Click to sort by this column';

                th.addEventListener('click', function () {
                    var asc = !(sortState.col === colIndex && sortState.asc);
                    sortState.col = colIndex;
                    sortState.asc = asc;

                    // Reset all indicators
                    table.querySelectorAll('.sort-indicator').forEach(function (el) {
                        el.textContent = '⇅';
                        el.style.opacity = '0.4';
                    });
                    var activeIndicator = th.querySelector('.sort-indicator');
                    if (activeIndicator) {
                        activeIndicator.textContent = asc ? '↑' : '↓';
                        activeIndicator.style.opacity = '1';
                    }

                    var rows = Array.from(tbody.querySelectorAll('tr'));
                    rows.sort(function (a, b) {
                        var aText = getCellText(a, colIndex);
                        var bText = getCellText(b, colIndex);
                        return asc
                            ? aText.localeCompare(bText, undefined, { numeric: true })
                            : bText.localeCompare(aText, undefined, { numeric: true });
                    });
                    rows.forEach(function (row) { tbody.appendChild(row); });
                });
            });
        });
    }

    function getCellText(row, colIndex) {
        var cell = row.children[colIndex];
        if (!cell) return '';
        var input = cell.querySelector('input[type="text"], input[type="number"], input[type="date"], input[type="email"], select');
        if (input) return (input.value || '').trim();
        return (cell.innerText || '').trim();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initTables);
    } else {
        initTables();
    }
})();
