# Igniter Test Suite Documentation

This directory and the accompanying `src/test/java/com/thread/Igniter/testing/` package provide testing coverage for the Igniter platform, spanning **Unit Tests**, **API Integration Tests**, and a complete **End-to-End Workflow Test**.

> [!NOTE]
> All existing application code and configuration files in `src/main/` and `pom.xml` remain 100% untouched.

---

## 1. Test Architecture & Structure

```
Igniter/
├── testing/                                      # Test scripts & documentation
│   ├── README.md                                 # Complete documentation of test suite
│   └── run-tests.sh                              # Execution script for all or filtered tests
│
└── src/test/java/com/thread/Igniter/
    ├── IgniterApplicationTests.java              # Default Spring Boot context test
    └── testing/                                  # Testing package
        ├── workflow/                             # Master End-to-End User Journey
        │   └── EndToEndWorkflowIntegrationTest.java
        │
        ├── integration/                          # Controller & Security Integration Tests
        │   ├── HealthControllerIntegrationTest.java
        │   ├── AuthControllerIntegrationTest.java
        │   ├── UserControllerIntegrationTest.java
        │   ├── ThreadControllerIntegrationTest.java
        │   ├── CommentControllerIntegrationTest.java
        │   ├── LikeControllerIntegrationTest.java
        │   ├── FollowControllerIntegrationTest.java
        │   └── NotificationControllerIntegrationTest.java
        │
        └── unit/                                 # Fast Mockito Unit Tests
            ├── AuthServiceUnitTest.java
            ├── UserServiceUnitTest.java
            ├── ThreadServiceUnitTest.java
            ├── CommentServiceUnitTest.java
            ├── LikeServiceUnitTest.java
            ├── FollowServiceUnitTest.java
            └── NotificationServiceUnitTest.java
```

---

## 2. API Coverage Matrix

| Endpoint | Method | Test Class | Type |
|---|---|---|---|
| `/health` | `GET` | `HealthControllerIntegrationTest` & `EndToEndWorkflowIntegrationTest` | Integration / Workflow |
| `/api/auth/login` | `POST` | `AuthControllerIntegrationTest` & `EndToEndWorkflowIntegrationTest` | Integration / Workflow |
| `/api/auth/logout` | `POST` | `AuthControllerIntegrationTest` & `EndToEndWorkflowIntegrationTest` | Integration / Workflow |
| `/api/users` | `POST` | `UserControllerIntegrationTest` & `EndToEndWorkflowIntegrationTest` | Integration / Workflow |
| `/api/users/me` | `GET` | `UserControllerIntegrationTest` & `EndToEndWorkflowIntegrationTest` | Integration / Workflow |
| `/api/users/me` | `PUT` | `UserControllerIntegrationTest` & `EndToEndWorkflowIntegrationTest` | Integration / Workflow |
| `/api/users/profile-picture` | `POST` | `UserControllerIntegrationTest` & `EndToEndWorkflowIntegrationTest` | Integration / Workflow |
| `/api/users/search` | `GET` | `UserControllerIntegrationTest` & `EndToEndWorkflowIntegrationTest` | Integration / Workflow |
| `/api/threads/create` | `POST` | `ThreadControllerIntegrationTest` & `EndToEndWorkflowIntegrationTest` | Integration / Workflow |
| `/api/threads` | `GET` | `ThreadControllerIntegrationTest` & `EndToEndWorkflowIntegrationTest` | Integration / Workflow |
| `/api/threads/{id}` | `GET` | `ThreadControllerIntegrationTest` & `EndToEndWorkflowIntegrationTest` | Integration / Workflow |
| `/api/threads/{id}` | `PUT` | `ThreadControllerIntegrationTest` & `EndToEndWorkflowIntegrationTest` | Integration / Workflow |
| `/api/threads/{id}` | `DELETE`| `ThreadControllerIntegrationTest` & `EndToEndWorkflowIntegrationTest` | Integration / Workflow |
| `/api/threads/{id}/comments`| `POST` | `CommentControllerIntegrationTest` & `EndToEndWorkflowIntegrationTest` | Integration / Workflow |
| `/api/threads/{id}/comments`| `GET`  | `CommentControllerIntegrationTest` & `EndToEndWorkflowIntegrationTest` | Integration / Workflow |
| `/api/threads/comments/{id}`| `PUT`  | `CommentControllerIntegrationTest` & `EndToEndWorkflowIntegrationTest` | Integration / Workflow |
| `/api/threads/comments/{id}`| `DELETE`| `CommentControllerIntegrationTest` & `EndToEndWorkflowIntegrationTest` | Integration / Workflow |
| `/api/threads/{id}/like` | `POST` | `LikeControllerIntegrationTest` & `EndToEndWorkflowIntegrationTest` | Integration / Workflow |
| `/api/threads/{id}/like` | `DELETE`| `LikeControllerIntegrationTest` & `EndToEndWorkflowIntegrationTest` | Integration / Workflow |
| `/api/users/{id}/follow` | `POST` | `FollowControllerIntegrationTest` & `EndToEndWorkflowIntegrationTest` | Integration / Workflow |
| `/api/users/{id}/unfollow`| `POST`| `FollowControllerIntegrationTest` & `EndToEndWorkflowIntegrationTest` | Integration / Workflow |
| `/api/users/{id}/following/status`| `GET` | `FollowControllerIntegrationTest` & `EndToEndWorkflowIntegrationTest` | Integration / Workflow |
| `/api/users/{id}/followers/count`| `GET`  | `FollowControllerIntegrationTest` & `EndToEndWorkflowIntegrationTest` | Integration / Workflow |
| `/api/users/{id}/following/count`| `GET`  | `FollowControllerIntegrationTest` & `EndToEndWorkflowIntegrationTest` | Integration / Workflow |
| `/api/users/{id}/followers`| `GET` | `FollowControllerIntegrationTest` & `EndToEndWorkflowIntegrationTest` | Integration / Workflow |
| `/api/users/{id}/following`| `GET` | `FollowControllerIntegrationTest` & `EndToEndWorkflowIntegrationTest` | Integration / Workflow |
| `/api/notifications` | `GET` | `NotificationControllerIntegrationTest` & `EndToEndWorkflowIntegrationTest` | Integration / Workflow |
| `/api/notifications/unread/count`| `GET`| `NotificationControllerIntegrationTest` & `EndToEndWorkflowIntegrationTest` | Integration / Workflow |
| `/api/notifications/{id}/read`| `PUT` | `NotificationControllerIntegrationTest` & `EndToEndWorkflowIntegrationTest` | Integration / Workflow |

