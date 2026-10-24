# Local release policy

- User preference: keep only the current complete installable mod set in `dist/latest/`.
- Publish by running `python3 tools/update_latest.py` after the necessary build/checks. Replace the same files; do not create revision folders, release ZIPs or historical production JAR backups.
- Temporary production JAR snapshots may exist while a test runs. Remove them when it finishes. Keep reports, original input hashes, logs, screenshots, source evidence, worlds and NBT files.
- Historical PASS records keep their original scope and hashes. Deleting old binaries does not make them evidence for newer bytes and does not require replaying unchanged checks.
- Do not edit or rebuild the separate Create port in `../26_3_porting` for this project; use its existing JAR.
