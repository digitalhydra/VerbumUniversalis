# 🛠️ ETL Pipeline: Verbum Seed DB

This pipeline generates `verbum_seed.db`, providing offline-first Bible texts, interlinear data, and theological lexicons.

## 🚀 Quick Start
1. Install dependencies: `pip install pysword`
2. Run build:
   ```bash
   python3 etl/build_verbum_seed.py
   ```

Detailed instructions, data requirements, and schema info are available in [**etl/README.md**](etl/README.md).

## 🏗️ DB Overview
- `books`: Metadata for 73 Catholic books.
- `verses`: Unique anchors for every verse.
- `texts`: Translation content (DR, PLA, VL).
- `interlinear_words`: Greek/Hebrew text + Transliteration + English Gloss.
- `lexicon`: Strong's definitions.
