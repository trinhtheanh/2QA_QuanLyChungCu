function filterApartmentsByCategory(selectElement) {
    const selectedCategoryId = selectElement.value;
    const rows = document.querySelectorAll('.apt-row');
    
    rows.forEach(row => {
        const rowCategoryId = row.getAttribute('data-category-id');
        if (selectedCategoryId === 'ALL' || selectedCategoryId === rowCategoryId) {
            row.style.display = '';
        } else {
            row.style.display = 'none';
        }
    });
}
