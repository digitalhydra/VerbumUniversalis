#!/usr/bin/env python3
"""
Translate CCC from English to Spanish using Anthropic API.
Processes batches and generates INSERT SQL for verbum_ccc.db.
"""
import json
import os
import sys
import re
import google.genai as genai
import time
import sqlite3
from pathlib import Path

BATCH_DIR = Path("/mnt/disk2/dev/VerbunUniversalis/.ccc_translation")
OUTPUT_DIR = Path("/mnt/disk2/dev/VerbunUniversalis/.ccc_translation/sql")
OUTPUT_DIR.mkdir(exist_ok=True)

# Database path
DB_PATH = Path("./app/src/main/assets/verbum_ccc.db")

client = genai.Client(api_key=os.environ["GEMINI_API_KEY"])

SYSTEM_PROMPT = "You are a translator for the Catechism of the Catholic Church. Translate English text to SPANISH using proper Catholic theological terminology. Preserve all formatting, references, and structure exactly. Output ONLY valid SQL."

def escape_sql(val):
    """Escape a string for SQLite INSERT."""
    if val is None:
        return "NULL"
    s = str(val)
    s = s.replace("'", "''")
    return f"'{s}'"

def build_sql_insert(entry, translated_plain, translated_json):
    """Build a single SQL INSERT statement."""
    number = entry["number"]
    toc = entry["toc_path"]
    # Translate TOC path to Spanish
    toc_es = translate_toc(toc)
    
    return (
        f"INSERT INTO ccc_paragraphs (number, lang, toc_path, plain_text, formatted_json) VALUES ("
        f"{number}, "
        f"'es', "
        f"{escape_sql(toc_es)}, "
        f"{escape_sql(translated_plain)}, "
        f"{escape_sql(translated_json)}"
        f");"
    )

