## Phase 1: Backend Fix
- Resolve DTA-2894: Fix franchisee_superuser role_definitions to grant editPromotionsLocked
- Validate permissions suite in production

## Phase 2: Data Upload
- Deploy verbum_catena.db and verbum_cross_refs.db to GitHub releases (https://github.com/digitalhydra/VerbumUniversalis/releases)
- App downloads these files from GitHub releases using versioned URLs:
    * verbum_catena.db: https://github.com/digitalhydra/VerbumUniversalis/releases/download/v1.0.0/verbum_catena.db
    * verbum_cross_refs.db: https://github.com/digitalhydra/VerbumUniversalis/releases/download/v1.0.0/verbum_cross_refs.db
- Update DataDownloader.kt to use release URLs instead of raw.githubusercontent.com
- Add versioned cloud sync endpoints

## Phase 3: Delivery
- Package for beta testing
- Finalize SSH key integration
- Complete LWW conflict resolution
- Implement settings screen with submenus: Download Catena, Sync, Notes, Theme
- Update app launcher icons using new assets from ./AppIcons/android/mipmap-*
- Changed settings access from gear icon in dashboard to bottom navigation bar (Settings icon)
- Implemented Download Catena screen with actual download functionality from GitHub releases