# Novaris Pokédex v0.5.6 — Remote data updates

Source: https://github.com/novarisartifex/Novaris-Pokedex-Data

## Required integration

1. Fetch the published JSON manifest via HTTPS on a background thread.
2. Require schema_version 1, status published, and min_app_version_code <= installed app version code.
3. Compare data_version with the installed dataset; only offer newer versions.
4. Download the declared release asset to app-private staging storage, with a size limit.
5. Verify byte length and SHA-256 against the manifest before processing.
6. Validate the compressed database format and SQLite integrity before activation.
7. Preserve the previous working database; switch only after validation and restore it on failure.
8. Never perform network or database replacement on the UI thread.
9. Build and test a signed v0.5.6 APK, retaining the existing signing certificate.

The current v0.5.5 workflow reconstructs native Android source from a private archive; this document is not yet wired into the APK. No updater is claimed to be functional until integration and tests pass.
