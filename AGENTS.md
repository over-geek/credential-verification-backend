# AGENTS.md — Academic Credential Verification POC

This file instructs an agentic coding AI on how to build this project. Follow it as the
source of truth for scope, sequencing, and boundaries. If a request conflicts with this
file, flag the conflict instead of silently deviating.

---

## 1. Role and Objectives

You are acting as the implementing engineer for a **proof-of-concept** academic
credential verification system. Your objective is to produce working, demoable code
across four components:

1. **Backend API** — hosted service, source of truth for credential records.
2. **Local reader bridge agent** — small local Java app that reads an NFC/contactless
   chip UID via a USB PC/SC reader and exposes it over localhost.
3. **Admin web app** — used by a single admin to create credential records, trigger a
   chip read, generate a QR code, and download a printable PDF certificate.
4. **Mobile app** — used by a verifier to scan the QR code or tap the NFC chip and
   fetch/display the matching credential.

This is explicitly a **POC**. Optimize for a working end-to-end demo over production
concerns. Do not add authentication, encryption, rate limiting, or other security
hardening unless explicitly asked — see Guard Rails.

---

## 2. Context

### 2.1 Concept
A physical certificate carries two independent identifiers of the same underlying
credential record:
- A **printed QR code** encoding the record's UUID (or a verify URL containing it).
- An **embedded NFC chip** serving as an offline data carrier. It stores an encrypted
  copy of the credential data, cryptographically bound to its factory UID via a digital
  signature (to prevent cloning).

An admin creates the record first (via a web app). During enrollment, the system generates
a signed, encrypted payload and writes it to the physical chip. It then generates and
downloads a QR-embedded PDF certificate for printing. A mobile app later resolves either
the QR code (online) or the NFC tap (offline data + online photo fetch) back to the
credential record.

### 2.2 Data model (single table, POC — no normalization)
Table: `credentials`

| Column      | Type                          | Notes                                      |
|-------------|-------------------------------|---------------------------------------------|
| id          | UUID, PK                      | Generated on creation. Encoded in the QR.   |
| chip_uid    | string, unique, nullable       | Captured from reader at creation time.      |
| first_name  | string                         |                                              |
| last_name   | string                         |                                              |
| course      | string                         |                                              |
| university  | string                         |                                              |
| duration    | string (e.g. "2021 - 2024")    | Store as plain string for POC simplicity.   |
| class       | string (e.g. "First Class")    | Free text/enum, no separate lookup table.   |

Do not introduce separate `student`, `university`, or `course` entities. One table only.

### 2.3 Architecture constraint (important)
A browser **cannot** access a USB PC/SC smart-card reader directly. The local bridge
agent is a separate small Java process running on the admin's PC, exposing a localhost
HTTP endpoint (e.g. `http://localhost:9000/read-chip`) that the admin web app's
frontend calls directly from the browser (localhost calls from JS are permitted). This
agent is the only component that must run physically next to the reader. The hosted
backend and admin frontend do not need to be co-located with the reader.

### 2.4 Two independent lookup paths, one record (as implemented)
The QR code does **not** encode the credential's UUID directly. It encodes an opaque
token in the form `cv://verify/{qrToken}`, stored on the record as a separate
`qr_token` column. This means a generic QR scanner reveals only an opaque URI, not the
credential ID — the mobile app is the only medium that resolves it to a record. The
`id` (UUID) is still the primary key and is used for admin-side operations (chip
linking, PDF download), but is not exposed via the QR path.

- QR path: mobile scans QR → parses `cv://verify/{qrToken}` → extracts `qrToken` →
  `GET /credentials/by-qr/{qrToken}` (online verification).
- NFC path: mobile taps chip → authenticates with custom keys → reads raw data blocks →
  decrypts payload → verifies digital signature against chip UID → parses offline credential data.
  It then optionally fetches the photo online via `GET /credentials/{id}/photo`.

Both verification paths result in rendering the same credential data, but the NFC path
must work completely offline for text fields, while the QR path relies entirely on
the backend.

