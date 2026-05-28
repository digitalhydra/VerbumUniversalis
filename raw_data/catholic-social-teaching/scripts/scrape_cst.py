#!/usr/bin/env python3
"""
Scrape Catholic Social Teaching documents from Vatican.va v3.
Better old-layout handling: extract content outside navigation tables.
"""

import os, re, time, json, sys, logging
from datetime import datetime

import requests
from bs4 import BeautifulSoup, NavigableString, Tag
import html2text

# --- Config ---
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DELAY = 2.5
TIMEOUT = 60
USER_AGENT = "CST-Scraper/1.0 (educational project; respectful crawl)"

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler(os.path.join(BASE_DIR, "scripts", "scrape.log"), mode="w"),
    ],
)
log = logging.getLogger(__name__)

DOCUMENTS = [
    {
        "id": "compendium-social-doctrine",
        "title": "Compendium of the Social Doctrine of the Church",
        "author": "Pontifical Council for Justice and Peace",
        "date": "2004-10-25",
        "type": "compendium",
        "subdir": "compendium",
        "filename": "compendium-social-doctrine.md",
        "url": "https://www.vatican.va/roman_curia/pontifical_councils/justpeace/documents/rc_pc_justpeace_doc_20060526_compendio-dott-soc_en.html",
    },
    {
        "id": "rerum-novarum",
        "title": "Rerum Novarum",
        "author": "Pope Leo XIII",
        "date": "1891-05-15",
        "type": "encyclical",
        "subdir": "encyclicals",
        "filename": "1891-rerum-novarum.md",
        "url": "https://www.vatican.va/content/leo-xiii/en/encyclicals/documents/hf_l-xiii_enc_15051891_rerum-novarum.html",
    },
    {
        "id": "quadragesimo-anno",
        "title": "Quadragesimo Anno",
        "author": "Pope Pius XI",
        "date": "1931-05-15",
        "type": "encyclical",
        "subdir": "encyclicals",
        "filename": "1931-quadragesimo-anno.md",
        "url": "https://www.vatican.va/content/pius-xi/en/encyclicals/documents/hf_p-xi_enc_19310515_quadragesimo-anno.html",
    },
    {
        "id": "mater-et-magistra",
        "title": "Mater et Magistra",
        "author": "Pope John XXIII",
        "date": "1961-05-15",
        "type": "encyclical",
        "subdir": "encyclicals",
        "filename": "1961-mater-et-magistra.md",
        "url": "https://www.vatican.va/content/john-xxiii/en/encyclicals/documents/hf_j-xxiii_enc_15051961_mater.html",
    },
    {
        "id": "pacem-in-terris",
        "title": "Pacem in Terris",
        "author": "Pope John XXIII",
        "date": "1963-04-11",
        "type": "encyclical",
        "subdir": "encyclicals",
        "filename": "1963-pacem-in-terris.md",
        "url": "https://www.vatican.va/content/john-xxiii/en/encyclicals/documents/hf_j-xxiii_enc_11041963_pacem.html",
    },
    {
        "id": "populorum-progressio",
        "title": "Populorum Progressio",
        "author": "Pope Paul VI",
        "date": "1967-03-26",
        "type": "encyclical",
        "subdir": "encyclicals",
        "filename": "1967-populorum-progressio.md",
        "url": "https://www.vatican.va/content/paul-vi/en/encyclicals/documents/hf_p-vi_enc_26031967_populorum.html",
    },
    {
        "id": "laborem-exercens",
        "title": "Laborem Exercens",
        "author": "Pope John Paul II",
        "date": "1981-09-14",
        "type": "encyclical",
        "subdir": "encyclicals",
        "filename": "1981-laborem-exercens.md",
        "url": "https://www.vatican.va/content/john-paul-ii/en/encyclicals/documents/hf_jp-ii_enc_14091981_laborem-exercens.html",
    },
    {
        "id": "sollicitudo-rei-socialis",
        "title": "Sollicitudo Rei Socialis",
        "author": "Pope John Paul II",
        "date": "1987-12-30",
        "type": "encyclical",
        "subdir": "encyclicals",
        "filename": "1987-sollicitudo-rei-socialis.md",
        "url": "https://www.vatican.va/content/john-paul-ii/en/encyclicals/documents/hf_jp-ii_enc_30121987_sollicitudo-rei-socialis.html",
    },
    {
        "id": "centesimus-annus",
        "title": "Centesimus Annus",
        "author": "Pope John Paul II",
        "date": "1991-05-01",
        "type": "encyclical",
        "subdir": "encyclicals",
        "filename": "1991-centesimus-annus.md",
        "url": "https://www.vatican.va/content/john-paul-ii/en/encyclicals/documents/hf_jp-ii_enc_01051991_centesimus-annus.html",
    },
    {
        "id": "laudato-si",
        "title": "Laudato Si'",
        "author": "Pope Francis",
        "date": "2015-05-24",
        "type": "encyclical",
        "subdir": "encyclicals",
        "filename": "2015-laudato-si.md",
        "url": "https://www.vatican.va/content/francesco/en/encyclicals/documents/papa-francesco_20150524_enciclica-laudato-si.html",
    },
    {
        "id": "fratelli-tutti",
        "title": "Fratelli Tutti",
        "author": "Pope Francis",
        "date": "2020-10-03",
        "type": "encyclical",
        "subdir": "encyclicals",
        "filename": "2020-fratelli-tutti.md",
        "url": "https://www.vatican.va/content/francesco/en/encyclicals/documents/papa-francesco_20201003_enciclica-fratelli-tutti.html",
    },
    {
        "id": "gaudium-et-spes",
        "title": "Gaudium et Spes",
        "author": "Second Vatican Council",
        "date": "1965-12-07",
        "type": "vatican-ii",
        "subdir": "vatican-ii",
        "filename": "gaudium-et-spes.md",
        "url": "https://www.vatican.va/archive/hist_councils/ii_vatican_council/documents/vat-ii_const_19651207_gaudium-et-spes_en.html",
    },
]


