/**
 * Apartment Hub — Dynamic Table Utilities
 * Adds live searching, limit/rows-per-page dropdown, pagination, and sortable headers to every <table>.
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

            var currentPage = 1;
            var pageSize = 10;
            var searchQuery = '';
            var sortState = { col: -1, asc: true };

            // Toolbar wrap (limit dropdown + search bar)
            if (!table.previousElementSibling || !table.previousElementSibling.classList.contains('ams-table-toolbar')) {
                var toolbar = document.createElement('div');
                toolbar.className = 'ams-table-toolbar';

                // Limit Dropdown
                var limitBox = document.createElement('div');
                limitBox.className = 'ams-limit-box';
                limitBox.innerHTML = '<span>Show</span>';

                var limitSelect = document.createElement('select');
                limitSelect.className = 'ams-limit-select';
                [5, 10, 25, 50, 'All'].forEach(function (val) {
                    var opt = document.createElement('option');
                    opt.value = val;
                    opt.textContent = val;
                    if (val === 10) opt.selected = true;
                    limitSelect.appendChild(opt);
                });

                limitSelect.addEventListener('change', function () {
                    pageSize = this.value === 'All' ? Infinity : parseInt(this.value, 10);
                    currentPage = 1;
                    render();
                });

                limitBox.appendChild(limitSelect);
                limitBox.appendChild(document.createTextNode(' entries per page'));
                toolbar.appendChild(limitBox);

                // Search Bar
                var searchInput = document.createElement('input');
                searchInput.type = 'text';
                searchInput.className = 'form-control';
                searchInput.placeholder = '🔍 Search in table...';
                searchInput.style.maxWidth = '240px';
                searchInput.style.padding = '6px 12px';
                searchInput.style.fontSize = '13px';

                searchInput.addEventListener('keyup', function () {
                    searchQuery = this.value.toLowerCase().trim();
                    currentPage = 1;
                    render();
                });

                toolbar.appendChild(searchInput);
                table.parentNode.insertBefore(toolbar, table);
            }

            // Bottom Pagination Bar
            var paginationBar = document.createElement('div');
            paginationBar.className = 'ams-pagination-toolbar';
            table.parentNode.insertBefore(paginationBar, table.nextSibling);

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
                    render();
                });
            });

            function getFilteredRows() {
                if (!searchQuery) return allRows;
                return allRows.filter(function (row) {
                    return row.innerText.toLowerCase().includes(searchQuery);
                });
            }

            function render() {
                var filtered = getFilteredRows();
                var totalFiltered = filtered.length;
                var totalPages = pageSize === Infinity ? 1 : Math.ceil(totalFiltered / pageSize) || 1;

                if (currentPage > totalPages) currentPage = totalPages;
                if (currentPage < 1) currentPage = 1;

                var startIdx = pageSize === Infinity ? 0 : (currentPage - 1) * pageSize;
                var endIdx = pageSize === Infinity ? totalFiltered : Math.min(startIdx + pageSize, totalFiltered);

                // Hide all, append & show page rows
                allRows.forEach(function (row) { row.style.display = 'none'; });
                for (var i = startIdx; i < endIdx; i++) {
                    if (filtered[i]) {
                        filtered[i].style.display = '';
                        tbody.appendChild(filtered[i]);
                    }
                }

                // Render Pagination Info & Controls
                paginationBar.innerHTML = '';

                var info = document.createElement('div');
                if (totalFiltered === 0) {
                    info.textContent = 'Showing 0 to 0 of 0 entries';
                } else {
                    info.textContent = 'Showing ' + (startIdx + 1) + ' to ' + endIdx + ' of ' + totalFiltered + ' entries';
                }
                paginationBar.appendChild(info);

                var controls = document.createElement('div');
                controls.className = 'ams-pagination-controls';

                if (totalPages > 1) {
                    // Prev Button
                    var prevBtn = document.createElement('button');
                    prevBtn.type = 'button';
                    prevBtn.className = 'ams-page-btn';
                    prevBtn.textContent = '‹ Prev';
                    prevBtn.disabled = currentPage === 1;
                    prevBtn.addEventListener('click', function () {
                        if (currentPage > 1) { currentPage--; render(); }
                    });
                    controls.appendChild(prevBtn);

                    // Numbered Page Buttons
                    var startPage = Math.max(1, currentPage - 2);
                    var endPage = Math.min(totalPages, currentPage + 2);

                    for (var p = startPage; p <= endPage; p++) {
                        (function (page) {
                            var pBtn = document.createElement('button');
                            pBtn.type = 'button';
                            pBtn.className = 'ams-page-btn' + (page === currentPage ? ' active' : '');
                            pBtn.textContent = page;
                            pBtn.addEventListener('click', function () {
                                currentPage = page;
                                render();
                            });
                            controls.appendChild(pBtn);
                        })(p);
                    }

                    // Next Button
                    var nextBtn = document.createElement('button');
                    nextBtn.type = 'button';
                    nextBtn.className = 'ams-page-btn';
                    nextBtn.textContent = 'Next ›';
                    nextBtn.disabled = currentPage === totalPages;
                    nextBtn.addEventListener('click', function () {
                        if (currentPage < totalPages) { currentPage++; render(); }
                    });
                    controls.appendChild(nextBtn);
                }

                paginationBar.appendChild(controls);

                // Resync selection highlight after DOM reordering
                var checkedRadio = table.querySelector('input[type="radio"]:checked');
                table.querySelectorAll('tbody tr').forEach(function (r) {
                    var rRadio = r.querySelector('input[type="radio"]');
                    r.classList.toggle('selected-row', !!(rRadio && rRadio.checked));
                });
            }

            // Initial render
            render();
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
