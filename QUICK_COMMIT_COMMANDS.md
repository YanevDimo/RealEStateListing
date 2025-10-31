# Quick Commands to Fix Git Commits

## Current Situation
Your commits are already in the repository with messages like "init commit". 

## Option A: Manual Commits (If You Have Uncommitted Changes)

If you have uncommitted changes, commit them with proper format:

```powershell
# Commit any current changes properly
git add .
git commit -m "feat: implement [describe what you changed]"

# Example:
git add src/main/java/app/exception/
git commit -m "feat: add custom exception classes for error handling"
```

## Option B: Create Amended/New Commits for Recent Work

If you've been working but haven't committed yet, create proper commits:

```powershell
# 1. Commit the compliance report
git add REQUIREMENTS_COMPLIANCE_REPORT.md
git commit -m "docs: add requirements compliance analysis report"

# 2. Commit the git guide
git add GIT_COMMITS_FIX_GUIDE.md
git commit -m "docs: add git commits fix guide"

# Continue for any other uncommitted work...
```

## Option C: Interactive Rebase (Rewrite History)

⚠️ **Only if you haven't shared the repo extensively or are okay with force-push**

### Step 1: Check how many commits you want to rewrite
```powershell
git log --oneline -10
```

### Step 2: Start interactive rebase
```powershell
# Rebase last 5 commits
git rebase -i HEAD~5

# Or rebase all commits from beginning (careful!)
git rebase -i --root
```

### Step 3: In the editor, change `pick` to `reword` (or `r`)
```
pick 0a003a5 init commit
reword 4f3e19d init commit
reword d0a9bb4 init commit
```

### Step 4: For each commit, rewrite the message
- Change `init commit` to `feat: initial project setup with Spring Boot`
- Or `feat: implement domain entities and repositories`
- etc.

### Step 5: Force push (if shared repo)
```powershell
git push --force-with-lease origin master
```

## Option D: Squash and Organize (Recommended if Many Small Commits)

If you have many small commits, you can squash them into logical commits:

```powershell
# Start interactive rebase
git rebase -i HEAD~10

# In editor, change commits to:
pick abc1234  # Keep first commit
squash def5678  # Squash into previous
squash ghi9012  # Squash into previous
squash jkl3456  # Squash into previous

# Then combine them with a proper message
```

## Option E: Create New Branch with Clean History (Safest)

```powershell
# Create new branch from current state
git checkout -b clean-commits

# Reset to create fresh history (keep files)
git reset --soft HEAD~100  # or however many commits back

# Now create proper commits
git add src/main/java/app/entity/
git commit -m "feat: implement domain entities with UUID primary keys"

git add src/main/java/app/repository/
git commit -m "feat: add JPA repositories with Spring Data"

git add src/main/java/app/service/
git commit -m "feat: implement service layer with business logic"

# ... continue for all components

# When done, replace master
git checkout master
git reset --hard clean-commits
git push --force-with-lease origin master
```

## Recommended Approach for Your Situation

Since you're close to deadline and need quick fix:

1. **Check what's uncommitted:**
   ```powershell
   git status
   ```

2. **Commit any uncommitted work properly:**
   ```powershell
   git add .
   git commit -m "feat: [describe current work]"
   ```

3. **For the assignment, create 5+ meaningful commits going forward:**
   - When you add the REST microservice: `feat: implement notification microservice`
   - When you add error handlers: `feat: add global exception handlers`
   - When you add scheduling: `feat: implement scheduled jobs for cleanup`
   - When you add caching: `feat: add Spring caching configuration`
   - When you add tests: `test: add unit and integration tests`

4. **If you have time, rewrite recent commits:**
   ```powershell
   git rebase -i HEAD~5
   ```

## Verification

After fixing, verify commits:
```powershell
git log --oneline -10
```

Should see format like:
```
abc1234 feat: implement domain entities
def5678 feat: add JPA repositories  
ghi9012 fix: enable CSRF protection
jkl3456 refactor: replace RuntimeException
mno7890 docs: add README documentation
```

## Important Notes

- **You need at least 5 commits** following the format
- **For microservice**, you'll need another 5+ commits
- **Each commit should make sense** - match the actual code changes
- **Don't make fake commits** - each should represent real work

