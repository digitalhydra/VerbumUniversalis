#!/usr/bin/env python3
"""
Translate CCC English -> Spanish using Gemini 2.5 Flash.
Processes batches of 10 paragraphs at a time.
"""
import json, os, sys, time, re, math
from pathlib import Path
import google.genai as genai

BATCH_DIR = Path(".ccc_translation/batches_10")
OUTPUT_DIR = Path(".ccc_translation/sql2")
OUTPUT_DIR.mkdir(exist_ok=True)

client = genai.Client(api_key=os.environ["GEMINI_API_KEY"])

TOC_TRANSLATIONS = {
    "PROLOGUE": "PRÓLOGO",
    "PART ONE": "PRIMERA PARTE",
    "PART TWO": "SEGUNDA PARTE",
    "PART THREE": "TERCERA PARTE",
    "PART FOUR": "CUARTA PARTE",
    "SECTION ONE": "PRIMERA SECCIÓN",
    "SECTION TWO": "SEGUNDA SECCIÓN",
    "CHAPTER ONE": "CAPÍTULO PRIMERO",
    "CHAPTER TWO": "CAPÍTULO SEGUNDO",
    "CHAPTER THREE": "CAPÍTULO TERCERO",
    "CHAPTER FOUR": "CAPÍTULO CUARTO",
    "ARTICLE 1": "ARTÍCULO 1",
    "ARTICLE 2": "ARTÍCULO 2",
    "ARTICLE 3": "ARTÍCULO 3",
    "ARTICLE 4": "ARTÍCULO 4",
    "ARTICLE 5": "ARTÍCULO 5",
    "ARTICLE 6": "ARTÍCULO 6",
    "ARTICLE 7": "ARTÍCULO 7",
    "ARTICLE 8": "ARTÍCULO 8",
    "ARTICLE 9": "ARTÍCULO 9",
    "ARTICLE 10": "ARTÍCULO 10",
    "ARTICLE 11": "ARTÍCULO 11",
    "ARTICLE 12": "ARTÍCULO 12",
    "IN BRIEF": "EN RESUMEN",
    "PARAGRAPH 1": "PÁRRAFO 1",
    "PARAGRAPH 2": "PÁRRAFO 2",
    "PARAGRAPH 3": "PÁRRAFO 3",
    "PARAGRAPH 4": "PÁRRAFO 4",
    "PARAGRAPH 5": "PÁRRAFO 5",
    "PARAGRAPH 6": "PÁRRAFO 6",
    "PARAGRAPH 7": "PÁRRAFO 7",
    "THE PROFESSION OF FAITH": "LA PROFESIÓN DE LA FE",
    "THE CELEBRATION OF THE CHRISTIAN MYSTERY": "LA CELEBRACIÓN DEL MISTERIO CRISTIANO",
    "LIFE IN CHRIST": "LA VIDA EN CRISTO",
    "CHRISTIAN PRAYER": "LA ORACIÓN CRISTIANA",
    "'I BELIEVE' - 'WE BELIEVE'": "'CREO' - 'CREEMOS'",
    "THE PROFESSION OF THE CHRISTIAN FAITH": "LA PROFESIÓN DE LA FE CRISTIANA",
    "THE SACRAMENTAL ECONOMY": "LA ECONOMÍA SACRAMENTAL",
    "THE SEVEN SACRAMENTS OF THE CHURCH": "LOS SIETE SACRAMENTOS DE LA IGLESIA",
    "MAN'S VOCATION: LIFE IN THE SPIRIT": "LA VOCACIÓN DEL HOMBRE: LA VIDA EN EL ESPÍRITU",
    "THE TEN COMMANDMENTS": "LOS DIEZ MANDAMIENTOS",
    "PRAYER IN THE CHRISTIAN LIFE": "LA ORACIÓN EN LA VIDA CRISTIANA",
    "THE LORD'S PRAYER: 'OUR FATHER!'": "EL PADRENUESTRO: '¡PADRE NUESTRO!'",
    "MAN'S CAPACITY FOR GOD": "LA CAPACIDAD DEL HOMBRE PARA DIOS",
    "GOD COMES TO MEET MAN": "DIOS SALE AL ENCUENTRO DEL HOMBRE",
    "MAN'S RESPONSE TO GOD": "LA RESPUESTA DEL HOMBRE A DIOS",
    "I BELIEVE IN GOD THE FATHER": "CREO EN DIOS PADRE",
    "I BELIEVE IN JESUS CHRIST, THE ONLY SON OF GOD": "CREO EN JESUCRISTO, EL ÚNICO HIJO DE DIOS",
    "I BELIEVE IN THE HOLY SPIRIT": "CREO EN EL ESPÍRITU SANTO",
    "THE PASCHAL MYSTERY IN THE AGE OF THE CHURCH": "EL MISTERIO PASCUAL EN LA ERA DE LA IGLESIA",
    "THE SACRAMENTAL CELEBRATION": "LA CELEBRACIÓN SACRAMENTAL",
    "THE SACRAMENTS OF CHRISTIAN INITIATION": "LOS SACRAMENTOS DE LA INICIACIÓN CRISTIANA",
    "THE SACRAMENTS OF HEALING": "LOS SACRAMENTOS DE CURACIÓN",
    "THE SACRAMENTS AT THE SERVICE OF COMMUNION": "LOS SACRAMENTOS AL SERVICIO DE LA COMUNIÓN",
    "OTHER LITURGICAL CELEBRATIONS": "OTRAS CELEBRACIONES LITÚRGICAS",
    "THE DIGNITY OF THE HUMAN PERSON": "LA DIGNIDAD DE LA PERSONA HUMANA",
    "THE HUMAN COMMUNITY": "LA COMUNIDAD HUMANA",
    "GOD'S SALVATION: LAW AND GRACE": "LA SALVACIÓN DE DIOS: LA LEY Y LA GRACIA",
    "'YOU SHALL LOVE THE LORD YOUR GOD...'": "'AMARÁS AL SEÑOR TU DIOS...'",
    "'YOU SHALL LOVE YOUR NEIGHBOR AS YOURSELF'": "'AMARÁS A TU PRÓJIMO COMO A TI MISMO'",
    "THE REVELATION OF PRAYER": "LA REVELACIÓN DE LA ORACIÓN",
    "THE TRADITION OF PRAYER": "LA TRADICIÓN DE LA ORACIÓN",
    "THE LIFE OF PRAYER": "LA VIDA DE ORACIÓN",
    "THE SEVEN PETITIONS": "LAS SIETE PETICIONES",
    "THE FINAL DOXOLOGY": "LA DOXOLOGÍA FINAL",
    "THE OUR FATHER": "EL PADRENUESTRO",
}

