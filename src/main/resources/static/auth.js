/* Authentication helpers */

function logout(event) {

    if (event) {
        event.preventDefault();
    }

    localStorage.removeItem("token");
    localStorage.removeItem("interviewId");

    window.location.replace("login.html");
}