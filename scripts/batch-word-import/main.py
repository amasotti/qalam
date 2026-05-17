#!/usr/bin/env python3
"""Import vocabulary words from a CSV file into qalam."""

import argparse
import csv
import json
import sys
import urllib.request
import urllib.error

BASE_URL = "http://localhost:8085/api/v1"

VALID_POS = {
    "UNKNOWN", "NOUN", "VERB", "ADJECTIVE", "ADVERB",
    "PREPOSITION", "PARTICLE", "INTERJECTION", "CONJUNCTION", "PRONOUN",
}
VALID_DIALECTS = {"TUNISIAN", "MOROCCAN", "EGYPTIAN", "GULF", "LEVANTINE", "MSA", "IRAQI"}
VALID_DIFFICULTIES = {"BEGINNER", "INTERMEDIATE", "ADVANCED"}

OPTIONAL_FIELDS = {
    "transliteration": str,
    "translation": str,
    "partOfSpeech": str,
    "dialect": str,
    "difficulty": str,
    "notes": str,
}


def word_exists(arabic_text: str) -> bool:
    encoded = urllib.parse.quote(arabic_text)
    url = f"{BASE_URL}/words/by-arabic?q={encoded}"
    req = urllib.request.Request(url)
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status == 200
    except urllib.error.HTTPError as e:
        if e.code == 404:
            return False
        raise


def post_word(payload: dict) -> dict:
    url = f"{BASE_URL}/words"
    data = json.dumps(payload).encode()
    req = urllib.request.Request(
        url,
        data=data,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())


def build_payload(row: dict) -> dict:
    arabic = row.get("arabicText", "").strip()
    if not arabic:
        raise ValueError("arabicText is empty")

    payload: dict = {"arabicText": arabic}

    for field in OPTIONAL_FIELDS:
        val = row.get(field, "").strip()
        if not val:
            continue
        if field == "partOfSpeech" and val not in VALID_POS:
            raise ValueError(f"Invalid partOfSpeech '{val}'. Valid: {VALID_POS}")
        if field == "dialect" and val not in VALID_DIALECTS:
            raise ValueError(f"Invalid dialect '{val}'. Valid: {VALID_DIALECTS}")
        if field == "difficulty" and val not in VALID_DIFFICULTIES:
            raise ValueError(f"Invalid difficulty '{val}'. Valid: {VALID_DIFFICULTIES}")
        payload[field] = val

    return payload


def main() -> None:
    import urllib.parse  # noqa: PLC0415 — late import to keep top clean

    parser = argparse.ArgumentParser(description="Import vocabulary words into qalam")
    parser.add_argument("--file", default="words.csv", help="Path to CSV file (default: words.csv)")
    parser.add_argument(
        "--skip-duplicates",
        action="store_true",
        default=True,
        help="Skip words that already exist (checked by arabicText). Default: true.",
    )
    parser.add_argument(
        "--no-skip-duplicates",
        dest="skip_duplicates",
        action="store_false",
        help="Import even if arabicText already exists (will get 422 from API).",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Parse and validate CSV but do not send any requests.",
    )
    args = parser.parse_args()

    with open(args.file, encoding="utf-8") as f:
        reader = csv.DictReader(f)
        rows = list(reader)

    print(f"Read {len(rows)} rows from {args.file}")
    if args.dry_run:
        print("[dry-run] Validating rows only — no API calls.")

    ok = skipped = errors = 0

    for i, row in enumerate(rows, start=1):
        prefix = f"[{i}/{len(rows)}]"
        try:
            payload = build_payload(row)
        except ValueError as e:
            print(f"{prefix} SKIP (invalid): {e}", file=sys.stderr)
            errors += 1
            continue

        arabic = payload["arabicText"]

        if args.dry_run:
            print(f"{prefix} OK (dry-run) — {arabic}")
            ok += 1
            continue

        if args.skip_duplicates:
            try:
                exists = word_exists(arabic)
            except Exception as e:
                print(f"{prefix} ERROR checking existence of '{arabic}': {e}", file=sys.stderr)
                errors += 1
                continue
            if exists:
                print(f"{prefix} SKIP (exists) — {arabic}")
                skipped += 1
                continue

        try:
            result = post_word(payload)
            print(f"{prefix} OK — id={result['id']} {arabic} / {payload.get('translation', '')}")
            ok += 1
        except urllib.error.HTTPError as e:
            body = e.read().decode()
            print(f"{prefix} ERROR {e.code} for '{arabic}': {body}", file=sys.stderr)
            errors += 1

    print(f"\nDone: {ok} imported, {skipped} skipped, {errors} errors")
    if errors:
        sys.exit(1)


if __name__ == "__main__":
    main()
