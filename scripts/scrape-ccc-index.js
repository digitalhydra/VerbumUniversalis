/**
 * scrape-ccc-index.js
 * Scrapes the CCC index from scborromeo.org and saves as JSON.
 */

const http = require('http');
const fs = require('fs');
const path = require('path');

const BASE = 'http://www.scborromeo.org/ccc/index/';
const OUTPUT = path.join(__dirname, '..', 'raw_data', 'ccc-index.json');

// Page → letters mapping
const PAGE_LETTERS = {
  'a.htm': ['A'], 'b.htm': ['B'], 'c.htm': ['C'], 'd.htm': ['D'],
  'e.htm': ['E'], 'f.htm': ['F'], 'g.htm': ['G'], 'h.htm': ['H'],
  'i.htm': ['I'], 'j.htm': ['J'], 'k.htm': ['K'], 'l.htm': ['L'],
  'm.htm': ['M'], 'n.htm': ['N'], 'o.htm': ['O'], 'p.htm': ['P'],
  'qr.htm': ['Q', 'R'],
  's.htm': ['S'], 't.htm': ['T'], 'u.htm': ['U'], 'v.htm': ['V'],
  'wxyz.htm': ['W', 'X', 'Y', 'Z']
};

// ─── HTTP fetch ──────────────────────────────────────────────────────────────
function fetchPage(pageName) {
  return new Promise((resolve, reject) => {
    const pageUrl = BASE + pageName;
    http.get(pageUrl, (res) => {
      if (res.statusCode !== 200) return reject(new Error(`HTTP ${res.statusCode}`));
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => resolve(body));
    }).on('error', reject);
  });
}

// ─── HTML decode ─────────────────────────────────────────────────────────────
function decodeHtml(text) {
  return text
    .replace(/&nbsp;/g, ' ')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&mdash;/g, '—')
    .replace(/&ndash;/g, '–')
    .replace(/&#146;/g, "'")
    .replace(/&#133;/g, '…')
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
    .replace(/\uFFFD/g, '')  // replacement character
    .trim();
}

function stripTags(html) {
  return decodeHtml(html.replace(/<[^>]+>/g, ''));
}

// ─── Parse paragraph numbers from link text ──────────────────────────────────
function parseParagraphs(text) {
  const nums = [];
  const parts = text.split(/[,;\s]+/).filter(Boolean);
  for (const part of parts) {
    const m = part.match(/^(\d+)(?:-(\d+))?$/);
    if (!m) continue;
    const start = parseInt(m[1]);
    const end = m[2] ? parseInt(m[2]) : start;
    let actualEnd = end;
    if (end < start) {
      const s1 = m[1], s2 = m[2];
      const prefix = s1.substring(0, s1.length - s2.length);
      actualEnd = parseInt(prefix + s2);
    }
    for (let n = start; n <= actualEnd; n++) nums.push(n);
  }
  return nums;
}

// ─── Parse all terms from HTML content ───────────────────────────────────────
function parseTerms(htmlContent) {
  // Split by <A NAME="..."> anchors — each anchor starts a new term
  // Terms are separated by these anchors. Before the first anchor is the
  // prefatory note which we skip.
  const terms = [];
  
  // Find all term anchors: <A NAME="TermName"></A><B>TermName</B>
  const anchorRegex = /<A\s+NAME="([^"]+)"\s*><\/A>\s*<B>(.*?)<\/B>/gi;
  let match;
  let lastIndex = 0;
  
  while ((match = anchorRegex.exec(htmlContent)) !== null) {
    const termName = stripTags(match[2]).trim();
    const anchorEnd = match.index + match[0].length;
    
    // Find where this term's content ends (at next anchor or end)
    const nextAnchorIdx = htmlContent.indexOf('<A NAME="', anchorEnd);
    const contentEnd = nextAnchorIdx > 0 ? nextAnchorIdx : htmlContent.length;
    let content = htmlContent.substring(anchorEnd, contentEnd);
    
    // Handle inline content after </B> on the same line (e.g., Acedia: "</B>, 1866, 2733")
    // If there's content on the same HTML line after </B>, use it as the start
    // of the content block instead of the default (which also starts there)
    const afterBold = htmlContent.substring(match.index + match[0].length).split('\n')[0];
    if (afterBold.trim()) {
      // Content already starts with this line from anchorEnd — just ensure it's parsed
      // (the parseSubEntries function will handle it)
    }
    
    // Parse sub-entries from the content block
    const entries = parseSubEntries(content);
    const seeAlso = parseSeeAlso(content);
    
    const term = { term: termName, entries };
    if (seeAlso.length > 0) term.seeAlso = seeAlso;
    terms.push(term);
  }
  
  return terms;
}

