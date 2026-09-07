# Reliable Join Request Notifications Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add reliable Android system notifications for new shared-ledger join requests using Firebase Cloud Messaging (FCM), while preserving the existing approval flow and transaction sync.

**Architecture:** The Android app registers an FCM token and receives join-request notification payloads through a FirebaseMessagingService. A Firebase 2nd-gen Firestore trigger sends a notification to the ledger owner when a new join request is created; notification taps open the existing Shared Ledger approval screen.

**Tech Stack:** Kotlin, AndroidX, Firebase Cloud Messaging, Firebase Authentication/Firestore, Firebase Functions 2nd gen, Node.js/TypeScript, existing Jetpack Compose navigation.

**Spec:** Approved FCM architecture from the current AJ Udhar Book development conversation.

## Global Constraints

- Existing Firestore shared-ledger security rules must remain unchanged unless a minimal token-storage rule is strictly required.
- Existing real-time transaction sync must remain untouched.
- Only the shared-ledger owner receives join-request notifications.
- Notification data must contain `ledgerCode` and `requestId`; do not include unnecessary requester data.
- Duplicate notifications for the same request must be suppressed.
- Approved/rejected requests must not produce another pending notification.
- Android 13+ notification permission must be requested at runtime.
- Missing/invalid FCM tokens and transient server errors must fail safely.
- Never claim build/tests pass without fresh verification output.

---

### Task 1: Firebase Android configuration and FCM token registration

**Files:**
- Create: `app/google-services.json` (Firebase Android client config supplied by the user)
- Modify: Android module Gradle configuration in the actual Android project module
- Create: `FcmTokenManager.kt`
- Test: `FcmTokenManagerTest.kt`

**Interfaces:**
- Produces a token registration operation that writes only the signed-in user's token to `users/{uid}` with an update timestamp.

- [ ] Write failing unit tests for token update behavior and safe handling when no authenticated user exists.
- [ ] Implement minimal FCM token retrieval/update logic.
- [ ] Add Google Services and Firebase Messaging dependencies using the project's existing dependency-management conventions.
- [ ] Run focused tests.
- [ ] Commit the task.

### Task 2: Android notification service and permission/channel handling

**Files:**
- Create: `JoinRequestMessagingService.kt`
- Modify: `AndroidManifest.xml`
- Create/modify: notification helper and tests as needed

**Interfaces:**
- Consumes FCM data payload: `ledgerCode`, `requestId`.
- Produces an Android notification with title `🔔 New Join Request` and body `Someone wants to join your shared ledger.`

- [ ] Write failing tests for payload validation, notification identity, and duplicate suppression.
- [ ] Implement notification channel and service.
- [ ] Add `POST_NOTIFICATIONS` permission handling for Android 13+.
- [ ] Ensure duplicate request IDs do not create duplicate notifications.
- [ ] Run focused tests.
- [ ] Commit the task.

### Task 3: Notification tap deep-link into Shared Ledger

**Files:**
- Modify: `MainActivity.kt`
- Modify: `NavGraph.kt`
- Modify/create: deep-link helper and tests

**Interfaces:**
- Consumes notification extras/data: `ledgerCode`, `requestId`.
- Produces navigation to the existing Shared Ledger approval screen.

- [ ] Write failing tests for valid/invalid notification payloads.
- [ ] Implement intent handling without breaking normal app launch.
- [ ] Clear/consume the notification intent after handling to avoid repeated navigation.
- [ ] Run focused tests.
- [ ] Commit the task.

### Task 4: Firebase Cloud Function for new join requests

**Files:**
- Create: `functions/package.json`
- Create: `functions/tsconfig.json`
- Create: `functions/src/index.ts`
- Create: `functions/src/joinRequestNotification.ts`
- Test: `functions/src/joinRequestNotification.test.ts`
- Create/modify: `firebase.json` only if the project uses Firebase CLI configuration

**Interfaces:**
- Trigger: `sharedLedgers/{ledgerCode}/joinRequests/{requestId}` document creation.
- Reads ledger owner UID and owner FCM token from Firestore.
- Sends FCM data notification containing `ledgerCode` and `requestId`.

- [ ] Write failing tests for owner lookup, missing token, invalid request, and duplicate/event retry behavior.
- [ ] Implement 2nd-gen Firestore `onDocumentCreated` trigger.
- [ ] Send through Firebase Admin SDK from the trusted Functions environment.
- [ ] Handle missing/invalid tokens safely and avoid exposing tokens to clients.
- [ ] Run function tests.
- [ ] Commit the task.

### Task 5: End-to-end verification and documentation

**Files:**
- Modify: project documentation as needed

- [ ] Verify Android 13+ notification permission behavior.
- [ ] Verify owner-only delivery with a real Firebase project.
- [ ] Verify notification tap opens Shared Ledger approval screen.
- [ ] Verify duplicate suppression.
- [ ] Verify approve/reject flow remains unchanged.
- [ ] Verify existing transaction sync remains unchanged.
- [ ] Run all available unit tests and Android build checks.
- [ ] Record actual verification results; do not infer success.
- [ ] Commit final changes.
