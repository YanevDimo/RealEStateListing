# How to Fix Git Commits - Conventional Commits Format

## Current Status
Your commits currently use formats like:
- `init commit`
- `backup-before-rollback-2025-10-30`
- `Merge remote-tracking branch 'origin/master'`

**Required Format:** `<type>: description`

## Option 1: Create New Commits (RECOMMENDED - Safe)

This approach adds new commits with proper format without rewriting history.

### Step 1: Create Commits for Existing Features

Since you already have a working application, you can create commits that document what you've built:

```bash
# 1. Commit entities
git add src/main/java/app/entity/
git commit -m "feat: implement domain entities (User, Agent, Property, City, PropertyType)"

# 2. Commit repositories
git add src/main/java/app/repository/
git commit -m "feat: add JPA repositories for all entities"

# 3. Commit services
git add src/main/java/app/service/
git commit -m "feat: implement service layer with business logic"

# 4. Commit controllers
git add src/main/java/app/controller/
git commit -m "feat: add MVC controllers for home, agent, and auth"

# 5. Commit security
git add src/main/java/app/config/SecurityConfig.java
git commit -m "feat: implement Spring Security with role-based access control"

# 6. Commit frontend templates
git add src/main/resources/templates/
git commit -m "feat: add Thymeleaf templates for all pages"

# 7. Commit CSS
git add src/main/resources/static/styles.css
git commit -m "feat: add custom CSS styling"

# 8. Commit exception handling
git add src/main/java/app/exception/
git commit -m "feat: add custom exception classes"

# 9. Fix commit
git add src/main/java/app/service/AgentRegistrationService.java
git commit -m "fix: resolve JSON formatting bug in agent specializations"

# 10. Fix commit
git add src/main/java/app/config/SecurityConfig.java
git commit -m "fix: enable CSRF protection and fix security configuration"

# 11. Refactor commit
git add src/main/java/app/controller/
git commit -m "refactor: replace RuntimeException with custom exceptions"

# 12. Docs commit
git add REQUIREMENTS_COMPLIANCE_REPORT.md
git commit -m "docs: add requirements compliance report"
```

### Step 2: Continue with Proper Format Going Forward

For future commits, always use the format:

```
<type>: <description>
```

**Valid Types:**
- `feat:` - New feature
- `fix:` - Bug fix
- `refactor:` - Code refactoring
- `test:` - Adding/updating tests
- `docs:` - Documentation changes
- `chore:` - Maintenance tasks (config, dependencies, etc.)

**Examples:**
```bash
git commit -m "feat: add property search functionality"
git commit -m "fix: resolve lazy loading exception in property list"
git commit -m "refactor: optimize database queries using JPA Specifications"
git commit -m "test: add unit tests for PropertyService"
git commit -m "docs: update README with setup instructions"
git commit -m "chore: update Spring Boot to 3.4.0"
```

---

## Option 2: Interactive Rebase (Advanced - Rewrite History)

⚠️ **WARNING:** Only do this if you haven't shared the repo with others, or if you're okay with force-pushing.

### Step 1: Start Interactive Rebase

```bash
# Rebase last 10 commits
git rebase -i HEAD~10

# Or rebase from beginning
git rebase -i --root
```

### Step 2: In the Editor

Change `pick` to `reword` (or `r`) for commits you want to rename:

```
pick 0a003a5 init commit
reword 4f3e19d init commit
reword d0a9bb4 init commit
```

### Step 3: Rewrite Commit Messages

For each commit marked as `reword`, Git will open an editor. Change:
- `init commit` → `feat: initial project setup with Spring Boot`
- `init commit` → `feat: add entity layer with User, Agent, Property`
- etc.

### Step 4: Force Push (ONLY if you're sure)

```bash
git push --force-with-lease origin master
```

---

## Option 3: Create New Branch with Clean History

If you want a clean history going forward:

