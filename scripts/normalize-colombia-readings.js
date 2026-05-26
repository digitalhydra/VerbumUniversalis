/**
 * normalize-colombia-readings.js
 * 
 * Normalizes the Colombia daily mass readings JSON:
 * 1. Fixes celebration text (double spaces, leading zeros, CRLF, ALL CAPS)
 * 2. Fills missing gospel readings from Rome calendar
 * 3. Fills missing color values from season/celebration context
 * 4. Normalizes psalm reference formatting
 * 5. Converts array to date-keyed object
 */

const fs = require('fs');
const path = require('path');

const PROJECT_ROOT = path.resolve(__dirname, '..');
const COLOMBIA_PATH = path.join(PROJECT_ROOT, 'app/src/main/assets/plans/daily-mass-readings-colombia.json');
const ROME_PATH = path.join(PROJECT_ROOT, 'app/src/main/assets/plans/daily-mass-readings-rome.json');
const OUTPUT_PATH = path.join(PROJECT_ROOT, 'app/src/main/assets/plans/daily-mass-readings-colombia.json');
const BACKUP_PATH = path.join(PROJECT_ROOT, 'app/src/main/assets/plans/daily-mass-readings-colombia.json.bak');

// ─── Load data ───────────────────────────────────────────────────────────────
const colombiaData = JSON.parse(fs.readFileSync(COLOMBIA_PATH, 'utf8'));
const romeData = JSON.parse(fs.readFileSync(ROME_PATH, 'utf8'));

// Build Rome date index
const romeByDate = {};
for (const entry of romeData) {
  romeByDate[entry.date] = entry;
}

// ─── Color derivation helpers ────────────────────────────────────────────────
function deriveColor(celebration, season) {
  const c = (celebration || '').toLowerCase();
  
  // Advent and Lent are always purple
  if (season === 'Advent' || season === 'Lent') return 'Morado';
  
  // Christmas and Easter are white
  if (season === 'Christmas' || season === 'Easter') return 'Blanco';
  
  // Within celebration text
  if (c.includes('solemnidad')) return 'Blanco';
  if (c.includes('domingo de ramos')) return 'Rojo';
  if (c.includes('viernes santo')) return 'Rojo';
  if (c.includes('pentecostés') || c.includes('pentecostes')) return 'Rojo';
  if (c.includes('mártir') || c.includes('martir')) return 'Rojo';
  if (c.includes('apóstol') || c.includes('apostol')) return 'Rojo';
  if (c.includes('evangelista')) return 'Rojo';
  if (c.includes('exaltación') && c.includes('cruz')) return 'Rojo';
  
  // Ordinary time default
  if (season === 'Ordinary Time') return 'Verde';
  
  // Fallback
  return 'Verde';
}

// ─── Spanish month names ─────────────────────────────────────────────────────
const MONTHS = [
  'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
  'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
];

const DAYS_OF_WEEK = [
  'Domingo', 'Lunes', 'Martes', 'Miércoles', 'Miercoles', 'Jueves', 'Viernes', 'Sábado', 'Sabado'
];

// ─── Spanish title case ─────────────────────────────────────────────────────
// Words that stay lowercase in Spanish titles (unless they start the title)
const LOWERCASE_WORDS = new Set([
  'de', 'del', 'en', 'el', 'la', 'los', 'las', 'y', 'e', 'o', 'u',
  'a', 'ante', 'bajo', 'con', 'contra', 'desde', 'durante', 'entre',
  'hacia', 'hasta', 'para', 'por', 'según', 'sin', 'sobre', 'tras',
  'que', 'cual', 'cuales'
]);

function toSpanishTitleCase(text) {
  const words = text.toLowerCase().split(/\s+/);
  return words.map((word, i) => {
    // First word always capitalized
    if (i === 0) return word.charAt(0).toUpperCase() + word.slice(1);
    // Small words stay lowercase
    if (LOWERCASE_WORDS.has(word)) return word;
    // Capitalize proper nouns / significant words
    return word.charAt(0).toUpperCase() + word.slice(1);
  }).join(' ');
}

