# Missing Parts - Verbum Universalis

## Completed ✅

### Phase 2 - ETL & Database
- [x] **ETL script** - Generate `verbum_seed.db` from raw SWORD data (deterministic, same MD5)
- [x] **Latin Vulgate Clementina** - Added VUL texts to `texts` table
- [x] **Masoretic Hebrew** - Added OSHB interlinear + morphology (no separate Strong's needed)
- [x] **Greek Lexicon** - Added Strong's Greek (5,624 entries)
- [x] **Semantic Tag Legend** - Created `tags_legend.json` (20-color palette)

### Phase 3 - Reading Canvas
- [x] **3-Language Toggle** - EN (DRB), ES (Platense), LA (Vulgata) dropdown
- [x] **Last read passage persistence** - Saves to DataStore

### Phase 4 - Interlinear & Adaptive
- [x] **Hebrew Interlinear** - OSHB word blocks with lemma/morphology
- [x] **Greek Interlinear** - ABPGRK word blocks with lemma/morphology
- [x] **Catena Tab** - Data layer + UI complete (137K entries, ordered by author, period shown)
- [x] **References Tab** - Data layer + UI complete
- [x] **Tradition toggle** - Filter Catena by tradition (Catholic/Orthodox/Protestant)

### Phase 5 - User Data & Reading Plans
- [x] **Reading plan JSON files** - `bible-in-a-year.json` + era grouping
- [x] **Era-based UI** - 12 historical periods
- [x] **Semantic highlight rendering** - 20 desaturated colors

### Phase 1/7 - Dashboard & Liturgical
- [x] **Daily Liturgical Readings** - `liturgical_calendar.json` in assets
- [x] **Tier 3 Calendar API** - Static JSON (USCCB, CEC Colombia, calapi.inadiutorium.cz)
- [x] **Daily Mass Readings** - Bundled 2026, date picker

### Tier 2 - Catena & Cross-Refs Downloads
- [x] **Separate DBs** - verbum_catena.db, verbum_cross_refs.db (NOT bundled)
- [x] **On-demand download** - Prompts user to download when opening Catena/References tab
- [x] **URLs configured** - Points to digitalhydra/verbum-data repo

---

## Still Missing ❌

### Phase 6/7 - Git Sync (v1 - Manual Deploy Key)
- [x] **SSH Deploy Key** - Generate key, copy to clipboard, link to GitHub
- [x] **Repo URL config** - Input SSH URL in settings
- [x] **LWW conflict resolution** - Timestamp-based merging
- [x] **New device clone scenario** - Backup collision handling

### Phase 6/7 - Git Sync (v2 - GitHub API)
- [ ] **GitHub API integration** - Auto-add deploy key, create repo (optional)

### Phase 1/7 - Dashboard & Liturgical
- [x] **Liturgical reading navigation** - Auto-jump to daily readings (tapping navigates to ReadingScreen)
- [x] **Mass readings flow** - Shows First Reading, Psalm, Gospel with Next navigation
- [x] **Bible in a Year flow** - Navigate through daily readings with Next Day button

---

## Implementation Order (Complete)
1. ✅ **ETL Seed Generator** (Phase 2)
2. ✅ **Catena Data Source** (Phase 4)
3. ✅ **References Data Source** (Phase 4)
4. ✅ **Daily Mass Readings UI** (Phase 1)
5. ✅ **3-Language Toggle** (Phase 3)
6. ✅ **Tradition Toggle** (Phase 4)
7. ✅ **Download URLs** (Tier 2)
8. ✅ **Last Read Persistence** (Phase 3)

---

## Next Steps
1. GitHub OAuth + SSH sync (Phase 6/7)
2. Liturgical auto-jump (Phase 1/7)
3. **TODO:** Upload `verbum_catena.db` + `verbum_cross_refs.db` to digitalhydra/verbum-data repo and update URLs in DataDownloader.kt
