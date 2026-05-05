# 🛠️ ETL Pipeline: Verbum Seed DB

This script generate `verbum_seed.db` for offline-first mode.

## 🚀 How Run
1. Need Python 3.
2. Run command:
   ```bash
   python3 etl.py
   ```

## 📦 Result
- Script make `verbum_seed.db` in root.
- Move file to `src/main/assets/` for Android app use.

## 🏗️ DB Structure
- `books`: Metadata for Bible books.
- `verses`: Unique anchors for every verse.
- `texts`: Translation content (DR, SCIO, VL).
- `interlinear_words`: Morphology and original text.
- `lexicon`: Root definitions.

## ⚠️ Note
Current `etl.py` use sample data (Gen 1:1). For full Bible, update `etl.py` with raw JSON/Text sources before run.
