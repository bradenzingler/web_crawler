document.addEventListener('DOMContentLoaded', function() {
    const darkMode = document.getElementById('dark-mode-toggle');

    darkMode.addEventListener('click', function() {
        document.body.classList.toggle('dark');
        darkMode.textContent = document.body.classList.contains('dark') ? 'Light Mode' : 'Dark Mode';
        darkMode.style.backgroundColor = document.body.classList.contains('dark') ? 'white' : 'black';
        darkMode.style.color = document.body.classList.contains('dark') ? 'black' : 'white';
    });
});