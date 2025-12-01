# Release & Version Management

You are managing a release or version bump. Parse the argument to determine the action:

## Argument Patterns

- `/release ship` - **Full release workflow** (commit -> release -> bump next version)
- `/release tag <version>` - Create a git tag (e.g., `v0.2.0`)
- `/release bump <type>` - Bump version: `patch`, `minor`, or `major`
- `/release status` - Show current version and tags

---

## 1. Ship Release (`/release ship`) - RECOMMENDED

**Full automated release workflow:**

### Pre-flight Checks
1. Run `git status` to see changed files
2. Run `git diff --stat` to review changes
3. Determine release type from changes:
   - New features -> minor release
   - Bug fixes only -> patch release
   - Breaking changes -> major release

### Step 1: Commit Changes
1. Stage all changes: `git add -A`
2. Create commit with Conventional Commits format based on changes
3. Include footer

### Step 2: Prepare Release
1. Read current version from `pom.xml` (e.g., `0.1.0-SNAPSHOT`)
2. Extract release version (e.g., `0.1.0`)
3. Update CHANGELOG.md:
   - Change `## [Unreleased]` content to `## [X.Y.Z] - YYYY-MM-DD`
   - Add empty `## [Unreleased]` section at top

### Step 3: Update Version Files
Update version in all files:
- `pom.xml` - Remove `-SNAPSHOT` suffix
- `MANIFEST.MF` - Update Bundle-Version
- `FEATURES.md` - Add to version history table

### Step 4: Create Tag
```bash
git add -A
git commit -m "chore(release): release vX.Y.Z"
git tag -a vX.Y.Z -m "Release vX.Y.Z - <summary from CHANGELOG>"
```

### Step 5: Bump to Next Development Version
1. Increment version: X.Y.Z -> X.(Y+1).0-SNAPSHOT
2. Update all files with new development version
3. Commit: `chore: bump version to X.(Y+1).0-SNAPSHOT`

### Step 6: Push Everything
```bash
git push origin <current-branch>
git push origin vX.Y.Z
```

---

## 2. Tag Release (`/release tag v0.x.x`)

Steps:
1. Verify the version format (must start with `v`)
2. Check current version in `pom.xml`
3. Create annotated git tag with changelog summary
4. Push the tag
5. Update CHANGELOG.md
6. Update FEATURES.md
7. Commit changelog updates

## 3. Bump Version (`/release bump patch|minor|major`)

Bump rules:
- `patch`: X.Y.Z -> X.Y.(Z+1)
- `minor`: X.Y.Z -> X.(Y+1).0
- `major`: X.Y.Z -> (X+1).0.0

## 4. Status (`/release status`)

Show:
- Current version from pom.xml
- Last 5 git tags
- Unreleased changes from CHANGELOG.md
- Git status (uncommitted changes)

---

## Files to Update

| File | What to Update |
|------|----------------|
| `pom.xml` | `<version>` element (inherits from parent, check if needed) |
| `MANIFEST.MF` | `Bundle-Version` header |
| `CHANGELOG.md` | Version sections |
| `FEATURES.md` | Version history table |

## Commit Convention

Follow Conventional Commits:
- Feature release: `feat(ai): <summary>`
- Bug fix release: `fix(provider): <summary>`
- Release commit: `chore(release): release vX.Y.Z`
- Version bump: `chore: bump version to X.Y.Z-SNAPSHOT`

Always include footer:
```
🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
```

---

**User's request:** $ARGUMENTS
