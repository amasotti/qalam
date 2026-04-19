#!/usr/bin/env bash
# Run all Phase 1 migration scripts in order.
# Set OLD_DSN and NEW_DSN before running.
#
# Example:
#   export OLD_DSN="postgresql://localhost:5433/annahwi"
#   export NEW_DSN="postgresql://localhost:5432/qalam"
#   bash migration/run_all.sh

set -euo pipefail

: "${OLD_DSN:?OLD_DSN must be set}"
: "${NEW_DSN:?NEW_DSN must be set}"

DIR="$(cd "$(dirname "$0")" && pwd)"

scripts=(
    migrate_01_roots.py
    migrate_02_words.py
    migrate_03_word_examples.py
    migrate_04_dictionary_links.py
    migrate_05_word_progress.py
    migrate_06_texts.py
    migrate_07_text_tags.py
)

for script in "${scripts[@]}"; do
    echo ">>> $script"
    python3 "$DIR/$script"
done

echo ""
echo "Phase 1 migration complete. Run verification queries from docs/tasks/019_migration.md."
