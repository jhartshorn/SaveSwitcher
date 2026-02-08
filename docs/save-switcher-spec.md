# SaveSwitcher Android App Specification (MVP)

## 1. Purpose
SaveSwitcher manages per-user save files for emulators by swapping the emulator's active save file (e.g., `game_name.sav`) with user-owned variants (e.g., `game_name_james.sav`, `game_name_liz.sav`).

The app must support:
- Multiple emulators
- Multiple users
- Recursive scanning of emulator save directories
- Per-game user switching
- Safe file operations with clear ownership prompts
- Usable UI on small phone screens

## 2. Goals and Non-Goals
### 2.1 Goals
- Let users register emulator profiles (`name + save root path`).
- Detect game save files in nested directories.
- Let users switch the active player for each game.
- Preserve and surface metadata (owner, modified time, path).
- Prevent accidental save loss during switch operations.

### 2.2 Non-Goals (MVP)
- Cloud sync
- Cross-device conflict resolution
- Save-state binary parsing
- Emulator launch integration
- Multi-file save families requiring custom per-emulator plugins (can be added later)

## 3. Target Platform
- Android 10+ (min SDK 29, target latest stable SDK)
- Kotlin + Jetpack Compose
- Storage Access Framework (SAF) for user-selected folders

## 4. Key Concepts
- **Emulator**: Named profile with a persisted SAF tree URI to save root.
- **User**: Player profile (display name + normalized ID).
- **Game Save Base File**: File currently used by emulator (e.g., `Pokemon.sav`).
- **User Variant File**: Archived user-specific save (e.g., `Pokemon_james.sav`).
- **Switch Operation**: Archive current base save to a user variant, then activate target user's variant as base (or create blank/none if missing).

## 5. User Workflows
### 5.1 First-Time Setup
1. Open app.
2. Create one or more users (e.g., James, Liz).
3. Add emulator profile:
   - Enter emulator name.
   - Pick save root folder via SAF folder picker.
   - Select supported save extensions (default: `.sav`, `.dsv`, `.srm`).
4. App scans folder recursively and lists detected games.

### 5.2 Switch User for a Game (Base Save Exists)
1. Open emulator -> game.
2. Tap `Switch User`.
3. App shows:
   - Current base save file modified date/time.
   - Existing user variants and their modified date/time.
4. If base save ownership is unknown, ask: `Who owns current save?`
5. User selects target user.
6. App performs safe switch:
   - Copy base save to source owner's variant (`game_owner.ext`).
   - Replace base file with target user variant if present.
   - If target variant missing, leave no base replacement (or keep copied source only) and explain emulator will create a fresh save when played.
7. Show success summary.

### 5.3 Switch User Later (Both Users Have Saves)
1. User selects game and target user.
2. App shows current and stored variants with timestamps.
3. On confirm, app:
   - Archives current base to current owner variant.
   - Activates target variant as new base.
4. Logs operation for audit/undo.

## 6. Functional Requirements
### 6.1 Emulator Management
- Create, edit, delete emulator profiles.
- Persist:
  - `emulatorId`
  - `name`
  - `treeUri`
  - `allowedExtensions`
  - `createdAt`, `updatedAt`
- Validate URI permission before operations.

### 6.2 User Management
- Create, edit, archive users.
- Username rules:
  - 1-30 chars
  - Visible display name
  - Internal normalized ID (`[a-z0-9_]+`) for filenames
- Prevent duplicate normalized IDs.

### 6.3 Game Discovery
- Recursive scan of each emulator root.
- Detect base saves and user variants by filename pattern:
  - Base: `<game>.<ext>`
  - Variant: `<game>_<userId>.<ext>`
- Group by relative path + base game name + extension.
- Exclude hidden files and temp/backup suffixes (e.g., `.tmp`, `.bak`).

### 6.4 Ownership + Metadata
- Maintain local metadata table:
  - Last known base owner
  - Last switch timestamp
  - Last operation status
- On unknown owner, force explicit owner prompt before switching.

### 6.5 Switch Logic (Safety-Critical)
Given selected `game`, `sourceOwner`, `targetOwner`:
1. Resolve `base`, `sourceVariant`, `targetVariant` paths.
2. Preflight checks:
   - Read/write access
   - Enough free space
   - Target/source collision checks
