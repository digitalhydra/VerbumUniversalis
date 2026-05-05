#!/usr/bin/env python3
"""Test raw OSIS extraction from ABPGRK."""

import os
from pysword.bible import SwordBible, SwordModuleType

print(f"cwd: {os.getcwd()}")

# Load ABPGRK
abpgrk = SwordBible(
    module_path=os.path.abspath('raw_data/_extracted/modules/texts/ztext/abpgrk'),
    module_type=SwordModuleType.ZTEXT,
    versification='kjv',
    encoding='utf-8',
    source_type='OSIS',
    compress_type='ZIP'
)

# Get raw text (with tags)
print("\nRaw OSIS text for John 1:1:")
result = abpgrk.get(books=['John'], chapters=[1], verses=[1], clean=False)
texts = list(result)
print(texts[0][:800])

print("\n" + "="*50)
print("Cleaned text:")
result2 = abpgrk.get(books=['John'], chapters=[1], verses=[1], clean=True)
print(list(result2)[0])