def fetch_url(url, retries=3):
    headers = {"User-Agent": USER_AGENT}
    for attempt in range(1, retries + 1):
        try:
            log.info(f"  Fetching: {url} (attempt {attempt}/{retries})")
            resp = requests.get(url, headers=headers, timeout=TIMEOUT)
            resp.raise_for_status()
            resp.encoding = "utf-8"
            return resp.text
        except requests.exceptions.RequestException as e:
            log.warning(f"  Attempt {attempt} failed: {e}")
            if attempt < retries:
                time.sleep(DELAY * attempt)
            else:
                log.error(f"  FAILED: {url}")
                return None


def is_new_layout(soup):
    """Detect if this is a new-style Vatican page (/content/...)."""
    return bool(soup.select_one("div.documento")) and not is_old_layout(soup)


def is_old_layout(soup):
    """Detect if this is an old-style Vatican page (roman_curia, archive)."""
    # Old pages have specific markers
    return bool(
        soup.select_one("div#corpo")
        or soup.select_one("div.rounded")
        or soup.select_one("body[background]")
        or soup.select_one("a[name]")
    )


def extract_content(soup):
    """Extract main content using the right strategy."""

    # Strategy 1: New layout - clear div.documento
    doc_div = soup.select_one("div.documento")
    if doc_div:
        log.info("  Layout: new (div.documento)")
        return doc_div

    # Strategy 2: Old layout - extract all <p> tags from div#corpo or body
    corpo = soup.select_one("div#corpo, body")
    if corpo:
        log.info("  Layout: old (extracting <p> tags from container)")
        # Create a clean container with just the text <p> tags
        clean = soup.new_tag("div")
        for p in corpo.find_all("p"):
            # Skip navigation <p> elements
            text = p.get_text(strip=True)
            if not text:
                continue
            # Skip language bars
            if re.match(r'^\[?(?:BE|EL|EN|ES|FR|HU|ID|IT|LA|LV|NL|PL|PT|SQ|SW|UK|VI|ZH|AR|DE|CS|GE|KO|LT|PO|SL)(?:\s*[-,]\s*(?:BE|EL|EN|ES|FR|HU|ID|IT|LA|LV|NL|PL|PT|SQ|SW|UK|VI|ZH|AR|DE|CS|GE|KO|LT|PO|SL))*\]?\s*$', text):
                continue
            # Skip image alt-text-only elements
            if text.strip() in ["Index", "Back", "Top", "Print", "Facebook", "Twitter", "Google+", "Mail"]:
                continue
            clean.append(p)
        log.info(f"  Extracted {len(clean.find_all('p'))} content <p> tags")
        return clean

    # Strategy 3: Try main/article
    for sel in ["main", "article", "div.content"]:
        el = soup.select_one(sel)
        if el:
            log.info(f"  Layout: generic ({sel})")
            return el

    # Strategy 4: Fallback to body
    log.info("  Layout: fallback (body)")
    return soup.find("body") or soup


