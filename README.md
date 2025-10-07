# CodeCollab: A Real-Time Collaborative Code Editor 🚀

CodeCollab is a full-stack, microservices-based web application that provides a real-time, collaborative coding environment, similar to tools like CoderPad. It's designed for pair programming, technical interviews, and collaborative learning, featuring a live code editor, chat, secure code execution, and AI-powered assistance.

## Key Features ✨

* **Real-Time Collaboration:** Multiple users can type in the same editor simultaneously, with changes reflected instantly for everyone.
* **Microservices Architecture:** Built with 7 independent Spring Boot services (Discovery, Gateway, User, Session, Collaboration, Execution, AI) for scalability and resilience.
* **Secure Code Execution:** User-submitted code is executed in isolated Docker containers, preventing any harm to the host system.
* **AI-Powered Assistance:** Integrated with the Google Gemini API to provide on-demand explanations of selected code snippets.
* **Full Authentication & Authorization:** Secure JWT-based authentication and a granular permission system (Owner, Editor, Viewer).
* **Advanced Owner Controls:** Session owners can create private sessions, block users, and manage participant roles.
* **Join Request System:** Users can request to join private sessions, and the owner receives a real-time notification to approve or deny access.
* **Session History & Snapshots:** Owners can save snapshots of the code at any point and revert the session to a previous version.
* **Live Chat:** A real-time chat panel for users within a session to communicate.

## Architecture Overview



[Image of a microservices architecture diagram]


The application is built on a modern microservices architecture using Spring Cloud for service discovery and routing. The frontend is a single-page application built with React. Real-time communication is handled via WebSockets (STOMP), and secure code execution is achieved by dynamically creating and destroying Docker containers.

## Tech Stack 🛠️

| Area      | Technologies                                                                 |
| :-------- | :--------------------------------------------------------------------------- |
| **Backend** | Java 21, Spring Boot, Spring Cloud (Gateway, Eureka), Spring Security, Spring Data JPA, WebSockets, Docker, Maven, PostgreSQL |
| **Frontend**| React, Vite, React Bootstrap, SockJS & Stomp.js, Monaco Editor, JWT-Decode   |
| **AI** | Google Gemini API                                                            |

## Getting Started

### Prerequisites

* Java 21 JDK
* Apache Maven
* Docker Desktop (running)
* Node.js & npm
* PostgreSQL

### Local Setup

1.  **Backend:**
    * Navigate into the `codecollab-backend` directory.
    * Create the necessary PostgreSQL databases (`codecollab_users`, `codecollab_sessions`).
    * Update the database credentials in the `application.yml` files for `user-service` and `session-service`.
    * Run `mvn clean install` to build all microservices.
    * Start each of the 6 services (or run them from your IDE).

2.  **Frontend:**
    * Navigate into the `codecollab-frontend` directory.
    * Run `npm install` to install dependencies.
    * Run `npm run dev` to start the development server, usually on `http://localhost:5173`.
### Configuration

Before running, you must configure your secrets. For each service that has an `application-template.yml` file (`api-gateway`, `ai-service`, `user-service`, `session-service`):

1.  Navigate to its `src/main/resources` directory.
2.  Make a copy of `application-template.yml` and rename it to `application.yml`.
3.  Fill in your actual secrets (API Keys, DB passwords, etc.) in the new `application.yml` file.

The `application.yml` files are included in `.gitignore` and will not be committed to the repository.