def translate_toc(toc):
    parts = toc.split(" > ")
    return " > ".join(TOC_TRANSLATIONS.get(p.strip(), p.strip()) for p in parts)

def escape_sql(val):
    return "'" + str(val).replace("'", "''") + "'"

def call_gemini(prompt, max_tokens=16000):
    for attempt in range(5):
        try:
            resp = client.models.generate_content(
                model="models/gemini-2.5-flash",
                contents=prompt,
                config={"max_output_tokens": max_tokens, "temperature": 0.1}
            )
            return resp.text
        except Exception as e:
            err_msg = str(e)
            if "429" in err_msg or "RESOURCE_EXHAUSTED" in err_msg:
                wait = min(2 ** attempt * 10, 120)
                print(f"  Rate limited, waiting {wait}s...")
                time.sleep(wait)
            elif "SAFETY" in err_msg.upper():
                print(f"  Safety error, retrying with lower temp...")
                time.sleep(3)
            else:
                print(f"  Error (attempt {attempt+1}): {err_msg[:100]}")
                if attempt < 4:
                    time.sleep(5)
    return None

def process_batch(i, batch_data):
    desc = f"#{batch_data[0]['number']}-#{batch_data[-1]['number']}"
    out_path = OUTPUT_DIR / f"batch_{i}.sql"
    
    if out_path.exists() and out_path.stat().st_size > 50:
        print(f"  Batch {i} ({desc}) already done")
        return True
    
    paragraphs_json = json.dumps([
        {"n": p["number"], "pt": p["plain_text"], "fj": p["formatted_json"]}
        for p in batch_data
    ], ensure_ascii=False)
    
    prompt = f"""Translate these {len(batch_data)} CCC paragraphs from English to SPANISH.

For EACH paragraph:
1. Translate plain_text (pt) to Spanish using proper Catholic terminology
2. Parse formatted_json (fj) - it's a JSON array. For segments with "type":"text", translate the "text" field, keep "attrs" unchanged. For "type":"ref-ccc" or "type":"ref", keep EXACTLY as-is.
3. Output ONE SQL INSERT per paragraph

INPUT:
{paragraphs_json}

OUTPUT FORMAT (ONLY SQL, one line per INSERT, each on separate line with semicolon):
INSERT INTO ccc_paragraphs (number, lang, toc_path, plain_text, formatted_json) VALUES (N, 'es', 'TOC', 'TRANSLATED_TEXT', 'TRANSLATED_JSON');

RULES:
- Use Spanish Bible format: Mt 5,3 (not Mt 5:3)
- Use Catholic Spanish vocabulary
- Preserve all attrs (b=bold, i=italic, href) EXACTLY
- Preserve all ref-ccc and ref segments UNCHANGED
- In SQL: escape single quotes as '' (two single quotes)
- TOC path: translate section names to Spanish (e.g., "PART ONE" -> "PRIMERA PARTE", "CHAPTER ONE" -> "CAPÍTULO PRIMERO", "ARTICLE 1" -> "ARTÍCULO 1", "IN BRIEF" -> "EN RESUMEN", "SECTION ONE" -> "PRIMERA SECCIÓN")
- Keep the ">" separator between TOC path segments
- Output ONLY SQL statements, nothing else before or after"""

    result = call_gemini(prompt)
    if not result:
        return False
    
    insert_count = result.count("INSERT INTO ccc_paragraphs")
    expected = len(batch_data)
    
    if insert_count < expected:
        print(f"  Only got {insert_count}/{expected} INSERTs, retrying...")
        return False
    
    with open(out_path, "w") as f:
        f.write(f"-- CCC Spanish Batch {i} ({desc})\n")
        f.write(f"-- {len(batch_data)} paragraphs\n\n")
        f.write(result.strip())
        f.write("\n")
    
    return True

def main():
    total = len(list(BATCH_DIR.glob("batch_*.json")))
    print(f"Total batches: {total}")
    
    start = int(sys.argv[1]) if len(sys.argv) > 1 else 0
    end = int(sys.argv[2]) if len(sys.argv) > 2 else total - 1
    
    success = 0
    fail = 0
    
    for i in range(start, end + 1):
        batch_path = BATCH_DIR / f"batch_{i}.json"
        if not batch_path.exists():
            continue
        
        with open(batch_path) as f:
            batch_data = json.load(f)
        
        desc = f"#{batch_data[0]['number']}-#{batch_data[-1]['number']}"
        print(f"\nBatch {i}/{total-1} ({desc}, {len(batch_data)} items)...")
        
        if process_batch(i, batch_data):
            success += 1
            print(f"  ✓ Done")
        else:
            fail += 1
            print(f"  ✗ Failed")
        
        # Rate limit: small delay between batches
        time.sleep(1.5)
    
    print(f"\n{'='*40}")
    print(f"Results: {success} succeeded, {fail} failed")
    print(f"SQL files in: {OUTPUT_DIR}")

if __name__ == "__main__":
    main()
