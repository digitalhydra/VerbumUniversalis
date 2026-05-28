#!/usr/bin/env python3
"""
Scrape Spanish versions of all CST documents from Vatican.va.
Reuses processing logic from scrape_cst.py.
"""

import os, sys, re, time, json, logging

# Add parent dir so we can import from scripts/
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# Import the processing functions from the main scraper
from scripts.scrape_cst import (
    BASE_DIR, DELAY, TIMEOUT, USER_AGENT, log,
    fetch_url, extract_content, strip_navigation,
    convert_to_markdown, clean_markdown, build_frontmatter,
    save_document, check_quality, build_index,
    BeautifulSoup, logging,
)

# Spanish document definitions - derived from English DOCUMENTS
DOCUMENTS_ES = [
    {
        "id": "compendium-social-doctrine-es",
        "title": "Compendio de la Doctrina Social de la Iglesia",
        "author": "Consejo Pontificio para la Justicia y la Paz",
        "date": "2004-10-25",
        "type": "compendium",
        "subdir": "compendium",
        "filename": "compendium-social-doctrine-es.md",
        "url": "https://www.vatican.va/roman_curia/pontifical_councils/justpeace/documents/rc_pc_justpeace_doc_20060526_compendio-dott-soc_sp.html",
    },
    {
        "id": "rerum-novarum-es",
        "title": "Rerum Novarum",
        "author": "Papa León XIII",
        "date": "1891-05-15",
        "type": "encyclical",
        "subdir": "encyclicals",
        "filename": "1891-rerum-novarum-es.md",
        "url": "https://www.vatican.va/content/leo-xiii/es/encyclicals/documents/hf_l-xiii_enc_15051891_rerum-novarum.html",
    },
    {
        "id": "quadragesimo-anno-es",
        "title": "Quadragesimo Anno",
        "author": "Papa Pío XI",
        "date": "1931-05-15",
        "type": "encyclical",
        "subdir": "encyclicals",
        "filename": "1931-quadragesimo-anno-es.md",
        "url": "https://www.vatican.va/content/pius-xi/es/encyclicals/documents/hf_p-xi_enc_19310515_quadragesimo-anno.html",
    },
    {
        "id": "mater-et-magistra-es",
        "title": "Mater et Magistra",
        "author": "Papa Juan XXIII",
        "date": "1961-05-15",
        "type": "encyclical",
        "subdir": "encyclicals",
        "filename": "1961-mater-et-magistra-es.md",
        "url": "https://www.vatican.va/content/john-xxiii/es/encyclicals/documents/hf_j-xxiii_enc_15051961_mater.html",
    },
    {
        "id": "pacem-in-terris-es",
        "title": "Pacem in Terris",
        "author": "Papa Juan XXIII",
        "date": "1963-04-11",
        "type": "encyclical",
        "subdir": "encyclicals",
        "filename": "1963-pacem-in-terris-es.md",
        "url": "https://www.vatican.va/content/john-xxiii/es/encyclicals/documents/hf_j-xxiii_enc_11041963_pacem.html",
    },
    {
        "id": "populorum-progressio-es",
        "title": "Populorum Progressio",
        "author": "Papa Pablo VI",
        "date": "1967-03-26",
        "type": "encyclical",
        "subdir": "encyclicals",
        "filename": "1967-populorum-progressio-es.md",
        "url": "https://www.vatican.va/content/paul-vi/es/encyclicals/documents/hf_p-vi_enc_26031967_populorum.html",
    },
    {
        "id": "laborem-exercens-es",
        "title": "Laborem Exercens",
        "author": "Papa Juan Pablo II",
        "date": "1981-09-14",
        "type": "encyclical",
        "subdir": "encyclicals",
        "filename": "1981-laborem-exercens-es.md",
        "url": "https://www.vatican.va/content/john-paul-ii/es/encyclicals/documents/hf_jp-ii_enc_14091981_laborem-exercens.html",
    },
    {
        "id": "sollicitudo-rei-socialis-es",
        "title": "Sollicitudo Rei Socialis",
        "author": "Papa Juan Pablo II",
        "date": "1987-12-30",
        "type": "encyclical",
        "subdir": "encyclicals",
        "filename": "1987-sollicitudo-rei-socialis-es.md",
        "url": "https://www.vatican.va/content/john-paul-ii/es/encyclicals/documents/hf_jp-ii_enc_30121987_sollicitudo-rei-socialis.html",
    },
    {
        "id": "centesimus-annus-es",
        "title": "Centesimus Annus",
        "author": "Papa Juan Pablo II",
        "date": "1991-05-01",
        "type": "encyclical",
        "subdir": "encyclicals",
        "filename": "1991-centesimus-annus-es.md",
        "url": "https://www.vatican.va/content/john-paul-ii/es/encyclicals/documents/hf_jp-ii_enc_01051991_centesimus-annus.html",
    },
    {
        "id": "laudato-si-es",
        "title": "Laudato Si'",
        "author": "Papa Francisco",
        "date": "2015-05-24",
        "type": "encyclical",
        "subdir": "encyclicals",
        "filename": "2015-laudato-si-es.md",
        "url": "https://www.vatican.va/content/francesco/es/encyclicals/documents/papa-francesco_20150524_enciclica-laudato-si.html",
    },
    {
        "id": "fratelli-tutti-es",
        "title": "Fratelli Tutti",
        "author": "Papa Francisco",
        "date": "2020-10-03",
        "type": "encyclical",
        "subdir": "encyclicals",
        "filename": "2020-fratelli-tutti-es.md",
        "url": "https://www.vatican.va/content/francesco/es/encyclicals/documents/papa-francesco_20201003_enciclica-fratelli-tutti.html",
    },
    {
        "id": "gaudium-et-spes-es",
        "title": "Gaudium et Spes",
        "author": "Concilio Vaticano II",
        "date": "1965-12-07",
        "type": "vatican-ii",
        "subdir": "vatican-ii",
        "filename": "gaudium-et-spes-es.md",
        "url": "https://www.vatican.va/archive/hist_councils/ii_vatican_council/documents/vat-ii_const_19651207_gaudium-et-spes_sp.html",
    },
]