// ─── Celebration normalization ───────────────────────────────────────────────
function normalizeCelebration(rawText) {
  if (!rawText || rawText.trim() === '') return '';
  
  let text = rawText;
  
  // Remove CR/LF
  text = text.replace(/\r\n/g, ' ').replace(/\r/g, ' ').replace(/\n/g, ' ');
  
  // Collapse multiple spaces
  text = text.replace(/\s{2,}/g, ' ');
  
  // Trim
  text = text.trim();
  
  // Remove leading zeros from day numbers: "01 " → "1 ", "02 " → "2 ", etc.
  text = text.replace(/^0(\d)\s/, '$1 ');
  
  // Standardize abbreviations
  text = text.replace(/\bSem\.\s+del\s+TO\b/g, 'Semana del Tiempo Ordinario');
  text = text.replace(/\bSem\.\s+de\s+Cuaresma\b/g, 'Semana de Cuaresma');
  text = text.replace(/\bSem\.\s+de\s+Pascua\b/g, 'Semana de Pascua');
  
  // Fix ALL CAPS feast names → Spanish title case
  // Spanish convention: first letter + proper nouns capitalized; articles/prepositions lowercase
  const allCapsPattern = /\b([A-ZÁÉÍÓÚÑÜ]{2,}(?:\s+[A-ZÁÉÍÓÚÑÜ]{2,})*)\b/g;
  text = text.replace(allCapsPattern, (match) => {
    if (match.length <= 3) return match;
    return toSpanishTitleCase(match);
  });
  
  // Fix accents that get lost in all-caps scraping
  text = text.replace(/\bEpifania\b/g, 'Epifanía');
  text = text.replace(/\bSeñor\b/g, 'Señor');
  text = text.replace(/\bMartir\b/g, 'Mártir');
  text = text.replace(/\bApostol\b/g, 'Apóstol');
  text = text.replace(/\bPentecostes\b/g, 'Pentecostés');
  text = text.replace(/\bMiercoles\b/g, 'Miércoles');
  text = text.replace(/\bSabado\b/g, 'Sábado');
  
  // Clean up trailing commas and periods
  text = text.replace(/\s*,\s*$/, '');
  text = text.replace(/\.\.\./g, '…');
  
  // Clean up "En la Vigilia:" trailing text (move to separate note if needed)
  text = text.replace(/\s*En la Vigilia:\s*$/i, '');
  text = text.replace(/\s*En la Vigilia\s*$/i, '');
  
  // ── Strip redundant info already in other fields ──────────────────────────
  // Strip leading day number + month name (e.g., "1 Enero, " → "")
  // Pattern: "{digits} {MonthName}, " at start of string
  text = text.replace(/^\d{1,2}\s+(?:Enero|Febrero|Marzo|Abril|Mayo|Junio|Julio|Agosto|Septiembre|Octubre|Noviembre|Diciembre),\s*/i, '');
  
  // Strip trailing color (e.g., ", Blanco", ". Blanco", ", Verde o Rojo")
  // Colors can be combined with " o "
  text = text.replace(/[,.]\s*(?:Blanco|Verde|Rojo|Morado|Negro|Rosado|Azul)(?:\s+o\s+(?:Blanco|Verde|Rojo|Morado|Negro|Rosado|Azul)){0,2}\s*$/i, '');
  
  // Clean up any trailing/leading commas or dots left after stripping
  text = text.replace(/^[,\s.]+/, '');
  text = text.replace(/[,\s.]+$/, '');
  text = text.trim();
  
  return text;
}

// ─── Generate celebration when missing ──────────────────────────────────────
function generateCelebration(entry) {
  const [month, day] = (entry.monthDay || '').split('/').map(Number);
  if (!month || !day) return '';
  
  // Known feast overrides by date
  const knownFeasts = {
    '12-8':  { text: 'Inmaculada Concepción de la Santísima Virgen María, Solemnidad', color: 'Blanco' },
    '12-12': { text: 'Nuestra Señora de Guadalupe, Fiesta', color: 'Blanco' },
    '12-25': { text: 'Natividad del Señor (Navidad), Solemnidad', color: 'Blanco' },
  };
  const feastKey = `${month}-${day}`;
  const feast = knownFeasts[feastKey];
  
  if (feast) {
    if (!entry.color || entry.color === 'Morado') entry.color = feast.color;
    return feast.text;
  }
  
  const monthName = MONTHS[month - 1];
  const seasonName = entry.season || '';
  
  // Determine day name from date if available
  const date = new Date(entry.date + 'T12:00:00');
  const dayNames = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];
  const dayName = dayNames[date.getDay()];
  
  // Map season to Spanish
  const seasonMap = {
    'Advent': 'Adviento',
    'Christmas': 'Navidad',
    'Lent': 'Cuaresma',
    'Holy Week': 'Semana Santa',
    'Easter': 'Pascua',
    'Eastertide': 'Pascua',
    'Ordinary Time': 'Tiempo Ordinario',
  };
  const seasonEs = seasonMap[seasonName] || seasonName;
  
  return `${dayName}. ${seasonEs}`;
}

