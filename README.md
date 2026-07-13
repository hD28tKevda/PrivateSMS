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

## How to start private communication
To start encrypted SMS communication, you need to follow the following steps:
1. After installation, the application will automatically generate a pair of keys
2. Open the chat with the person, click the button with the optional actions in the upper right corner and click on the Send my Public Key button. After that, your current public key is inserted into the text field. Your interlocutor should do the same.
3. When you get the brothald of the interlocutor and copy the message by clapping it. After that, click on the contact name at the top of the chat and insert its public key by making double tap on the theatrical
Ready, you can communicate without doubting the confidentiality of your correspondence!
## Bug reports
This app may still contain bugs. If you encounter any issues, please report them to my [e-mail](mailto:zips-jaws-avert@duck.com).
