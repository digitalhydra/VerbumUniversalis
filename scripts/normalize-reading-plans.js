const fs = require('fs');
const path = require('path');

const RAW_DIR = './raw_data/reading-plan';
const OUTPUT_DIR = './src/data';

// Ensure output directory exists
if (!fs.existsSync(OUTPUT_DIR)) {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true });
}

// Read and normalize Bible in a Year plan
function normalizeBibleInAYear() {
  const data = JSON.parse(fs.readFileSync(path.join(RAW_DIR, 'bible_in_a_year.json'), 'utf8'));

  return {
    planId: data.planId || 'bible_in_a_year',
    title: data.title,
    description: data.description,
    type: 'bible_in_a_year',
    totalDays: data.days.length,
    days: data.days.map(day => ({
      day: day.day,
      era: day.era,
      readings: day.readings, // Array of Bible references
      readingCount: day.readings.length,
      progress: {
        completed: false,
        completedAt: null,
        readingsCompleted: []
      }
    }))
  };
}

// Normalize daily mass readings (remove apiEndpoint and usccbLink)
function normalizeDailyMassReadings() {
  const readings2025 = JSON.parse(fs.readFileSync(path.join(RAW_DIR, 'readings_2025.json'), 'utf8'));
  const readings2026 = JSON.parse(fs.readFileSync(path.join(RAW_DIR, 'readings_2026.json'), 'utf8'));

  // Combine and sort by date
  const allReadings = [...readings2025, ...readings2026].sort((a, b) =>
    new Date(a.date) - new Date(b.date)
  );

  return {
    planId: 'daily_mass_readings',
    title: 'Daily Mass Readings',
    description: 'Daily Catholic Mass readings following the liturgical calendar.',
    type: 'daily_mass',
    totalDays: allReadings.length,
    days: allReadings.map((reading, index) => {
      const { usccbLink, apiEndpoint, ...cleanReading } = reading;

      // Normalize readings to array format for consistent UI handling
      const readingArray = [];
      if (cleanReading.readings.firstReading) readingArray.push({ type: 'firstReading', reference: cleanReading.readings.firstReading });
      if (cleanReading.readings.secondReading) readingArray.push({ type: 'secondReading', reference: cleanReading.readings.secondReading });
      if (cleanReading.readings.psalm) readingArray.push({ type: 'psalm', reference: cleanReading.readings.psalm });
      if (cleanReading.readings.gospel) readingArray.push({ type: 'gospel', reference: cleanReading.readings.gospel });

      return {
        day: index + 1,
        date: cleanReading.date,
        monthDay: cleanReading.monthDay,
        season: cleanReading.season,
        subSeason: cleanReading.subSeason || null,
        readings: readingArray,
        readingCount: readingArray.length,
        progress: {
          completed: false,
          completedAt: null,
          readingsCompleted: []
        }
      };
    })
  };
}

// Main execution
try {
  console.log('Normalizing reading plans...\n');

  const bibleInAYear = normalizeBibleInAYear();
  const dailyMass = normalizeDailyMassReadings();

  // Write normalized plans
  fs.writeFileSync(
    path.join(OUTPUT_DIR, 'bible-in-a-year.json'),
    JSON.stringify(bibleInAYear, null, 2)
  );
  console.log(`✓ Bible in a Year: ${bibleInAYear.totalDays} days normalized`);

  fs.writeFileSync(
    path.join(OUTPUT_DIR, 'daily-mass-readings.json'),
    JSON.stringify(dailyMass, null, 2)
  );
  console.log(`✓ Daily Mass Readings: ${dailyMass.totalDays} days normalized (2025-2026)`);

  // Create a plans index file
  const plansIndex = {
    plans: [
      {
        planId: bibleInAYear.planId,
        title: bibleInAYear.title,
        description: bibleInAYear.description,
        type: bibleInAYear.type,
        totalDays: bibleInAYear.totalDays,
        file: 'bible-in-a-year.json'
      },
      {
        planId: dailyMass.planId,
        title: dailyMass.title,
        description: dailyMass.description,
        type: dailyMass.type,
        totalDays: dailyMass.totalDays,
        file: 'daily-mass-readings.json'
      }
    ],
    lastUpdated: new Date().toISOString()
  };

  fs.writeFileSync(
    path.join(OUTPUT_DIR, 'plans-index.json'),
    JSON.stringify(plansIndex, null, 2)
  );
  console.log('✓ Plans index created');

  console.log('\nDone! Normalized data saved to ./src/data/');

} catch (error) {
  console.error('Error:', error.message);
  process.exit(1);
}