def translate_toc(toc_path):
    """Translate TOC path segments to Spanish."""
    translations = {
        "PROLOGUE": "PRÓLOGO",
        "I. THE PROFESSION OF FAITH": "I. LA PROFESIÓN DE LA FE",
        "II. THE SACRAMENTS OF CHRISTIAN INITIATION": "II. LOS SACRAMENTOS DE LA INICIACIÓN CRISTIANA",
        "III. THE SACRAMENTS OF HEALING": "III. LOS SACRAMENTOS DE LA SANACIÓN",
        "IV. THE SACRAMENTS AT THE SERVICE OF COMMUNION": "IV. LOS SACRAMENTOS AL SERVICIO DE LA COMUNIÓN",
        "V. THE PRAYER OF THE BELIEVER": "V. LA ORACIÓN DEL CREYENTE",
        "VI. THE SOCIAL TEACHING OF THE CHURCH": "VI. LA DOCTRINA SOCIAL DE LA IGLESIA",
        "PART ONE": "PRIMERA PARTE",
        "PART TWO": "SEGUNDA PARTE",
        "PART THREE": "TERCERA PARTE",
        "PART FOUR": "CUARTA PARTE",
        "SECTION ONE": "SECCIÓN PRIMERA",
        "SECTION TWO": "SECCIÓN SEGUNDA",
        "SECTION THREE": "SECCIÓN TERCERA",
        "SECTION FOUR": "SECCIÓN CUARTA",
        "CHAPTER ONE": "CAPÍTULO PRIMERO",
        "CHAPTER TWO": "CAPÍTULO SEGUNDO",
        "CHAPTER THREE": "CAPÍTULO TERCERO",
        "CHAPTER FOUR": "CAPÍTULO CUARTO",
        "ARTICLE 1": "ARTÍCULO 1",
        "ARTICLE 2": "ARTÍCULO 2",
        "ARTICLE 3": "ARTÍCULO 3",
        "IN BRIEF": "EN RESUMEN",
        "I.": "I.",
        "II.": "II.",
        "III.": "III.",
        "IV.": "IV.",
        "V.": "V.",
        "VI.": "VI.",
        "VII.": "VII.",
        "VIII.": "VIII.",
        "IX.": "IX.",
        "X.": "X.",
        "GOD'S SALVATION: LAW AND GRACE": "LA SALVACIÓN DE DIOS: LEY Y GRACIA",
        "THE NEW LAW": "LA NUEVA LEY",
        "THE OLD LAW": "LA VIEJA LEY",
        "GRACE AND JUSTIFICATION": "LA GRACIA Y LA JUSTIFICACIÓN",
        "THE CHURCH: MOTHER AND TEACHER": "LA IGLESIA: MADRE Y MAESTRA",
        "THE COMMUNION OF SAINTS": "LA COMMUNIÓN DE LOS SANTOS",
        "THE KINGDOM OF GOD": "EL REINO DE DIOS",
        "THE LAST THINGS": "LAS ÚLTIMAS COSAS",
        "DIGNITY OF THE HUMAN PERSON": "LA DIGNIDAD DE LA PERSONA HUMANA",
        "THE HUMAN COMMUNITY": "LA COMUNIDAD HUMANA",
        "GOD'S APPROACH TO MAN": "EL ENFOQUE DE DIOS HACIA EL HOMBRE",
        "THE VOCATION OF MAN: THE SPIRIT OF WORK": "LA VOCACIÓN DEL HOMBRE: EL ESPÍRITU DEL TRABAJO",
        "THE RIGHT TO LIFE": "EL DERECHO A LA VIDA",
        "THE RIGHT TO A DIGNIFIED DEATH": "EL DERECHO A UNA MUERTE DIGNA",
        "THE RIGHT TO RELIGIOUS FREEDOM": "EL DERECHO A LA LIBERTAD RELIGIOSA",
        "THE RIGHT TO EDUCATION": "EL DERECHO A LA EDUCACIÓN",
        "THE RIGHT TO WORK": "EL DERECHO AL TRABAJO",
        "THE RIGHT TO FORM AND JOIN UNIONS": "EL DERECHO A FORMAR Y UNIR SINDICATOS",
        "THE RIGHT TO REST AND LEISURE": "DERECHO AL DESCANSO Y EL OCIO",
        "THE RIGHT TO PROTECTION OF PHYSICAL AND MENTAL HEALTH": "DERECHO A LA PROTECCIÓN DE LA SALUD FÍSICA Y MENTAL",
        "THE RIGHT TO SOCIAL SECURITY": "EL DERECHO A LA SEGURIDAD SOCIAL",
        "THE RIGHT TO PROTECTION OF THE ENVIRONMENT": "EL DERECHO A LA PROTECCIÓN DEL MEDIO AMBIENTE",
        "THE RIGHT TO SUSTAINABLE DEVELOPMENT": "DERECHO AL DESARROLLO SOSTENIBLE",
        "THE RIGHT TO DEVELOPMENT": "DERECHO AL DESARROLLO",
        "THE RIGHT TO PEACE": "EL DERECHO A LA PAZ",
        "THE RIGHT TO DISARMAMENT": "EL DERECHO AL DESARME",
        "THE RIGHT TO INTERNATIONAL SOLIDARITY": "EL DERECHO A LA SOLIDARIDAD INTERNACIONAL",
        "THE RIGHT TO A NATIONALITY": "DERECHO A UNA NACIONALIDAD",
        "THE RIGHT TO MARRIAGE AND FAMILY": "DERECHO AL MATRIMONIO Y LA FAMILIA",
        "THE RIGHT TO OWN PROPERTY": "DERECHO A POSEER PROPIEDAD",
        "THE RIGHT TO INHERIT": "DERECHO A HEREDAR",
        "THE RIGHT TO EDUCATION": "EL DERECHO A LA EDUCACIÓN"
    }
    
    result = toc_path
    for eng, spa in translations.items():
        result = result.replace(eng, spa)
    
    return result

