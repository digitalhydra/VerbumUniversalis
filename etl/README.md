# 🏗️ Verbum Seed ETL Pipeline

This directory contains the pipeline to generate `verbum_seed.db`, the offline-first SQLite database for the Verbum Universalis Android app.

## 📋 Prerequisites

1.  **Python 3.10+**
2.  **pysword**: Python library for reading SWORD modules.
    ```bash
    pip install pysword
    ```
3.  **sqlite3**: CLI tool for verifying outputs (optional).
4.  **Raw Data**: Ensure `raw_data/` contains:
    - SWORD modules in `_extracted/modules/` (drc, spaplatense, vulgclementine, abpgrk, oshb).
    - Strong's Concordance XMLs:
        - `strongs-master/greek/StrongsGreekDictionaryXML_1.4/strongsgreek.xml`
        - `strongs-master/hebrew/StrongHebrewG.xml`

## 🚀 How to Run

Execute the main build script from the root or the `etl/` directory:

```bash
cd etl/
python3 build_verbum_seed.py
```

## 🏗️ Pipeline Phases

1.  **Fresh DB Creation**: Initializes the schema with `NOT NULL` constraints to match Room entities.
2.  **SWORD Text Parsing**: Extracts Bible texts for Douay-Rheims (EN), Platense (ES), and Vulgata (LA).
3.  **Interlinear Extraction**: Parses ABPGRK (Greek) and OSHB (Hebrew) OSIS tags to populate `interlinear_words`.
4.  **Automated Transliteration**: Generates phonetic pronunciations for all 424,000+ words using custom algorithmic mapping for Greek and Hebrew scripts.
5.  **Strong's Glossing**: Map Concise English meanings from Strong's XMLs into the `literal` column of `interlinear_words`.
6.  **Full Lexicon Build**: Populates the `lexicon` table with complete theological definitions.

## 📦 Output

- **Primary Output**: `verbum_seed.db` in the project root.
- **Auto-Asset Update**: The script automatically copies the result to `app/src/main/assets/verbum_seed.db` for the next Android build.

## ⚠️ Known Issues & Notes

- **Timeouts**: The script processes ~425k rows. On slower disks, it may take 5-10 minutes. 
- **Determinism**: The pipeline is deterministic; running it with the same inputs will produce an identical binary DB.
