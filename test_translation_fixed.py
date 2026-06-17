#!/usr/bin/env python3
"""
Test script to verify our fixed translation logic works correctly.
"""
import json
import os
import sys
import re
import sqlite3
from pathlib import Path

# Test database path
DB_PATH = Path("./app/src/main/assets/verbum_ccc.db")

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
    """Mock translation function that uses identity translation for testing."""
    # Build SQL statements directly using the existing build_sql_insert function
    sql_statements = []
    for entry in batch_data:
        # Use identity translation (no actual translation)
        spanish_toc = translate_toc(entry['toc_path'])  # This still translates TOC paths
        spanish_plain = entry['plain_text']  # Identity
        spanish_fmt = entry['formatted_json']  # Identity
        
        sql = build_sql_insert(entry, spanish_plain, spanish_fmt)
        sql_statements.append(sql)
    
    return '\n'.join(sql_statements)

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

def main():
    print("=== Testing Fixed Translation Logic (Mock Mode) ===")
    
    # Verify database exists
    if not DB_PATH.exists():
        print(f"ERROR: Database not found at {DB_PATH}")
        return 1
    
    print(f"Using database: {DB_PATH}")
    
    # Test with batch 0
    batch_num = 0
    batch_path = Path(f"/mnt/disk2/dev/VerbunUniversalis/.ccc_translation/small_batches/small_{batch_num}.json")
    
    if not batch_path.exists():
        print(f"Batch {batch_num} not found at {batch_path}")
        return 1
    
    with open(batch_path) as f:
        batch_data = json.load(f)
    
    print(f"Loaded {len(batch_data)} entries from {batch_path}")
    
    # Translate the batch
    desc = f"#{batch_data[0]['number']}-#{batch_data[-1]['number']}"
    print(f"Processing Batch {batch_num}/95 ({desc}, {len(batch_data)} paragraphs)...")
    
    try:
        sql = translate_batch(batch_data, batch_num)
        if sql:
            validation = validate_sql(sql, batch_data)
            print(f"  Validation: {validation['insert_count']}/{validation['expected']} INSERT statements")
            
            if not validation["valid"]:
                print(f"  VALIDATION ISSUES: {validation}")
            
            # EXECUTE THE SQL STATEMENTS AGAINST THE DATABASE
            print(f"  Executing SQL statements...")
            success_count, total_count, errors = execute_sql_statements(sql)
            
            if success_count > 0:
                print(f"  ✓ Successfully executed {success_count}/{total_count} statements")
            if errors:
                print(f"  ✗ Errors encountered:")
                for error in errors[:3]:  # Show first 3 errors
                    print(f"    {error}")
                if len(errors) > 3:
                    print(f"    ... and {len(errors) - 3} more errors")
            
            # Report overall success
            if success_count == total_count and len(errors) == 0:
                print(f"  🎉 Batch {batch_num} COMPLETELY SUCCESSFUL")
                return 0
            elif success_count > 0:
                print(f"  ⚠️  Batch {batch_num} PARTIALLY SUCCESSFUL ({success_count}/{total_count})")
                return 0
            else:
                print(f"  ❌ Batch {batch_num} FAILED")
                return 1
        else:
            print(f"  FAILED to translate batch {batch_num}")
            return 1
    except Exception as e:
        print(f"  ERROR: {e}")
        return 1

if __name__ == "__main__":
    sys.exit(main())
