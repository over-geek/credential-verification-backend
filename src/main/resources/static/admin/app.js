const bridgeUrl = "http://localhost:9000/read-chip";

const form = document.querySelector("#credentialForm");
const rows = document.querySelector("#credentialRows");
const chipUidInput = document.querySelector("#chipUid");
const readChipButton = document.querySelector("#readChipButton");
const resetCardButton = document.querySelector("#resetCardButton");
const saveButton = document.querySelector("#saveButton");
const resetButton = document.querySelector("#resetButton");
const refreshButton = document.querySelector("#refreshButton");
const formStatus = document.querySelector("#formStatus");
const bridgeStatus = document.querySelector("#bridgeStatus");

readChipButton.addEventListener("click", readChip);
resetCardButton.addEventListener("click", resetCard);
refreshButton.addEventListener("click", loadCredentials);
resetButton.addEventListener("click", resetForm);
form.addEventListener("submit", saveCredential);

loadCredentials();

async function readChip() {
    setBridgeStatus("Reading chip...", "");
    readChipButton.disabled = true;

    try {
        const response = await fetch(bridgeUrl);
        const payload = await response.json();

        if (!response.ok) {
            throw new Error(payload.error || "Unable to read chip.");
        }

        chipUidInput.value = payload.chip_uid;
        setBridgeStatus("Chip ready", "ok");
        setFormStatus("Chip UID captured.", "ok");
    } catch (error) {
        setBridgeStatus("Bridge error", "error");
        setFormStatus(error.message, "error");
    } finally {
        readChipButton.disabled = false;
    }
}

async function resetCard() {
    setBridgeStatus("Resetting chip...", "");
    resetCardButton.disabled = true;

    try {
        const payload = await sendJson("http://localhost:9000/reset-chip", "POST", {
            chip_uid: chipUidInput.value || ""
        });
        setBridgeStatus("Chip reset successfully", "ok");
        setFormStatus("Card is blank.", "ok");
    } catch (error) {
        setBridgeStatus("Bridge error", "error");
        setFormStatus(error.message, "error");
    } finally {
        resetCardButton.disabled = false;
    }
}

async function saveCredential(event) {
    event.preventDefault();
    setFormStatus("Saving...", "");
    saveButton.disabled = true;

    const data = new FormData(form);
    const chipUid = data.get("chip_uid");

    try {
        let saved = await sendFormData("/credentials", data);

        if (saved.offline_payload && chipUid) {
            setFormStatus("Writing to chip...", "");
            await sendJson("http://localhost:9000/write-chip", "POST", {
                chip_uid: chipUid,
                payload: saved.offline_payload
            });
            setBridgeStatus("Chip written", "ok");
        }

        setFormStatus(`Saved ${saved.first_name} ${saved.last_name}.`, "ok");
        form.reset();
        await loadCredentials();
    } catch (error) {
        setFormStatus(error.message, "error");
    } finally {
        saveButton.disabled = false;
    }
}

async function loadCredentials() {
    rows.innerHTML = `<tr><td colspan="7" class="empty">Loading...</td></tr>`;

    try {
        const response = await fetch("/credentials");
        const credentials = await response.json();

        if (!response.ok) {
            throw new Error(credentials.message || "Unable to load credentials.");
        }

        renderRows(credentials);
    } catch (error) {
        rows.innerHTML = `<tr><td colspan="7" class="empty">${escapeHtml(error.message)}</td></tr>`;
    }
}

function renderRows(credentials) {
    if (!credentials.length) {
        rows.innerHTML = `<tr><td colspan="7" class="empty">No credentials yet.</td></tr>`;
        return;
    }

    rows.innerHTML = credentials.map((credential) => `
        <tr>
            <td>
                <strong>${escapeHtml(credential.first_name)} ${escapeHtml(credential.last_name)}</strong>
                <div class="mono">${escapeHtml(credential.id)}</div>
            </td>
            <td>${escapeHtml(credential.course)}</td>
            <td>${escapeHtml(credential.university)}</td>
            <td>${escapeHtml(credential.duration)}</td>
            <td>${escapeHtml(credential.class)}</td>
            <td class="mono">${escapeHtml(credential.chip_uid || "Not linked")}</td>
            <td>
                <a class="secondary pdf-button" href="/credentials/${encodeURIComponent(credential.id)}/certificate.pdf">
                    Download
                </a>
            </td>
        </tr>
    `).join("");
}

async function sendJson(url, method, body) {
    const response = await fetch(url, {
        method,
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(body)
    });
    const payload = await response.json();

    if (!response.ok) {
        throw new Error(payload.message || payload.error || "Request failed.");
    }

    return payload;
}

async function sendFormData(url, formData) {
    const response = await fetch(url, {
        method: "POST",
        body: formData
    });
    const payload = await response.json();

    if (!response.ok) {
        throw new Error(payload.message || payload.error || "Request failed.");
    }

    return payload;
}

function resetForm() {
    form.reset();
    setFormStatus("", "");
    setBridgeStatus("Bridge idle", "");
}

function setFormStatus(message, type) {
    formStatus.textContent = message;
    formStatus.className = `status ${type}`.trim();
}

function setBridgeStatus(message, type) {
    bridgeStatus.textContent = message;
    bridgeStatus.className = `bridge-status ${type}`.trim();
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#039;");
}
