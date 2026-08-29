/* Protect authenticated pages */

(function () {

    function checkAuthentication() {

        const token = localStorage.getItem("token");

        if (!token) {
            window.location.replace("login.html");
            return false;
        }

        return true;
    }

    if (!checkAuthentication()) {
        return;
    }

    window.addEventListener("pageshow", function () {
        checkAuthentication();
    });

})();