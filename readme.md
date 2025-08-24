1) High-level: ako má OpenRooms fungovať (end-to-end)

Hlavná myšlienka
•	Každý používateľ si spustí vlastný server (spustiteľný JAR). Tento server slúži ako relay pre WebSocket správy, vytvára rooms a vydáva linky.
•	Správy sú end-to-end šifrované v prehliadači (klienti). Server nikdy neuchováva dešifrovaný obsah správ.

Kľúčové komponenty (BE)
•	REST API: vytváranie miestností + získanie salt pre KDF.
•	POST /api/rooms — vytvorenie roomu (zakladateľ pošle verifier, server uloží salt + verifier).
•	GET  /api/rooms/{roomId} — vráti salt + meta (online count).
•	WebSocket endpoint /ws — klient sa autentifikuje auth rámcom (posiela roomId + proof) → ak OK, server priradí socket do roomu a len forwarduje msg rámce.
•	In-memory alebo DB úložisko pre rooms (MVP: in-memory map).

Flow (detail)
1.	Zakladateľ v UI zadá heslo (na klientovi sa vygeneruje salt + kľúč cez PBKDF2).
•	Derivácia: K = PBKDF2(password, salt, iterations=310000, HMAC-SHA256) (frontend).
2.	Zakladateľ v klientovi vygeneruje verifier (napr. HMAC(rawKey, “join:”) alebo priamo HMAC(rawKey, “server-auth”)) a pošle do POST /api/rooms spolu so salt.
•	Server uloží {roomId, salt, verifier}.
3.	Zakladateľ dostane link https://<host>/r/<roomId>. Heslo posiela out-of-band (SMS/Signal/email).
4.	Pripojujúci sa použije GET /api/rooms/{roomId} — získa salt → derivuje K lokalne (v prehliadači).
5.	Klient vypočíta proof = HMAC(rawKey, "join:" + roomId) a otvorí WebSocket → odošle {type:"auth", roomId, proof}.
6.	Server porovná proof s uloženým verifier → ak sa rovnajú, socket dostane prístup do roomu.
7.	Od tej chvíle posielajú klienti cez WebSocket rámce {type:"msg", payload: <AES-GCM-ciphertext base64>}. Server len broadcastuje payload ostatným.

Dôležité: server nikdy nevie z verifier získať heslo ani kľúč. Server môže vidieť metadá (IP adresy, timestamps), ale nie obsah.

⸻

2) Bezpečnostné rozhodnutia a návrhy (kľúčové body)
   •	KDF: PBKDF2-HMAC-SHA256 (iterations 310k alebo viac) alebo Argon2 (ak chceš moderné). WebCrypto podporuje PBKDF2.
   •	Sym. šifra: AES-GCM (IV 12B, tag 16B). Frontend šifruje/dešifruje.
   •	Verifier / proof:
   •	MVP: uložíme verifier = HMAC(keyRaw, "join:" + roomId) priamo — server pri join porovná príchozí proof s uloženým verifier.
   •	Lepšie: server uloží PRF-kľúč (kAuth) a pri join overí proof computed-server-side (nižšie leak riziko). (MVP -> jednoduchá verzia, neskôr zlepšiť.)
   •	Transport: HTTPS / WSS v produkcii (Caddy/Nginx s Let’s Encrypt), pre lokálne testy môže ísť aj HTTP.
   •	Metadata: serveru stále vidno IP, čas a veľkosť správy. Zakladateľ teda vidí, kto sa pripojil (IP/WS session) — ak to chceš minimalizovať, používať relay cez TOR/obscuring, ale to je pokročilé.
   •	Storage: v MVP ponechávame room meta v pamäti (ultra-low persistence). Ak chceš perzistenciu, šifruj sensitive fields v DB alebo ukladaj iba verifier+salt (bez plaintextov).

⸻

3) Dátové modely (BE → jednoduché)

Room {
roomId: String (PK)
salt: bytes (Base64 in API)
verifier: bytes (Base64)
createdAt: timestamp
ttlHours: int
maxParticipants: int
}


Endpointy:
•	POST /api/rooms
body: { "roomId": <opt>, "verifierBase64": "<...>", "saltBase64": "<...>" }
returns: { "roomId": "...", "saltBase64": "...", "joinUrl": "https://..." }
•	GET /api/rooms/{roomId}
returns: { "roomId": "...", "saltBase64": "...", "online": <n> }
•	WS /ws
frames:
•	auth: { "type":"auth", "roomId":"...", "proof":"<base64>" }
•	msg: { "type":"msg", "roomId":"...", "payload":"<base64>" }

⸻

4) Ktoré DB použiť? (odpoveď na doplňujúcu otázku)

Krátko: pre MVP odporúčam H2 (file-based) alebo embedded H2 (alebo SQLite cez JDBC), pretože:
•	Žiadna externá infra — jednoduché spustenie java -jar a všetko funguje.
•	Môžeš mať perzistenciu pri reštartoch (nevyprchá ti room).
•	Rýchle nastavenie, žiadna konfigurácia.

Pre produkčné self-host inštancie (ak chceš odporučiť používateľom, aby si vybrali DB):
•	PostgreSQL — odporúčam pre seriózne nasadenia (stabilita, bezpečnosť, replikácia).
•	Alternatívy: MySQL, MariaDB.

Tradeoffs:
•	H2 (file) = jednoduché, ale menej robustné; migrácie cez Flyway sú stále možné.
•	PostgreSQL = robustné + škálovanie (ak by niekto chcel spustiť centralizované relay clustre). Vyžaduje viac ops práce (alebo Docker).

Moja odporúčaná stratégia:
•	MVP: H2 (file) alebo in-memory (ak nechceš perzistenciu). H2 umožní ľahký prechod na PostgreSQL neskôr (JDBC).
•	Produkcia: Postgres + Flyway/Prisma a zálohy.
