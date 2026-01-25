# JWT Authentication Implementation Plan

## Overview
Migrate from Basic Auth with session cookies to JWT token-based authentication with refresh tokens.

**Current State:**
- Spring Boot 4.0.1 with pluggable auth providers
- Currently using `no_auth` (no authentication)
- Basic auth available with form-based login and session cookies
- In-memory user storage (InMemoryUserDetailsManager)
- No JWT implementation
- Frontend has no authentication logic

**Target State:**
- JWT access tokens (15 min expiration)
- JWT refresh tokens (7 day expiration) stored in database
- Pluggable JWT auth provider following existing pattern
- Frontend login page with token storage and auto-refresh

---

## Technology Stack Updates

### Backend Dependencies (build.gradle)
Add JWT library:
```gradle
implementation 'io.jsonwebtoken:jjwt-api:0.12.3'
runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.3'
runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.3'
```

### Database Schema Changes
Create new table for refresh tokens:
```sql
CREATE TABLE refresh_tokens (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);
```

---

## Implementation Steps

### Phase 1: Backend Configuration

#### 1.1 Update application.properties
Add JWT configuration:
```properties
# JWT Configuration
application.auth.provider=jwt
application.auth.provider.jwt.secret=CHANGE_THIS_TO_SECURE_RANDOM_SECRET_MIN_256_BITS
application.auth.provider.jwt.access-token-expiration=900000  # 15 minutes in ms
application.auth.provider.jwt.refresh-token-expiration=604800000  # 7 days in ms
```

#### 1.2 Create RefreshToken Entity
**File:** `api/src/main/java/tech/onova/flagd_admin_server/domain/entity/RefreshToken.java`
- Fields: id (UUID), userId (String), expiresAt (LocalDateTime), createdAt (LocalDateTime)
- JPA annotations: @Entity, @Table, @Id, @Column
- Repository interface

#### 1.3 Create JWT Utilities
**File:** `api/src/main/java/tech/onova/flagd_admin_server/security/jwt/JwtUtil.java`
Methods:
- `generateAccessToken(UserDetails)` - Creates access token with 15 min expiry
- `generateRefreshToken(UserDetails)` - Creates refresh token with 7 day expiry
- `extractUsername(String)` - Extract username from token
- `extractExpiration(String)` - Get expiration date from token
- `isTokenExpired(String)` - Check if token is expired
- `validateToken(String, UserDetails)` - Validate token against user
- `isRefreshToken(String)` - Distinguish access from refresh tokens

#### 1.4 Create RefreshTokenService
**File:** `api/src/main/java/tech/onova/flagd_admin_server/security/jwt/RefreshTokenService.java`
Methods:
- `createRefreshToken(UserDetails)` - Generate and store refresh token
- `findByToken(String)` - Lookup refresh token in DB
- `verifyExpiration(RefreshToken)` - Check if token is expired
- `deleteByUserId(String)` - Invalidate all user tokens
- `deleteRefreshToken(RefreshToken)` - Remove specific token

#### 1.5 Create JWT Authentication Filter
**File:** `api/src/main/java/tech/onova/flagd_admin_server/security/jwt/JwtAuthenticationFilter.java`
Extends `OncePerRequestFilter`
- Override `doFilterInternal()`
- Extract JWT from Authorization header (Bearer token)
- Validate token and set SecurityContext
- Handle token validation errors

#### 1.6 Create JWT Auth Provider
**File:** `api/src/main/java/tech/onova/flagd_admin_server/security/providers/JwtAuthProvider.java`
Implements `AuthProvider` interface (follows existing pattern)
- Name: "jwt"
- `securityFilterChain()`:
  - Disable CSRF (stateless)
  - Configure CORS
  - Permit `/api/v1/auth/login`, `/api/v1/auth/refresh`
  - Require authentication for all other endpoints
  - Add JwtAuthenticationFilter before UsernamePasswordAuthenticationFilter
  - Stateless session management
- `userDetailsService()`: Use InMemoryUserDetailsManager (same as BasicAuthProvider)
- `passwordEncoder()`: BCryptPasswordEncoder

#### 1.7 Create Authentication Controller
**File:** `api/src/main/java/tech/onova/flagd_admin_server/controller/AuthController.java`
Endpoints:
- `POST /api/v1/auth/login`
  - Request: `{ username, password }`
  - Response: `{ accessToken, refreshToken, type: "Bearer" }`
  - Validate credentials with AuthenticationManager
  - Generate JWT pair
  - Store refresh token in DB
- `POST /api/v1/auth/refresh`
  - Request: `{ refreshToken }`
  - Response: `{ accessToken, refreshToken }`
  - Validate refresh token
  - Check expiration in DB
  - Generate new token pair
  - Delete old refresh token, store new one
- `POST /api/v1/auth/logout`
  - Request: `{ refreshToken }`
  - Response: 204 No Content
  - Delete refresh token from DB
  - Clear security context

---

### Phase 2: Frontend Implementation

#### 2.1 Create Auth Context
**File:** `ui/src/contexts/AuthContext.jsx`
- State: user, accessToken, refreshToken, loading
- Methods: login, logout, refreshAccessToken
- Auto-refresh token before expiration
- Store tokens in localStorage or httpOnly cookies

