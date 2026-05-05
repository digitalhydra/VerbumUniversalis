# Missing Parts - Verbum Universalis

## Completed ✅

### Phase 2 - ETL & Database
- [x] **ETL script** - Generate `verbum_seed.db` from raw SWORD data
- [x] **Latin Vulgate Clementina** - Added VUL texts to `texts` table
- [x] **Masoretic Hebrew** - Added Hebrew interlinear + morphology
- [x] **Strong's/Thayer's Lexicon** - Populated `lexicon` table (14,197 entries)
- [x] **Semantic Tag Legend** - Created `tags_legend.json` (20-color palette)

### Phase 4 - Interlinear & Adaptive
- [x] **Hebrew Interlinear** - Masoretic text word blocks (data loaded)
- [x] **Catena Tab** - Data layer + UI complete (137K entries, ordered by author, period shown)

### Phase 5 - User Data & Reading Plans
- [x] **Reading plan JSON files** - `bible-in-a-year.json` + era grouping in `assets/plans/`
- [x] **Era-based UI** - 12 historical periods in plan data
- [x] **Semantic highlight rendering** - 20 desaturated colors from `tags_legend.json`

### Phase 1/7 - Dashboard & Liturgical
- [x] **Daily Liturgical Readings** - `liturgical_calendar.json` in assets
- [x] **Tier 3 Calendar API** - Static JSON (USCCB, CEC Colombia, calapi.inadiutorium.cz)

---

## Still Missing ❌

### Phase 3 - Reading Canvas
- [ ] **Latin Canvas toggle** - Add LA (Vulgata) to language toggle
- [ ] **Last read passage persistence** - Save/restore position

### Phase 4 - Interlinear & Adaptive
- [ ] **References Tab** - TSK cross-reference data (`references.json` exists, needs UI)
- [ ] **Tradition toggle** - Show/hide Catena in Study Inspector

### Phase 6/7 - Git Sync
- [ ] **GitHub OAuth flow** - Custom Tabs + callback
- [ ] **SSH key generation** - RSA/ED25519 + Deploy Key upload
- [ ] **GitHub API integration** - List/create repos, add keys
- [ ] **LWW conflict resolution** - Timestamp-based merging
- [ ] **New device clone scenario** - Backup collision handling

### Phase 1/7 - Dashboard & Liturgical
- [ ] **Liturgical reading navigation** - Auto-jump to daily readings (UI)

### Tier 2 - Catena Commentary Engine (Not in any phase)
- [ ] **Rolling cache** - Current month auto-download
- [ ] **Manual download UI** - Full book downloads
- [ ] **CCEL dataset parsing** - XML/JSON patristic data

### Phase 2 - ETL & Database
- [ ] **Spanish (SpaScioNT)** - LZSS compression not supported by pysword, needs custom parser

---

## Implementation Order
1. ~~**ETL Seed Generator** (Phase 2 blocker)~~ ✅ DONE
2. ~~**Catena Data Source (Phase 4/7)**~~ ✅ DONE
3. ~~**References Data Source (Phase 4/7)**~~ ✅ DONE (data exists)
4. **References Tab UI** - Wire up `references.json` to Study Inspector
5. **GitHub OAuth + SSH** (Phase 6/7)
6. **Daily Liturgical Readings UI** (Phase 1/7)
7. Remaining gaps...
