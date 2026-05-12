# MSCMS Frontend Integration Guide — Wallet, Store, Auctions, Donations

This document is for the **frontend team**. It explains how to integrate the new Wallet, Store, Auction and Donation features into the MSCMS web/mobile app.

All endpoints below go through the **API Gateway** (default: `http://localhost:8080` in dev). Authenticate every request with the JWT you already use today.

> The Stripe SDK never touches your servers. The user pays directly on Stripe's hosted checkout page; we update the wallet only after Stripe sends us a verified webhook.

---

## Table of contents

1. [Authentication recap](#1-authentication-recap)
2. [Architecture at a glance](#2-architecture-at-a-glance)
3. [Wallet System](#3-wallet-system)
4. [Charging the wallet (Stripe flow)](#4-charging-the-wallet-stripe-flow)
5. [Store: Products & Orders](#5-store-products--orders)
6. [Auctions & Bids (with live SSE)](#6-auctions--bids-with-live-sse)
7. [Donations](#7-donations)
8. [Error handling reference](#8-error-handling-reference)
9. [Local dev setup checklist](#9-local-dev-setup-checklist)

---

## 1. Authentication recap

Every endpoint (except product/auction browse and the Stripe webhook) requires a Keycloak JWT in the `Authorization` header:

```
Authorization: Bearer <access_token>
Content-Type: application/json
```

Get the token from the existing `/auth/login` endpoint (no change). The user's identity is taken from `jwt.sub` (the Keycloak user id) — **the frontend never sends a user id** in the request body. The backend reads it from the token.

---

## 2. Architecture at a glance

```
                       ┌──────────────────┐
                       │   Browser / App  │
                       └────────┬─────────┘
                                │ JWT
                                ▼
                       ┌──────────────────┐
                       │   API Gateway    │  http://localhost:8080
                       └────────┬─────────┘
                  ┌─────────────┼──────────────┬────────────────┐
                  ▼             ▼              ▼                ▼
        ┌───────────────┐ ┌─────────────┐ ┌────────────┐ ┌──────────────┐
        │ wallet-       │ │ payment-    │ │ store-     │ │ (existing    │
        │ service       │ │ service     │ │ service    │ │ services)    │
        │ /wallets      │ │ /payments   │ │ /products  │ │              │
        │               │ │ /webhooks   │ │ /orders    │ │              │
        │               │ │             │ │ /auctions  │ │              │
        │               │ │             │ │ /donations │ │              │
        └──────┬────────┘ └──────┬──────┘ └─────┬──────┘ └──────────────┘
               │                 │              │
               │     (internal HTTP, X-Internal-Service-Token header)
               │◄────────────────┴──────────────┘
               ▼
          PostgreSQL
          (mscms_wallet | mscms_payment | mscms_store)
```

- **wallet-service** owns money. Every debit / credit / reserve / release / capture goes through it.
- **payment-service** integrates with Stripe and credits the wallet when Stripe says the payment cleared.
- **store-service** runs the product catalog, orders, auctions, bids, and donations. It calls the wallet for every money movement.

---

## 3. Wallet System

### 3.1 Get my wallet (auto-creates on first call)

`GET /wallets/me`

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": {
    "id": 1,
    "userKeycloakId": "8f1a...",
    "availableBalance": 250.00,
    "reservedBalance": 50.00,
    "totalBalance": 300.00,
    "currency": "USD",
    "createdAt": "2026-05-13T11:14:33",
    "updatedAt": "2026-05-13T11:18:01"
  }
}
```

- `availableBalance` — what the user can spend now.
- `reservedBalance` — held for active auction bids (cannot be spent elsewhere).
- `totalBalance` = available + reserved.

### 3.2 Transaction history

`GET /wallets/me/transactions?page=0&size=20`

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 42,
        "type": "DEPOSIT",
        "amount": 100.00,
        "balanceAfter": 250.00,
        "status": "SUCCESS",
        "externalPaymentId": "pi_3ABcd...",
        "referenceType": "PAYMENT",
        "referenceId": "7",
        "description": "Wallet top-up via STRIPE",
        "createdAt": "2026-05-13T11:14:30"
      }
    ],
    "totalElements": 12,
    "totalPages": 1,
    "size": 20,
    "number": 0
  }
}
```

Transaction `type` values your UI may see:
| Type | Meaning |
|---|---|
| `DEPOSIT` | Wallet topped up via Stripe |
| `PURCHASE` | Bought products from the store |
| `AUCTION_BID_RESERVE` | Funds moved to reserved for a bid |
| `AUCTION_BID_RELEASE` | Bid lost / was outbid — money back to available |
| `AUCTION_WIN_CAPTURE` | Auction won — reserved amount permanently consumed |
| `DONATION` | Donated to the club |
| `REFUND` | Refund received |
| `ADMIN_ADJUSTMENT` | Admin adjusted the balance manually |

`status` is `PENDING`, `SUCCESS`, `FAILED`, or `CANCELLED`. In normal flows you'll see `SUCCESS`.

### 3.3 Admin endpoints

These need `ROLE_ADMIN`:

- `GET /wallets/admin/{keycloakId}` — view any wallet.
- `POST /wallets/admin/{keycloakId}/adjust` — adjust balance with audit log.
  ```json
  { "amount": -25.00, "reason": "Refund for cancelled order #123" }
  ```

---

## 4. Charging the wallet (Stripe flow)

### 4.1 The flow

```
Frontend                Backend                 Stripe              Webhook → Backend
   │                       │                       │                       │
   │  POST /payments/      │                       │                       │
   │  charge {amount:100}  │                       │                       │
   ├──────────────────────►│                       │                       │
   │                       │  create checkout      │                       │
   │                       ├──────────────────────►│                       │
   │                       │  ◄─sessionId, url─────│                       │
   │  ◄─checkoutUrl────────│                       │                       │
   │                                                                       │
   │  window.location = checkoutUrl  (user pays on Stripe's page)          │
   │                                                                       │
   │  Stripe → success/cancel URL on your frontend                         │
   │                                                                       │
   │                       │   Stripe POSTs       │                        │
   │                       │   /webhooks/stripe  ◄┤                        │
   │                       │                       │                       │
   │                       │   verify signature,   │                       │
   │                       │   credit wallet       │                       │
   │                                                                       │
   │  GET /wallets/me  → shows new balance                                 │
```

The **frontend never sees the card data**. Stripe's hosted page collects it and PCI compliance stays on Stripe's side.

### 4.2 Step-by-step

**1. Create a payment session**

`POST /payments/charge`

```json
{ "amount": 100.00 }
```

Optional fields: `currency`, `successUrl`, `cancelUrl` (override defaults).

Response:

```json
{
  "success": true,
  "message": "Checkout session created",
  "data": {
    "id": 7,
    "amount": 100.00,
    "currency": "USD",
    "provider": "STRIPE",
    "providerSessionId": "cs_test_a1b2c3...",
    "checkoutUrl": "https://checkout.stripe.com/c/pay/cs_test_a1b2c3...",
    "status": "PENDING",
    "expiresAt": "2026-05-13T11:45:00"
  }
}
```

**2. Redirect the user**

```js
const res = await fetch('/payments/charge', {
  method: 'POST',
  headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
  body: JSON.stringify({ amount: 100.00 })
});
const { data } = await res.json();
window.location.href = data.checkoutUrl;     // ← user pays on Stripe
```

**3. After Stripe returns to your `successUrl`**

Stripe redirects to: `${successUrl}?session_id=cs_test_a1b2c3...`

The wallet is credited **after the Stripe webhook arrives** (usually within seconds), not when the user lands on `successUrl`. Two practical ways to show "Success":

- **Poll** `GET /payments/sessions/{id}` for a few seconds (~5–10 retries) until `status === "COMPLETED"`.
- **Or** poll `GET /wallets/me` until balance increases.

Sample success-page logic:

```js
async function waitForCompletion(sessionId) {
  for (let i = 0; i < 10; i++) {
    const r = await fetch(`/payments/sessions/${sessionId}`,
        { headers: { Authorization: `Bearer ${token}` } });
    const { data } = await r.json();
    if (data.status === 'COMPLETED') return data;
    if (data.status === 'FAILED' || data.status === 'CANCELLED') throw new Error(data.status);
    await new Promise(r => setTimeout(r, 1500));
  }
  throw new Error('TIMEOUT — webhook not received yet, refresh in a few seconds.');
}
```

**Possible session statuses**: `PENDING`, `COMPLETED`, `FAILED`, `CANCELLED`, `EXPIRED`.

### 4.3 Webhook explanation (for the team's awareness)

You do **not** call the webhook from the frontend. Stripe calls it directly:

```
POST /webhooks/stripe
Stripe-Signature: t=...,v1=...
<raw JSON>
```

The backend verifies the signature with `STRIPE_WEBHOOK_SECRET`. Duplicate deliveries are de-duplicated by `event.id` (idempotency table). Mismatched amounts are rejected as `FAILED`.

---

## 5. Store: Products & Orders

### 5.1 Browse products (public)

`GET /products?category=SHIRT&activeOnly=true&page=0&size=20`

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Home Jersey 2026",
        "description": "Official home shirt",
        "category": "SHIRT",
        "imageUrl": "https://cdn.example.com/jersey.jpg",
        "stockQuantity": 50,
        "price": 79.99,
        "status": "ACTIVE"
      }
    ],
    "totalElements": 12, "totalPages": 1, "number": 0
  }
}
```

Categories: `SHIRT`, `SCARF`, `SHOES`, `ACCESSORY`, `EQUIPMENT`, `OTHER`.

Single product: `GET /products/{id}`.

### 5.2 Admin product CRUD (`ROLE_ADMIN`)

- `POST /products` — create
- `PUT /products/{id}` — update
- `DELETE /products/{id}` — soft-delete (status → `DISCONTINUED`)

### 5.3 Place an order

`POST /orders`

```json
{
  "items": [
    { "productId": 1, "quantity": 2 },
    { "productId": 5, "quantity": 1 }
  ],
  "shippingAddress": "12 Main St, Cairo, Egypt"
}
```

Response:

```json
{
  "success": true,
  "message": "Order placed",
  "data": {
    "id": 17,
    "totalAmount": 179.97,
    "currency": "USD",
    "status": "PAID",
    "shippingAddress": "12 Main St, Cairo, Egypt",
    "walletTransactionId": "152",
    "items": [
      { "id": 31, "productId": 1, "productName": "Home Jersey 2026", "quantity": 2, "itemPrice": 79.99, "lineTotal": 159.98 },
      { "id": 32, "productId": 5, "productName": "Scarf", "quantity": 1, "itemPrice": 19.99, "lineTotal": 19.99 }
    ],
    "createdAt": "2026-05-13T11:22:01"
  }
}
```

What happens on the backend:

1. Validates products exist, are `ACTIVE`, and have enough stock.
2. Debits the wallet for the total.
3. Decrements stock; sets `status=PAID`.
4. Returns the saved order.

If the wallet has insufficient funds you get **HTTP 422 + `Wallet Operation Failed`** — show "Top up your wallet" CTA.

Other order endpoints:

- `GET /orders/me?page=0&size=20` — my orders
- `GET /orders/{id}` — one of my orders
- `GET /orders/admin?...` — admin: all orders

Order statuses: `PENDING`, `PAID`, `SHIPPED`, `DELIVERED`, `CANCELLED`, `REFUNDED`.

---

## 6. Auctions & Bids (with live SSE)

### 6.1 Browse auctions (public)

`GET /auctions?status=ACTIVE&page=0&size=20`

Statuses: `SCHEDULED` (not started yet), `ACTIVE`, `ENDED`, `CANCELLED`.

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 5,
        "title": "Captain's signed shirt — 2024 Final",
        "description": "Game-worn, autographed",
        "imageUrl": "https://cdn.example.com/auction5.jpg",
        "startingPrice": 100.00,
        "currentHighestBid": 350.00,
        "currentHighestBidder": "8f1a...",
        "auctionStartTime": "2026-05-13T10:00:00",
        "auctionEndTime": "2026-05-15T10:00:00",
        "status": "ACTIVE",
        "minimumBidIncrement": 1.00
      }
    ]
  }
}
```

Single auction: `GET /auctions/{id}`. Bid history: `GET /auctions/{id}/bids?page=0&size=20`.

### 6.2 Place a bid

`POST /auctions/{id}/bids`

```json
{ "amount": 360.00 }
```

Requirements:
- `amount` ≥ `currentHighestBid + minimumBidIncrement` (or ≥ `startingPrice` if no bids yet).
- You can't outbid yourself.
- You must have at least `amount` available in your wallet.

When the call succeeds:
- The backend **reserves** `amount` on your wallet (it moves from `availableBalance` to `reservedBalance`).
- The previous highest bidder's reservation is **released** (back to their `availableBalance`).
- All connected SSE clients receive a `bid.placed` event.

Response:

```json
{
  "success": true,
  "message": "Bid placed",
  "data": {
    "id": 102,
    "auctionId": 5,
    "bidderKeycloakId": "8f1a...",
    "amount": 360.00,
    "status": "ACTIVE",
    "bidTime": "2026-05-13T11:30:11"
  }
}
```

Bid statuses: `ACTIVE` (current high), `OUTBID`, `WON`, `REFUNDED`.

### 6.3 Live updates via Server-Sent Events

`GET /auctions/{id}/stream` — `Content-Type: text/event-stream`

```js
const evt = new EventSource(`/auctions/5/stream`);  // EventSource does not send Authorization automatically
evt.addEventListener('connected', (e) => console.log('connected', JSON.parse(e.data)));
evt.addEventListener('bid.placed', (e) => {
  const payload = JSON.parse(e.data);
  // payload = { type, auctionId, currentHighestBid, currentHighestBidder, bidId, timestamp }
  updatePriceLabel(payload.currentHighestBid);
});
evt.addEventListener('auction.ended', (e) => {
  const payload = JSON.parse(e.data);
  showWinner(payload.winnerKeycloakId, payload.currentHighestBid);
  evt.close();
});
evt.addEventListener('heartbeat', () => { /* no-op, just keep-alive */ });
evt.onerror = () => { /* browser will auto-reconnect; consider exponential backoff for retries */ };
```

> **Important — auth header**: The browser `EventSource` API does not let you set `Authorization`. Three options:
> 1. The stream endpoint is intentionally permissive for browse — you can keep it that way (current default).
> 2. Pass the token as a query param and have the gateway accept it — quick but logs the token.
> 3. Use a small wrapper (e.g. `@microsoft/fetch-event-source`) that supports custom headers. **Recommended.**

### 6.4 What happens when an auction ends

A scheduler runs every ~10s. When the end time passes:

1. The winning bid's reservation is **captured** (permanently consumed → it leaves the wallet).
2. The auction moves to `ENDED`, `winnerKeycloakId` is set.
3. An `auction.ended` SSE event is broadcast.
4. (Already-outbid bidders had their reservations released when they were outbid, so nothing to refund.)

If the admin cancels an `ACTIVE` auction via `POST /auctions/{id}/cancel`, **every** active reservation is released.

---

## 7. Donations

### 7.1 Donate from wallet

`POST /donations`

```json
{
  "amount": 25.00,
  "message": "Up the club!",
  "anonymous": false
}
```

Response:

```json
{
  "success": true,
  "message": "Thank you for your donation",
  "data": {
    "id": 9,
    "userKeycloakId": "8f1a...",
    "amount": 25.00,
    "currency": "USD",
    "message": "Up the club!",
    "anonymous": false,
    "createdAt": "2026-05-13T11:40:02"
  }
}
```

### 7.2 List donations

- `GET /donations/me?page=0&size=20` — my donations (donor-visible).
- `GET /donations?page=0&size=20` — public list; `userKeycloakId` is hidden on anonymous donations unless caller is admin.
- `GET /donations/analytics` (admin) — `{ totalAmount, totalDonations, uniqueDonors }`.

---

## 8. Error handling reference

Every error response uses this shape:

```json
{
  "timestamp": "2026-05-13T11:42:00",
  "status": 422,
  "error": "Insufficient Funds",
  "message": "Insufficient wallet balance. Available: 12.50, Required: 100.00",
  "path": "/orders",
  "validationErrors": null
}
```

Status codes you should special-case:

| Status | When | Frontend should… |
|--------|------|------------------|
| **400** | Bad input (missing/invalid fields) | Show field errors from `validationErrors` |
| **401** | No/invalid JWT | Redirect to login |
| **403** | JWT valid but no permission (e.g., non-admin POSTing a product) | Show "not allowed" |
| **404** | Resource doesn't exist or isn't yours | Show "not found" |
| **409** | Duplicate (e.g., same idempotency_key) | Usually safe to ignore — show success |
| **422** | Business rule violation (no funds, no stock, bid too low, etc.) | Show the `message` to the user |
| **429** | Rate limit exceeded | Show "slow down" message, optionally retry after a few seconds |
| **500** | Bug or downstream failure | Show generic "try again" with retry button |
| **502** | Stripe is unreachable | Same as 500 + maybe "Payments temporarily unavailable" |

### 8.1 Rate limits (HTTP 429)

The gateway enforces per-endpoint rate limits using **Bucket4j**. The buckets are:

| Bucket | Endpoints | Limit | Keyed by |
|---|---|---|---|
| **Auth** | `/auth/login`, `/auth/signup`, `/auth/refresh` | 10/min | client IP |
| **Webhooks** | `/webhooks/**` | 200/min | client IP |
| **Sensitive** | `/payments/charge`, `/auctions/*/bids`, `/donations`, `/orders` | 30/min | JWT token |
| **Default** | everything else | 300/min | JWT (or IP if anonymous) |

Excluded: `/actuator/**`, `/v3/api-docs/**`, `/swagger-ui/**`, and the SSE stream `/auctions/*/stream`.

Every response includes a header:

```
X-Rate-Limit-Remaining: 27
```

When you exceed a bucket, you get **HTTP 429** with body:

```json
{
  "success": false,
  "message": "Rate limit exceeded for this operation. Please slow down.",
  "status": 429
}
```

Frontend handling:

```ts
async function callApi(url: string, opts: RequestInit) {
  const res = await fetch(url, opts);
  if (res.status === 429) {
    const body = await res.json();
    toast.error(body.message);
    // optional: read 'X-Rate-Limit-Remaining' header from previous responses to warn proactively
    return null;
  }
  return res;
}
```

> **Tip:** for bidding wars, watch `X-Rate-Limit-Remaining`. When it gets below ~5, disable the "Bid" button for a few seconds rather than letting the user hit 429.

### Frontend examples

```ts
async function placeBid(auctionId: number, amount: number) {
  const res = await fetch(`/auctions/${auctionId}/bids`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ amount }),
  });
  if (res.status === 422) {
    const err = await res.json();
    toast.error(err.message);    // e.g., "Bid must be at least 361.00"
    return;
  }
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return (await res.json()).data;
}
```

---

## 9. Local dev setup checklist

Backend side (already wired):

1. Edit `D:\mscms\docker-compose.yml` and set real values on the **`payment-service`** block — replace `STRIPE_API_KEY=sk_test_replace_me` and `STRIPE_WEBHOOK_SECRET=whsec_replace_me` with real Stripe test keys (get them at https://dashboard.stripe.com/test/apikeys). Also pick a strong `INTERNAL_SERVICE_TOKEN` (32+ random chars) and use the **same value** on all three services: `wallet-service`, `payment-service`, `store-service`.
2. Run the **Stripe CLI** to forward webhooks to your local backend:
   ```bash
   stripe listen --forward-to localhost:8080/webhooks/stripe
   ```
   The CLI prints a `whsec_...` secret → set that as `STRIPE_WEBHOOK_SECRET` in `docker-compose.yml`.
3. `docker compose up -d` at `D:\mscms\`.

Frontend dev:

- Base URL: `http://localhost:8080`.
- For Stripe success/cancel pages, set up routes (e.g., Angular `/wallet/charge/success` and `/wallet/charge/cancel`) and adjust `PAYMENT_SUCCESS_URL` / `PAYMENT_CANCEL_URL` in `docker-compose.yml` if your frontend runs on a different host/port.
- Use a Stripe test card to pay: `4242 4242 4242 4242`, any future expiry, any CVC, any ZIP.
- For SSE, prefer `@microsoft/fetch-event-source` so you can attach the `Authorization` header.

---

## Cheat sheet — endpoints summary

| Method | Path | Auth | What |
|--------|------|------|------|
| GET | `/wallets/me` | user | Get my wallet (auto-creates) |
| GET | `/wallets/me/transactions` | user | Paginated history |
| GET | `/wallets/admin/{kcId}` | admin | View any wallet |
| POST | `/wallets/admin/{kcId}/adjust` | admin | Adjust balance |
| POST | `/payments/charge` | user | Create Stripe checkout session |
| GET | `/payments/sessions/{id}` | user | Poll payment session status |
| POST | `/webhooks/stripe` | Stripe only | Webhook receiver (signed) |
| GET | `/products` / `/products/{id}` | public | Browse |
| POST/PUT/DELETE | `/products[/{id}]` | admin | Manage catalog |
| POST | `/orders` | user | Place order (charges wallet) |
| GET | `/orders/me` / `/orders/{id}` | user | My orders |
| GET | `/orders/admin` | admin | All orders |
| GET | `/auctions` / `/auctions/{id}` / `/auctions/{id}/bids` | public | Browse |
| POST | `/auctions` | admin | Create auction |
| POST | `/auctions/{id}/cancel` | admin | Cancel auction |
| POST | `/auctions/{id}/bids` | user | Place a bid |
| GET | `/auctions/{id}/stream` | user | SSE live updates |
| POST | `/donations` | user | Donate |
| GET | `/donations/me` | user | My donations |
| GET | `/donations` | user | All donations (public, hides anon) |
| GET | `/donations/analytics` | admin | Donation totals |

---

Questions / changes? Ping the backend team. The OpenAPI/Swagger docs are aggregated at `http://localhost:8080/swagger-ui.html` after the stack is up.