### 2.5 Tech stack assumptions (adjust only if asked)
- Backend: Java, Spring Boot, REST, Postgres or MySQL. **(built)**
- Bridge agent: Java, `javax.smartcardio` (PC/SC), minimal embedded HTTP server.
  **(built)**
- Admin web app: any standard frontend (React, or Thymeleaf/Spring MVC if staying
  all-Java) calling the hosted backend and the local bridge agent. **(built)**
- PDF generation: iText or Apache PDFBox, server-side. **(built)**
- QR generation: ZXing, encoding `cv://verify/{qrToken}`. **(built)**
- Mobile app: **Kotlin, native Android** (not Flutter/React Native). NFC is a
  first-class native Android API; cross-platform NFC plugins are the least reliable
  part of both Flutter's and RN's ecosystems, and reliable NFC tap behavior matters
  for this project's core demo. iOS remains out of scope for this POC.

---

## 3. Actions the Agent Should Take

Work in phases. Complete and confirm one phase before starting the next unless told
otherwise. Within each phase, prefer the simplest implementation that satisfies the
requirement.

**Phase 1 — Backend & database**
- Create the `credentials` table per the schema in 2.2.
- Implement: create credential, update credential with `chip_uid`, fetch by `id`,
  fetch by `chip_uid`, list all.
- Return consistent JSON shape across all fetch endpoints.

**Phase 2 — Local reader bridge agent (Update)**
- Standalone Java app, no dependency on the backend or frontend framework.
- Expose an endpoint that triggers a chip write: it takes the signed, encrypted payload
  from the admin app and writes it to the chip's raw sectors (MIFARE Classic/Plus)
  using custom access keys, returning success or failure.
- Expose a "reset chip" endpoint that authenticates with the custom keys, zeroes out the payload, and restores the sector keys to their factory default (`FF FF FF FF FF FF`) so test cards can be reused.

**Phase 3 — Admin web app (Update)**
- Form to create a credential (all fields from 2.2 except `id`, `chip_uid`).
- On save, call backend to create the record; backend generates a digitally signed and
  encrypted payload (binding the data to the chip UID).
- Admin app calls the bridge agent to write this payload to the chip.
- Add a "Reset Test Card" action button on the admin console that calls the bridge agent's reset endpoint to wipe a previously enrolled card.
- Dashboard listing existing records with a "Download PDF" action per record.

**Phase 4 — PDF generation**
- Server-side generation of a certificate-style PDF containing the record's fields
  and the QR code image.
- Downloadable from the admin app.

**Phase 5 — Mobile app (Kotlin, native Android)**

Stack: Kotlin, single-module Android app. CameraX + ML Kit (or ZXing's Android
integration) for QR scanning. Native `NfcAdapter` / foreground dispatch for NFC tag
reading. Retrofit (or Ktor client) for calling the backend. No login, no auth, no
security implementation of any kind — this app has exactly one purpose: scan or tap,
then display.

Screens (minimum viable set):
1. **Home / scan screen** — a single screen offering both verification modes at once:
   a camera preview for QR scanning, active by default, and NFC tap listening enabled
   simultaneously in the background (Android allows both to be live at once; whichever
   fires first drives the result). No mode toggle needed — the verifier just scans or
   taps, whichever is convenient for the physical certificate in hand.
2. **Result screen** — renders a `CredentialResponseDto` (first name, last name,
   course, university, duration, class). Identical layout regardless of whether the
   result came from the QR or NFC path — the screen should not know or care which path
   was used.
3. **Not-found state** — shown when the backend returns no matching record for a
   scanned/tapped token. Must be visually distinct from a network/connectivity error
   (e.g. "No credential found for this code" vs. "Couldn't reach the server").

QR handling:
- On scan, read the raw QR payload as a string.
- Parse it as `cv://verify/{qrToken}`. Extract `qrToken` (reject/ignore payloads that
  don't match this scheme — do not attempt to fall back to treating arbitrary scanned
  text as a token).
- Call `GET /credentials/by-qr/{qrToken}`.

NFC handling:
- Use Android's NFC foreground dispatch to detect a tapped tag while the scan screen
  is active.
- Authenticate with the chip using custom keys, read the raw data blocks, decrypt the
  payload, and verify the digital signature (Ed25519/ECDSA) against the chip's physical UID.