function parseSubEntries(content) {
  const entries = [];
  
  // Sub-entries are separated by newlines (not <br> tags!)
  const lines = content.split(/\n/);
  
  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed) continue;
    
    // Skip <A NAME="..."> and <B>...</B> lines (term headers)
    if (/^<A\s+NAME="/i.test(trimmed)) continue;
    if (/^<B>/i.test(trimmed)) continue;
    
    // Skip See also lines ONLY if they have no paragraph links
    if (/<I>\s*See also/i.test(trimmed) || /<I>\s*See\s/i.test(trimmed)) {
      if (!trimmed.includes('href=')) continue;
    }
    
    // Extract text and paragraph links
    const linkRegex = /<a\s+[^>]*href="[^"]*"[^>]*>([^<]+)<\/a>/gi;
    const paragraphNums = [];
    let linkMatch;
    
    while ((linkMatch = linkRegex.exec(trimmed)) !== null) {
      const nums = parseParagraphs(linkMatch[1].trim());
      paragraphNums.push(...nums);
    }
    
    // Remove link tags but keep their content (for paragraph number extraction)
    // We already extracted paragraph numbers above; now just strip the tags
    let cleanLine = trimmed.replace(/<a\s+[^>]*>/gi, '').replace(/<\/a>/gi, '');
    cleanLine = stripTags(cleanLine).trim();
    
    // Remove leading commas, periods, spaces
    cleanLine = cleanLine.replace(/^[,.\s\uFFFD]+/, '').trim();
    // Remove trailing commas
    cleanLine = cleanLine.replace(/[,.\s]+$/, '').trim();
    
    // Skip pure blank/separator lines
    if (!cleanLine && paragraphNums.length === 0) continue;
    
    entries.push({
      text: cleanLine || null,
      paragraphs: [...new Set(paragraphNums)].sort((a, b) => a - b),
    });
  }
  
  return entries;
}

function parseSeeAlso(content) {
  const refs = [];
  const seeMatch = content.match(/<I>\s*See also\s*(.*?)<\/I>/i);
  if (!seeMatch) return refs;
  
  const seeText = stripTags(seeMatch[1]).trim();
  // Split by ; or , — but careful with names that have commas
  // Format is usually: "See also Term; AnotherTerm" or "See also Term, Another Term"
  const parts = seeText.split(/;(?:\s*_?_?)/);
  for (const part of parts) {
    const cleaned = part.replace(/^_|_$/g, '').trim();
    if (cleaned) {
      refs.push(cleaned);
    }
  }
  return refs;
}

