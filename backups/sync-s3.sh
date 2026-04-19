#!/usr/bin/env bash
# Sync local backups/ to s3://am-qalam/db-backups/
# Usage: bash backups/sync-s3.sh [LOCAL_DIR] [PROFILE] [--delete]
set -euo pipefail

usage() {
    echo "Usage: $0 [LOCAL_DIR] [PROFILE] [--delete]"
    echo
    echo "  LOCAL_DIR   Local backup directory (default: backups/)"
    echo "  PROFILE     AWS CLI profile        (default: default)"
    echo "  --delete    Remove S3 files absent from LOCAL_DIR"
    exit 1
}

LOCAL_DIR="$(cd "$(dirname "$0")" && pwd)"
PROFILE="default"
DELETE_OPTION=""
LOCAL_DIR_SET=""
PROFILE_SET=""

for arg in "$@"; do
    case "$arg" in
        --delete)  DELETE_OPTION="--delete" ;;
        --help)    usage ;;
        *)
            if [[ -z "$LOCAL_DIR_SET" ]]; then
                LOCAL_DIR="$arg"; LOCAL_DIR_SET=1
            elif [[ -z "$PROFILE_SET" ]]; then
                PROFILE="$arg"; PROFILE_SET=1
            else
                echo "Error: too many arguments"; usage
            fi
            ;;
    esac
done

S3_BUCKET="s3://am-qalam/db-backups"

if [[ ! -d "$LOCAL_DIR" ]]; then
    echo "Error: '$LOCAL_DIR' does not exist."
    exit 1
fi

[[ -n "$DELETE_OPTION" ]] && echo "Delete flag set: remote files absent locally will be removed."

echo "Syncing '$LOCAL_DIR' → '$S3_BUCKET' (profile: $PROFILE)..."
aws s3 sync "$LOCAL_DIR" "$S3_BUCKET" $DELETE_OPTION \
    --exclude "*.sh" \
    --profile "$PROFILE"
echo "Sync complete."