- If signature is valid, display the offline data immediately, and asynchronously fetch
  the photo online. Reject if signature or UID mismatch (anti-cloning).
- Do not rely on an online fetch for the core text data when tapping the chip.

Error handling:
- Distinguish three outcomes on both paths: successful match (show result screen),
  no match / 404 (show not-found state), and network/server error (show a retry-able
  error state). Do not collapse these into a single generic error message.

Permissions:
- Camera permission (for QR) and NFC (declared in manifest; no runtime permission
  prompt required for NFC on Android, but confirm NFC hardware is present on the
  device and show a clear message if it isn't, rather than failing silently).

**Phase 6 — End-to-end physical test**
- Confirm a real embedded chip + printed QR on the same physical card both resolve
  to the same record from the mobile app.

Before moving to a new phase, the agent should state which phase it completed and
what was verified, so the human can check progress.

---

## 4. Code Architecture & Modularity (Backend)

Follow the **`spring-boot-layered-architecture`** skill for backend package structure,
thin controllers, service/impl separation, and DTO usage. Apply it to the backend
component of this project (not the local bridge agent — see note below).

Project-specific notes on top of that skill:
- The primary entity is `Credential`, mapped from the `credentials` table.
- Example DTOs: `CredentialRequestDto` (fields the admin submits — no `id`/`chip_uid`,
  since those are server-generated/reader-populated), `CredentialResponseDto` (full
  record shape returned to both the admin app and the mobile app).
- Service logic should also orchestrate QR generation and PDF generation triggering
  where relevant (e.g. within `CredentialServiceImpl`), per the skill's guidance on
  keeping controllers thin and business logic in `service.impl`.
- Keep the local bridge agent's structure simple and separate from this package
  layout — it's a standalone utility, not part of the Spring Boot backend, and does
  not need the same layering.

---

## 5. Guard Rails

- **No security work unless explicitly requested.** No auth, no encryption, no HTTPS
  enforcement, no input sanitization beyond what's needed for basic correctness. This
  is intentional for POC scope — do not "helpfully" add it.
- **Do not introduce additional entities/tables.** One `credentials` table only, per
  2.2. Resist normalizing into `student` / `university` / `course` tables even if it
  seems cleaner.
- **Mandatory Offline NFC Verification.** The NFC chip must carry the credential payload.
  You must write this data to the chip during enrollment.
- **Cryptographic Binding (Anti-Cloning).** The payload written to the chip must be signed
  by the backend (e.g. Ed25519/ECDSA) incorporating the chip's factory UID, to prevent
  copying valid ciphertext to blank cards.
- **Data Obfuscation.** Generic NFC tools must not be able to read the plain text data.
  The data must be encrypted, and the chip sectors should ideally be locked with custom keys.
- **Do not attempt browser-to-PC/SC access.** Any design that tries to call
  `javax.smartcardio` or a PC/SC driver directly from browser JavaScript is invalid —
  route through the local bridge agent instead.
- **Keep the bridge agent single-purpose.** It should only read the chip and return
  the UID. It must not talk to the hosted backend or database itself.
- **Do not change the data model's field names or types** without flagging it first —
  PDF generation and all three lookup paths (`id`, `chip_uid`, `qr_token`) depend on
  these fields existing exactly as specified.
- **Keep all lookup paths returning identical response shape.** Divergence here will
  break the mobile app's single result screen.
- **Do not expose the credential `id` (UUID) via the QR path.** The QR code encodes
  `qr_token`, not `id` — this separation is intentional so a generic QR scanner can't
  reveal a usable identifier. The mobile app is the only medium expected to resolve
  the QR payload into a record.
- **Mobile app: no auth, no login, no security implementation.** Consistent with the
  rest of this POC — do not add any of this even if it seems like an obvious addition
  for a "verification" app.
- **Ask before scope expansion.** If a request implies multi-university support,
  role-based access, revocation workflows, or similar production features, flag it
  as out of scope for this POC rather than building it silently.
- **Prefer working over polished.** Skipping styling, edge-case handling, or code
  elegance is acceptable; skipping a functioning end-to-end path between phases is
  not.
