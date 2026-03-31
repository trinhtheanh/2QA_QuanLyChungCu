function filterInvoices(type, buttonElement) {
    // Cập nhật giao diện nút bấm
    const buttons = document.querySelectorAll('.custom-filter-btn');
    buttons.forEach(btn => {
        btn.classList.remove('btn-primary');
        btn.classList.add('btn-outline-primary');
    });
    buttonElement.classList.remove('btn-outline-primary');
    buttonElement.classList.add('btn-primary');

    // Lọc các hàng trong bảng
    const rows = document.querySelectorAll('.invoice-row');
    rows.forEach(row => {
        if (type === 'ALL') {
            row.style.display = '';
        } else {
            if (row.getAttribute('data-type') === type) {
                row.style.display = '';
            } else {
                row.style.display = 'none';
            }
        }
    });
}
