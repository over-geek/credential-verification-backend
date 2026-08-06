# Credential Verification API

This repository contains the backend service and admin web application for the Academic Credential Verification Proof-of-Concept. It serves as the central source of truth for credential records, orchestrating the generation of both online and offline verification tokens.

## Features

- **Credential Management**: RESTful endpoints to create, update, and manage academic credential records.
- **Offline Payload Generation**: Cryptographically secures credential data for offline NFC verification. It generates a payload encrypted with AES-GCM and digitally signed with Ed25519 (bound to the chip's physical UID) to prevent cloning and tampering.
- **Certificate Generation**: Dynamically generates downloadable PDF certificates containing the student's details and an embedded QR code for online verification.
- **Admin Dashboard**: A built-in web frontend that allows administrators to input credential details, interact with a local bridge agent to write to physical NFC smart cards, and manage records.
- **Unified Verification Endpoints**: Provides consistent API responses for resolving credentials via QR code tokens, physical NFC chip UIDs, or direct UUID lookups.

## Tech Stack

- **Framework**: Java Spring Boot
- **Database**: PostgreSQL mapping a unified `credentials` table
- **Cryptography**: Standard Java Cryptography Architecture (Ed25519, AES-GCM)
- **PDF Generation**: iText / Apache PDFBox
- **QR Generation**: ZXing
- **Frontend**: HTML/JS/CSS (served statically via Spring MVC)