def split_sql_statements(sql_text):
    """
    Split SQL text into individual statements, respecting semicolons inside string literals.
    Returns a list of SQL statements (each ending with semicolon).
    """
    statements = []
    current = ""
    in_string = False
    string_char = None  # Either "'" or '"' when inside a string
    escape_next = False
    
    i = 0
    while i < len(sql_text):
        char = sql_text[i]
        
        if escape_next:
            # Previous character was a backslash, so this character is escaped
            current += char
            escape_next = False
        elif char == '\\':
            # Backslash - next character will be escaped
            current += char
            escape_next = True
        elif not in_string and char in ("'", '"'):
            # Starting a string literal
            current += char
            in_string = True
            string_char = char
        elif in_string and char == string_char:
            # Ending a string literal
            current += char
            in_string = False
            string_char = None
        elif not in_string and char == ';':
            # Statement terminator (outside of string)
            current += char
            statements.append(current)
            current = ""
        else:
            # Regular character
            current += char
        
        i += 1
    
    # Don't forget the last statement if there's no trailing semicolon
    if current.strip():
        statements.append(current)
    
    return statements

def execute_sql_statements(sql_text):
    """
    Execute SQL statements against the database.
    Returns (success_count, total_count, error_messages)
    """
    if not sql_text or not sql_text.strip():
        return 0, 0, ["Empty SQL text"]
    
    # Split into statements
    statements = split_sql_statements(sql_text)
    
    if not statements:
        return 0, 0, ["No statements found in SQL text"]
    
    # Connect to database
    try:
        conn = sqlite3.connect(str(DB_PATH))
    except Exception as e:
        return 0, len(statements), [f"Failed to connect to database: {e}"]
    
    # Ensure table exists (it should, but let's be safe)
    try:
        conn.execute('''CREATE TABLE IF NOT EXISTS ccc_paragraphs (
            number INTEGER PRIMARY KEY,
            lang TEXT NOT NULL,
            toc_path TEXT NOT NULL,
            plain_text TEXT NOT NULL,
            formatted_json TEXT NOT NULL
        )''')
    except Exception as e:
        conn.close()
        return 0, len(statements), [f"Failed to ensure table exists: {e}"]
    
    # Execute each statement
    success_count = 0
    error_messages = []
    
    for i, stmt in enumerate(statements):
        stmt = stmt.strip()
        if not stmt:
            continue
            
        try:
            conn.execute(stmt)
            conn.commit()
            success_count += 1
        except Exception as e:
            error_messages.append(f"Statement {i+1} failed: {str(e)[:200]}")
            conn.rollback()
    
    conn.close()
    
    return success_count, len(statements), error_messages

