# 🛡️ Secure-Schedule: Sandbox Optimization System

![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?logo=spring&logoColor=white)
![React](https://img.shields.io/badge/React-20232A?logo=react&logoColor=61DAFB)
![Vite](https://img.shields.io/badge/Vite-B73BFE?logo=vite&logoColor=FFD62E)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?logo=mysql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white)
![Hyper-V](https://img.shields.io/badge/Hyper--V-0078D4?logo=microsoft&logoColor=white)
![Security](https://img.shields.io/badge/Zero_Trust-JWT_%26_BCrypt-red?)

## 📖 About
**Secure-Schedule** is an automated, high-security Personnel Task Scheduling Problem (PTSP) solver. Designed with a strict **Zero-Trust architecture**, the system assigns workers to tasks based on complex constraints, dynamic priorities, and availability, minimizing idle time and preventing employee burnout while enforcing organizational labor laws.

The project uniquely bridges traditional enterprise web development with an isolated, computationally heavy algorithmic sandbox.

## 🛠 Tech Stack

### Main Backend (REST API)
* **Core:** Java 17, Spring Boot 3
* **Security:** Spring Security, Stateless JWT, BCrypt
* **Data Persistence:** Spring Data JPA, Hibernate, MySQL 8.0
* **Architecture:** DTO Pattern (explicit mapping, never exposing database entities)

### Algorithm Engine (Isolated Microservice)
* **Core:** Standalone Java 17 Engine
* **Algorithms:** Topological Sort, Greedy Scheduling, Round-Robin, Constraint Programming, and Memetic/Evolutionary strategies.
* **Design Patterns:** Strategy Pattern (scheduling logic), Builder Pattern (complex task instantiation).

### Frontend (SPA)
* **Framework:** React 18 + TypeScript
* **Build Tool:** Vite (Hot Module Replacement)
* **Styling:** Modern CSS3
* **HTTP Client:** Axios with dynamic interceptors

### Infrastructure & DevOps
* **Containerization:** Docker
* **Sandboxing:** Hyper-V (ensuring absolute isolation of the algorithm process)

## ✨ Technical Highlights & Features

### 🔐 Enterprise Security Posture
* **National ID Authentication:** Login logic is strictly tied to `nationalId`, moving away from standard usernames/emails.
* **Stateless Authentication:** Fully implemented JWT flow. No session cookies are used, natively mitigating CSRF attacks.
* **Admin-Only Registration:** No public sign-up. User management (`POST /api/users`) is strictly restricted to `ROLE_ADMIN`.

### 🏗️ Microservices-Lite & Zero-Trust Architecture
* **Isolated Algorithm Engine:** The heavy scheduling computations are run in a completely isolated process (sandboxed via Hyper-V/Docker).
* **Zero Database Access:** The algorithm engine has **NO** direct access to the MySQL database. It receives anonymous, lightweight DTOs from the Main Backend, processes them, and returns the optimized schedule.

### 🧠 Advanced Scheduling Logic
* **Strategy Pattern Implementation:** Plug-and-play scheduling strategies via the `SchedulingStrategy` interface.
* **Constraint Handling:** Accurately processes strict dependencies (e.g., `FINISH_TO_START`), worker availability, maximum daily tasks, and strict deadlines.

## 🚀 Quick Start

### 1. Prerequisites
* Java 17 (JDK)
* Node.js & npm
* MySQL 8.0
* Docker Desktop / Hyper-V (For sandbox environment)

### 2. Database Setup
Create the MySQL schema:
```sql
CREATE DATABASE secure_schedule_db;
```

### 3. Start the Main Backend
Update your application.properties with your database credentials and JWT secret, then run:
```Bash
cd main-backend
./mvnw spring-boot:run
```

### 4. Start the Algorithm Engine
Run the isolated algorithmic microservice:
```Bash
cd algorithm
./mvnw spring-boot:run
```
The engine listens for DTO payloads from the Main Backend on http://localhost:8081

### 5. Start the Frontend
```Bash
cd frontend
npm install
npm run dev
```
The React client will be available on http://localhost:5173

### 📁 Project Structure
```
📦 Secure-Schedule
 ├── 📂 algorithm/          # Isolated Java Engine, Topological Sorter, Schedulers
 ├── 📂 frontend/           # React + Vite + TypeScript Client
 ├── 📂 main-backend/       # Spring Boot API, Security, JPA, Auth Controllers
 ├── 📂 side-backend/       # Spring Boot API, Algorithm Controllers
 └── 📜 README.md
```

---

Developed as a Final Engineering Project (Software Engineering) - Showcasing secure architecture, algorithm design, and full-stack integration.