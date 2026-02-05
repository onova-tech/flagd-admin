# OPENCODE.md

Repository update workflow documentation for standardized version management and deployment.

## Overview

This documentation establishes a standardized workflow for updating repositories with proper version management, branching strategies, and release processes. Following this guide ensures:

- **Consistent versioning** across all components
- **Clean Git history** with meaningful branch names
- **Proper release management** with accurate tagging
- **Reproducible deployments** with synchronized Docker references

## Prerequisites

Before beginning any update process, ensure:

- ✅ **Git installed and configured** with proper credentials
- ✅ **Clean working directory** (no uncommitted changes)
- ✅ **Write permissions** for all project files
- ✅ **Development environment** ready for testing changes

## Standard Update Workflow

### Step 1: Preparation

1. **Check current status**:
   ```bash
   git status
   ```

2. **Ensure clean state**:
   ```bash
   # If there are uncommitted changes, commit or stash them first
   git add .
   git commit -m "WIP: Saving work before update"
   ```

3. **Switch to main branch**:
   ```bash
   git checkout main
   git pull origin main
   ```

### Step 2: Branch Creation

Always create a new branch before making changes:

1. **Use semantic naming convention**:
   ```bash
   # For new features
   git checkout -b feature/feature-name-meaningful
   
   # For bug fixes
   git checkout -b fix/issue-description
   
   # For maintenance tasks
   git checkout -b chore/task-description
   
   # For code refactoring
   git checkout -b refactor/improvement-description
   ```

2. **Good examples**:
   - `feature/security-fixes`
   - `fix/validation-error`
   - `chore/version-bump`
   - `refactor/remove-deprecated-methods`

3. **Bad examples** (avoid):
   - `branch-1`
   - `temp`
   - `work`
   - `updates`

### Step 3: Version Updates

#### Version Format Rules

All versions must follow **semantic versioning**: `X.Y.Z`

- **X** = Major version (breaking changes)
- **Y** = Minor version (new features)
- **Z** = Patch version (bug fixes)

**Valid formats**: `0.1.0`, `1.2.3`, `10.0.5`  
**Invalid formats**: `v0.1.0`, `0.1`, `0.1.0-beta`

#### Current Stack File Locations

**API Version Update** (`build.gradle`):
```gradle
# Find this line (typically line 9):
version = '0.1.0'  # ← UPDATE THIS LINE

# Change to:
version = '0.1.1'
```

**UI Version Update** (`ui/package.json`):
```json
{
  "name": "flagd-admin-ui",
  "version": "0.1.0",  // ← UPDATE THIS LINE
  "private": true,
  // ...
}
```

**Docker References to Update**:

1. **Main Dockerfile** (root directory):
   ```dockerfile
   # Find and update this line:
   COPY --from=api-builder /build/build/libs/flagd_admin_server-0.1.0.jar /app/api.jar
   
   # Change to:
   COPY --from=api-builder /build/build/libs/flagd_admin_server-0.1.1.jar /app/api.jar
   ```

2. **API Dockerfile** (`api/Dockerfile`):
   ```dockerfile
   # Find and update this line:
   COPY --from=builder /build/build/libs/flagd_admin_server-0.1.0.jar /app/api.jar
   
   # Change to:
   COPY --from=builder /build/build/libs/flagd_admin_server-0.1.1.jar /app/api.jar
   ```

#### Version Update Commands

```bash
# Update API version
sed -i "s/version = '[0-9]\+\.[0-9]\+\.[0-9]\+'/version = '0.1.1'/" api/build.gradle

# Update UI version
sed -i 's/"version": "[0-9]\+\.[0-9]\+\.[0-9]\+"/"version": "0.1.1"/' ui/package.json

# Update Docker references
sed -i 's/flagd_admin_server-[0-9]\+\.[0-9]\+\.[0-9]\+/flagd_admin_server-0.1.1/g' Dockerfile
sed -i 's/flagd_admin_server-[0-9]\+\.[0-9]\+\.[0-9]\+/flagd_admin_server-0.1.1/g' api/Dockerfile
```

### Step 4: Code Changes & Testing

1. **Make required code modifications**
2. **Test changes locally**:
   ```bash
   # For API
   cd api && ./gradlew test
   
   # For UI
   cd ui && npm run test
   ```

3. **Verify version consistency**:
   ```bash
   # Check all version references match
   grep -n "0.1.1" api/build.gradle ui/package.json Dockerfile api/Dockerfile
   ```

### Step 5: Git Operations

1. **Stage all changes**:
   ```bash
   git add .
   ```

2. **Commit with descriptive message**:
   ```bash
   git commit -m "chore: bump version to 0.1.1 and update Docker configurations

   - Update API version from 0.1.0 to 0.1.1
   - Update UI version from 0.1.0 to 0.1.1  
   - Update Docker JAR references to new version
   - [Other specific changes made]
   "
   ```