// ─── Psalm reference normalization ──────────────────────────────────────────
function normalizePsalmRef(ref) {
  if (!ref || typeof ref !== 'string') return ref;
  
  // Fix "Psalm 67:2-3, 5.6+8" → "Psalm 67:2-3, 5, 6, 8"
  // Fix "Psalm 67:2-3, 5.6+8" pattern (periods and plus signs used as separators)
  let normalized = ref;
  
  // Replace period separators in verse lists with commas, but NOT in ranges (e.g., 5.6 → 5,6 not 5.6)
  normalized = normalized.replace(/(\d)\.(\d)/g, '$1, $2');
  
  // Replace + with ", "
  normalized = normalized.replace(/\+(\d)/g, ', $1');
  
  // Normalize spaces after commas
  normalized = normalized.replace(/,\s*/g, ', ');
  
  // Fix "Psalm 95" (non-breaking space) → "Psalm 95"
  normalized = normalized.replace(/\u00A0/g, ' ');
  
  return normalized;
}

// ─── Normalize readings object ───────────────────────────────────────────────
function normalizeReadings(readings) {
  if (!readings || typeof readings !== 'object') return {};
  
  const normalized = {};
  for (const [key, value] of Object.entries(readings)) {
    if (typeof value === 'string') {
      normalized[key] = normalizePsalmRef(value);
    } else {
      normalized[key] = value;
    }
  }
  return normalized;
}

// ─── Process ─────────────────────────────────────────────────────────────────
const changes = {
  gospelFilled: [],
  gospelFilledFromRome: [],
  colorFilled: [],
  celebrationNormalized: [],
  readingsNormalized: [],
};

const normalizedData = {};

for (let i = 0; i < colombiaData.length; i++) {
  const entry = { ...colombiaData[i] };
  const date = entry.date;
  const romeEntry = romeByDate[date];
  const originalEntry = colombiaData[i];
  
  // ── 1. Normalize celebration ──────────────────────────────────────────────
  if (entry.celebration !== undefined) {
    const original = entry.celebration;
    entry.celebration = normalizeCelebration(entry.celebration);
    if (original !== entry.celebration) {
      changes.celebrationNormalized.push({
        date,
        before: original.substring(0, 120),
        after: entry.celebration.substring(0, 120),
      });
    }
  }
  
  // ── 2. Fill missing color ─────────────────────────────────────────────────
  if (!entry.color || entry.color.trim() === '') {
    const derivedColor = deriveColor(entry.celebration, entry.season);
    entry.color = derivedColor;
    changes.colorFilled.push({ date, color: derivedColor, celebration: entry.celebration?.substring(0, 60) });
  }
  
  // ── 2b. Generate celebration if still empty ───────────────────────────────
  if (!entry.celebration || entry.celebration.trim() === '') {
    entry.celebration = generateCelebration(entry);
    changes.celebrationNormalized.push({
      date,
      before: '(empty)',
      after: entry.celebration.substring(0, 120),
    });
  }
  
  // ── 3. Fill missing gospel from Rome ──────────────────────────────────────
  if (!entry.readings || Object.keys(entry.readings).length === 0) {
    // Entire readings object is empty → replace from Rome entirely
    if (romeEntry && romeEntry.readings) {
      entry.readings = { ...romeEntry.readings };
      changes.gospelFilledFromRome.push({
        date,
        celebration: entry.celebration?.substring(0, 60),
        readingsFilled: Object.keys(entry.readings).join(', ')
      });
    }
  } else if (entry.readings.gospel === undefined || entry.readings.gospel === null || entry.readings.gospel === '') {
    // Gospel key exists but is empty → fill from Rome
    if (romeEntry && romeEntry.readings && romeEntry.readings.gospel) {
      const oldReadings = { ...entry.readings };
      entry.readings.gospel = romeEntry.readings.gospel;
      changes.gospelFilled.push({
        date,
        celebration: entry.celebration?.substring(0, 60),
        gospelBefore: oldReadings.gospel || '(empty)',
        gospelAfter: entry.readings.gospel,
      });
    }
  }
  
  // ── 4. Normalize readings references ──────────────────────────────────────
  if (entry.readings && Object.keys(entry.readings).length > 0) {
    const originalReadings = JSON.stringify(entry.readings);
    entry.readings = normalizeReadings(entry.readings);
    if (originalReadings !== JSON.stringify(entry.readings)) {
      changes.readingsNormalized.push({
        date,
        before: originalReadings.substring(0, 120),
        after: JSON.stringify(entry.readings).substring(0, 120),
      });
    }
  }
  
  // ── 5. Collect into array (matching Rome format) ──────────────────────────
  normalizedData[date] = entry;
}

