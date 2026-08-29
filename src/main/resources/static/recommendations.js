document.addEventListener(
    "DOMContentLoaded",
    loadRecommendations
);

// Load recommendations
async function loadRecommendations() {

    showState("loading");

    try {

        const token =
            localStorage.getItem("token");

        if (!token) {

            throw new Error(
                "Authentication required. Please login again."
            );
        }

        const response =
            await fetch(
                "/api/recommendations",
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
                "You are not authorized to view recommendations."
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

        const data =
            await response.json();

        console.log(
            "Recommendations:",
            data
        );

        if (!data) {

            throw new Error(
                "Invalid recommendation response."
            );
        }

        const weakAreas =
            Array.isArray(data.weakAreas)
                ? data.weakAreas
                : [];

        const recommendations =
            Array.isArray(data.recommendations)
                ? data.recommendations
                : [];

        const strengths =
            Array.isArray(data.strengths)
                ? data.strengths
                : [];

        if (
            weakAreas.length === 0 &&
            recommendations.length === 0 &&
            strengths.length === 0
        ) {

            showState("empty");

            return;
        }

        renderWeakAreas(weakAreas);
        renderRecommendations(recommendations);
        renderStrengths(strengths);

        showState("data");

    } catch (error) {

        console.error(
            "Recommendation loading failed:",
            error
        );

        document.getElementById(
            "errorMsg"
        ).textContent =
            error.message ||
            "Unable to load recommendations.";

        showState("error");
    }
}

// Render weak areas
function renderWeakAreas(items) {

    const container =
        document.getElementById(
            "weakAreasList"
        );

    container.innerHTML = "";

    if (!items.length) {

        container.innerHTML =
            createEmptyMessage(
                "No major weaknesses identified yet."
            );

        return;
    }

    items.forEach(
        (item, index) => {

            container.innerHTML +=
                createRecommendationItem(
                    item,
                    index + 1,
                    "weakness"
                );
        }
    );
}

// Render recommendations
function renderRecommendations(items) {

    const container =
        document.getElementById(
            "recommendationsList"
        );

    container.innerHTML = "";

    if (!items.length) {

        container.innerHTML =
            createEmptyMessage(
                "No additional recommendations yet."
            );

        return;
    }

    items.forEach(
        (item, index) => {

            container.innerHTML +=
                createRecommendationItem(
                    item,
                    index + 1,
                    "recommendation"
                );
        }
    );
}

// Render strengths
function renderStrengths(items) {

    const container =
        document.getElementById(
            "strengthsList"
        );

    container.innerHTML = "";

    if (!items.length) {

        container.innerHTML =
            createEmptyMessage(
                "Complete more interviews to identify your strengths."
            );

        return;
    }

    items.forEach(
        (item, index) => {

            container.innerHTML +=
                createRecommendationItem(
                    item,
                    index + 1,
                    "strength"
                );
        }
    );
}

// Create recommendation item
function createRecommendationItem(
    item,
    index,
    type
) {

    const text =
        escapeHtml(
            item?.text ||
            "No description available."
        );

    const count =
        Number(item?.count || 0);

    let iconClass =
        "fa-solid fa-circle";

    if (type === "weakness") {

        iconClass =
            "fa-solid fa-triangle-exclamation";
    }

    if (type === "recommendation") {

        iconClass =
            "fa-solid fa-lightbulb";
    }

    if (type === "strength") {

        iconClass =
            "fa-solid fa-circle-check";
    }

    const countHtml =
        count > 1
            ? `
                <span class="recommendation-count">
                    Mentioned ${count} times
                </span>
              `
            : "";

    return `
        <div class="recommendation-item">

            <div class="recommendation-icon ${type}">
                <i class="${iconClass}"></i>
            </div>

            <div class="recommendation-content">

                <div class="recommendation-text">
                    ${text}
                </div>

                ${countHtml}

            </div>

        </div>
    `;
}

// Create an empty-state message
function createEmptyMessage(message) {

    return `
        <div class="recommendation-empty">
            ${escapeHtml(message)}
        </div>
    `;
}

// Update page state
function showState(state) {

    const loading =
        document.getElementById(
            "loadingState"
        );

    const error =
        document.getElementById(
            "errorState"
        );

    const empty =
        document.getElementById(
            "emptyState"
        );

    const data =
        document.getElementById(
            "recommendationsData"
        );

    loading.classList.add("hidden");
    error.classList.add("hidden");
    empty.classList.add("hidden");
    data.classList.add("hidden");

    if (state === "loading") {

        loading.classList.remove("hidden");

    } else if (state === "error") {

        error.classList.remove("hidden");

    } else if (state === "empty") {

        empty.classList.remove("hidden");

    } else if (state === "data") {

        data.classList.remove("hidden");
    }
}

// Escape user/API data before inserting it into HTML
function escapeHtml(value) {

    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}