3. **Push branch to remote**:
   ```bash
   git push -u origin feature/version-bump-0.1.1
   ```

### Step 6: Version Release Decision

**CRITICAL STEP**: Only create tags when explicitly releasing a new version.

1. **Ask user confirmation**:
   ```
   Is this a version release that needs a tag? (yes/no): 
   ```

2. **If user confirms "yes"**:
   ```bash
   # Create annotated tag
   git tag -a v0.1.1 -m "Release v0.1.1

   - [List of key changes in this version]
   - [Security improvements]
   - [New features]
   - [Bug fixes]
   "

   # Push tag to remote
   git push origin v0.1.1
   ```

3. **If user says "no"**:
   - No tag created
   - Branch remains for future development
   - Can be merged back to main when ready

## Verification Checklist

Before completing any update, verify:

- [ ] **New branch created** with proper naming convention
- [ ] **All version files updated** consistently (API, UI, Docker)
- [ ] **Version format is X.Y.Z** semantic versioning
- [ ] **Docker JAR references** match new version
- [ ] **Local tests pass** after changes
- [ ] **Clean commit** with descriptive message
- [ ] **Branch pushed** to remote successfully
- [ ] **Tag created only if user confirms** version release

## Common Troubleshooting

### Version Mismatch Issues

**Problem**: Docker builds fail with JAR not found errors
**Solution**: Verify all Docker files reference the same version:
```bash
# Check all Docker references
grep -r "flagd_admin_server" . --include="*Dockerfile*"

# Ensure all match the version in build.gradle
```

### Git Workflow Issues

**Problem**: "Working directory not clean" errors
**Solution**: Handle uncommitted changes first:
```bash
# Option 1: Commit changes
git add .
git commit -m "WIP: Save work before update"

# Option 2: Stash changes
git stash push -m "Temporary stash"
```

### Rollback Procedures

**If something goes wrong**:

1. **Reset to clean state**:
   ```bash
   git reset --hard HEAD~1
   ```

2. **Switch to main**:
   ```bash
   git checkout main
   git reset --hard origin/main
   ```

3. **Delete problematic branch**:
   ```bash
   git branch -D feature/problematic-branch
   ```

## Example Workflows

### Example 1: Security Fix Release

1. **Create branch**: `git checkout -b fix/security-vulnerability`
2. **Fix code**: Implement security patches
3. **Update version**: `0.1.0` → `0.1.1`
4. **Update Docker**: All JAR references to `0.1.1`
5. **Commit**: Detailed security fix message
6. **Push**: `git push -u origin fix/security-vulnerability`
7. **Ask user**: "Is this a version release?" → **"yes"**
8. **Create tag**: `git tag -a v0.1.1` and push

### Example 2: Feature Development

1. **Create branch**: `git checkout -b feature/flag-editor`
2. **Implement**: New feature functionality
3. **Update version**: `0.1.1` → `0.2.0`
4. **Commit**: Feature implementation message
5. **Push**: `git push -u origin feature/flag-editor`
6. **Ask user**: "Is this a version release?" → **"no"**
7. **No tag**: Branch continues for development

## Current Stack Specifics

This documentation is optimized for:

- **Backend**: Java Spring Boot with Gradle
- **Frontend**: React with npm/package.json
- **Deployment**: Docker multi-stage builds
- **Database**: SQLite (file-based)
- **Authentication**: JWT-based

## Adapting for Other Projects

To use this workflow with different project types:

### Python Projects
- **Version file**: `setup.py` or `pyproject.toml`
- **Update pattern**: Look for `version = "X.Y.Z"`

### Go Projects  
- **Version file**: `go.mod`
- **Update pattern**: Look for `module X.Y.Z`

### .NET Projects
- **Version file**: `.csproj` files
- **Update pattern**: Look for `<Version>X.Y.Z</Version>`

### Ruby Projects
- **Version file**: `Gemfile`
- **Update pattern**: Look for `gem 'name', 'X.Y.Z'`

## Best Practices

### Commit Messages
- **Format**: `type: brief description`
- **Types**: `feat`, `fix`, `chore`, `refactor`, `docs`, `test`, `style`
- **Body**: Detailed explanation of changes and reasoning
- **Examples**: `feat: add user authentication`, `fix: resolve validation error`

### Branch Management
- **One feature per branch**: Keep branches focused
- **Short-lived branches**: Merge or delete when done
- **Descriptive names**: Future team members should understand purpose
- **Regular cleanup**: Delete merged branches

### Release Management
- **Semantic versioning**: Follow strict X.Y.Z format
- **Changelog maintenance**: Keep tag messages informative
- **Release notes**: Document important changes per version
- **Rollback capability**: Always know how to revert

---

**Following this documentation ensures consistent, maintainable repository updates across any team or project.**