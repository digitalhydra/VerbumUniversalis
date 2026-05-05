# 📋 Phase 6 Specification: Git Sync Integration

## 1. Goal
Implement a professional, user-owned cloud backup system that allows the user to sync their local JSON data to a private GitHub repository using OAuth and SSH Deploy Keys.

## 2. Authentication & Repository Flow

### A. The OAuth Onboarding
Instead of manual token entry, the app implements a secure OAuth flow:
1. **GitHub Redirect:** The user is redirected to GitHub via `Custom Tabs` to authorize the Verbum Universalis app.
2. **Repo Selection/Creation:** Through the GitHub API, the user can:
    - Select an existing private repository.
    - Create a new private repository specifically for their Bible study.
3. **SSH Key Generation:** 
    - The app generates a unique SSH Key Pair (RSA/ED25519) locally on the device.
    - The **Private Key** is stored in `EncryptedSharedPreferences`.
    - The **Public Key** is automatically uploaded to the selected GitHub repository as a **Deploy Key** with write access via the GitHub API.

### B. Sync Triggers
- **Immediate:** If a connection is available, changes to `.json` files are pushed automatically after a short debounce period.
- **Fallback:** If offline, sync is attempted on the next app load.
- **Manual:** A "Sync Now" button in the Settings page.

## 3. Sync Logic & Conflict Resolution

### A. The Sync Pipeline
The app follows a strict `Pull` $\rightarrow$ `Merge` $\rightarrow$ `Push` pipeline:
1. **Pull:** Fetch the latest state from the remote repository.
2. **Conflict Check:** If remote changes exist and differ from local changes:
    - **Light Conflict:** User is warned that remote changes were found.
    - **Heavy Conflict:** If the same JSON file was modified on two devices, the app applies a **Last Write Wins (LWW)** strategy based on the timestamp metadata inside the JSON objects.
3. **Push:** Commit the merged local state and push to the remote.

### B. The "New Device" Scenario (Collision Handling)
When a user syncs on a new device for the first time:
1. **Clone:** The app performs a `git clone` of the repository into the `/userdata/` folder.
2. **Local Data Collision:** If the device already contains local JSON data (unsynced):
    - The app moves the existing local data to a `/userdata/backup_timestamp/` folder.
    - The remote data is cloned into the main folder.
    - The user is notified that their local data was archived to prevent loss.
3. **Validation:** The app validates the JSON integrity before loading the data into the UI.

## 4. UI & Settings

### A. Sync Settings Page
- **Account Status:** "Connected to GitHub as [User]" with a "Disconnect" button.
- **Repository Info:** Displays the name and visibility (Private/Public) of the synced repo.
- **Visibility Warning:** A prominent warning if the repository is Public, advising the user to make it Private to protect personal notes.
- **Sync Logs:** A simple text view showing the last 5 sync events (e.g., "Push successful", "Pull conflict resolved").

## 5. Technical Architecture

### A. GitSyncManager (JGit)
- **SSH Transport:** Configured to use the private key from `EncryptedSharedPreferences` for all Git operations.
- **Atomic Commits:** All changes are bundled into a single commit per sync session to keep the Git history clean.

### B. SyncWorker (WorkManager)
- A background worker that manages the network-aware synchronization and handles the `Pull` $\rightarrow$ `Push` cycle.

## 6. Acceptance Criteria (Definition of Done)
1. [ ] User can authenticate via GitHub OAuth and select/create a private repository.
2. [ ] SSH Deploy Keys are generated and uploaded correctly via API.
3. [ ] User data is successfully pushed to and pulled from the remote repository.
4. [ ] A new device can clone existing data and handle local collisions via the backup folder.
5. [ ] "Pull before Push" works, and LWW resolves heavy conflicts.
6. [ ] The Settings page clearly displays sync status and repository visibility warnings.
7. [ ] Sync triggers immediately on connection or manually via settings.