def strip_navigation(soup):
    """Remove navigation, images, scripts, and other non-content elements."""
    # Scripts, styles, forms
    for tag in soup.find_all(["script", "style", "noscript", "iframe", "form", "button", "input", "select"]):
        tag.decompose()

    # Images (remove entirely - they're decorative)
    for img in soup.find_all("img"):
        img.decompose()

    # Horizontal rules
    for hr in soup.find_all("hr"):
        hr.decompose()

    # Font tags (old Vatican uses <font> extensively - keep the text)
    for font in soup.find_all("font"):
        font.unwrap()

    # <center> tags
    for center in soup.find_all("center"):
        center.unwrap()

    # <br> tags between text - keep as line breaks
    # We'll let html2text handle these

    return soup


def convert_to_markdown(soup):
    """Convert cleaned HTML to markdown."""
    h = html2text.HTML2Text()
    h.body_width = 0
    h.ignore_links = False
    h.ignore_images = True
    h.ignore_emphasis = False
    h.protect_links = False
    h.unicode_snob = True
    h.skip_internal_links = True
    h.inline_links = True
    h.wrap_links = False
    h.mark_code = False
    h.escape_snob = False

    html_str = str(soup)
    markdown = h.handle(html_str)
    return markdown


def clean_markdown(text):
    """Post-process markdown text."""
    # Remove excessive blank lines
    text = re.sub(r"\n{4,}", "\n\n\n", text)

    # Remove horizontal rules
    text = re.sub(r"\n\*{3,}\n", "\n", text)
    text = re.sub(r"\n-{3,}\n", "\n", text)

    # Remove trailing whitespace
    text = re.sub(r"[ \t]+\n", "\n", text)

    # Clean escaped periods in paragraph numbers: "2\. " -> "2. "
    text = re.sub(r"\\(\\.)", r"\1", text)
    # Also handle standalone backslashes before punctuation
    text = re.sub(r"\\([.!?\\-])", r"\1", text)

    # Remove image markdown
    text = re.sub(r"!\[.*?\]\(.*?\)", "", text)

    # Remove color/size remnants
    text = re.sub(r'<font[^>]*>', '', text)
    text = re.sub(r'</font>', '', text)
    text = re.sub(r'<span[^>]*>', '', text)
    text = re.sub(r'</span>', '', text)

    # Remove common Vatican boilerplate
    text = re.sub(r"(?i)^\[?(?:home|index|search|back|top|print|help|contact|site\s*map|vatican|roman\s*curia)\]?.*?\n", "", text, flags=re.MULTILINE)

    # Remove "you are here" / breadcrumb lines
    text = re.sub(r"(?i)^.*(?:you are here|home >|vatican >|roman curia >).*\n?", "", text)

    # Remove lines that are just "|" or separators
    text = re.sub(r"^[\s|]+\n", "", text, flags=re.MULTILINE)

    # Remove JavaScript links
    text = re.sub(r"\[.*?\]\(javascript:.*?\)", "", text)

    # Make paragraph numbers bold for scanability
    text = re.sub(r"^(\d{1,3}\.\s)", r"**\1**", text, flags=re.MULTILINE)
    # Also handle numbered paragraphs that have escaped dots
    text = re.sub(r"(?m)^\*\*(\d{1,3}\.\s)\*\*", r"**\1**", text)

    # Remove excess whitespace
    text = re.sub(r"\n{3,}", "\n\n", text)

    text = text.strip()
    return text


def build_frontmatter(doc):
    lines = ["---"]
    lines.append(f'title: "{doc["title"]}"')
    lines.append(f'author: "{doc["author"]}"')
    lines.append(f"date: {doc['date']}")
    lines.append(f'url: {doc["url"]}')
    lines.append(f'type: {doc["type"]}')
    lines.append(f'source: "Official text from the Holy See – vatican.va"')
    lines.append("---")
    return "\n".join(lines)


