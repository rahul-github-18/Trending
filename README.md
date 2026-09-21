# Trending

A modern, full-stack microblogging social media application built with a **React 19 + TypeScript** frontend and a **Spring Boot** backend with PostgreSQL.

## Project Structure

```
Trending/
├── frontend/    # React 19 + TypeScript + Vite + Lucide React + React Router v7
└── backend/     # Spring Boot + Java + Spring Security (JWT) + JPA/Hibernate + PostgreSQL
```

## Getting Started

### Backend Setup
1. Ensure PostgreSQL is running and database `thread_db` is created.
2. Navigate to `backend/`:
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```
3. Backend runs at `http://localhost:8080`.

### Frontend Setup
1. Navigate to `frontend/`:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
2. Frontend runs at `http://localhost:5173`.
