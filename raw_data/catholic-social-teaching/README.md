# Catholic Social Teaching Documents

A curated, machine-readable collection of official Catholic Social Teaching documents in Markdown format, sourced directly from [vatican.va](https://www.vatican.va). Available in **English** and **Spanish**.

## Contents

### Compendium

| File | Size | Lang | Description |
|------|------|------|-------------|
| `compendium/compendium-social-doctrine.md` | 1.0 MB | EN | Compendium of the Social Doctrine of the Church (2004) |
| `compendium/compendium-social-doctrine-es.md` | 964 KB | ES | Compendio de la Doctrina Social de la Iglesia |

### Encyclicals (chronological)

| File | Pope | Year | Lang |
|------|------|------|------|
| `encyclicals/1891-rerum-novarum.md` | Leo XIII | 1891 | EN |
| `encyclicals/1891-rerum-novarum-es.md` | León XIII | 1891 | ES |
| `encyclicals/1931-quadragesimo-anno.md` | Pius XI | 1931 | EN |
| `encyclicals/1931-quadragesimo-anno-es.md` | Pío XI | 1931 | ES |
| `encyclicals/1961-mater-et-magistra.md` | John XXIII | 1961 | EN |
| `encyclicals/1961-mater-et-magistra-es.md` | Juan XXIII | 1961 | ES |
| `encyclicals/1963-pacem-in-terris.md` | John XXIII | 1963 | EN |
| `encyclicals/1963-pacem-in-terris-es.md` | Juan XXIII | 1963 | ES |
| `encyclicals/1967-populorum-progressio.md` | Paul VI | 1967 | EN |
| `encyclicals/1967-populorum-progressio-es.md` | Pablo VI | 1967 | ES |
| `encyclicals/1981-laborem-exercens.md` | John Paul II | 1981 | EN |
| `encyclicals/1981-laborem-exercens-es.md` | Juan Pablo II | 1981 | ES |
| `encyclicals/1987-sollicitudo-rei-socialis.md` | John Paul II | 1987 | EN |
| `encyclicals/1987-sollicitudo-rei-socialis-es.md` | Juan Pablo II | 1987 | ES |
| `encyclicals/1991-centesimus-annus.md` | John Paul II | 1991 | EN |
| `encyclicals/1991-centesimus-annus-es.md` | Juan Pablo II | 1991 | ES |
| `encyclicals/2015-laudato-si.md` | Francis | 2015 | EN |
| `encyclicals/2015-laudato-si-es.md` | Francisco | 2015 | ES |
| `encyclicals/2020-fratelli-tutti.md` | Francis | 2020 | EN |
| `encyclicals/2020-fratelli-tutti-es.md` | Francisco | 2020 | ES |

### Vatican II Documents

| File | Lang | Description |
|------|------|-------------|
| `vatican-ii/gaudium-et-spes.md` | EN | Pastoral Constitution on the Church in the Modern World |
| `vatican-ii/gaudium-et-spes-es.md` | ES | Constitución Pastoral sobre la Iglesia en el Mundo Actual |

### Metadata

| File | Description |
|------|-------------|
| `metadata/index.json` | Machine-readable index of all 24 documents |

## File Format

Every Markdown file includes:

- **YAML frontmatter** with title, author, date, source URL, and document type
- **Preserved paragraph numbers** in bold (e.g., **1.**)
- **Attribution** to the Holy See

Example frontmatter:
```yaml
---
title: "Rerum Novarum"
author: "Pope Leo XIII"
date: 1891-05-15
url: https://www.vatican.va/content/leo-xiii/en/encyclicals/documents/...
type: encyclical
source: "Official text from the Holy See – vatican.va"
---
```

## Usage

```bash
# Count words in all documents
wc -w compendium/*.md encyclicals/*.md vatican-ii/*.md

# Search across all documents (English)
grep -r "subsidiarity" encyclicals/*.md

# Search across all documents (Spanish)
grep -r "subsidiariedad" encyclicals/*-es.md

# Process with Python
python3 -c "
import json
with open('metadata/index.json') as f:
    docs = json.load(f)
for d in docs:
    print(f\"{d['date'][:4]} {d['type']:12s} {d['file']}\")
"
```

## Re-scraping

To re-scrape or update all documents:

```bash
pip install requests beautifulsoup4 html2text
# English
python3 scripts/scrape_cst.py
# Spanish
python3 scripts/scrape_cst_es.py
```

The script respects `robots.txt`, adds 2.5-second delays between requests, and logs all activity to `scripts/scrape.log`.

## Source

All documents are sourced from the official Vatican website (vatican.va). These are public Church documents in the public domain.

**Official text from the Holy See – vatican.va**
