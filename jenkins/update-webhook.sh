#!/usr/bin/env sh
# Points the GitHub webhook at the current Cloudflare tunnel address.
# Needs the GitHub CLI (gh) logged in to an account that owns the repository.
set -eu
REPO=Paradox-73/Calculator_REST_API
cd "$(dirname "$0")"

URL=$(docker compose logs tunnel 2>&1 | grep -o 'https://[a-z0-9-]*\.trycloudflare\.com' | tail -1)
[ -n "$URL" ] || { echo "No tunnel address yet. Wait a few seconds and run again." >&2; exit 1; }
HOOK="$URL/github-webhook/"

ID=$(gh api "repos/$REPO/hooks" --jq '.[] | select(.config.url | contains("github-webhook")) | .id' | head -1)
if [ -n "$ID" ]; then
    gh api -X PATCH "repos/$REPO/hooks/$ID" -f "config[url]=$HOOK" -f "config[content_type]=json" >/dev/null
else
    gh api -X POST "repos/$REPO/hooks" -f name=web -F active=true -f "events[]=push" \
        -f "config[url]=$HOOK" -f "config[content_type]=json" >/dev/null
fi
echo "GitHub webhook now points at $HOOK"
