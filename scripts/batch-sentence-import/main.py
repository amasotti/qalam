#!/usr/bin/env python3
"""Import sentences from a JSON file into a qalam text."""

import argparse
import json
import sys
import urllib.request
import urllib.error

BASE_URL = "http://localhost:8085/api/v1"


def post_sentence(text_id: str, sentence: dict) -> dict:
    url = f"{BASE_URL}/texts/{text_id}/sentences"
    payload = json.dumps(sentence).encode()
    req = urllib.request.Request(
        url,
        data=payload,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())


def main() -> None:
    parser = argparse.ArgumentParser(description="Import sentences into a qalam text")
    parser.add_argument("text_id", help="UUID of the target text")
    parser.add_argument(
        "--file",
        default="text.json",
        help="Path to JSON file (default: text.json)",
    )
    args = parser.parse_args()

    with open(args.file, encoding="utf-8") as f:
        sentences = json.load(f)

    print(f"Importing {len(sentences)} sentences into text {args.text_id}")

    for i, sentence in enumerate(sentences):
        try:
            result = post_sentence(args.text_id, sentence)
            print(f"[{i + 1}/{len(sentences)}] OK — id={result['id']} pos={result['position']}")
        except urllib.error.HTTPError as e:
            body = e.read().decode()
            print(f"[{i + 1}/{len(sentences)}] ERROR {e.code}: {body}", file=sys.stderr)
            sys.exit(1)


if __name__ == "__main__":
    main()
