# Security Policy

## Privacy & Threat Model

**Folio** is designed from the ground up as a **100% offline, single-user, local-first Android eBook reader**:

- **Zero Network Access**: `INTERNET` and `ACCESS_NETWORK_STATE` permissions are intentionally absent from `AndroidManifest.xml`. The app makes no telemetry, analytics, cloud-sync, or external HTTP requests.
- **No Accounts or Cloud Backup Leakage**: `android:allowBackup="false"` together with `backup_rules.xml` and `data_extraction_rules.xml` exclude local databases, imported books, covers, highlights, and preferences from Android Auto Backup and device-to-device transfers.
- **Untrusted Document & Archive Parsing Hardening**:
  - **EPUB & Backup ZIP Archives**: Canonical path confinement (`canonicalFile.path` verification) prevents Zip Slip / directory traversal writes; strict entry-count, per-entry, and total uncompressed byte caps prevent decompression bombs.
  - **Backup Restore Validation**: Cover filenames are stripped and sanitized before writing or resolving paths, and restored `localCopyPath` references are rejected unless canonically confined inside the app's private `books/` directory.
  - **DOCX & FB2 XML Parsing**: `XmlPullParserFactory` explicitly disables DOCTYPE processing and external general/parameter entities (`process-docdecl`, `external-general-entities`, `external-parameter-entities`), with bounded stream reads.
  - **Custom Font Imports**: Only `.ttf` / `.otf` files up to 32 MB matching valid SFNT / OpenType / TTC magic headers are accepted into private storage.
- **Zero-Permission Storage Access**: Uses Android Storage Access Framework (`OpenMultipleDocuments`, `OpenDocumentTree`, `CreateDocument`) and Photo Picker (`PickVisualMedia`) without requesting broad storage permissions.

## Supported Versions

| Version | Supported |
| --- | --- |
| `1.0` (`main`) | Yes |

## Reporting a Vulnerability

If you discover a security or privacy vulnerability in Folio (such as an archive traversal issue, parser denial-of-service, or unintended data exposure), please **do not open a public issue with exploit payloads** if it affects other users.

1. Use **GitHub Private Vulnerability Reporting** (`Security` tab -> `Report a vulnerability`) on this repository, or
2. Submit a minimal, non-destructive description requesting a private channel first.

## Maintainer Pre-Push Checklist

Before pushing commits to a public branch, verify that none of the following are tracked:

- Keystores or certificates (`*.jks`, `*.keystore`, `*.p12`, `debug.keystore*`)
- Environment secret files (`.env`, `.env.local`, `keystore.properties`, `google-services.json`)
- Personal screenshots or device test exports outside `Version/`
