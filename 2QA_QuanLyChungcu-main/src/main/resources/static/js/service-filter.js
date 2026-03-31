function filterServicesByCategory(selectElement) {
    const selectedCategoryId = selectElement.value;
    const rows = document.querySelectorAll('.service-row');
    
    rows.forEach(row => {
        const rowCategoryId = row.getAttribute('data-category-id');
        if (selectedCategoryId === 'ALL' || selectedCategoryId === rowCategoryId) {
            row.style.display = '';
        } else {
            row.style.display = 'none';
        }
    });
}
