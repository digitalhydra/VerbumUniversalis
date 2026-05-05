# 📋 Phase 7 Specification: Missing Features & Polish

## 1. Goal

Complete all stubs, implement missing authentication (OAuth), and add Liturgical Daily Readings.

## 2. Complete Phase-4 Stubs (Study Tools)

### A. Catena Tab (Church Fathers)

- **Source:** Integrate "Catena Aurea" (Golden Chain) by Aquinas or similar open-source patristic database.
- **Data:** Parse XML/JSON of patristic commentaries keyed by verse reference.
- **UI:** Feed-style list in `CatenaView.kt`.
- **Status:** Currently a stub.
- if catena not downloaded yet add empty view message in catena view and button to download

### B. References Tab (Cross-References)

- **Source:** Use "Treasury of Scripture Knowledge" (TSK) or similar open-source cross-reference data.
- **Data:** Map of verse_id $\rightarrow$ list of reference verse IDs.
- **UI:** Clickable list in `ReferencesView.kt` that navigates to the referenced verse.
- **Status:** Currently a stub.

- if references not downloaded yet add empty view message in references view and button to download

## 3. Complete Phase-6 (Git Sync Advanced)

### A. GitHub OAuth Flow

- **Implementation:** Use `androidx.oauth:oauth2` or manual `CustomTabs` with GitHub OAuth endpoints.
- **Flow:** Redirect to GitHub $\rightarrow$ callback $\rightarrow$ store token in `EncryptedSharedPreferences`.
- **UI:** "Connect to GitHub" button in Settings.

### B. SSH Key Management

- **Generation:** Use `java.security.KeyPairGenerator` (RSA 4096 or ED25519).
- **Storage:** Private key in `EncryptedSharedPreferences`, Public key uploaded via GitHub API.
- **JGit Config:** Configure JGit to use the SSH key at `~/.ssh/id_rsa`.

### C. GitHub API Integration

- **Library:** Use `com.google.api-client:google-api-client` or simple `HttpURLConnection` for GitHub REST API.
- **Endpoints:**
  - List user repos (`GET /user/repos`).
  - Create repo (`POST /user/repos`).
  - Add deploy key (`POST /repos/{owner}/{repo}/keys`).

### D. Conflict Resolution (LWW)

- **Logic:** When pulling remote changes, compare `lastUpdated` timestamps in JSON objects.
- **Strategy:** Last Write Wins (LWW) - the object with the newest timestamp wins.
- **UI:** If both local and remote modified the same object, show a "Conflict Resolved" snackbar.

### E. New Device Scenario

- **Clone:** On first sync, `git clone` into `/userdata/`.
- **Collision:** If local data exists, move to `/userdata/backup_$timestamp/`.
- **Validation:** Ensure JSON integrity after clone (try-catch deserialization).

## 4. Missing Phase-1 Feature (Daily Liturgical Readings)

### A. Dashboard Daily Readings

- **Spec:** Section 1 of Dashboard should show "Today's Liturgical Reading" (based on calendar).
- **Source:** Integrate a liturgical calendar API or static JSON mapping (e.g., USCCB daily readings).
- **UI:** Display the reading reference (e.g., "John 3:16-21") with a "Read Now" button.

## 5. Acceptance Criteria (Definition of Done)

### Phase-4 Completion

1. [ ] Catena Tab loads real patristic commentaries for the selected verse.
2. [ ] References Tab loads real cross-references and allows navigation.
3. [ ] Feed-style layout works for long Catena texts.

### Phase-6 Completion

1. [ ] User can authenticate via GitHub OAuth (no manual token entry).
2. [ ] SSH keys are generated and uploaded as Deploy Keys.
3. [ ] Conflict resolution uses LWW strategy based on timestamps.
4. [ ] New device scenario handles local collisions gracefully.

### Phase-1 Completion

1. [ ] Dashboard shows "Today's Liturgical Reading" based on the current date.
2. [ ] Liturgical reading data is fetched from an API or local JSON.

## 6. Implementation Order

1. **Phase-7 Step-1:** Implement Catena data source and UI.
2. **Phase-7 Step-2:** Implement References data source and UI.
3. **Phase-7 Step-3:** Add Liturgical Daily Readings to Dashboard.
4. **Phase-7 Step-4:** Implement GitHub OAuth flow.
5. **Phase-7 Step-5:** Implement SSH key generation and GitHub API.
6. **Phase-7 Step-6:** Implement LWW conflict resolution.
