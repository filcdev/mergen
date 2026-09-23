<!--
TEMPLATE — replace every <PLACEHOLDER> before publishing, and have the school's
data protection officer review it. Then host it at a public URL and paste that
URL into Google Play Console (App content → Privacy policy) and App Store Connect.
-->

# Privacy Policy — Mergen (Filcapp)

**Effective date:** `<EFFECTIVE DATE>`

**Data controller:** `<PUBLISHER NAME>`, `<POSTAL ADDRESS>`

**Contact:** `<CONTACT EMAIL>`

---

## 1. About this policy

This policy explains how Mergen ("the App", package `hu.petrik.filcapp`) handles personal data. The App is the mobile client of the Filc school system, operated by `<PUBLISHER NAME>` ("we"). It is intended for students and teachers of the school.

## 2. In short

- We process only the data needed to sign you in and show your school timetable.
- Sign-in uses your **school Microsoft account** (Microsoft Entra ID).
- Your credentials are stored **encrypted on your device only**.
- The App uses **no** advertising, analytics, or crash-reporting services.
- Timetable and school news are fetched from the school's servers over an authenticated connection.
- You can ask for access to, or deletion of, your data at `<CONTACT EMAIL>`.

## 3. What data we process

### 3.1 Account and identity (from Microsoft Entra ID sign-in)

When you sign in with your school Microsoft account we receive (`openid profile email offline_access`):

- your name / display name,
- email address,
- nickname,
- account identifier, roles and permissions,
- class (cohort) identifier.

**Purpose:** to authenticate you and personalise the App.

### 3.2 Credentials stored on your device

To keep you signed in, the App stores the following **encrypted on your device**:

- the Microsoft Entra ID token and refresh token,
- the session cookie for the school backend (`filc.session_token`),
- temporary sign-in data (PKCE verifier, state, nonce).

On Android these are encrypted with a device-bound key in the Android Keystore; on iOS they are stored in the Keychain. They are not readable from device backups.

### 3.3 Profile settings

You can change your nickname, your selected class/group, and the notification language. These are stored on the school's backend.

### 3.4 Content shown in the App

Timetable, substitutions, moved lessons, classrooms, teacher names, announcements and school news. This data relates to your school account; it is fetched over an authenticated connection and shown in the App.

### 3.5 Technical data

As with any online service, the school's servers process technical data (for example IP address and request time) to deliver content and keep the service secure.

We do **not** collect location, contacts, camera, photos, files, or advertising identifiers. The Android app requests only the **Internet** permission; the iOS app requests no special permissions.

## 4. Purposes and legal bases (GDPR Article 6)

- **Providing the App and your timetable** — performance of the school's task, or our legitimate interest in running the school service.
- **Authenticating you and keeping your session secure** — necessary to provide the service.
- **Showing school news** — legitimate interest.

We do not use your data for advertising or profiling.

## 5. Who we share data with

- **Microsoft** (Microsoft Entra ID, `login.microsoftonline.com`) — the identity provider used for sign-in.
- **The school's backend ("Chronos", `filc.petrik.hu`)** — stores your account and profile settings and serves the content shown in the App.
- **`petrik.hu`** (school website) — an anonymous request for public news; no account data is sent.

There are no analytics, advertising, or data-broker services. We do not sell your personal data.

## 6. International transfers

Sign-in is handled by Microsoft, which may process data outside the EEA under appropriate safeguards (for example the EU Standard Contractual Clauses).

## 7. Retention and deletion

- Credentials remain on your device until you sign out; signing out removes them.
- Uninstalling the App removes all locally stored data.
- Data stored on the school backend (account and profile settings) is retained by the school according to its rules. To request deletion, contact `<CONTACT EMAIL>`.

## 8. Security

Local credentials are encrypted with device-bound keys. All communication uses HTTPS/TLS. Access to backend data requires an authenticated school account.

## 9. Children's privacy

The App is a school tool and may be used by students under 18. Data is processed as part of the school's educational service under the school's authority; no advertising is shown and no behavioural profiling is performed. If you believe a child's data is being processed improperly, contact `<CONTACT EMAIL>`.

## 10. Your rights

Under the GDPR you may request access to, rectification of, erasure of, or restriction of your data, request data portability, and object to processing. You may also lodge a complaint with the supervisory authority (`<SUPERVISORY AUTHORITY>`, for Hungary: the National Authority for Data Protection and Freedom of Information, NAIH, https://naih.hu). To exercise your rights, contact `<CONTACT EMAIL>`.

## 11. Changes to this policy

We may update this policy. The effective date will change and, for significant changes, we will notify you in the App or by email.

## 12. Contact

`<PUBLISHER NAME>`, `<POSTAL ADDRESS>` — `<CONTACT EMAIL>`