def save_document(markdown, doc):
    outdir = os.path.join(BASE_DIR, doc["subdir"])
    os.makedirs(outdir, exist_ok=True)
    outpath = os.path.join(outdir, doc["filename"])

    frontmatter = build_frontmatter(doc)
    attribution = "\n\n---\n\n*Official text from the Holy See – vatican.va*\n"

    full_content = frontmatter + "\n\n" + markdown + "\n" + attribution

    with open(outpath, "w", encoding="utf-8") as f:
        f.write(full_content)

    log.info(f"  Saved: {outpath} ({len(full_content):,} chars)")
    return outpath


def check_quality(filepath, doc):
    issues = []
    try:
        with open(filepath, "r", encoding="utf-8") as f:
            content = f.read()
    except Exception as e:
        return [f"Cannot read: {e}"]

    if not content.startswith("---"):
        issues.append("No frontmatter")

    word_count = len(content.split())
    if word_count < 500:
        issues.append(f"Too small ({word_count} words)")

    para_count = len(re.findall(r"^\d{1,3}\.\s", content, re.MULTILINE))
    if para_count < 3:
        issues.append(f"Few paragraphs ({para_count})")

    raw_tags = re.findall(r"<[a-z]+[^>]*>", content)
    if raw_tags:
        html_len = len(''.join(raw_tags))
        if html_len > 500:
            issues.append(f"HTML tags ({len(raw_tags)} instances, {html_len} chars)")

    long_lines = [l for l in content.split("\n") if len(l) > 200]
    if len(long_lines) > 50:
        issues.append(f"{len(long_lines)} lines >200 chars")

    if issues:
        log.warning(f"  Issues: {'; '.join(issues)}")
    else:
        log.info(f"  OK ({word_count:,} words, {para_count} paragraphs)")

    return issues


def build_index(successful):
    index = []
    for doc, path, _issues in successful:
        index.append({
            "id": doc["id"],
            "title": doc["title"],
            "author": doc["author"],
            "date": doc["date"],
            "type": doc["type"],
            "url": doc["url"],
            "file": os.path.relpath(path, BASE_DIR),
        })
    index.sort(key=lambda x: x["date"])
    return index


def process_document(doc, html):
    """Process one document end-to-end."""
    soup = BeautifulSoup(html, "html.parser")

    # Extract content area
    content_el = extract_content(soup)

    # Strip navigation elements
    content_el = strip_navigation(content_el)

    # Convert to markdown
    markdown = convert_to_markdown(content_el)
    markdown = clean_markdown(markdown)

    return markdown


def main():
    log.info("=" * 60)
    log.info("CST Document Scraper v3")
    log.info(f"Base: {BASE_DIR}")
    log.info(f"Documents: {len(DOCUMENTS)}")
    log.info("=" * 60)

    successful = []
    failed = []

    for i, doc in enumerate(DOCUMENTS, 1):
        log.info(f"\n[{i}/{len(DOCUMENTS)}] {doc['title']} ({doc['date']})")

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

        if i < len(DOCUMENTS):
            log.info(f"  Waiting {DELAY}s...")
            time.sleep(DELAY)

    # index.json
    log.info("\n--- Building index.json ---")
    index = build_index(successful)
    index_path = os.path.join(BASE_DIR, "metadata", "index.json")
    with open(index_path, "w", encoding="utf-8") as f:
        json.dump(index, f, indent=2, ensure_ascii=False)
    log.info(f"Saved: {index_path}")

    log.info("\n" + "=" * 60)
    log.info(f"SUCCESSFUL: {len(successful)}/{len(DOCUMENTS)}")
    for doc, path, issues in successful:
        status = "OK" if not issues else "WARN"
        log.info(f"  [{status}] {doc['title']}")
    if failed:
        log.info(f"FAILED: {len(failed)}/{len(DOCUMENTS)}")
        for doc, reason in failed:
            log.info(f"  [FAIL] {doc['title']}: {reason}")
    log.info("Done.")

    return len(failed) == 0


if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)
