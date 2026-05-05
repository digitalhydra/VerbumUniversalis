#!/usr/bin/env python3
"""Test parsing OSIS w tags."""

import os
import re
from pysword.bible import SwordBible, SwordModuleType

cwd = os.getcwd()
print(f"cwd: {cwd}")

# Load Greek module
greek = SwordBible(
    module_path=os.path.abspath('raw_data/_extracted/modules/texts/ztext/abpgrk'),
    module_type=SwordModuleType.ZTEXT,
    versification='kjv',
    encoding='utf-8',
    source_type='OSIS',
    compress_type='ZIP'
)

# Get raw text for John 1:1
result = greek.get_iter(books=['John'], chapters=[1], verses=[1], clean=False)
raw = list(result)[0]
print(f"\nRaw text (first 500 chars):\n{raw[:500]}\n")

# Parse w tags
pattern = r'<w\b([^>]*)>(.*?)</w>'
matches = re.findall(pattern, raw, re.DOTALL)
print(f"Found {len(matches)} w tags\n")

for attrs, content in matches[:5]:
    lemma = None
    lm = re.search(r'lemma="([^"]*)"', attrs)
    if lm:
        lemma = lm.group(1)

    morph = None
    mm = re.search(r'morph="([^"]*)"', attrs)
    if mm:
        morph = mm.group(1)

    print(f"Word: {content.strip()}")
    print(f"  Lemma: {lemma}")
    print(f"  Morph: {morph}")
    print()
