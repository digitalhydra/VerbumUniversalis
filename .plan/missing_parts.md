# Missing Parts - Verbum Universalis

## Critical Path Gaps

### Phase 2 - ETL & Database
- [x] **ETL script** - Generate `verbum_seed.db` from raw SWORD data
- [x] **Latin Vulgate Clementina** - Added VUL texts to `texts` table
- [x] **Masoretic Hebrew** - Added Hebrew interlinear + morphology
- [ ] **Strong's/Thayer's Lexicon** - Populate `lexicon` table with lemma definitions
- [ ] **Semantic Tag Legend** - Create `tags_legend.json` for 20-color palette
- [ ] **Spanish (SpaScioNT)** - LZSS compression not supported by pysword, needs custom parser

### Phase 3 - Reading Canvas
- [ ] **Latin Canvas toggle** - Add LA (Vulgata) to language toggle
- [ ] **Last read passage persistence** - Save/restore position

### Phase 4 - Interlinear & Adaptive
- [ ] **Catena Tab** - Real data from CCEL (patristic commentaries)
- [ ] **References Tab** - TSK cross-reference data
- [ ] **Hebrew Interlinear** - Masoretic text word blocks
- [ ] **Tradition toggle** - Show/hide Catena in Study Inspector

### Phase 5 - User Data & Reading Plans
- [ ] **Reading plan JSON files** - `bible_in_a_year.json` + era grouping
- [ ] **Era-based UI** - 12 historical periods display
- [ ] **Semantic highlight rendering** - 20 desaturated colors from legend

### Phase 6/7 - Git Sync
- [ ] **GitHub OAuth flow** - Custom Tabs + callback
- [ ] **SSH key generation** - RSA/ED25519 + Deploy Key upload
- [ ] **GitHub API integration** - List/create repos, add keys
- [ ] **LWW conflict resolution** - Timestamp-based merging
- [ ] **New device clone scenario** - Backup collision handling

### Phase 1/7 - Dashboard & Liturgical
- [ ] **Daily Liturgical Readings** - Dashboard Section 1
- [ ] **Tier 3 Calendar API** - USCCB, CEC Colombia, calapi.inadiutorium.cz
- [ ] **Liturgical reading navigation** - Auto-jump to daily readings

### Tier 2 - Catena Commentary Engine (Not in any phase)
- [ ] **Rolling cache** - Current month auto-download
- [ ] **Manual download UI** - Full book downloads
- [ ] **CCEL dataset parsing** - XML/JSON patristic data

---

## Implementation Order
1. **ETL Seed Generator** (Phase 2 blocker)
2. Catena Data Source (Phase 4/7)
3. References Data Source (Phase 4/7)
4. GitHub OAuth + SSH (Phase 6/7)
5. Daily Liturgical Readings (Phase 1/7)
6. Remaining gaps...
