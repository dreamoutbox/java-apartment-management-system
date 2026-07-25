/**
 * Apartment Hub — Dynamic Table Utilities
 * Supports table row selection and column header sorting for <table> elements.
 * Displays all table rows without pagination limits or search toolbar.
 */
(function () {
    'use strict';

    document.addEventListener('click', function (e) {
        // Exclude clicks on actual interactive elements, not ancestor <form> wrappers
        if (e.target.closest('button, a, select, textarea')) return;
        if (e.target.tagName === 'INPUT' && e.target.type !== 'radio') return;

        var tr = e.target.closest('tbody tr');
        if (!tr) return;

        var radio = tr.querySelector('input[type="radio"]');
        if (!radio || radio.disabled || radio === e.target) return;

        radio.checked = true;
        radio.dispatchEvent(new Event('change', { bubbles: true }));
    });

    // Highlight sync: fires on programmatic check above AND native radio clicks
    document.addEventListener('change', function (e) {
        if (e.target.tagName !== 'INPUT' || e.target.type !== 'radio') return;

        var table = e.target.closest('table');
        if (!table) return;

        table.querySelectorAll('tbody tr').forEach(function (r) {
            var rRadio = r.querySelector('input[type="radio"]');
            r.classList.toggle('selected-row', !!(rRadio && rRadio.checked));
        });
    });

    function initTables() {
        document.querySelectorAll('table').forEach(function (table) {
            var tbody = table.querySelector('tbody');
            if (!tbody) return;

            var allRows = Array.from(tbody.querySelectorAll('tr'));
            if (allRows.length === 0) return;

            var sortState = { col: -1, asc: true };

            // Show all table rows by default
            allRows.forEach(function (row) { row.style.display = ''; });

            // Sortable Headers
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

                    table.querySelectorAll('.sort-indicator').forEach(function (el) {
                        el.textContent = '⇅';
                        el.style.opacity = '0.4';
                    });
                    var activeIndicator = th.querySelector('.sort-indicator');
                    if (activeIndicator) {
                        activeIndicator.textContent = asc ? '↑' : '↓';
                        activeIndicator.style.opacity = '1';
                    }

                    allRows.sort(function (a, b) {
                        var aText = getCellText(a, colIndex);
                        var bText = getCellText(b, colIndex);
                        return asc
                            ? aText.localeCompare(bText, undefined, { numeric: true })
                            : bText.localeCompare(aText, undefined, { numeric: true });
                    });

                    allRows.forEach(function (row) {
                        row.style.display = '';
                        tbody.appendChild(row);
                    });

                    // Resync selection highlight
                    var checkedRadio = table.querySelector('input[type="radio"]:checked');
                    table.querySelectorAll('tbody tr').forEach(function (r) {
                        var rRadio = r.querySelector('input[type="radio"]');
                        r.classList.toggle('selected-row', !!(rRadio && rRadio.checked));
                    });
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