3. Create backup of base (`.bak`) if base exists.
4. Archive base to `sourceVariant` using copy + fsync + replace.
5. If `targetVariant` exists, copy/replace to `base`.
6. If `targetVariant` missing:
   - Remove base if required by emulator behavior, or keep archived-only state.
   - Show clear message: fresh save expected on next game launch.
7. Record operation log.
8. Clean up temporary files.

### 6.6 Operation Log and Undo
- Keep operation history with:
  - Emulator, game path, source owner, target owner
  - Before/after file mtimes and sizes
  - Timestamp and result
- Provide one-tap rollback for most recent successful switch per game.

## 7. File Naming Rules
- Variant format: `<baseName>_<userId>.<ext>`
- If `<baseName>` already ends with `_something`, still append user ID once at end.
- Preserve exact extension case where possible.
- Handle collisions by replace-with-confirmation + backup.

## 8. UI/UX Requirements (Small-Screen First)
### 8.1 Layout Principles
- Support width from 320dp and up.
- Single-column primary flows.
- No essential action below fold without sticky footer action.
- Minimum touch target: 48dp.
- Text scales up to 1.3x without clipping/truncating critical labels.

### 8.2 Navigation
- Bottom navigation tabs:
  - Emulators
  - Games
  - Users
  - History
- Deep flows use top app bar + back navigation.

### 8.3 Core Screens
1. **Emulator List**: cards with name, root path summary, game count.
2. **Add/Edit Emulator**: name field, folder picker, extension chips.
3. **User List**: simple list with add/edit/archive.
4. **Game List**: filter by emulator, search by filename, ownership badge.
5. **Game Detail**:
   - Current base save info (mtime, owner state)
   - Variant list with owners + mtimes
   - Primary `Switch User` CTA
6. **Switch Sheet/Dialog**:
   - Select current owner (if unknown)
   - Select target owner
   - Confirmation summary of file operations
7. **History Screen**: recent operations and rollback button.

### 8.4 Small-Screen Behavior
- Use modal bottom sheets (not wide dialogs).
- Truncate long paths with middle ellipsis and tap-to-expand.
- Place destructive actions in overflow menu with confirmation.
- Keep max 3 actions visible per row; overflow rest.

## 9. Data Model (Local)
Use Room database.

Tables:
- `emulators(id, name, tree_uri, extensions_json, created_at, updated_at)`
- `users(id, display_name, normalized_id, is_archived, created_at, updated_at)`
- `games(id, emulator_id, relative_dir, base_name, extension, last_seen_at)`
- `game_state(game_id, current_owner_user_id_nullable, last_switch_at, notes)`
- `switch_ops(id, game_id, emulator_id, source_owner_id, target_owner_id, started_at, ended_at, status, details_json)`

## 10. Permissions and Storage
- Use SAF tree URI persisted permissions (`takePersistableUriPermission`).
- Do not request broad storage permissions by default.
- Validate URI access on app start and before switch operations.

## 11. Error Handling
- Show actionable messages:
  - Permission lost
  - File missing during operation
  - Insufficient storage
  - Copy/rename failure
- Never silently discard current base save.
- If operation fails mid-way, restore from `.bak` automatically when possible.

## 12. Performance Requirements
- Incremental scan supported (full + delta scan).
- Scans should run off main thread with progress indicator.
- Initial scan target: <= 10s for 5,000 files on mid-range device (best effort).

## 13. Testing Requirements
### 13.1 Unit Tests
- Filename parsing/grouping
- User ID normalization
- Switch path resolution
- Conflict handling

### 13.2 Integration Tests
- SAF read/write mock flows
- Unknown-owner prompt logic
- Backup/restore on failures

### 13.3 UI Tests
- Small-screen snapshots (320dp width)
- Font scaling at 1.3x
- Switch flow end-to-end

## 14. Security and Privacy
- All metadata stored locally on device.
- No external network required in MVP.
- No upload of save files.

## 15. Open Decisions for v1 Implementation
- Behavior when target user has no variant:
  - Option A: delete/clear base to force fresh save
  - Option B: keep base until game first launch prompt
- Support for non-standard emulator save bundle patterns.
- Maximum retained rollback history per game.

## 16. Implementation Milestones
1. Core data model + SAF emulator setup
2. User management + game scan
3. Game detail + ownership metadata
4. Safe switch engine (backup, copy, replace)
5. History + rollback
6. UI polish and small-screen accessibility pass
7. Test suite and release candidate

