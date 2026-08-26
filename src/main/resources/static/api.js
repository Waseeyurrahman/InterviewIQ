/* ============================================
   Backend API Client
   ============================================ */

const API_BASE = ""; // Same origin

/*
 * Get JWT authentication headers.
 */
function authHeaders() {

    const token = localStorage.getItem("token");

    if (!token) {
        throw new Error(
            "Authentication required. Please login again."
        );
    }

    return {
        "Content-Type": "application/json",
        "Authorization": "Bearer " + token
    };
}


/*
 * ============================================
 * GET QUESTIONS
 * ============================================
 *
 * Backend:
 * GET /questions/{interviewId}
 *
 * The backend decides the number of questions
 * based on interview duration:
 *
 * 10 minutes -> 5 questions
 * 20 minutes -> 7 questions
 * 30 minutes -> 10 questions
 */
async function generateInterview() {

    const interviewId =
        localStorage.getItem("interviewId");

    if (!interviewId) {
        throw new Error(
            "No interviewId in localStorage. " +
            "Did /interview/create run successfully?"
        );
    }

    const token =
        localStorage.getItem("token");

    if (!token) {
        throw new Error(
            "Authentication required. Please login again."
        );
    }


   console.log("[api.js] interviewId:", interviewId);
   console.log("[api.js] token exists:", !!token);

   const response =
       await fetch(
           API_BASE + "/questions/" + interviewId,
           {
               method: "GET",
               headers: {
                   "Authorization": "Bearer " + token
               }
           }
       );

   console.log(
       "[api.js] /questions status:",
       response.status
   );


    /*
     * JWT expired / invalid
     */
    if (response.status === 401) {

        localStorage.removeItem("token");

        window.location.href =
            "login.html";

        return;
    }


    /*
     * Forbidden
     */
    if (response.status === 403) {

        throw new Error(
            "You are not authorized to access these questions."
        );
    }


    /*
     * Other errors
     */
    if (!response.ok) {

        throw new Error(
            "Failed to load questions: HTTP " +
            response.status
        );
    }


    const rows =
        await response.json();


    /*
     * Convert backend Question objects
     * into the format used by live-interview.html.
     *
     * Backend:
     * {
     *   id,
     *   interviewId,
     *   questionText
     * }
     *
     * Frontend:
     * {
     *   id,
     *   question
     * }
     */
    const questions =
        rows.map(row => ({
            id: row.id,
            question: row.questionText
        }));


    console.log(
        "[api.js] Questions received:",
        questions.length
    );


    return questions;
}



async function startInterview() {

    const interviewId =
        localStorage.getItem("interviewId");

    if (!interviewId) {
        throw new Error("No interviewId found.");
    }

    const response = await fetch(
        "/interview/" + interviewId + "/start",
        {
            method: "POST",
            headers: authHeaders()
        }
    );

    if (!response.ok) {
        throw new Error(
            "Failed to start interview: HTTP " +
            response.status
        );
    }

    return response.json();
}


/*
 * ============================================
 * SAVE ANSWERS
 * ============================================
 *
 * POST /answers
 */
async function submitAnswersToBackend(answers) {

    const response =
        await fetch(
            API_BASE + "/answers",
            {
                method: "POST",
                headers: authHeaders(),
                body: JSON.stringify({
                    answers: answers.map(answer => ({
                        questionId:
                            answer.questionId,

                        answerText:
                            answer.answer
                    }))
                })
            }
        );


    /*
     * JWT expired
     */
    if (response.status === 401) {

        localStorage.removeItem("token");

        window.location.href =
            "login.html";

        return;
    }


    /*
     * Forbidden
     */
    if (response.status === 403) {

        throw new Error(
            "You are not authorized to save these answers."
        );
    }


    if (!response.ok) {

        throw new Error(
            "Failed to save answers: HTTP " +
            response.status
        );
    }


    return response.json();
}


/*
 * ============================================
 * EVALUATE INTERVIEW
 * ============================================
 *
 * POST /evaluate-answer
 */
async function submitInterview() {

    const interviewId =
        localStorage.getItem("interviewId");

    if (!interviewId) {
        throw new Error(
            "No interviewId found."
        );
    }

    const response =
        await fetch(
            API_BASE +
            "/interviews/" +
            interviewId +
            "/evaluate",
            {
                method: "POST",
                headers: authHeaders()
            }
        );


    /*
     * JWT expired
     */
    if (response.status === 401) {

        localStorage.removeItem("token");

        window.location.href =
            "login.html";

        return;
    }


    /*
     * Forbidden
     */
    if (response.status === 403) {

        throw new Error(
            "You are not authorized to evaluate this interview."
        );
    }


    /*
     * Other errors
     */
    if (!response.ok) {

        throw new Error(
            "Failed to evaluate interview: HTTP " +
            response.status
        );
    }


    const data =
        await response.json();


    /*
     * Convert backend response
     * into frontend format.
     */
     return {

            score:
                data.score,

            strengths:
                data.strengths || [],

            weaknesses:
                data.weaknesses || [],

            suggestions:
                data.recommendations || [],

            fillerWords:
                data.fillerWords ?? 0,

            confidence:
                data.confidence ?? 0,

            relevance:
                data.relevance || "medium",

            answeredCount:
                data.strengths ||
                data.weaknesses
                    ? undefined
                    : 0
        };
}