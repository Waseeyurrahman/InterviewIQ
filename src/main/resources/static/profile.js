document.addEventListener(
    "DOMContentLoaded",
    loadProfile
);

// Load profile
async function loadProfile() {

    showState("loading");

    try {

        const token =
            localStorage.getItem("token");

        if (!token) {

            window.location.replace(
                "login.html"
            );

            return;
        }

        const response =
            await fetch(
                "/api/profile",
                {
                    method: "GET",

                    headers: {
                        "Authorization":
                            "Bearer " + token
                    }
                }
            );

        if (response.status === 401) {

            localStorage.removeItem("token");

            window.location.replace(
                "login.html"
            );

            return;
        }

        if (response.status === 403) {

            throw new Error(
                "You are not authorized to view this profile."
            );
        }

        if (!response.ok) {

            let message =
                `Request failed: HTTP ${response.status}`;

            try {

                const errorData =
                    await response.json();

                if (errorData?.message) {

                    message =
                        errorData.message;
                }

            } catch (_) {
                // Ignore invalid JSON.
            }

            throw new Error(message);
        }

        const profile =
            await response.json();

        console.log(
            "Profile:",
            profile
        );

        renderProfile(profile);

        showState("data");

    } catch (error) {

        console.error(
            "Profile loading failed:",
            error
        );

        document.getElementById(
            "errorMsg"
        ).textContent =
            error.message ||
            "Unable to load your profile.";

        showState("error");
    }
}

// Render profile data
function renderProfile(profile) {

    const name =
        profile?.name ||
        "User";

    const email =
        profile?.email ||
        "—";

    const id =
        profile?.id ??
        "—";

    const totalInterviews =
        Number(
            profile?.totalInterviews || 0
        );

    document.getElementById(
        "profileName"
    ).textContent = name;

    document.getElementById(
        "profileEmail"
    ).textContent = email;

    document.getElementById(
        "infoName"
    ).textContent = name;

    document.getElementById(
        "infoEmail"
    ).textContent = email;

    document.getElementById(
        "infoId"
    ).textContent = id;

    document.getElementById(
        "totalInterviews"
    ).textContent =
        totalInterviews;

    const firstLetter =
        name
            .trim()
            .charAt(0)
            .toUpperCase();

    document.getElementById(
        "profileAvatar"
    ).textContent =
        firstLetter || "U";
}

// Show page state
function showState(state) {

    const loading =
        document.getElementById(
            "loadingState"
        );

    const error =
        document.getElementById(
            "errorState"
        );

    const data =
        document.getElementById(
            "profileData"
        );

    loading.classList.add("hidden");
    error.classList.add("hidden");
    data.classList.add("hidden");

    if (state === "loading") {

        loading.classList.remove("hidden");

    } else if (state === "error") {

        error.classList.remove("hidden");

    } else if (state === "data") {

        data.classList.remove("hidden");
    }
}