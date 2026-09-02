# InterviewIQ -- AI-Powered Mock Interview Platform

A full-stack AI-powered mock interview platform built with Java and
Spring Boot. InterviewIQ provides JWT-based authentication, protected
interview workflows, timed interview sessions, answer submission,
AI-powered evaluation using Google Gemini, and performance tracking.

**Repository:** https://github.com/Waseeyurrahman/InterviewIQ\
**Deployment:** AWS EC2 + Amazon RDS (MySQL) + Docker + Nginx + HTTPS

------------------------------------------------------------------------

## Table of Contents

-   [Features](#features)
-   [Tech Stack](#tech-stack)
-   [Architecture](#architecture)
-   [API Modules](#api-modules)
-   [Interview Workflow](#interview-workflow)
-   [AI Evaluation Workflow](#ai-evaluation-workflow)
-   [Security](#security)
-   [Database Design](#database-design)
-   [Docker Deployment](#docker-deployment)
-   [AWS Deployment](#aws-deployment)
-   [Configuration](#configuration)
-   [Getting Started](#getting-started)
-   [Project Structure](#project-structure)
-   [API Documentation](#api-documentation)
-   [Future Improvements](#future-improvements)

------------------------------------------------------------------------

## Features

-   **JWT Authentication** --- User registration and login with
    stateless JWT-based authentication
-   **Spring Security** --- Protected REST endpoints and ownership-based
    authorization for interview resources
-   **Interview Creation** --- Create interviews based on role,
    experience level, difficulty, and duration
-   **Timed Interview Sessions** --- Start, conduct, and finish
    interview sessions with server-side interview lifecycle tracking
-   **Question Management** --- Retrieve questions associated with a
    specific interview
-   **Answer Submission** --- Submit and persist answers for interview
    questions
-   **AI Evaluation** --- Evaluate completed interviews using Google
    Gemini
-   **Structured Feedback** --- Evaluation includes score, relevance,
    confidence, filler words, strengths, weaknesses, and recommendations
-   **Performance Dashboard** --- View interview history and performance
    information
-   **Recommendations** --- Application includes recommendation
    functionality based on interview performance
-   **Centralized Error Handling** --- Application-specific exceptions
    and structured API responses
-   **Containerized Deployment** --- Multi-stage Docker build with a
    Java 17 runtime image
-   **AWS Deployment** --- Spring Boot application deployed on EC2 with
    MySQL hosted on RDS
-   **Reverse Proxy** --- Nginx handles public HTTP/HTTPS traffic and
    forwards requests to Spring Boot
-   **HTTPS** --- Let's Encrypt TLS certificate configured for the
    deployed application

------------------------------------------------------------------------

## Tech Stack

Layer              Technology
  ------------------ ----------------------------
Language           Java 17
Framework          Spring Boot 3.3.5
Security           Spring Security, JWT
ORM                Spring Data JPA, Hibernate
Database           MySQL
Frontend           HTML, CSS, JavaScript
AI                 Google Gemini API
Build Tool         Maven
Containerization   Docker
Cloud Compute      AWS EC2
Managed Database   AWS RDS
Reverse Proxy      Nginx
HTTPS              Let's Encrypt

------------------------------------------------------------------------

## Architecture

### High-Level Architecture

``` text
┌─────────────────────────────────────────────────────────────┐
│                         Browser                             │
│                  HTML / CSS / JavaScript                    │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTPS
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                         AWS EC2                             │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                       Nginx                           │  │
│  │             Reverse Proxy / HTTPS                     │  │
│  │                   Port 80 / 443                       │  │
│  └──────────────────────────┬────────────────────────────┘  │
│                             │                               │
│                             │ 127.0.0.1:8080                │
│                             ▼                               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                 Docker Container                      │  │
│  │                                                       │  │
│  │              Spring Boot Application                  │  │
│  │                     Port 8080                         │  │
│  └──────────────────────┬────────────────────────────────┘  │
└─────────────────────────┼───────────────────────────────────┘
                          │
              ┌───────────┴────────────┐
              │                        │
              ▼                        ▼
┌──────────────────────────┐  ┌──────────────────────────────┐
│       AWS RDS            │  │       Google Gemini API      │
│         MySQL            │  │      AI Evaluation Service   │
│        Port 3306         │  │                              │
└──────────────────────────┘  └──────────────────────────────┘
```

### Backend Architecture

``` text
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                  Security Layer                       │  │
│  │        SecurityConfig + JWT Filter + JWT Service      │  │
│  └──────────────────────────┬────────────────────────────┘  │
│                             ▼                               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                  Controller Layer                     │  │
│  │ Auth | Interview | Question | Answer | Evaluation     │  │
│  │ Profile | Dashboard | Recommendation                  │  │
│  └──────────────────────────┬────────────────────────────┘  │
│                             ▼                               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                    Service Layer                      │  │
│  │ Auth | Interview | Question | Answer | Evaluation     │  │
│  │ Dashboard | Profile | Recommendation | AI             │  │
│  └──────────────────────────┬────────────────────────────┘  │
│                             ▼                               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                  Repository Layer                     │  │
│  │                Spring Data JPA                        │  │
│  └──────────────────────────┬────────────────────────────┘  │
│                             ▼                               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                  Entity / Database                    │  │
│  │        User | Interview | Question | Answer           │  │
│  │                  | Evaluation                         │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Design:** - Controller → Service → Repository layered architecture -
DTOs used for request/response data transfer - Spring Data JPA and
Hibernate for persistence - Dedicated security layer for JWT
authentication - Dedicated AI service for Gemini integration - Resource
ownership checks for protected interview operations

------------------------------------------------------------------------

## API Modules

### Authentication --- `/auth`

Method   Endpoint           Description
  -------- ------------------ -----------------------------------
POST     `/auth/register`   Register a new user
POST     `/auth/login`      Authenticate user and receive JWT

### Interview --- `/interview`

  ----------------------------------------------------------------------------
Method                  Endpoint                     Description
  ----------------------- ---------------------------- -----------------------
POST                    `/interview/create`          Create a new interview

POST                    `/interview/{id}/start`      Start an interview

GET                     `/interview/{id}`            Get interview details

POST                    `/interview/{id}/finish`     Finish an interview

GET                     `/interview/my-interviews`   Get interviews
belonging to the
authenticated user
  ----------------------------------------------------------------------------

### Questions --- `/questions`

Method   Endpoint                     Description
  -------- ---------------------------- --------------------------------
GET      `/questions/{interviewId}`   Get questions for an interview

### Answers

The answer controller handles answer submission and retrieval for
interview questions.

### Evaluation --- `/interviews`

  ----------------------------------------------------------------------------------------
Method                  Endpoint                                 Description
  ----------------------- ---------------------------------------- -----------------------
POST                    `/interviews/{interviewId}/evaluate`     Evaluate an interview
using the AI evaluation
flow

GET                     `/interviews/{interviewId}/evaluation`   Retrieve evaluation
details
  ----------------------------------------------------------------------------------------

### Profile

Profile operations are handled through `ProfileController` and
`ProfileService`.

### Dashboard

Dashboard operations are handled through `DashboardController` and
`DashboardService`.

### Recommendations

Recommendation operations are handled through `RecommendationController`
and `RecommendationService`.

------------------------------------------------------------------------

## Interview Workflow

The main interview lifecycle is:

``` text
1. User logs in
        ↓
2. Frontend receives JWT
        ↓
3. User creates an interview
        ↓
4. Backend stores interview configuration
        ↓
5. User starts the interview
        ↓
6. Backend records the interview start state
        ↓
7. Frontend retrieves interview questions
        ↓
8. User submits answers
        ↓
9. Answers are persisted
        ↓
10. User finishes the interview
        ↓
11. Evaluation endpoint is triggered
        ↓
12. AI evaluation is generated
        ↓
13. Evaluation is processed and stored
        ↓
14. Dashboard displays interview performance
```

### Interview Creation

An interview is created using parameters including:

-   Role
-   Experience level
-   Difficulty
-   Duration

The authenticated user's identity is obtained from the security context
and associated with the interview.

### Interview Ownership

Protected interview operations verify that the authenticated user owns
the requested interview before allowing the operation.

------------------------------------------------------------------------

## AI Evaluation Workflow

InterviewIQ integrates Google Gemini through a dedicated `AIService`.

``` text
User submits interview answers
             ↓
       EvaluationController
             ↓
       EvaluationService
             ↓
           AIService
             ↓
      Google Gemini API
             ↓
      AI-generated result
             ↓
     Response validation /
       result processing
             ↓
       EvaluationService
             ↓
       Persist evaluation
             ↓
      Return evaluation data
             ↓
          Dashboard
```

### Evaluation Data

The evaluation response can include:

Metric               Description
  -------------------- -------------------------------------
Score                Overall interview score
Filler Words         Detected filler-word information
Confidence           Confidence-related evaluation
Relevance            Relevance of submitted answers
Strengths            Identified strengths
Weaknesses           Identified weaknesses
Recommendations      Suggested areas for improvement
Total Questions      Number of interview questions
Answered Questions   Number of answered questions
Skipped Questions    Number of skipped questions
AI Availability      Whether AI evaluation was available

The AI integration is isolated in `AIService`, while `EvaluationService`
handles the interview-level evaluation workflow and persistence.

------------------------------------------------------------------------

## Security

### Authentication Flow

``` text
POST /auth/login
        ↓
Validate credentials
        ↓
Authenticate user
        ↓
Generate JWT
        ↓
Return JWT to frontend
```

For protected requests:

``` text
Frontend
   │
   │ Authorization: Bearer <JWT>
   ▼
JwtAuthenticationFilter
   │
   ├── Extract token
   ├── Validate token
   └── Authenticate request
            │
            ▼
      Spring Security
            │
            ▼
       Controller
```

### Security Implementation

-   Spring Security
-   JWT-based stateless authentication
-   `JwtAuthenticationFilter`
-   `JwtService`
-   `CustomUserDetailsService`
-   Password hashing through the application's authentication
    configuration
-   `@PreAuthorize` for endpoint authorization
-   Interview ownership checks through `InterviewSecurity`
-   Protected user-specific interview resources
-   Secrets supplied through environment variables
-   RDS database kept private through AWS Security Groups
-   Spring Boot port 8080 not publicly accessible through the EC2
    Security Group

------------------------------------------------------------------------

## Database Design

The application uses MySQL with Spring Data JPA and Hibernate.

The main domain entities are:

``` text
User
 |
 +----< Interview
          |
          +----< Question
          |
          +----< Answer
          |
          +----< Evaluation
```

### Main Relationships

``` text
User
 └── OneToMany → Interview

Interview
 ├── ManyToOne → User
 ├── OneToMany → Question
 ├── OneToMany → Answer
 └── OneToMany → Evaluation

Question
 └── OneToMany → Answer

Answer
 └── Linked to Interview / Question / User context

Evaluation
 └── Linked to Interview / Answer evaluation data
```

The exact relational mappings are implemented through JPA entity
relationships in the `entity` package.

------------------------------------------------------------------------

## Docker Deployment

InterviewIQ uses a multi-stage Docker build.

### Build Stage

``` text
Maven + Java 17
       ↓
Compile Spring Boot project
       ↓
Generate executable JAR
```

### Runtime Stage

``` text
Java 17 JRE
       ↓
Copy generated JAR
       ↓
Run Spring Boot application
```

This keeps Maven/build tooling out of the final runtime image.

### Build Image

``` bash
docker build -t interviewiq:latest .
```

### Run Container

``` bash
docker run -d \
  --name interviewiq-app \
  --restart unless-stopped \
  --env-file .env \
  -p 8080:8080 \
  interviewiq:latest
```

### Useful Docker Commands

``` bash
docker ps
docker ps -a
docker images
docker logs interviewiq-app
docker logs -f interviewiq-app
docker restart interviewiq-app
docker stop interviewiq-app
docker start interviewiq-app
docker rm interviewiq-app
docker inspect interviewiq-app
```

Check restart policy:

``` bash
docker inspect -f '{{.HostConfig.RestartPolicy.Name}}' interviewiq-app
```

Expected:

``` text
unless-stopped
```

------------------------------------------------------------------------

## AWS Deployment

InterviewIQ is deployed using AWS EC2 and Amazon RDS.

### Deployment Architecture

``` text
                         Internet
                            │
                     HTTP / HTTPS
                            │
                            ▼
                    ┌───────────────┐
                    │   AWS EC2     │
                    │    Ubuntu     │
                    └───────┬───────┘
                            │
                            ▼
                    ┌───────────────┐
                    │     Nginx     │
                    │   :80 / :443 │
                    └───────┬───────┘
                            │
                     127.0.0.1:8080
                            │
                            ▼
                    ┌───────────────┐
                    │ Docker        │
                    │ Spring Boot   │
                    │    :8080      │
                    └───────┬───────┘
                            │
                    ┌───────┴────────┐
                    │                │
                    ▼                ▼
              AWS RDS MySQL     Gemini API
                 :3306
```

### EC2

The EC2 instance runs:

-   Ubuntu
-   Docker
-   InterviewIQ container
-   Nginx
-   Certbot / Let's Encrypt configuration

### RDS

MySQL is hosted on Amazon RDS.

The RDS instance is not publicly accessible. Its security group allows
MySQL traffic only from the EC2 application's security group.

### Security Groups

EC2:

Port   Source                   Purpose
  ------ ------------------------ -------------------------------
22     Developer IP `/32`       SSH
80     `0.0.0.0/0`              HTTP / certificate validation
443    `0.0.0.0/0`              HTTPS
8080   No public inbound rule   Internal application port

RDS:

Port   Source               Purpose
  ------ -------------------- ---------
3306   EC2 Security Group   MySQL

This prevents direct public access to the Spring Boot application and
database.

------------------------------------------------------------------------

## Nginx and HTTPS

Nginx is used as a reverse proxy.

``` text
Client
  │
  │ HTTPS :443
  ▼
Nginx
  │
  │ http://127.0.0.1:8080
  ▼
Docker Container
  │
  ▼
Spring Boot
```

HTTP traffic is redirected to HTTPS.

### Useful Nginx Commands

``` bash
sudo nginx -t
sudo systemctl status nginx
sudo systemctl reload nginx
sudo systemctl restart nginx
```

### Certificate Management

``` bash
certbot --version
sudo certbot certificates
sudo certbot renew --dry-run
```

The deployed application uses a Let's Encrypt TLS certificate.

------------------------------------------------------------------------

## Configuration

Production configuration is supplied through environment variables.

Example:

``` text
SPRING_DATASOURCE_URL=...
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...

JWT_SECRET=...
JWT_EXPIRATION=3600000

AI_GEMINI_API_KEY=...
AI_GEMINI_MODEL=gemini-3.5-flash-lite

SERVER_PORT=8080

SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_JPA_SHOW_SQL=false
SPRING_JPA_PROPERTIES_HIBERNATE_JDBC_TIME_ZONE=Asia/Kolkata
```

### Secrets

Never commit real values for:

-   Database username/password
-   JWT secret
-   Gemini API key
-   Production environment files

The production `.env` file is kept outside Git tracking.

------------------------------------------------------------------------

## Getting Started

### Prerequisites

-   Java 17+
-   Maven
-   MySQL
-   Google Gemini API key
-   Git

### Clone the Repository

``` bash
git clone https://github.com/Waseeyurrahman/InterviewIQ.git
cd InterviewIQ
```

### Configure the Database

Create a MySQL database named:

``` text
interview_starter
```

Configure the required database environment variables.

### Configure Gemini

Provide your Gemini API key through:

``` text
AI_GEMINI_API_KEY=your_api_key
```

Do not place the real API key directly in the repository.

### Run with Maven

Linux/macOS:

``` bash
./mvnw spring-boot:run
```

Windows:

``` bash
mvnw.cmd spring-boot:run
```

### Build the Application

``` bash
./mvnw clean package
```

------------------------------------------------------------------------

## Project Structure

``` text
InterviewIQ/
│
├── .mvn/
│   └── wrapper/
│
├── docs/
│   ├── images/
│   └── ...
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/interviewiq/interviewstarter/
│   │   │       ├── config/
│   │   │       │   ├── JwtAuthenticationFilter.java
│   │   │       │   └── SecurityConfig.java
│   │   │       │
│   │   │       ├── controller/
│   │   │       │   ├── AnswerController.java
│   │   │       │   ├── AuthController.java
│   │   │       │   ├── DashboardController.java
│   │   │       │   ├── EvaluationController.java
│   │   │       │   ├── Interviewcontroller.java
│   │   │       │   ├── ProfileController.java
│   │   │       │   ├── QuestionController.java
│   │   │       │   └── RecommendationController.java
│   │   │       │
│   │   │       ├── dto/
│   │   │       ├── entity/
│   │   │       ├── exception/
│   │   │       ├── repository/
│   │   │       ├── security/
│   │   │       └── service/
│   │   │           ├── AIService.java
│   │   │           ├── AnswerService.java
│   │   │           ├── AuthSerivce.java
│   │   │           ├── DashboardService.java
│   │   │           ├── EvaluationService.java
│   │   │           ├── InterviewService.java
│   │   │           ├── JwtService.java
│   │   │           ├── ProfileService.java
│   │   │           ├── QuestionService.java
│   │   │           └── RecommendationService.java
│   │   │
│   │   └── resources/
│   │       └── static/
│   │           ├── HTML
│   │           ├── CSS
│   │           └── JavaScript
│   │
│   └── test/
│
├── .dockerignore
├── .gitignore
├── Dockerfile
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

------------------------------------------------------------------------

## API Documentation

Detailed API documentation is available in the `docs/` directory.

The API is organized around:

-   Authentication
-   Interviews
-   Questions
-   Answers
-   Evaluations
-   Profile
-   Dashboard
-   Recommendations

------------------------------------------------------------------------

## Future Improvements

-   Automated CI/CD deployment
-   More advanced interview analytics
-   Additional interview categories
-   Improved AI evaluation criteria
-   More detailed performance comparison
-   Better monitoring and observability
-   Automated deployment rollback
-   Expanded test coverage

------------------------------------------------------------------------

## Author

**Waseeyur Rahman**

GitHub: https://github.com/Waseeyurrahman/InterviewIQ
