/**
 * Apartment Hub — Table Utilities + Modal Helpers
 * Adds a live search bar and sortable column headers to every <table> on the page.
 * Also provides openModal / closeModal used by per-row Update buttons.
 */

function openModal(id) {
    var el = document.getElementById(id);
    if (el) el.style.display = 'flex';
}

function closeModal(id) {
    var el = document.getElementById(id);
    if (el) el.style.display = 'none';
}

(function () {
    'use strict';

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
