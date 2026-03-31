document.getElementById('feedbackForm').addEventListener('submit', function(e) {
    e.preventDefault();

    // Hiển thị Toast của Bootstrap
    const toastLiveExample = document.getElementById('liveToast');
    const toastBootstrap = bootstrap.Toast.getOrCreateInstance(toastLiveExample);
    toastBootstrap.show();

    // Xóa nội dung
    document.getElementById('feedbackInput').value = "";
});