def translate_batch(batch_data, batch_num):
    """Translate a batch of paragraphs using the Anthropic API."""
    
    # Build prompt
    paragraphs_text = json.dumps([
        {
            "number": p["number"],
            "plain_text": p["plain_text"],
            "formatted_json": p["formatted_json"]
        }
        for p in batch_data
    ], indent=2, ensure_ascii=False)
    
    numbers_desc = f"#{batch_data[0]['number']}-#{batch_data[-1]['number']}"
    
    prompt = f"""Translate the following {len(batch_data)} CCC paragraphs from English to SPANISH.

For EACH paragraph:
1. TRANSLATE plain_text to Spanish (use proper Catholic theological terminology)
2. PARSE formatted_json (it's a JSON array). For each segment:
   - type "ref-ccc" or "ref": KEEP EXACTLY AS-IS - DO NOT MODIFY
   - type "text": TRANSLATE the "text" field, KEEP "attrs" unchanged
3. Generate a SQL INSERT statement

INPUT JSON (paragraphs {numbers_desc}):
{paragraphs_text}

OUTPUT FORMAT (ONLY SQL, no explanation):
INSERT INTO ccc_paragraphs (number, lang, toc_path, plain_text, formatted_json) VALUES (N, 'es', 'TOC_PATH', 'TRANSLATED_TEXT', 'TRANSLATED_JSON');

RULES:
- The formatted_json MUST be valid JSON when inserted (properly escape quotes)
- Bible references keep Spanish abbreviations (Mt 5,3 instead of Mt 5:3)
- Section headers like "II. HANDING ON THE FAITH: CATECHESIS" -> "II. TRANSMISIÓN DE LA FE: CATEQUESIS"
- Use standard Catholic Spanish: Gracia, Redención, Sacramento, Iglesia, Catequesis, Eucaristía, Bautismo, etc.
- Preserve ALL attrs (b for bold, i for italic, href for links) EXACTLY
- Preserve ALL ref-ccc and ref segments EXACTLY as they appear
- . . . (ellipsis with spaces) should stay as . . . 
- Output one INSERT per line, each terminated with semicolon
- Output ONLY the SQL, nothing else before or after"""

    max_retries = 3
    for attempt in range(max_retries):
        try:
            response = client.models.generate_content(
                model="models/gemini-3-pro-preview",
                contents=prompt,
                config={
                    "max_output_tokens": 64000,
                    "temperature": 0.1,
                }
            )
            sql_text = response.text
            
            # Basic validation - count INSERT statements
            insert_count = sql_text.count("INSERT INTO ccc_paragraphs")
            expected = len(batch_data)
            
            if insert_count < expected:
                print(f"  WARNING: Got {insert_count} INSERTs, expected {expected}. Retrying...")
                if attempt < max_retries - 1:
                    time.sleep(2)
                    continue
            
            return sql_text
            
        except Exception as e:
            print(f"  API error (attempt {attempt+1}): {e}")
            if attempt < max_retries - 1:
                time.sleep(5)
            else:
                raise
    
    return None

def validate_sql(sql_text, batch_data):
    """Basic validation of generated SQL."""
    insert_count = sql_text.count("INSERT INTO ccc_paragraphs")
    expected = len(batch_data)
    
    # Count unique numbers mentioned
    numbers_in_sql = set()
    for line in sql_text.split("\n"):
        match = re.search(r"VALUES\s*\((\d+)", line)
        if match:
            numbers_in_sql.add(int(match.group(1)))
    
    expected_numbers = set(p["number"] for p in batch_data)
    missing = expected_numbers - numbers_in_sql
    extra = numbers_in_sql - expected_numbers
    
    return {
        "insert_count": insert_count,
        "expected": expected,
        "missing_numbers": sorted(missing),
        "extra_numbers": sorted(extra),
        "valid": len(missing) == 0 and len(extra) == 0 and insert_count == expected
    }

def process_footnotes():
    """Translate footnotes."""
    fn_path = Path("/tmp/ccc_footnotes_en.json")
    if not fn_path.exists():
        print("Footnotes file not found")
        return
    
    with open(fn_path) as f:
        footnotes = json.load(f)
    
    print(f"Translating {len(footnotes)} footnotes...")
    
    # Process in batches of 50
    BATCH = 50
    all_sql = []
    
    for i in range(0, len(footnotes), BATCH):
        batch = footnotes[i:i+BATCH]
        batch_num = i // BATCH
        
        fn_json = json.dumps([
            {"ccc_number": fn["ccc_number"], "footnote_number": fn["footnote_number"], "footnote_text": fn["footnote_text"]}
            for fn in batch
        ], indent=2, ensure_ascii=False)
        
        prompt = f"""Translate these {len(batch)} CCC footnotes from English to SPANISH.

For each footnote, translate footnote_text to Spanish while preserving Bible references in Spanish format.

INPUT:
{fn_json}

OUTPUT (ONLY SQL):
INSERT INTO ccc_footnotes (ccc_number, footnote_number, footnote_text) VALUES (N, FN, 'TRANSLATED_TEXT');

One INSERT per line, each terminated with semicolon. Output ONLY SQL."""
        
        try:
            response = client.models.generate_content(
                model="models/gemini-3-pro-preview",
                contents=prompt,
                config={
                    "max_output_tokens": 32000,
                    "temperature": 0.1,
                }
            )
            sql_text = response.text
            all_sql.append(sql_text)
            print(f"  FN batch {batch_num}/{len(footnotes)//BATCH}: {len(batch)} footnotes")
        except Exception as e:
            print(f"  FN batch {batch_num} error: {e}")
        
        time.sleep(0.5)
    
    output_path = OUTPUT_DIR / "footnotes_es.sql"
    with open(output_path, "w") as f:
        f.write("-- CCC Footnotes (Spanish)\n\n")
        for sql in all_sql:
            f.write(sql)
            f.write("\n")
    
    print(f"Footnotes SQL written to {output_path}")

