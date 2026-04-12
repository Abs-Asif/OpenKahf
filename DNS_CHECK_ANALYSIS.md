# Comprehensive Analysis of DNS Status Verification Methods

This document analyzes various methods for verifying if a device is using a specific Private DNS provider (e.g., DNS for Family / Kahfguard).

## 1. API-based Verification (HTTPS)
This method involves making an HTTPS request to a known endpoint that returns information about the client's DNS resolver.

*   **Mechanism**: The app calls `https://verify.dnsforfamily.com/`. The server identifies the incoming request's DNS resolver and returns a JSON response like `{"success": true}`.
*   **Pros**:
    *   Highly reliable as it tests the actual path of the network traffic.
    *   Works across all Android versions that support HTTPS.
    *   Independent of device settings or manufacturer-specific UI.
*   **Cons**:
    *   Requires active internet connectivity.
    *   May be affected by VPNs or other proxy settings that intercept traffic.
    *   Slightly slower than system API calls due to network latency.

## 2. System LinkProperties Inspection
Android (9.0+) provides APIs to inspect the current network's properties, including Private DNS settings.

*   **Mechanism**: Use `ConnectivityManager.getLinkProperties()` and check the `privateDnsServerName` property.
*   **Pros**:
    *   Instantaneous (no network request needed).
    *   Can detect the configured DNS even if the network is currently down.
*   **Cons**:
    *   Only works on Android 9 (API 28) and above.
    *   Returns `null` if the user is using "Automatic" mode or "Off". It only identifies "Private DNS provider hostname" mode.
    *   Doesn't guarantee that the DNS is *actually* blocking content (e.g., if the resolver is reachable but not filtering).

## 3. DNS Probing (A/AAAA/TXT Records)
Directly performing DNS lookups for specific records that only exist (or are specifically modified) on the target DNS.

*   **Mechanism**: Perform a `java.net.InetAddress.getAllByName("whoami.dnsforfamily.com")` call and check for a specific IP or TXT record.
*   **Pros**:
    *   Low overhead.
    *   Specifically tests DNS resolution without full HTTP handshakes.
*   **Cons**:
    *   Harder to implement in pure Java/Kotlin on Android without 3rd party libraries (system DNS resolver usually abstracts these details).
    *   Can be cached by the OS, leading to stale results.

## Conclusion and Implementation Strategy
The most robust approach is a **Hybrid Model**:
1.  **Primary**: Use **API-based verification** for the most accurate "real-world" status.
2.  **Secondary**: Use **LinkProperties** as a fallback or to provide instant UI feedback when the user manually configures the hostname.
3.  **Active Monitoring**: Use a `ConnectivityManager.NetworkCallback` to trigger re-checks whenever the network state changes.

OpenKahf implements this hybrid strategy to ensure the "Active" status is both accurate and responsive.
