#!/usr/bin/env python3
"""Parse Strong's dictionaries (JS format) into lexicon table. Fixed."""

import os
import json
import sqlite3
import re

def strip_js_to_json(filepath):
    """Strip JavaScript variable assignment to get pure JSON."""
    with open(filepath, 'r', encoding='utf-8') as f:
        js_text = f.read()

    # Find the JSON object - look for first { and last }
    # This is more robust than trying to match braces
    start = js_text.find('{')
    if start == -1:
        raise ValueError("No JSON object found")

    # Find the last } (not just matching braces)
    end = js_text.rfind('}')
    if end == -1:
        raise ValueError("No closing brace found")

    json_str = js_text[start:end+1]
    return json_str


def load_strongs_js(filepath):
    """Load Strong's dictionary from JS file."""
    print(f"Loading {filepath}...")
    json_str = strip_js_to_json(filepath)
    data = json.loads(json_str)
    print(f"  Loaded {len(data)} entries")
    return data


def main():
    cwd = os.getcwd()
    print(f"Working directory: {cwd}\n")

    db_path = os.path.join(cwd, 'app/src/main/assets/verbum_seed.db')
    print(f"Database: {db_path}\n")

    if not os.path.exists(db_path):
        print("ERROR: Database not found.")
        return

    conn = sqlite3.connect(db_path)

    # Clear existing lexicon data
    conn.execute('DELETE FROM lexicon')
    conn.commit()
    print("Cleared existing lexicon table\n")

    # Load Greek Strong's
    greek_path = 'strongs-master/greek/strongs-greek-dictionary.js'

    if os.path.exists(greek_path):
        greek_data = load_strongs_js(greek_path)

        print("Inserting Greek entries...")
        count = 0
        for strong_num, entry in greek_data.items():
            # Build definition from available fields
            definition_parts = []

            if 'strongs_def' in entry and entry['strongs_def']:
                definition_parts.append(f"Strong's: {entry['strongs_def']}")

            if 'kjv_def' in entry and entry['kjv_def']:
                definition_parts.append(f"KJV: {entry['kjv_def']}")

            if 'derivation' in entry and entry['derivation']:
                definition_parts.append(f"Derivation: {entry['derivation']}")

            if 'translit' in entry and entry['translit']:
                definition_parts.append(f"Translit: {entry['translit']}")

            if 'lemma' in entry and entry['lemma']:
                definition_parts.append(f"Lemma: {entry['lemma']}")

            definition = ' | '.join(definition_parts)

            conn.execute(
                'INSERT OR REPLACE INTO lexicon (lemma, language, definition) VALUES (?, ?, ?)',
                (strong_num, 'grc', definition)
            )
            count += 1

        conn.commit()
        print(f"  Inserted {count} Greek entries\n")
    else:
        print(f"WARNING: Greek dictionary not found at {greek_path}\n")

    # Load Hebrew Strong's
    hebrew_path = 'strongs-master/hebrew/strongs-hebrew-dictionary.js'

    if os.path.exists(hebrew_path):
        hebrew_data = load_strongs_js(hebrew_path)

        print("Inserting Hebrew entries...")
        count = 0
        for strong_num, entry in hebrew_data.items():
            # Build definition from available fields
            definition_parts = []

            if 'strongs_def' in entry and entry['strongs_def']:
                definition_parts.append(f"Strong's: {entry['strongs_def']}")

            if 'kjv_def' in entry and entry['kjv_def']:
                definition_parts.append(f"KJV: {entry['kjv_def']}")

            if 'derivation' in entry and entry['derivation']:
                definition_parts.append(f"Derivation: {entry['derivation']}")

            if 'xlit' in entry and entry['xlit']:
                definition_parts.append(f"Translit: {entry['xlit']}")

            if 'lemma' in entry and entry['lemma']:
                definition_parts.append(f"Lemma: {entry['lemma']}")

            definition = ' | '.join(definition_parts)

            conn.execute(
                'INSERT OR REPLACE INTO lexicon (lemma, language, definition) VALUES (?, ?, ?)',
                (strong_num, 'hbo', definition)
            )
            count += 1

        conn.commit()
        print(f"  Inserted {count} Hebrew entries\n")
    else:
        print(f"WARNING: Hebrew dictionary not found at {hebrew_path}\n")

    # Print stats
    print("=" * 50)
    print("Lexicon processing complete!\n")

    total = conn.execute("SELECT COUNT(*) FROM lexicon").fetchone()[0]
    greek_count = conn.execute("SELECT COUNT(*) FROM lexicon WHERE language='grc'").fetchone()[0]
    hebrew_count = conn.execute("SELECT COUNT(*) FROM lexicon WHERE language='hbo'").fetchone()[0]

    print("Stats:")
    print(f"  Total lexicon entries: {total}")
    print(f"  Greek entries: {greek_count}")
    print(f"  Hebrew entries: {hebrew_count}")

    # Show sample entries
    print("\nSample Greek entry (G746):")
    sample = conn.execute("SELECT definition FROM lexicon WHERE lemma='G746'").fetchone()
    if sample:
        print(f"  {sample[0][:200]}...")

    print("\nSample Hebrew entry (H07225):")
    sample = conn.execute("SELECT definition FROM lexicon WHERE lemma='strong:H07225'").fetchone()
    if sample:
        print(f"  {sample[0][:200]}...")
    else:
        sample = conn.execute("SELECT definition FROM lexicon WHERE lemma='H1'").fetchone()
        if sample:
            print(f"  {sample[0][:200]}...")

    conn.close()


if __name__ == '__main__':
    main()
