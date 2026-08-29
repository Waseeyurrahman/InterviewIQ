const API_BASE = "";

// Authentication
function getToken() {
    const token = localStorage.getItem("token");

    if (!token) {
        throw new Error(
            "Authentication required. Please login again."
        );
    }

    return token;
}

function authHeaders() {
    return {
        "Content-Type": "application/json",
        "Authorization": "Bearer " + getToken()
    };
}

// Handle API responses
async function parseResponse(response) {
    let data = null;

    try {
        data = await response.json();
    } catch (_) {
        // Response may not contain JSON.
    }

    if (response.status === 401) {
        localStorage.removeItem("token");
        window.location.href = "login.html";

        throw new Error(
            "Your session has expired. Please login again."
        );
    }

    if (response.status === 403) {
        throw new Error(
            "You are not authorized to perform this action."
        );
    }

    if (!response.ok) {
        throw new Error(
            data?.message ||
            `Request failed: HTTP ${response.status}`
        );
    }

    return data;
}

// Get interview ID from URL
function getInterviewId() {
    const params =
        new URLSearchParams(window.location.search);

    const interviewId =
        params.get("interviewId");

    if (!interviewId) {
        throw new Error(
            "No interview ID was provided."
        );
    }

    return interviewId;
}

// Get interview
async function getInterview(interviewId) {
    const response = await fetch(
        API_BASE +
        "/interview/" +
        encodeURIComponent(interviewId),
        {
            method: "GET",
            headers: {
                "Authorization":
                    "Bearer " + getToken()
            }
        }
    );

    return parseResponse(response);
}

// Create interview
async function createInterview(request) {
    const response = await fetch(
        API_BASE + "/interview/create",
        {
            method: "POST",
            headers: authHeaders(),
            body: JSON.stringify(request)
        }
    );

    return parseResponse(response);
}

// Start interview
async function startInterview(interviewId) {
    const response = await fetch(
        API_BASE +
        "/interview/" +
        encodeURIComponent(interviewId) +
        "/start",
        {
            method: "POST",
            headers: authHeaders()
        }
    );

    return parseResponse(response);
}

// Get interview questions
async function getQuestions(interviewId) {
    const response = await fetch(
        API_BASE +
        "/questions/" +
        encodeURIComponent(interviewId),
        {
            method: "GET",
            headers: {
                "Authorization":
                    "Bearer " + getToken()
            }
        }
    );

    const rows = await parseResponse(response);

    if (!Array.isArray(rows)) {
        throw new Error(
            "Invalid question response from server."
        );
    }

    return rows.map(row => ({
        id: Number(row.id),
        question: row.questionText
    }));
}

// Save or update one answer
async function saveAnswerToServer(
    questionId,
    answerText
) {
    if (!questionId) {
        throw new Error(
            "Question ID is required."
        );
    }

    if (!answerText || !answerText.trim()) {
        throw new Error(
            "Answer cannot be empty."
        );
    }

    const response = await fetch(
        API_BASE +
        "/answers/" +
        encodeURIComponent(questionId),
        {
            method: "POST",
            headers: authHeaders(),
            body: JSON.stringify({
                answerText: answerText.trim()
            })
        }
    );

    return parseResponse(response);
}

// Get saved answers
async function getAnswers(interviewId) {
    const response = await fetch(
        API_BASE +
        "/answers/interview/" +
        encodeURIComponent(interviewId),
        {
            method: "GET",
            headers: {
                "Authorization":
                    "Bearer " + getToken()
            }
        }
    );

    const data = await parseResponse(response);

    if (!Array.isArray(data)) {
        throw new Error(
            "Invalid saved answers response from server."
        );
    }

    return data.map(answer => ({
        questionId: Number(answer.questionId),
        answerText: answer.answerText || ""
    }));
}

// Finish interview
async function finishInterview(interviewId) {
    const response = await fetch(
        API_BASE +
        "/interview/" +
        encodeURIComponent(interviewId) +
        "/finish",
        {
            method: "POST",
            headers: authHeaders()
        }
    );

    return parseResponse(response);
}

// Evaluate interview
async function evaluateInterview(interviewId) {
    const response = await fetch(
        API_BASE +
        "/interviews/" +
        encodeURIComponent(interviewId) +
        "/evaluate",
        {
            method: "POST",
            headers: authHeaders()
        }
    );

    return parseResponse(response);
}

// Get detailed evaluation
async function getEvaluation(interviewId) {
    const response = await fetch(
        API_BASE +
        "/interviews/" +
        encodeURIComponent(interviewId) +
        "/evaluation",
        {
            method: "GET",
            headers: {
                "Authorization":
                    "Bearer " + getToken()
            }
        }
    );

    return parseResponse(response);
}