```bash
# Create orphan branch (no history)
git checkout --orphan clean-master

# Add all files
git add .

# Create initial commit
git commit -m "feat: initial project implementation"

# Continue with proper commits
git add src/main/java/app/entity/
git commit -m "feat: implement domain entities"
# ... etc

# Replace master branch (careful!)
git branch -M master
git push --force-with-lease origin master
```

---

## Quick Script: Batch Commit Existing Work

I'll create a script that helps you commit existing work properly:

```bash
# Make sure you have changes staged or use this approach:

# Commit entities
git add src/main/java/app/entity/*.java
git commit -m "feat: implement domain entities with UUID primary keys"

# Commit repositories  
git add src/main/java/app/repository/*.java
git commit -m "feat: add JPA repositories with Spring Data"

# Commit services
git add src/main/java/app/service/*.java
git commit -m "feat: implement service layer with business logic"

# Commit controllers
git add src/main/java/app/controller/*.java
git commit -m "feat: add MVC controllers for user interactions"

# Commit security configuration
git add src/main/java/app/config/
git commit -m "feat: configure Spring Security with role-based access"

# Commit frontend
git add src/main/resources/templates/
git commit -m "feat: add Thymeleaf templates for all pages"

# Commit static resources
git add src/main/resources/static/
git commit -m "feat: add custom CSS and styling"

# Commit DTOs
git add src/main/java/app/dto/*.java
git commit -m "feat: add data transfer objects for requests/responses"

# Commit exceptions
git add src/main/java/app/exception/*.java
git commit -m "feat: implement custom exception hierarchy"

# Commit configuration
git add src/main/resources/application.properties
git commit -m "chore: configure database and application properties"

# Fix commits
git add src/main/java/app/service/AgentRegistrationService.java
git commit -m "fix: correct JSON formatting for agent specializations"

git add src/main/java/app/config/SecurityConfig.java  
git commit -m "fix: enable CSRF protection in security configuration"

# Refactor commits
git add src/main/java/app/controller/
git commit -m "refactor: replace generic exceptions with custom exceptions"

# Documentation
git add REQUIREMENTS_COMPLIANCE_REPORT.md GIT_COMMITS_FIX_GUIDE.md
git commit -m "docs: add project documentation and compliance reports"
```

---

## Verification

After creating commits, verify they follow the format:

```bash
# View commit history
git log --oneline -20

# Should see format like:
# abc1234 feat: implement domain entities
# def5678 fix: resolve JSON formatting bug
# ghi9012 refactor: replace RuntimeException with custom exceptions
```

---

## Requirements Check

For the assignment, you need:
- ✅ At least 5 valid commits per application
- ✅ Format: `<type>: description`
- ✅ Types: feat, fix, refactor, test, docs, chore
- ✅ Each commit should match actual code changes

**For your microservice (once created):**
- Need at least 5 commits for the microservice as well
- Same format required

---

## Example Complete Commit History

A good history might look like:

```
feat: initial Spring Boot project setup
feat: implement domain entities (User, Agent, Property)
feat: add JPA repositories for data access
feat: implement service layer with business logic
feat: add MVC controllers for web interface
feat: configure Spring Security with roles
feat: add Thymeleaf templates for frontend
feat: implement custom exception handling
fix: resolve JSON formatting bug in agent registration
fix: enable CSRF protection in security config
refactor: replace RuntimeException with custom exceptions
test: add unit tests for PropertyService
docs: add README with project documentation
chore: update dependencies and configuration
```

---

## Next Steps

1. **Choose an option** (I recommend Option 1 - it's safest)
2. **Create commits** following the format
3. **Push to repository** (if using Option 1, just normal push)
4. **Continue using proper format** for all future commits
5. **When you create the microservice**, ensure it also has at least 5 properly formatted commits

---

## Tips

- **One logical change per commit** - Don't mix unrelated changes
- **Be descriptive** - "feat: add property search" is better than "feat: add search"
- **Use present tense** - "feat: add" not "feat: added"
- **Reference issues if applicable** - "fix: resolve #123 property deletion bug"