def process_document(doc, html):
    """Process one Spanish document end-to-end (same logic as EN scraper)."""
    soup = BeautifulSoup(html, "html.parser")

    content_el = extract_content(soup)
    content_el = strip_navigation(content_el)

    markdown = convert_to_markdown(content_el)
    markdown = clean_markdown(markdown)

    return markdown


def main():
    log.info("=" * 60)
    log.info("CST Document Scraper - SPANISH")
    log.info(f"Base: {BASE_DIR}")
    log.info(f"Documents: {len(DOCUMENTS_ES)}")
    log.info("=" * 60)

    successful = []
    failed = []

    for i, doc in enumerate(DOCUMENTS_ES, 1):
        log.info(f"\n[{i}/{len(DOCUMENTS_ES)}] {doc['title']} (ES)")

        html = fetch_url(doc["url"])
        if html is None:
            failed.append((doc, "Network error"))
            continue

        markdown = process_document(doc, html)

        if len(markdown.strip()) < 200:
            failed.append((doc, "Empty content"))
            log.warning("  Content too short, skipping")
            continue

        path = save_document(markdown, doc)
        issues = check_quality(path, doc)
        successful.append((doc, path, issues))

        if i < len(DOCUMENTS_ES):
            log.info(f"  Waiting {DELAY}s...")
            time.sleep(DELAY)

    # Build index.json (append to or merge with existing)
    log.info("\n--- Building Spanish index ---")
    index_es = build_index(successful)
    index_path = os.path.join(BASE_DIR, "metadata", "index.json")

    # Merge with existing index if present
    if os.path.exists(index_path):
        with open(index_path, "r", encoding="utf-8") as f:
            try:
                existing = json.load(f)
                # Remove any existing ES entries
                existing = [e for e in existing if not e["id"].endswith("-es")]
                index_es = existing + index_es
            except json.JSONDecodeError:
                pass

    with open(index_path, "w", encoding="utf-8") as f:
        json.dump(index_es, f, indent=2, ensure_ascii=False)
    log.info(f"Updated index: {index_path} ({len(index_es)} entries)")

    # Summary
    log.info("\n" + "=" * 60)
    log.info(f"SPANISH - SUCCESSFUL: {len(successful)}/{len(DOCUMENTS_ES)}")
    for doc, path, issues in successful:
        status = "OK" if not issues else "WARN"
        log.info(f"  [{status}] {doc['title']}")
    if failed:
        log.info(f"FAILED: {len(failed)}/{len(DOCUMENTS_ES)}")
        for doc, reason in failed:
            log.info(f"  [FAIL] {doc['title']}: {reason}")
    log.info("Done.")

    return len(failed) == 0


if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)
