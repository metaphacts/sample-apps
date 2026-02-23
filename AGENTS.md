# Agent Instructions for App extensions

## Project Context

This repository contains app extensions for the metaphactory platform. The apps include various capabilities such as REST endpoints, custom HTML5 components, or event subscribers.

## Code Review Guidelines

### 1. Java Development Standards

- **Java Version**: Ensure all code is compatible with Java 21 (sourceCompatibility and targetCompatibility)
- **Code Quality**: Follow standard Java naming conventions and best practices
- **Null Safety**: Check for proper null handling and consider using `Optional` where appropriate
- **Exception Handling**: Verify proper exception handling with meaningful error messages. The recommended logging pattern is WARN for the summary, and DEBUG for details and the stacktrace
- **Logging**: Use appropriate logging levels (trace, debug, info, warn, error) and include context in log messages

### 2. Metaphactory App Structure

- **Plugin Properties**: Verify that `plugin.properties` files are properly configured with:
  - Correct plugin ID and version. The plugin ID MUST correspond to the folder name


### 3. Extension Development

#### REST Extensions
- Verify proper JAX-RS annotations (`@Path`, `@GET`, `@POST`, etc.)
- Check that endpoints have appropriate security constraints
- Ensure proper content type handling and response formats
- Validate input parameters and provide meaningful error responses
- Each endpoint MUST require authentication (`@RequiresAuthentication`) and a permission check (`PermissionUtil.checkPermission()`)
- Check that endpoints do have appropriate Swagger documentation (`@Operation`, `@Schema`, `@ApiResponse`)
- Verify that a RestExtension is registered in `/META-INF/services/com.metaphacts.plugin.extension.RestExtension`


#### Custom HTML5 Components
- Validate Javascript/TypeScript component implementations
- Check for proper prop types and validation
- Ensure accessibility standards are met
- Verify component registration in `web-extensions.json`


#### Event Subscribers
- Verify proper event type handling
- Check for async processing where appropriate
- Ensure error handling doesn't break the event chain
- Validate cleanup of resources in event handlers


### 4. Security Considerations

- **Authentication**: Verify proper authentication checks for protected resources
- **Authorization**: Ensure role-based access control is correctly implemented
- **Input Validation**: Check all user inputs are validated and sanitized
- **Secrets Management**: No credentials or API keys in source code

### 5. Documentation

- **README Files**: Each app should have a clear README with:
  - Purpose and description
  - Setup instructions
  - Usage examples
  - Configuration options
  - Known limitations
- **Code Comments**: 
  - Public APIs should have JavaDoc comments
  - Complex logic should have inline comments explaining the approach
  - Configuration examples should be well-documented

### 6. Testing

- **Unit Tests**: Check for adequate test coverage of business logic
- **Integration Tests**: Verify tests for extension endpoints and components

### 7. Build and Dependencies

- **Dependency Management**: 
  - Use platform-provided dependencies where possible. Platform provided dependencies can be found in `.sdk/dependencies.include.gradle`
  - Avoid version conflicts
  - Check for security vulnerabilities in third-party libraries
- **Build**: 
  - A build file (`build.gradle`) is optional and can be used to declare custom dependencies and specific build steps 


### 8. Code Review Checklist

When reviewing pull requests, verify:

- [ ] Code follows Java 21 standards and conventions
- [ ] All new functionality has appropriate tests
- [ ] Documentation is updated (README, JavaDoc, inline comments)
- [ ] Security best practices are followed
- [ ] No hardcoded credentials or sensitive data
- [ ] Error handling is comprehensive and meaningful
- [ ] Logging is appropriate and follows best practices
- [ ] Configuration files are valid and well-documented
- [ ] Dependencies are properly declared and version-compatible
- [ ] Performance implications have been considered
- [ ] Accessibility standards are met for UI components
- [ ] Changes are backward compatible or migration path is documented

### 11. Common Issues to Flag

- Unused imports or variables
- Potential resource leaks (unclosed streams, connections)
- Inefficient SPARQL queries or Java collection operations
- Missing or inadequate error messages
- Hardcoded paths or URLs that should be configurable
- Inconsistent code formatting
- Missing transaction handling for database operations
- Improper use of platform APIs or extension points
- Security vulnerabilities (SQL injection, XSS, CSRF)
- Thread safety issues in concurrent contexts
- Logging pattern for try/catch block (WARN & DEBUG)

## Response Format

When providing code review feedback:
1. Start with positive observations about the code
2. Group related issues by category (security, performance, maintainability, etc.)
3. Provide specific suggestions with code examples where helpful
4. Prioritize issues by severity (critical, important, minor)
5. Explain the reasoning behind recommendations
6. Suggest best practices and alternative approaches when applicable
