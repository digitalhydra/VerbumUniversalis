import json
import os
from datetime import datetime

def unify_calendar_2026():
    calendar_dir = '/mnt/disk2/dev/VerbunUniversalis/raw_data/reading-plan/liturgical-calendar/2026/'
    output_file = '/mnt/disk2/dev/VerbunUniversalis/app/src/main/assets/liturgical_calendar.json'

    entries = []
    # Sort files by date to keep it organized
    files = sorted([f for f in os.listdir(calendar_dir) if f.endswith('.json')])

    for filename in files:
        with open(os.path.join(calendar_dir, filename), 'r') as f:
            data = json.load(f)
            # Normalize celebration type for consistency
            if 'celebration' in data:
                # The app expects Celebration object from LiturgicalEntities.kt
                entries.append({
                    "date": data["date"],
                    "monthDay": data["monthDay"],
                    "season": data.get("season"),
                    "celebration": data["celebration"],
                    "readings": [] # Placeholder as requested by schema
                })

    with open(output_file, 'w') as f:
        json.dump(entries, f, indent=2)
    print(f"Unified {len(entries)} calendar entries to {output_file}")

def transform_readings_2026():
    input_file = '/mnt/disk2/dev/VerbunUniversalis/raw_data/reading-plan/readings_2026.json'
    output_file = '/mnt/disk2/dev/VerbunUniversalis/app/src/main/assets/plans/daily-mass-readings.json'

    with open(input_file, 'r') as f:
        raw_readings = json.load(f)

    days = []
    for i, entry in enumerate(raw_readings):
        readings_list = []
        for r_type, ref in entry["readings"].items():
            readings_list.append({
                "type": r_type,
                "reference": ref
            })

        days.append({
            "day": i + 1,
            "date": entry["date"],
            "monthDay": entry["monthDay"],
            "season": entry["season"],
            "subSeason": None,
            "readings": readings_list,
            "readingCount": len(readings_list),
            "progress": {
                "completed": False,
                "completedAt": None,
                "readingsCompleted": []
            }
        })

    final_data = {
        "planId": "daily_mass_readings",
        "title": "Daily Mass Readings 2026",
        "description": "Daily Catholic Mass readings for 2026.",
        "type": "daily_mass",
        "totalDays": len(days),
        "days": days
    }

    with open(output_file, 'w') as f:
        json.dump(final_data, f, indent=2)
    print(f"Transformed {len(days)} readings to {output_file}")

if __name__ == "__main__":
    unify_calendar_2026()
    transform_readings_2026()