---

## 3. End-to-End Workflow Test Sequence

`EndToEndWorkflowIntegrationTest.java` executes 21 ordered steps validating realistic user behavior:
1. **Health Check**: Validates `/health` is up.
2. **Registration**: Registers two users, `Alice` and `Bob`.
3. **Login**: Obtains JWT access tokens for both users.
4. **Current Profile**: Alice verifies her profile via `/api/users/me`.
5. **Profile Update**: Alice updates her profile bio.
6. **Avatar Upload**: Alice uploads an avatar image multipart file.
7. **User Search**: Alice finds Bob using `/api/users/search`.
8. **Follow**: Alice follows Bob.
9. **Follow Verification**: Checks follow status, follower count, following count, and follower/following lists.
10. **Follow Notification**: Bob checks unread notification count, views the follow notification, and marks it as read.
11. **Thread Creation**: Alice posts a new thread.
12. **Thread Feed & Detail**: Bob retrieves the thread feed and inspects Alice's thread by ID.
13. **Thread Edit**: Alice edits the thread content.
14. **Thread Like**: Bob likes Alice's thread, like count increments, and Alice receives a notification.
15. **Comment Creation**: Bob posts a comment on Alice's thread, and Alice receives a notification.
16. **Comment Edit**: Bob updates his comment content.
17. **Unlike**: Bob unlikes the thread, like count decrements to 0, and notification is cleaned up.
18. **Delete Comment**: Bob deletes his comment.
19. **Delete Thread**: Alice deletes her thread.
20. **Unfollow**: Alice unfollows Bob.
21. **Logout & Token Revocation**: Alice logs out; her revoked JWT token is immediately rejected with HTTP 401.

---

## 4. How to Run the Tests

### Option A: Using the Test Runner Script
```bash
# Run everything (all 100 tests)
./testing/run-tests.sh all

# Run only fast unit tests
./testing/run-tests.sh unit

# Run controller integration tests
./testing/run-tests.sh integration

# Run the full end-to-end user workflow test
./testing/run-tests.sh workflow
```

### Option B: Using Maven Wrapper Directly
```bash
# Run all tests
./mvnw test

# Run only EndToEndWorkflowIntegrationTest
./mvnw test -Dtest=EndToEndWorkflowIntegrationTest

# Run only Unit Tests
./mvnw test "-Dtest=*UnitTest"

# Run only Integration Tests
./mvnw test "-Dtest=*IntegrationTest"
```