// ─── Main ────────────────────────────────────────────────────────────────────
async function main() {
  console.log('Scraping CCC Index from scborromeo.org...\n');
  
  const allLetters = {};
  let totalTerms = 0;
  
  const pageNames = Object.keys(PAGE_LETTERS);
  for (let i = 0; i < pageNames.length; i++) {
    const pageName = pageNames[i];
    const letters = PAGE_LETTERS[pageName];
    console.log(`[${i + 1}/${pageNames.length}] Fetching ${pageName} (${letters.join(', ')})...`);
    
    try {
      const html = await fetchPage(pageName);
      
      // Extract content: after the alphabet nav table, before copyright footer
      // The alphabet nav table ends with </table></center> and is followed by content
      const contentStart = html.indexOf('</table></center>');
      if (contentStart < 0) {
        console.warn(`  WARN: No </table></center> found in ${pageName}`);
        for (const l of letters) allLetters[l] = [];
        continue;
      }
      
      const contentEnd = html.indexOf('<p align="center">', contentStart);
      const content = html.substring(
        contentStart + 17,  // skip past </table></center>
        contentEnd > 0 ? contentEnd : html.length
      );
      
      // Parse all terms from this page
      const terms = parseTerms(content);
      console.log(`  Parsed ${terms.length} raw terms`);
      
      // Split terms by letter
      // For single-letter pages, all terms belong to that letter
      if (letters.length === 1) {
        allLetters[letters[0]] = terms;
        totalTerms += terms.length;
        console.log(`  ${letters[0]}: ${terms.length} terms`);
        continue;
      }
      
      // For combined pages, partition terms by letter anchor positions
      // Find positions of each letter anchor in the HTML
      const anchorPositions = {};
      for (const letter of letters) {
        const pattern = new RegExp(`<[Aa]\\s+[Nn][Aa][Mm][Ee]="${letter}"`, '');
        const match = html.match(pattern);
        if (match) {
          anchorPositions[letter] = match.index;
        }
      }
      
      // The first letter may not have an anchor - terms before first anchor belong to it
      const sortedAnchors = Object.entries(anchorPositions)
        .sort((a, b) => a[1] - b[1]);
      
      // Map each letter to its position (first letter gets position 0)
      const letterStartPositions = {};
      for (const letter of letters) {
        letterStartPositions[letter] = 0;
      }
      for (let i = 0; i < sortedAnchors.length; i++) {
        letterStartPositions[sortedAnchors[i][0]] = sortedAnchors[i][1];
      }
      
      // Initialize letter arrays
      for (const letter of letters) {
        allLetters[letter] = [];
      }
      
      // Assign terms based on their position relative to letter anchors
      for (const term of terms) {
        const escaped = term.term.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        const termPattern = new RegExp(`<A\\s+NAME="${escaped}"`, 'i');
        const termMatch = html.match(termPattern);
        const termPos = termMatch ? termMatch.index : -1;
        
        // Find which letter this term belongs to
        let assigned = false;
        // Check from last to first letter anchor
        for (let i = sortedAnchors.length - 1; i >= 0; i--) {
          if (termPos >= sortedAnchors[i][1]) {
            allLetters[sortedAnchors[i][0]].push(term);
            assigned = true;
            break;
          }
        }
        if (!assigned) {
          // Before any letter anchor → first letter
          allLetters[letters[0]].push(term);
        }
      }
      
      for (const letter of letters) {
        totalTerms += allLetters[letter].length;
        console.log(`  ${letter}: ${allLetters[letter].length} terms`);
      }
    } catch (err) {
      console.error(`  ERROR: ${err.message}`);
      for (const l of letters) allLetters[l] = [];
    }
  }
  
  // ─── Output ────────────────────────────────────────────────────────────────
  // Count total sub-entries
  let totalSubEntries = 0;
  for (const terms of Object.values(allLetters)) {
    for (const term of terms) {
      totalSubEntries += term.entries.length;
    }
  }
  
  const output = {
    source: BASE,
    description: 'CCC Index Analyticus (English) — from Catechism of the Catholic Church, Second Edition',
    scraped: new Date().toISOString(),
    totalTerms,
    totalSubEntries,
    letters: allLetters,
  };
  
  const outDir = path.dirname(OUTPUT);
  if (!fs.existsSync(outDir)) fs.mkdirSync(outDir, { recursive: true });
  
  fs.writeFileSync(OUTPUT, JSON.stringify(output, null, 2));
  console.log(`\n✅ Written ${totalTerms} terms (${totalSubEntries} sub-entries) across ${Object.keys(allLetters).length} letters`);
  console.log(`   → ${OUTPUT}`);
  
  console.log('\n=== PER-LETTER SUMMARY ===');
  for (const letter of Object.keys(allLetters).sort()) {
    const terms = allLetters[letter];
    const subEntries = terms.reduce((sum, t) => sum + t.entries.length, 0);
    console.log(`  ${letter}: ${terms.length} terms, ${subEntries} sub-entries`);
  }
}

main().catch(err => { console.error('Fatal:', err); process.exit(1); });
