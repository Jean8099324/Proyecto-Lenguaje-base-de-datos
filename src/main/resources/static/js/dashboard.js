document.addEventListener("DOMContentLoaded", function () {

    const sidebar = document.getElementById("sidebar");
    const menuButton = document.getElementById("menuButton");
    const closeSidebar = document.getElementById("closeSidebar");
    const sidebarOverlay = document.getElementById("sidebarOverlay");

    function openSidebar() {
        sidebar.classList.add("open");
        sidebarOverlay.classList.add("show");
    }

    function closeSidebarMenu() {
        sidebar.classList.remove("open");
        sidebarOverlay.classList.remove("show");
    }

    if (menuButton) {
        menuButton.addEventListener("click", openSidebar);
    }

    if (closeSidebar) {
        closeSidebar.addEventListener("click", closeSidebarMenu);
    }

    if (sidebarOverlay) {
        sidebarOverlay.addEventListener("click", closeSidebarMenu);
    }

    window.addEventListener("resize", function () {
        if (window.innerWidth > 1050) {
            closeSidebarMenu();
        }
    });

});