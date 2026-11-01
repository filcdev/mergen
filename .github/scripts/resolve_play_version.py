#!/usr/bin/env python3
"""Choose a fresh Android version before building a new Google Play release.

Requires google-auth and requests in CI. Pure version helpers can be tested with
stdlib unittest without network access or store credentials.
"""

import json
import os
import re
from pathlib import Path

VERSION_RE = re.compile(r"^v?(\d{1,3})\.(\d{1,3})\.(\d{1,3})$")
PLAY_MAX_VERSION_CODE = 2_100_000_000
VERSION_COMPONENT_LIMIT = 999


def tag_version_code(tag: str) -> int | None:
    match = VERSION_RE.fullmatch(tag)
    if match is None:
        return None
    major, minor, patch = (int(part) for part in match.groups())
    return major * 1_000_000 + minor * 1_000 + patch


def get_track_version_codes(tracks: list[dict]) -> list[int]:
    codes = []
    for track in tracks:
        for release in track.get("releases", []):
            for code in release.get("versionCodes", []):
                value = int(code)
                if not (0 < value <= PLAY_MAX_VERSION_CODE):
                    raise ValueError(f"Invalid Play versionCode: {code}")
                codes.append(value)
    return codes


def next_version(store_codes: list[int], tags: list[str]) -> tuple[int, str]:
    tagged_codes = [value for tag in tags if (value := tag_version_code(tag)) is not None]
    highest = max([0, *store_codes, *tagged_codes])
    version_code = highest + 1
    if version_code > PLAY_MAX_VERSION_CODE:
        raise ValueError("Google Play versionCode limit (2100000000) exceeded")
    major, remainder = divmod(version_code, 1_000_000)
    minor, patch = divmod(remainder, 1_000)
    if max(major, minor, patch) > VERSION_COMPONENT_LIMIT:
        raise ValueError("Version exceeds the project's MAJOR.MINOR.PATCH limit (999 each)")
    return version_code, f"{major}.{minor}.{patch}"


def fetch_github_tags(session, repository: str) -> list[str]:
    tags = []
    token = os.environ["GH_TOKEN"]
    headers = {
        "Authorization": f"Bearer {token}",
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
    }
    # GitHub exposes 100 results per page; stop only after an incomplete page.
    for page in range(1, 51):
        url = f"https://api.github.com/repos/{repository}/tags"
        response = session.get(url, headers=headers, params={"per_page": 100, "page": page}, timeout=30)
        response.raise_for_status()
        results = response.json()
        tags.extend(item["name"] for item in results)
        if len(results) < 100:
            return tags
    raise ValueError("Too many Git tags; refusing to guess the latest version")


def fetch_play_tracks(session, package_name: str) -> list[dict]:
    from google.auth.transport.requests import Request
    from google.oauth2.service_account import Credentials

    service_account = os.environ.get("PLAY_SERVICE_ACCOUNT_JSON", "")
    if not service_account.strip():
        raise ValueError("Missing PLAY_SERVICE_ACCOUNT_JSON GitHub Actions secret")
    credentials = Credentials.from_service_account_info(
        json.loads(service_account),
        scopes=["https://www.googleapis.com/auth/androidpublisher"],
    )
    credentials.refresh(Request())
    headers = {"Authorization": f"Bearer {credentials.token}"}
    base = f"https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{package_name}/edits"
    response = session.post(base, headers=headers, json={}, timeout=30)
    response.raise_for_status()
    edit_id = response.json()["id"]
    try:
        response = session.get(f"{base}/{edit_id}/tracks", headers=headers, timeout=30)
        response.raise_for_status()
        return response.json().get("tracks", [])
    finally:
        # Do not commit the read-only edit: the upload action creates its own.
        session.delete(f"{base}/{edit_id}", headers=headers, timeout=30).raise_for_status()


def main() -> None:
    import requests

    repo = os.environ["GITHUB_REPOSITORY"]
    package = os.environ["PLAY_PACKAGE_NAME"]
    with requests.Session() as session:
        tags = fetch_github_tags(session, repo)
        tracks = fetch_play_tracks(session, package)
    store_codes = get_track_version_codes(tracks)
    code, name = next_version(store_codes, tags)
    print(f"Google Play highest currently tracked code: {max(store_codes, default=0)}")
    print(f"Git latest semantic tag code: {max((tag_version_code(t) or 0 for t in tags), default=0)}")
    print(f"Building fresh version {name} (versionCode {code})")
    output_env = Path(os.environ["GITHUB_ENV"])
    with output_env.open("a", encoding="utf-8") as out:
        out.write(f"VERSION={name}\nVERSION_CODE={code}\n")
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with Path(summary).open("a", encoding="utf-8") as out:
            out.write(f"## Android closed-testing release\n\nNew AAB: **{name}** (versionCode **{code}**)\n")


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        raise SystemExit(f"::error::Unable to resolve a safe Android version: {error}") from error
