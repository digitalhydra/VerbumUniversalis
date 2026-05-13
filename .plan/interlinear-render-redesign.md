# Interlinear Reader — 3-Layer Word Block Redesign

**Date**: 2026-05-12
**Status**: Planning

## Goal

Redesign the interlinear word block to show exactly 3 layers per word:
1. **Top**: Pronunciation / Transliteration (e.g., "en arch-ay")
2. **Middle**: Original Greek word (e.g., "ἐν ἀρχῇ") — largest, bold
3. **Bottom**: English literal gloss (e.g., "in the beginning")

Morphology hidden by default (can be toggled later). Grammar metadata left for a future option.

## Current State Analysis

### Data (verbum_seed.db: interlinear_words table)
| Field | Populated | Status |
|-------|-----------|--------|
| original | ✅ 424,972/424,972 | Greek/Hebrew word text |
| transliteration | ❌ 0/424,972 | Empty — needs generation |
| literal | ❌ 0/424,972 | Empty — needs generation |
| morphology | ⚠️ 289,644/424,972 | Partial (68%) |
| lemma | ✅ 424,972/424,972 | Strong's number (e.g., "strong:G976") |

### Current UI (InterlinearWordBlock.kt)
Renders 5 elements top-to-bottom:
```
┌─ MORPH badge ─┐
│  LITERAL       │  ← mostly shows "-" (data missing)
│  ORIGINAL      │  ← largest
│  TRANSLIT      │  ← mostly shows nothing (data missing)
│  STRONG'S      │
└────────────────┘
```

### SWORD Module Investigation (Answer: NO, data not in modules)

**ABPGRK (Greek NT)**: Only `lemma="strong:GXXXX"` + `src="N"` attributes. NO translit, NO morph, NO gloss.
```xml
<w lemma="strong:G976" src="1">βίβλος</w>
```

**OSHB (Hebrew OT)**: Has `lemma="strong:HXXXXX"` + `morph="oshm:..."`. NO translit, NO gloss.
```xml
<w lemma="strong:H07225" morph="oshm:HR/Ncfsa">בְּרֵאשִׁ֖ית</w>
```

**Conclusion**: Neither SWORD interlinear module has transliteration or English gloss data. Must generate both from other sources.

### Available Data Sources

**Transliteration**
- Strong's Greek XML entries have `<greek translit="..."/>` — but this is for the **lemma root** (dictionary headword), not the inflected verse form
  - Example: Verse has `εγέννησε` (egennēse), lemma G1080's Strong's entry shows `γεννάω` (gennáō) — different words!
  - **Decision**: Use algorithmic Greek→Latin character mapping on the actual `original` text for accuracy
- **Hebrew**: Similar situation — Strong's Hebrew XML has `xlit` on entries, but algorithmic mapping needed for verse forms

**English Glosses (literal translation)**
- **Strong's Greek XML** (`strongs-master/greek/StrongsGreekDictionaryXML_1.4/strongsgreek.xml`, 5,624 entries)
  - Each entry has `<kjv_def>` with concise English equivalents: `--book.`, `--child`, `--and`, `--the` etc.
  - **98.1% coverage**: 4,744/4,834 unique Greek lemmas found in Strong's
  - Mapping: `strong:G976` → number `976` → lookup Strong's entry → extract `kjv_def`
- **Strong's Hebrew XML** (`strongs-master/hebrew/StrongHebrewG.xml`)
  - Uses OSIS format with `<w gloss="..." xlit="...">` — but `gloss` references TWOT numbers, not English words
  - Definition items in `<list><item>` tags. First item is the primary gloss.
  - Coverage TBD for Hebrew (284K words, format `strong:HXXXXX`)