def main():
    print("=" * 60)
    print("CCC Spanish Translation")
    print("=" * 60)
    
    # Verify database exists
    if not DB_PATH.exists():
        print(f"ERROR: Database not found at {DB_PATH}")
        return 1
    
    print(f"Using database: {DB_PATH}")
    
    start_batch = int(sys.argv[1]) if len(sys.argv) > 1 else 0
    end_batch = int(sys.argv[2]) if len(sys.argv) > 2 else 999
    
    # Process paragraph batches
    for i in range(start_batch, min(end_batch + 1, 96)):
        batch_path = BATCH_DIR / f"small_batch_{i}.json"
        if not batch_path.exists():
            print(f"Batch {i} not found, skipping")
            continue
        
        with open(batch_path) as f:
            batch_data = json.load(f)
        
        # Check if already done
        out_path = OUTPUT_DIR / f"batch_{i}.sql"
        if out_path.exists() and out_path.stat().st_size > 100:
            print(f"Batch {i} already done, skipping")
            continue
        
        desc = f"#{batch_data[0]['number']}-#{batch_data[-1]['number']}"
        print(f"\nBatch {i}/95 ({desc}, {len(batch_data)} paragraphs)...")
        
        try:
            sql = translate_batch(batch_data, i)
            if sql:
                validation = validate_sql(sql, batch_data)
                if not validation["valid"]:
                    print(f"  VALIDATION ISSUES: {validation}")
                    print(f"  Saving anyway...")
                
                # EXECUTE THE SQL STATEMENTS AGAINST THE DATABASE
                print(f"  Executing {validation['insert_count']} SQL statements...")
                success_count, total_count, errors = execute_sql_statements(sql)
                
                if success_count > 0:
                    print(f"  ✓ Successfully executed {success_count}/{total_count} statements")
                if errors:
                    print(f"  ✗ Errors encountered:")
                    for error in errors[:3]:  # Show first 3 errors
                        print(f"    {error}")
                    if len(errors) > 3:
                        print(f"    ... and {len(errors) - 3} more errors")
                
                # Still write SQL to file for persistence
                with open(out_path, "w") as f:
                    f.write(f"-- CCC Spanish Batch {i} ({desc})\n")
                    f.write(f"-- {len(batch_data)} paragraphs\n\n")
                    f.write(sql)
                    f.write("\n")
                
                print(f"  Saved {out_path.name} ({len(sql)} chars, {validation['insert_count']}/{validation['expected']} INSERTs)")
                
                # Report overall success
                if success_count == total_count and len(errors) == 0:
                    print(f"  🎉 Batch {i} COMPLETELY SUCCESSFUL")
                elif success_count > 0:
                    print(f"  ⚠️  Batch {i} PARTIALLY SUCCESSFUL ({success_count}/{total_count})")
                else:
                    print(f"  ❌ Batch {i} FAILED")
                    
            else:
                print(f"  FAILED to translate batch {i}")
        except Exception as e:
            print(f"  ERROR: {e}")
        
        time.sleep(1)  # Rate limiting
    
    print("\nParagraph translation complete!")
    return 0

if __name__ == "__main__":
    sys.exit(main())
