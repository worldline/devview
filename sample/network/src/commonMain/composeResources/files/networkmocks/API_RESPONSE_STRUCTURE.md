# API Response Structure - Best Practices

## Consistent Response Format

All mock responses in this test suite follow REST API best practices by maintaining **consistent response structures** across all status codes.

## Design Principle

**Rule**: The response JSON structure should be the same regardless of success or failure. Only the values and the presence of an `error` field should differ.

### Why This Matters

❌ **Bad Practice** (Inconsistent structures):
```json
// 200 Success
{
  "id": 1,
  "name": "John"
}

// 404 Error (completely different structure!)
{
  "error": "Not found"
}
```

This forces clients to:
- Use different parsing logic for errors vs success
- Write more complex error handling
- Can't reuse data models/types

✅ **Good Practice** (Consistent structures):
```json
// 200 Success
{
  "id": 1,
  "name": "John",
  "error": null
}

// 404 Error (same structure, null data fields)
{
  "id": null,
  "name": null,
  "error": {
    "code": "NOT_FOUND",
    "message": "Resource not found"
  }
}
```

This allows clients to:
- Use the same data model for all responses
- Check `error` field for errors
- Access data fields consistently
- Simplify parsing and error handling

## Response Structure Pattern

All responses in this test suite follow this pattern:

### Success Response (2xx status codes)
```json
{
  "field1": "actual data",
  "field2": "actual data",
  "error": null  // or omitted
}
```

### Error Response (4xx, 5xx status codes)
```json
{
  "field1": null,
  "field2": null,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message",
    // Optional additional fields:
    "validationErrors": { /* field-specific errors */ },
    "details": "Additional context"
  }
}
```

## Examples from Test Files

### Example 1: User Resource

**GET /users/1 → 200 Success**
```json
{
  "id": 1,
  "name": "Leanne Graham",
  "username": "Bret",
  "email": "Sincere@april.biz",
  "address": { /* ... */ },
  "phone": "1-770-736-8031",
  "website": "hildegard.org",
  "company": { /* ... */ }
}
```

**GET /users/999 → 404 Not Found**
```json
{
  "id": null,
  "name": null,
  "username": null,
  "email": null,
  "address": null,
  "phone": null,
  "website": null,
  "company": null,
  "error": {
    "code": "USER_NOT_FOUND",
    "message": "The requested user does not exist"
  }
}
```

**GET /users/1 → 500 Server Error**
```json
{
  "id": null,
  "name": null,
  "username": null,
  "email": null,
  "address": null,
  "phone": null,
  "website": null,
  "company": null,
  "error": {
    "code": "INTERNAL_SERVER_ERROR",
    "message": "An unexpected error occurred"
  }
}
```

### Example 2: Post Resource

**POST /posts → 201 Created**
```json
{
  "id": 101,
  "title": "New Post Title",
  "body": "Post content",
  "userId": 1
}
```

**POST /posts → 400 Validation Error**
```json
{
  "id": null,
  "title": null,
  "body": null,
  "userId": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Title and body are required fields",
    "validationErrors": {
      "title": "Title cannot be empty",
      "body": "Body cannot be empty"
    }
  }
}
```

**POST /posts → 401 Unauthorized**
```json
{
  "id": null,
  "title": null,
  "body": null,
  "userId": null,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Authentication required to create posts"
  }
}
```

## Client-Side Handling

With this consistent structure, client code becomes simpler:

### TypeScript Example
```typescript
interface User {
  id: number | null;
  name: string | null;
  username: string | null;
  email: string | null;
  address: Address | null;
  phone: string | null;
  website: string | null;
  company: Company | null;
  error?: {
    code: string;
    message: string;
    [key: string]: any;
  };
}

async function getUser(id: number): Promise<User> {
  const response = await fetch(`/users/${id}`);
  const user: User = await response.json();
  
  // Same parsing for all status codes!
  if (user.error) {
    throw new Error(user.error.message);
  }
  
  return user;
}
```

### Kotlin Example
```kotlin
@Serializable
data class User(
    val id: Int?,
    val name: String?,
    val username: String?,
    val email: String?,
    val address: Address?,
    val phone: String?,
    val website: String?,
    val company: Company?,
    val error: ApiError? = null
)

@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val validationErrors: Map<String, String>? = null,
    val details: String? = null
)

suspend fun getUser(id: Int): User {
    val response = client.get("/users/$id")
    val user = response.body<User>()
    
    // Same model for all status codes!
    user.error?.let { error ->
        throw ApiException(error.code, error.message)
    }
    
    return user
}
```

## Benefits

1. **Type Safety**: Single data model for all response codes
2. **Simpler Error Handling**: Check one field (`error`) instead of status code branching
3. **Consistent Parsing**: Same deserialization logic for success and errors
4. **Better Testing**: Can test error scenarios without changing data structures
5. **Future-Proof**: Adding new fields doesn't break error responses

## Error Field Structure

All error fields follow this standard structure:

```typescript
{
  "error": {
    "code": string,           // Machine-readable error code (e.g., "USER_NOT_FOUND")
    "message": string,        // Human-readable error message
    "validationErrors"?: {    // Optional: field-level validation errors
      [fieldName: string]: string
    },
    "details"?: string,       // Optional: additional context
    "statusCode"?: number     // Optional: duplicate of HTTP status
  }
}
```

### Error Code Conventions

- **NOT_FOUND errors**: `RESOURCE_NOT_FOUND`, `USER_NOT_FOUND`, `POST_NOT_FOUND`
- **Validation errors**: `VALIDATION_ERROR`
- **Auth errors**: `UNAUTHORIZED`, `FORBIDDEN`
- **Server errors**: `INTERNAL_SERVER_ERROR`

## Summary

✅ All test files now follow REST API best practices:
- Consistent JSON structure across all status codes
- Data fields set to `null` on errors
- Standard `error` object for all error cases
- Same parsing logic for clients
- Better type safety and error handling

This makes the mock responses realistic and helps integrators build proper API clients with consistent error handling.