**Lexicon table**: Currently 0 entries in verbum_seed.db (ETL's `parse_strongs()` never ran successfully or DB was rebuilt without it).

## Implementation Plan

### Step 1: ETL — Generate Transliteration from Original Text

**File**: `etl/build_verbum_seed.py`

**Why algorithmic, not from Strong's**: The Strong's XML has transliteration for **lemma headwords** (dictionary forms). But the verse contains **inflected forms** which differ. Example:
- Verse word: `εγέννησε` → should be `egennēse` (inflected: "he begat")
- Lemma G1080 Strong's entry: `γεννάω` → `gennáō` (infinitive: "to beget")

**Approach**: Write a pure Python function that maps Greek Unicode characters (including polytonic diacritics) to Latin (SBL-style). Operate directly on the `original` text column in the DB.

```python
def transliterate_greek(text: str) -> str:
    """
    SBL-style transliteration: Greek polytonic → Latin.
    Handles: letters, breathing marks, accents, iota subscript.
    """
    mapping = {
        # Alpha
        'α': 'a', 'ἀ': 'a', 'ἁ': 'ha', 'ἂ': 'a', 'ἃ': 'ha',
        'ἄ': 'a', 'ἅ': 'ha', 'ἆ': 'a', 'ἇ': 'ha',
        'ὰ': 'a', 'ά': 'a', 'ᾶ': 'a', 'ᾷ': 'a',
        'Α': 'A', 'Ἀ': 'A', 'Ἁ': 'Ha', 'Ἂ': 'A', 'Ἃ': 'Ha',
        'Ἄ': 'A', 'Ἅ': 'Ha', 'Ἆ': 'A', 'Ἇ': 'Ha',
        # ... full mapping for all Greek chars including diacritics
    }
    result = []
    for ch in text:
        if ch in mapping:
            result.append(mapping[ch])
        elif ch.isspace():
            result.append(ch)
    return ''.join(result)
```

Add post-processing step after interlinear insertions:
```python
conn.execute("UPDATE interlinear_words SET transliteration = ? WHERE transliteration IS NULL",
             (transliterate_greek(row[0]),))
```

Also handle Hebrew transliteration similarly (separate function).

**Alternative**: Could use the `translit` attribute from `<greek>` elements in Strong's XML for Greek words that match exactly, but algorithmic is more reliable for inflected forms and has 100% coverage.

### Step 2: ETL — Generate English Glosses from Strong's Lexicon

**File**: `etl/build_verbum_seed.py`

**Source**: Strong's Greek XML (`strongsgreek.xml`) — 98.1% coverage of Greek lemmas. Each entry has `<kjv_def>` with concise English equivalents.

**Approach**:
1. Parse Strong's Greek XML → build in-memory dict: `lemma_number → kjv_gloss`
2. Lemma format normalization:
   - Interlinear: `strong:G976` → extract `976`
   - Strong's XML: `strongs="https://976"` or `strongs="00976"` → normalize with `int()`
3. Extract `kjv_def` from Strong's, clean formatting (`--book.` → `book`)
4. Update interlinear_words after all insertions:

```sql
-- After parsing Strong's and building the gloss dict:
UPDATE interlinear_words SET literal = ? WHERE lemma = ?
```

**For Hebrew**: Parse Strong's Hebrew XML — extract first `<item>` from `<list>` as gloss. Format: `strong:H07225` → extract `7225` → lookup.

**Fallback for missing lemmas** (~1.9% Greek, TBD for Hebrew): Leave `literal` NULL, UI renders `original` text instead.

Note: This also populates the `lexicon` table with full Strong's definitions (the existing `parse_strongs()` function already does this — it just never ran on the current DB). The gloss for interlinear uses a trimmed `kjv_def` version.

### Step 3: ETL — Rebuild Seed DB

Run the full ETL pipeline to produce updated `verbum_seed.db` with populated `transliteration` and `literal` fields.

### Step 4: UI — Redesign InterlinearWordBlock

**File**: `app/src/main/java/com/verbum/universalis/ui/reader/InterlinearWordBlock.kt`

New layout (top to bottom, centered):
```
┌──────────────────────┐
│  agathós             │  ← pronunciation: italic, gray, 12sp
│                      │
│  ἀγαθός              │  ← original: serif, bold, 20sp, primary color
│                      │
│  good                │  ← literal/gloss: serif, 14sp, green-ish
└──────────────────────┘
```

Remove:
- Morphology badge (hidden by default)
- Strong's number display
- Current literal position

Keep:
- Rounded corner card (4dp radius, slight padding)
- Selection/highlight states
- Click handler
- FlowRow wrapping

### Step 5: UI — Add Morphology Toggle (Optional / Phase 2)

Add a compact toggle in the interlinear toolbar to show/hide morphology badges below the literal translation. Can be a simple chip or icon button.

## Files Changed

| File | Change |
|------|--------|
| `etl/build_verbum_seed.py` | Add transliteration function + literal gloss extraction from Strong's XML + UPDATE steps |
| `app/src/.../reader/InterlinearWordBlock.kt` | Redesign to 3-layer layout |
| `app/src/.../reader/ReadingCanvas.kt` | Update call to InterlinearWordBlock (remove showMorphology, new param names) |
| `app/src/main/assets/verbum_seed.db` | Regenerated with populated fields |

## Acceptance Criteria

1. [ ] Each Greek interlinear word renders: pronunciation (top), original Greek (middle), English gloss (bottom)
2. [ ] Transliteration is algorithmic and consistent (SBL style)
3. [ ] English glosses come from Strong's dictionary (KJV definitions)
4. [ ] 3-layer layout is clean, centered, properly spaced
5. [ ] Words wrap naturally in FlowRow
6. [ ] Selection/highlight states still work
7. [ ] No regressions: verse navigation, language toggle, study inspector
8. [ ] Build passes: `./gradlew assembleDebug`

## Estimated Effort

- ETL changes: ~2 hours (transliteration function + Strong's XML parsing + gloss extraction)
- UI changes: ~1 hour (InterlinearWordBlock redesign)
- Testing/rebuild: ~1 hour
- **Total**: ~4 hours
