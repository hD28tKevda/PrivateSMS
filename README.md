# PrivateSMS
E2E encrypted SMS messanger for Android

**PrivateSMS** is an Android application that brings **end‑to‑end encryption** to your ordinary SMS messages.  
It seamlessly integrates with the system SMS database, so you can continue using your existing messaging app while keeping your conversations private.

## How It Works
PrivateSMS uses a **hybrid encryption scheme**:
1. Each outgoing message is encrypted with a random **AES‑256** key.
2. The AES key is then wrapped with the recipient’s **RSA public key** (2048‑bit or 4096‑bit).
3. The encrypted package is sent as a standard SMS.
4. On the receiving side, the app automatically intercepts the SMS, decrypts the AES key with the user’s private RSA key, and displays the original plain text.

Because all encryption happens on the device and keys never leave it, even your mobile operator cannot read the content of your messages.

## Feachers
- **Automatic encryption & decryption** – send and receive encrypted SMS transparently.
- **Key management** – generate, export, and import RSA key pairs in PEM format.
- **Per‑contact encryption toggle** – enable or disable encryption for each conversation individually.
- **Unread message indicators** – purple dot for new messages; badge disappears when you open the chat.

## Bug reports
This app may still contain bugs. If you encounter any issues, please report them to my [e-mail](mailto:zips-jaws-avert@duck.com).
