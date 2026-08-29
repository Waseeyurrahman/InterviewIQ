// Authentication
function getToken() {
    const token = localStorage.getItem("token");

    if (!token) {
        window.location.replace("login.html");
        throw new Error("Authentication required.");
    }

    return token;
}

// Load interviews
async function loadInterviews() {

    const loading =
        document.getElementById("loadingState");

    const error =
        document.getElementById("errorState");

    const empty =
        document.getElementById("emptyState");

    const data =
        document.getElementById("interviewsData");

    loading.classList.remove("hidden");
    error.classList.add("hidden");
    empty.classList.add("hidden");
    data.classList.add("hidden");

    try {

        const token = getToken();

        const response = await fetch(
            "/interview/my-interviews",
            {
                method: "GET",
                headers: {
                    "Authorization": "Bearer " + token
                }
            }
        );

        if (response.status === 401) {

            localStorage.removeItem("token");
            localStorage.removeItem("interviewId");

            window.location.replace("login.html");

            return;
        }

        if (response.status === 403) {

            throw new Error(
                "You are not authorized to view these interviews."
            );
        }

        if (!response.ok) {

            throw new Error(
                "Failed to load interviews. HTTP " +
                response.status
            );
        }

        const interviews =
            await response.json();

        loading.classList.add("hidden");

        if (
            !Array.isArray(interviews) ||
            interviews.length === 0
        ) {

            empty.classList.remove("hidden");

            return;
        }

        renderInterviews(interviews);

        data.classList.remove("hidden");

    } catch (err) {

        console.error(
            "[my-interviews] Error:",
            err
        );

        loading.classList.add("hidden");

        document.getElementById("errorMsg").textContent =
            err.message ||
            "Something went wrong.";

        error.classList.remove("hidden");
    }
}

// Render interview cards
function renderInterviews(interviews) {

    const container =
        document.getElementById("interviewsList");

    const sortedInterviews =
        interviews
            .slice()
            .sort(
                (a, b) =>
                    Number(b.interviewId) -
                    Number(a.interviewId)
            );

    container.innerHTML =
        sortedInterviews
            .map(createInterviewCard)
            .join("");
}

// Create an interview card
function createInterviewCard(interview) {

    const evaluated =
        interview.status === "EVALUATED" &&
        interview.score !== null &&
        interview.score !== undefined;

    const score =
        evaluated
            ? `${interview.score}/100`
            : "—";

    const date =
        formatDate(
            interview.completedAt ||
            interview.startedAt
        );

    let action;

    if (evaluated) {

        action = `
            <button
                class="interview-action"
                onclick="viewResult(${interview.interviewId})">

                <i class="fa-solid fa-eye"></i>
                <span>View Result</span>

            </button>
        `;

    } else if (interview.status === "IN_PROGRESS") {

        action = `
            <button
                class="interview-action"
                onclick="continueInterview(${interview.interviewId})">

                <i class="fa-solid fa-play"></i>
                <span>Continue</span>

            </button>
        `;

    } else if (interview.status === "CREATED") {

        action = `
            <button
                class="interview-action"
                onclick="startInterviewFromHistory(${interview.interviewId})">

                <i class="fa-solid fa-play"></i>
                <span>Start</span>

            </button>
        `;

    } else {

        action = `
            <span class="interview-status-text">
                ${escapeHtml(interview.status || "UNKNOWN")}
            </span>
        `;
    }

    return `
        <article class="my-interview-card">

            <div class="interview-card-main">

                <div class="interview-card-title">

                    <h3>
                        ${escapeHtml(
                            interview.role ||
                            "Interview"
                        )}
                    </h3>

                    <span
                        class="status-badge ${statusClass(
                            interview.status
                        )}">

                        ${escapeHtml(
                            interview.status ||
                            "UNKNOWN"
                        )}

                    </span>

                </div>

                <div class="interview-meta">

                    <span>
                        <i class="fa-solid fa-briefcase"></i>

                        ${escapeHtml(
                            interview.experienceLevel ||
                            "Not specified"
                        )}
                    </span>

                    <span>
                        <i class="fa-solid fa-bullseye"></i>

                        ${escapeHtml(
                            interview.difficulty ||
                            "Not specified"
                        )}
                    </span>

                    <span>
                        <i class="fa-regular fa-clock"></i>

                        ${interview.duration ?? "—"} min
                    </span>

                    <span>
                        <i class="fa-regular fa-calendar-days"></i>

                        ${date}
                    </span>

                </div>

            </div>

            <div class="interview-card-score">

                <div class="score-label">
                    Score
                </div>

                <div class="score-value">
                    ${score}
                </div>

                ${action}

            </div>

        </article>
    `;
}

// Interview actions
function viewResult(interviewId) {

    window.location.href =
        "result.html?interviewId=" +
        encodeURIComponent(interviewId);
}

function continueInterview(interviewId) {

    window.location.href =
        "live-interview.html?interviewId=" +
        encodeURIComponent(interviewId);
}

function startInterviewFromHistory(interviewId) {

    window.location.href =
        "live-interview.html?interviewId=" +
        encodeURIComponent(interviewId);
}

// Interview status classes
function statusClass(status) {

    switch (status) {

        case "EVALUATED":
            return "status-evaluated";

        case "IN_PROGRESS":
            return "status-progress";

        case "COMPLETED":
            return "status-completed";

        case "CREATED":
            return "status-created";

        default:
            return "status-default";
    }
}

// Format date
function formatDate(value) {

    if (!value) {
        return "Not started";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return "Unknown date";
    }

    return date.toLocaleDateString(
        undefined,
        {
            month: "short",
            day: "numeric",
            year: "numeric"
        }
    );
}

// Escape HTML
function escapeHtml(value) {

    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

document.addEventListener(
    "DOMContentLoaded",
    loadInterviews
);