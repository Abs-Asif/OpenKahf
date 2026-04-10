# OpenKahf Android App Project Plan

## Phase 1: Project Setup
- [ ] Initialize Android project with Kotlin and Jetpack Compose.
- [ ] Set package name to `com.open.kahf`.
- [ ] Configure `AndroidManifest.xml` with necessary permissions (Internet, Accessibility Service).
- [ ] Setup GitHub Actions workflow for auto-building debug APK and creating a release.

## Phase 2: Core Functionality - DNS & Status
- [ ] Implement UI to guide users to set Private DNS to `dns-dot.dnsforfamily.com`.
- [ ] Implement a check to see if the device is currently using DNS for Family by querying `https://check.dnsforfamily.com/`.
- [ ] Display the DNS status on the home screen.

## Phase 3: Accessibility Service & Prevention Features
- [ ] Create `OpenKahfAccessibilityService` to monitor system settings and package installs/uninstalls.
- [ ] Implement "Prevent Change" logic: Detect if the user is in the Private DNS settings page and "kick them out" (e.g., go back or home) if enabled.
- [ ] Implement "Prevent Uninstall" logic: Detect if the user is trying to uninstall OpenKahf and prevent it if enabled.
- [ ] Implement the 5-minute delay mechanism for disabling these settings.

## Phase 4: UI Development
- [ ] Design Home Screen with:
    - DNS Status.
    - Guidance to set Private DNS.
    - Toggle for "Prevent Change" (Highly recommended, requires Accessibility).
    - Toggle for "Prevent Uninstall" (Requires Accessibility).
    - Blocklist search documentation (API details).
- [ ] Implement the countdown timer for disabling "Prevent" settings.

## Phase 5: Testing & Verification
- [ ] Verify Accessibility Service works as intended.
- [ ] Verify DNS status check works.
- [ ] Verify GitHub Action builds the APK correctly.

## Phase 6: Finalization
- [ ] Complete pre-commit steps.
- [ ] Submit the project.
