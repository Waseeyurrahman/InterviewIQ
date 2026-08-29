const $ = id => document.getElementById(id);

const esc = value =>
    String(value ?? "").replace(
        /[&<>"']/g,
        char => ({
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#39;"
        }[char])
    );

const fmt = (iso, options) => {
    const date = new Date(iso);

    return isNaN(date)
        ? (iso || "—")
        : date.toLocaleDateString("en-US", options);
};

const show = state => {
    [
        ["loadingState", "loading"],
        ["errorState", "error"],
        ["dashboardData", "ready"]
    ].forEach(([id, expectedState]) => {
        $(id).classList.toggle(
            "hidden",
            state !== expectedState
        );
    });
};

document.addEventListener("DOMContentLoaded", load);

// Load dashboard data
function load() {

    show("loading");

    const token = localStorage.getItem("token");

    if (!token) {
        window.location.replace("login.html");
        return;
    }

    fetch("/api/dashboard", {
        method: "GET",
        headers: {
            "Authorization": "Bearer " + token,
            "Content-Type": "application/json"
        }
    })
        .then(response => {

            if (response.status === 401) {
                localStorage.removeItem("token");
                window.location.replace("login.html");
                return null;
            }

            if (!response.ok) {
                throw new Error(
                    "HTTP " + response.status
                );
            }

            return response.json();
        })
        .then(data => {

            if (!data) {
                return;
            }

            console.log(
                "[dashboard] API response:",
                data
            );

            render(data);
            show("ready");
        })
        .catch(error => {

            console.error(
                "[dashboard]",
                error
            );

            $("errorMsg").textContent =
                "Couldn't reach the server. " +
                error.message;

            show("error");
        });
}

// Render dashboard
function render(data) {

    $("statTotal").textContent =
        data.totalInterviews ?? 0;

    $("statAvg").textContent =
        Math.round(data.averageScore ?? 0) + "%";

    $("statBest").textContent =
        (data.bestScore ?? 0) + "%";

    $("statTime").textContent =
        data.totalPracticeTime || "0m";

    lineChart(data.scoreTrend || []);
    weaknesses(data.weakAreas || []);
    table(data.recentInterviews || []);
    strengths(data.strengths || []);
}

// Render score trend
function lineChart(trend) {

    const wrap = $("lineChart");

    if (!trend.length) {

        wrap.innerHTML =
            '<p style="color:#9CA3AF;font-size:12px">' +
            'No score data yet.' +
            '</p>';

        return;
    }

    const W = 700;
    const H = 200;

    const pX = 50;
    const pY = 40;

    const iw = W - pX * 2;
    const ih = H - pY * 2;

    const step =
        trend.length > 1
            ? iw / (trend.length - 1)
            : 0;

    const pts = trend.map((point, index) => ({
        x: pX + step * index,

        y:
            pY +
            (1 - (point.score || 0) / 100) * ih,

        label: fmt(point.date, {
            month: "short",
            day: "numeric"
        }),

        score: point.score
    }));

    const poly =
        pts
            .map(point =>
                `${point.x},${point.y}`
            )
            .join(" ");

    const area =
        `M ${pts[0].x},${pts[0].y} ` +
        pts
            .slice(1)
            .map(point =>
                `L ${point.x},${point.y}`
            )
            .join(" ") +
        ` L ${pts.at(-1).x},${H - 20}` +
        ` L ${pts[0].x},${H - 20} Z`;

    const dots =
        pts
            .map(point => `
                <circle
                    cx="${point.x}"
                    cy="${point.y}"
                    r="4.5"
                    fill="white"
                    stroke="#6C63FF"
                    stroke-width="2.5">
                    <title>
                        ${esc(point.label)}: ${point.score}%
                    </title>
                </circle>
            `)
            .join("");

    const labels =
        pts
            .map(point => `
                <text
                    x="${point.x}"
                    y="${H - 5}"
                    fill="#9CA3AF"
                    font-size="11"
                    text-anchor="middle">
                    ${esc(point.label)}
                </text>
            `)
            .join("");

    wrap.innerHTML = `
        <svg
            viewBox="0 0 ${W} ${H}"
            preserveAspectRatio="none"
            aria-label="Interview score trend">

            <defs>
                <linearGradient
                    id="areaGrad"
                    x1="0"
                    y1="0"
                    x2="0"
                    y2="1">

                    <stop
                        offset="0%"
                        stop-color="#6C63FF"
                        stop-opacity="0.6"/>

                    <stop
                        offset="100%"
                        stop-color="#6C63FF"
                        stop-opacity="0"/>

                </linearGradient>
            </defs>

            <g stroke="#F1F2F6">

                <line
                    x1="0"
                    y1="${pY}"
                    x2="${W}"
                    y2="${pY}"/>

                <line
                    x1="0"
                    y1="${pY + ih / 2}"
                    x2="${W}"
                    y2="${pY + ih / 2}"/>

                <line
                    x1="0"
                    y1="${pY + ih}"
                    x2="${W}"
                    y2="${pY + ih}"/>

            </g>

            <path
                d="${area}"
                fill="url(#areaGrad)"
                opacity="0.35"/>

            <polyline
                points="${poly}"
                fill="none"
                stroke="#6C63FF"
                stroke-width="3"
                stroke-linecap="round"
                stroke-linejoin="round"/>

            <g>
                ${dots}
            </g>

            <g font-family="Poppins">
                ${labels}
            </g>

        </svg>
    `;
}

// Render weaknesses
function weaknesses(items) {

    const container = $("weaknessList");

    if (!items.length) {

        container.innerHTML = `
            <div class="no-weaknesses">
                No weaknesses identified yet.
            </div>
        `;

        return;
    }

    container.innerHTML =
        items
            .map((item, index) => {

                const text =
                    typeof item === "string"
                        ? item
                        : item.text;

                return `
                    <div class="weakness-item">

                        <span class="weakness-number">
                            ${index + 1}
                        </span>

                        <span class="weakness-text">
                            ${esc(text)}
                        </span>

                    </div>
                `;
            })
            .join("");
}

// Render recent interviews
function table(rows) {

    const tbody = $("recentTbody");

    if (!rows.length) {

        tbody.innerHTML = `
            <tr>
                <td
                    colspan="6"
                    style="
                        text-align:center;
                        color:#9CA3AF;
                        padding:18px;
                    ">
                    No interviews yet.
                    Click <b>Start Interview</b> above!
                </td>
            </tr>
        `;

        return;
    }

    const badge = status => {

        if (status === "Completed") {
            return "green";
        }

        if (status === "Needs Review") {
            return "amber";
        }

        return "purple";
    };

    tbody.innerHTML =
        rows
            .map(row => {

                const interviewId =
                    row.interviewId;

                return `
                    <tr
                        class="interview-row"
                        ${interviewId
                            ? `onclick="viewInterview(${interviewId})"`
                            : ""
                        }>

                        <td>
                            ${esc(row.role)}
                        </td>

                        <td>
                            ${esc(
                                row.experienceLevel ||
                                "Not specified"
                            )}
                        </td>

                        <td>
                            <span class="difficulty-badge">
                                ${esc(
                                    row.difficulty ||
                                    "Not specified"
                                )}
                            </span>
                        </td>

                        <td>
                            ${fmt(row.date, {
                                month: "short",
                                day: "2-digit"
                            })}
                        </td>

                        <td>
                            <b>${row.score}%</b>
                        </td>

                        <td>
                            <span class="badge ${badge(row.status)}">
                                ${esc(row.status)}
                            </span>
                        </td>

                    </tr>
                `;
            })
            .join("");
}

// Open interview result
function viewInterview(interviewId) {

    if (!interviewId) {
        return;
    }

    window.location.href =
        "result.html?interviewId=" +
        encodeURIComponent(interviewId);
}

// Render strengths
function strengths(items) {

    const list = $("strengthList");

    if (!items.length) {

        list.innerHTML = `
            <p style="
                color:#9CA3AF;
                font-size:12px;
            ">
                No strengths data yet.
            </p>
        `;

        return;
    }

    list.innerHTML =
        items
            .map(item => `
                <div class="strength-row">

                    <div class="top">

                        <span>
                            ${esc(item.name)}
                        </span>

                        <b>
                            ${item.value}%
                        </b>

                    </div>

                    <div class="strength-track">

                        <div
                            class="strength-fill"
                            data-w="${item.value}">
                        </div>

                    </div>

                </div>
            `)
            .join("");

    requestAnimationFrame(() => {

        list
            .querySelectorAll(".strength-fill")
            .forEach((element, index) => {

                setTimeout(() => {

                    element.style.width =
                        element.dataset.w + "%";

                }, 120 + index * 100);
            });
    });
}