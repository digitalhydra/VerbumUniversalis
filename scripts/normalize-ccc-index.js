/**
 * normalize-ccc-index.js
 * Normalizes the scraped CCC index:
 * 1. Joins sub-entries with text but no paragraphs to the next sub-entry
 * 2. Removes footer/copyright artifacts
 * 3. Converts pure "See ..." entries into seeAlso references
 */

const fs = require('fs');
const path = require('path');

const INPUT = path.join(__dirname, '..', 'raw_data', 'ccc-index.json');
const OUTPUT = path.join(__dirname, '..', 'raw_data', 'ccc-index.json');
const BACKUP = path.join(__dirname, '..', 'raw_data', 'ccc-index.json.bak');

const data = JSON.parse(fs.readFileSync(INPUT, 'utf8'));

// ─── Footer/artifact patterns to remove ──────────────────────────────────────
const FOOTER_PATTERNS = [
  /^Copyright permission/i,
  /^&copy;\s*\d{4}/i,
  /^All Rights Reserved/i,
  /^Powered by/i,
  /^case number/i,
  /^Saint Charles Borromeo/i,
  /^Picayune/i,
];

function isFooterArtifact(text) {
  if (!text) return false;
  for (const pattern of FOOTER_PATTERNS) {
    if (pattern.test(text)) return true;
  }
  return false;
}

// ─── Normalize a term's sub-entries ──────────────────────────────────────────
function normalizeEntries(entries) {
  // Step 1: Filter out footer artifacts
  let filtered = entries.filter(e => !isFooterArtifact(e.text));
  
  // Step 2: Process "See" references — extract them as seeAlso instead of sub-entries
  const seeRefs = [];
  filtered = filtered.filter(e => {
    // "See TermName" or ". See TermName"
    const seeMatch = e.text?.match(/^(?:\.\s*)?See\s+(.*?)\s*$/i);
    if (seeMatch && e.paragraphs.length === 0) {
      const ref = seeMatch[1].trim();
      if (ref) seeRefs.push(ref);
      return false;
    }
    // "See also TermName" (at start of entry text)
    const seeAlsoMatch = e.text?.match(/^(?:\.\s*)?See also\s+(.*?)\s*$/i);
    if (seeAlsoMatch && e.paragraphs.length === 0) {
      const refs = seeAlsoMatch[1].split(/[,;]/).map(r => r.trim()).filter(Boolean);
      seeRefs.push(...refs);
      return false;
    }
    return true;
  });
  
  // Step 3: Join entries with text but no paragraphs to the next one
  const merged = [];
  let carry = null;
  
  for (let i = 0; i < filtered.length; i++) {
    const current = filtered[i];
    
    if (current.paragraphs.length === 0 && current.text) {
      // This entry has text but no paragraphs — carry it forward
      if (carry) {
        carry.text += ' ' + current.text;
      } else {
        carry = { ...current };
      }
    } else if (carry) {
      // We have a carried entry — join with current
      carry.text = carry.text ? (carry.text + ' ' + (current.text || '')) : current.text;
      carry.paragraphs = [...new Set([...carry.paragraphs, ...current.paragraphs])]
        .sort((a, b) => a - b);
      merged.push(carry);
      carry = null;
    } else {
      merged.push(current);
    }
  }
  
  // If there's a carried entry at the end with no more entries to join
  if (carry) {
    // If it has useful text, keep it even without paragraphs
    if (carry.text && carry.text.length > 2) {
      merged.push(carry);
    }
  }
  
  // Step 4: Clean up any entries where text became just whitespace after merging
  merged.forEach(e => {
    if (e.text) e.text = e.text.replace(/\s+/g, ' ').trim();
  });
  
  return { entries: merged, seeRefs };
}

// ─── Process all letters ─────────────────────────────────────────────────────
let totalMerged = 0;
let totalRemoved = 0;
let totalSeeAdded = 0;

for (const [letter, terms] of Object.entries(data.letters)) {
  for (const term of terms) {
    const originalCount = term.entries.length;
    const { entries, seeRefs } = normalizeEntries(term.entries);
    term.entries = entries;
    
    if (seeRefs.length > 0) {
      term.seeAlso = [...new Set([...(term.seeAlso || []), ...seeRefs])];
      totalSeeAdded += seeRefs.length;
    }
    
    totalMerged += originalCount - entries.length;
  }
}

// ─── Update stats ────────────────────────────────────────────────────────────
let totalSubEntries = 0;
for (const terms of Object.values(data.letters)) {
  for (const term of terms) {
    totalSubEntries += term.entries.length;
  }
}
data.totalSubEntries = totalSubEntries;
data.normalized = new Date().toISOString();

// ─── Save ────────────────────────────────────────────────────────────────────
fs.copyFileSync(INPUT, BACKUP);
fs.writeFileSync(OUTPUT, JSON.stringify(data, null, 2));

console.log('✅ Normalization complete');
console.log(`   Entries merged/removed: ${totalMerged}`);
console.log(`   See references added: ${totalSeeAdded}`);
console.log(`   Final sub-entries: ${totalSubEntries}`);
console.log(`   Backup: ${BACKUP}`);
