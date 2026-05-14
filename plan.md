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
- Implemented settings screen submenus (Download Catena, Sync, Notes, Theme) with wired navigation (onNavigate → navController.navigate)
- Update app launcher icons using new assets from ./AppIcons/android/mipmap-*
- Removed gear icon from dashboard top bar; settings access is via MainScreen.kt NavigationBar (Options item → Settings)
- Removed duplicate BottomAppBar from DashboardScreen; fixed layout by wrapping content in parent Column to prevent header/readings overlap
- Implemented Download Catena screen with DownloadCatenaViewModel (fixed ClassCastException from hiltViewModel on non-ViewModel)
- Implemented Notes screen with NotesViewModel (fixed same ClassCastException); real FileManager/BibleRepository wired
- Implemented Theme screen with light/dark/system theme switching