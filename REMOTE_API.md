# AeroWeb remote browser API

When AeroWeb is open, its local HTTP API listens on port 8787. The menu's “Conector para asistentes” screen shows the phone's LAN URL and a random 256-bit Bearer token stored in app-private preferences. The server stops when the app process stops. Use only over a trusted Wi-Fi network or inside an HTTPS tunnel; do not post the token in public chats.

Every endpoint except `/health` requires `Authorization: Bearer <token>`:

- `GET /v1/status` — active tab URL/title and endpoint list.
- `GET /v1/page` — current title, URL and up to 30,000 characters of visible page text.
- `GET /v1/resources` — scans the current page, scrolls up to 20 viewports to trigger lazy loading, returns up to 300 detected HTTP(S) resource URLs and metadata, and restores scroll position.
- `POST /v1/navigate` — JSON `{"url":"https://example.com"}`; accepts HTTP(S).
- `POST /v1/back` — go back if possible.
- `POST /v1/scroll` — JSON `{"direction":"down"}` or `{"direction":"up"}`.

The API only reads content already available in the normal WebView; it does not solve CAPTCHAs, sign in, click arbitrary controls, bypass DRM, or download all resources automatically. Actions are not per-agent rate limited, as this personal build is intended for trusted use. Keep the token private and rotate it by clearing AeroWeb app data. An Internet-reachable deployment needs an HTTPS tunnel; Cloudflare Quick Tunnels provide a temporary URL without a VPS or domain, but the URL changes on restart and anyone with it can reach the service if they also have its token.

Example:

```sh
curl -H 'Authorization: Bearer YOUR_TOKEN' http://PHONE_LAN_IP:8787/v1/status
curl -H 'Authorization: Bearer YOUR_TOKEN' -H 'Content-Type: application/json' -d '{"url":"https://example.com"}' http://PHONE_LAN_IP:8787/v1/navigate
```
