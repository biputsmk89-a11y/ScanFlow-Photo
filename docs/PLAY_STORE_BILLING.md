# Google Play Billing Setup Guide for ScanFlow Photo

This guide provides step-by-step instructions for registering and activating the Pro features in the **Google Play Console**.

---

## 1. Product IDs & Configuration Summary

The ScanFlow Photo application is wired with the following production product IDs defined in `com.scanflow.photocompressor.domain.billing.BillingConstants`:

| Product Type | Product ID / SKU | Suggested Price | Description |
|---|---|---|---|
| **In-App Product (One-Time)** | `scanflow_pro_lifetime` | $9.99 / Rp 149.000 | Lifetime access to all Pro features |
| **Subscription (Monthly)** | `scanflow_pro_monthly` | $1.99 / Rp 29.000 / month | Recurring monthly Pro access |

---

## 2. Google Play Console Configuration Steps

### Step A: Configure One-Time In-App Product
1. Open [Google Play Console](https://play.google.com/console).
2. Select **ScanFlow Photo**.
3. In the left navigation menu, go to **Monetize > Products > In-app products**.
4. Click **Create product**.
5. Fill in the fields:
   - **Product ID:** `scanflow_pro_lifetime`
   - **Name:** `ScanFlow Pro Lifetime`
   - **Description:** `Unlock unlimited batch compression, target file size tuning, passport studio, and remove all ads forever.`
6. Set the price according to your target markets.
7. Click **Save** and then **Activate**.

---

### Step B: Configure Recurring Subscription
1. In the left navigation menu, go to **Monetize > Products > Subscriptions**.
2. Click **Create subscription**.
3. Fill in the fields:
   - **Subscription ID:** `scanflow_pro_monthly`
   - **Name:** `ScanFlow Pro Monthly`
   - **Description:** `Monthly access to unlimited batching, target size compression, and passport photo tools.`
4. Under **Base plans**, click **Add base plan**:
   - **Base plan ID:** `monthly-plan`
   - **Billing period:** `Monthly`
   - **Renewal type:** `Auto-renewing`
   - Set the monthly price.
5. Under **Offers** (Optional): Configure any free trial (e.g., 7 days) if desired.
6. Click **Save** and **Activate**.

---

### Step C: Add License Testers (Free Testing)
To test the Google Play Billing flow without being charged:
1. In Google Play Console, go to **Setup > License testing**.
2. Add your Gmail test accounts in the **License testers** list.
3. Set **License test response** to `RESPOND_NORMALLY`.
4. Testers will see `Test Purchase (Always Approves)` on real devices.

---

## 3. Server-Side Purchase Verification (Production Architecture)
ScanFlow Photo includes an extensible verification interface `ServerVerificationProvider` located at:
`app/src/main/java/com/scanflow/photocompressor/domain/billing/BillingManager.kt`

When integrating with a remote backend or Google Play Developer API:
1. Provide an implementation of `ServerVerificationProvider` in your DI module (`AppModule.kt`).
2. Verify the `purchaseToken` against the official Google Play Developer API endpoint:
   `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{packageName}/purchases/subscriptions/{subscriptionId}/tokens/{token}`
3. The app architecture automatically validates and activates `UserEntitlement` without storing any private keys or credentials on the mobile client.