// Convert to array sorted by date (matching Rome format)
const outputArray = Object.keys(normalizedData)
  .sort()
  .map(date => normalizedData[date]);

// ─── Backup original ────────────────────────────────────────────────────────
fs.copyFileSync(COLOMBIA_PATH, BACKUP_PATH);
console.log('Backup saved to:', BACKUP_PATH);

// ─── Write normalized output ─────────────────────────────────────────────────
fs.writeFileSync(OUTPUT_PATH, JSON.stringify(outputArray, null, 2), 'utf8');
console.log('Normalized file written to:', OUTPUT_PATH);

// ─── Summary ─────────────────────────────────────────────────────────────────
console.log('\n═══════════════════════════════════════════════════════════════');
console.log('  NORMALIZATION SUMMARY');
console.log('═══════════════════════════════════════════════════════════════\n');

console.log(`📅 Total entries processed: ${outputArray.length}\n`);

// Gospel fills
console.log('─── GOSPEL FILLS ───');
console.log(`  📖 Entire readings filled from Rome: ${changes.gospelFilledFromRome.length}`);
changes.gospelFilledFromRome.forEach(c => {
  console.log(`     ${c.date}: ${c.celebration}`);
  console.log(`       → Filled: ${c.readingsFilled}`);
});
console.log(`  📖 Gospel only filled from Rome: ${changes.gospelFilled.length}`);
changes.gospelFilled.forEach(c => {
  console.log(`     ${c.date}: ${c.celebration}`);
  console.log(`       → Gospel: ${c.gospelAfter}`);
});

// Color fills
console.log(`\n─── COLOR FILLS ───`);
console.log(`  🎨 Missing colors derived: ${changes.colorFilled.length}`);
const colorCounts = {};
changes.colorFilled.forEach(c => {
  colorCounts[c.color] = (colorCounts[c.color] || 0) + 1;
});
for (const [color, count] of Object.entries(colorCounts)) {
  console.log(`     ${color}: ${count}`);
}
// Show samples
console.log('  Samples:');
changes.colorFilled.slice(0, 5).forEach(c => {
  console.log(`     ${c.date}: → ${c.color} | ${c.celebration}`);
});
if (changes.colorFilled.length > 5) console.log(`     ... and ${changes.colorFilled.length - 5} more`);

// Celebration normalization
console.log(`\n─── CELEBRATION TEXT ───`);
console.log(`  📝 Celebrations normalized: ${changes.celebrationNormalized.length}`);
console.log('  Samples (first 5):');
changes.celebrationNormalized.slice(0, 5).forEach(c => {
  console.log(`     ${c.date}:`);
  console.log(`       Before: "${c.before}"`);
  console.log(`       After:  "${c.after}"`);
});

// Readings normalization
console.log(`\n─── READINGS FORMAT ───`);
console.log(`  📖 Readings references normalized: ${changes.readingsNormalized.length}`);
changes.readingsNormalized.slice(0, 5).forEach(c => {
  console.log(`     ${c.date}`);
  console.log(`       Before: ${c.before}`);
  console.log(`       After:  ${c.after}`);
});

console.log('\n═══════════════════════════════════════════════════════════════');
console.log('  DONE');
console.log('═══════════════════════════════════════════════════════════════');