#### 2.2 Create Login Page
**File:** `ui/src/pages/Login.jsx`
- Login form with username/password
- Call `/api/v1/auth/login` on submit
- Store tokens in AuthContext
- Redirect to dashboard on success
- Error handling for invalid credentials

#### 2.3 Create Protected Route Component
**File:** `ui/src/components/ProtectedRoute.jsx`
- Check if user is authenticated
- Redirect to login if not
- Show loading state while checking auth

#### 2.4 Update API Client
**File:** `ui/src/utils/api.js`
- Add Authorization header with Bearer token
- Handle 401 Unauthorized responses
- Trigger token refresh on 401
- Retry failed request after refresh

#### 2.5 Update App.jsx
- Wrap application with AuthContext.Provider
- Add ProtectedRoute wrapper for authenticated pages
- Add login route to Router
- Add logout functionality (menu item/button)

#### 2.6 Update Existing Pages
- Add logout button to header/navigation
- Show user info (username) if authenticated

---

## File Structure

### New Backend Files
```
api/src/main/java/tech/onova/flagd_admin_server/
├── domain/entity/RefreshToken.java
├── security/jwt/
│   ├── JwtUtil.java
│   ├── JwtAuthenticationFilter.java
│   └── RefreshTokenService.java
├── security/providers/JwtAuthProvider.java
├── repository/RefreshTokenRepository.java
└── controller/AuthController.java
```

### New Frontend Files
```
ui/src/
├── contexts/AuthContext.jsx
├── pages/Login.jsx
├── components/ProtectedRoute.jsx
└── utils/api.js (modified)
```

### Modified Files
```
api/
├── build.gradle (add JWT dependencies)
├── src/main/resources/application.properties (add JWT config)
├── src/main/java/tech/onova/flagd_admin_server/security/SecurityConfig.java (auto-discover JwtAuthProvider)

ui/
├── src/App.jsx (add AuthContext, routes)
├── src/config.js (no changes needed if api.js handles auth)
└── src/main.jsx (no changes needed)
```

---

## Configuration Changes Summary

### application.properties Additions
```properties
# Auth Configuration
application.auth.provider=jwt
application.auth.provider.jwt.secret=<generate-secure-random-string>
application.auth.provider.jwt.access-token-expiration=900000
application.auth.provider.jwt.refresh-token-expiration=604800000
```

### build.gradle Additions
```gradle
dependencies {
    implementation 'io.jsonwebtoken:jjwt-api:0.12.3'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.3'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.3'
}
```

---

## Security Considerations

1. **JWT Secret**: Must be at least 256 bits (32 bytes) for HS256 algorithm
2. **Token Storage**: Consider httpOnly cookies for refresh tokens to prevent XSS
3. **HTTPS**: Required in production to prevent token interception
4. **Refresh Token Rotation**: Always generate new refresh token on refresh
5. **Token Revocation**: Implement logout to delete refresh tokens from DB
6. **CORS**: Restrict to specific origins in production

---

## Testing Plan

### Backend Tests
1. **Unit Tests:**
   - JwtUtil: Token generation, validation, expiration
   - RefreshTokenService: Create, verify, delete operations
2. **Integration Tests:**
   - AuthController: Login, refresh, logout endpoints
   - JwtAuthProvider: Security filter chain configuration
3. **Security Tests:**
   - Invalid credentials on login
   - Expired tokens
   - Malformed tokens
   - Refresh token reuse detection

### Frontend Tests
1. **Component Tests:**
   - Login form submission
   - ProtectedRoute redirect behavior
2. **Integration Tests:**
   - Login flow (form → API → token storage → redirect)
   - Token refresh on 401
   - Logout flow (delete tokens → redirect)

### Manual Testing
1. Enable JWT provider in application.properties
2. Test login with valid credentials
3. Verify token in Authorization header
4. Wait 15 minutes, verify auto-refresh
5. Test logout functionality
6. Verify protected endpoints fail without token

---

## Migration Checklist

- [ ] Add JWT dependencies to build.gradle
- [ ] Create RefreshToken entity and repository
- [ ] Implement JwtUtil class
- [ ] Implement RefreshTokenService
- [ ] Create JwtAuthenticationFilter
- [ ] Create JwtAuthProvider
- [ ] Create AuthController with endpoints
- [ ] Update application.properties with JWT config
- [ ] Update SecurityConfig (auto-discovery will pick up JwtAuthProvider)
- [ ] Create AuthContext in frontend
- [ ] Create Login page
- [ ] Create ProtectedRoute component
- [ ] Update API client with token handling
- [ ] Update App.jsx with auth context and routes
- [ ] Add logout button to UI
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Test login flow end-to-end
- [ ] Test token refresh flow
- [ ] Test logout flow
- [ ] Generate secure JWT secret for production

---

## Rollback Plan

If issues arise:
1. Change `application.auth.provider` back to `basic` or `no_auth`
2. Remove JwtAuthProvider from classpath or rename to prevent auto-discovery
3. Frontend will need to be reverted to stateless API calls

---

## References

- [JJWT Documentation](https://github.com/jwtk/jjwt)
- [Spring Security JWT Guide](https://spring.io/guides/gs/securing-web/)
- [OWASP JWT Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)
