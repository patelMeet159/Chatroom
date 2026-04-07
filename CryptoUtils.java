import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * CryptoUtils
 *
 * Utility class for:
 * - Ed25519 signing
 * - X25519 key agreement
 * - HKDF-SHA-256
 * - AES-GCM
 */
public class CryptoUtils {

    private static final SecureRandom random = new SecureRandom();

    /**
     * Generates a long-term Ed25519 signing key pair.
     */
    public static KeyPair generateSigningKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
        return kpg.generateKeyPair();
    }

    /**
     * Generates an ephemeral X25519 key pair for Diffie-Hellman.
     */
    public static KeyPair generateEphemeralKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("X25519");
        return kpg.generateKeyPair();
    }

    /**
     * Signs arbitrary bytes with Ed25519.
     */
    public static byte[] sign(byte[] data, PrivateKey signingPrivateKey) throws Exception {
        Signature sig = Signature.getInstance("Ed25519");
        sig.initSign(signingPrivateKey);
        sig.update(data);
        return sig.sign();
    }

    /**
     * Verifies an Ed25519 signature.
     */
    public static boolean verify(byte[] data, byte[] signatureBytes, PublicKey signingPublicKey) throws Exception {
        Signature sig = Signature.getInstance("Ed25519");
        sig.initVerify(signingPublicKey);
        sig.update(data);
        return sig.verify(signatureBytes);
    }

    /**
     * Performs X25519 key agreement.
     */
    public static byte[] deriveSharedSecret(PrivateKey myPrivateKey, PublicKey peerPublicKey) throws Exception {
        KeyAgreement ka = KeyAgreement.getInstance("X25519");
        ka.init(myPrivateKey);
        ka.doPhase(peerPublicKey, true);
        return ka.generateSecret();
    }

    /**
     * Derives directional AES keys from the shared secret and session context.
     */
    public static SecretKey deriveDirectionalKey(byte[] sharedSecret, String sessionId, String directionLabel) throws Exception {
        byte[] salt = sha256(sessionId.getBytes(StandardCharsets.UTF_8));
        byte[] prk = hkdfExtract(salt, sharedSecret);
        byte[] okm = hkdfExpand(prk, directionLabel.getBytes(StandardCharsets.UTF_8), 32);
        return new SecretKeySpec(okm, "AES");
    }

    /**
     * Encrypts plaintext with AES-GCM.
     */
    public static EncryptedPacket encrypt(String plaintext, SecretKey key, String aadText) throws Exception {
        byte[] nonce = new byte[12];
        random.nextBytes(nonce);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, nonce);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        if (aadText != null) {
            cipher.updateAAD(aadText.getBytes(StandardCharsets.UTF_8));
        }

        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return new EncryptedPacket(nonce, ciphertext);
    }

    /**
     * Decrypts ciphertext with AES-GCM.
     */
    public static String decrypt(byte[] nonce, byte[] ciphertext, SecretKey key, String aadText) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, nonce);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        if (aadText != null) {
            cipher.updateAAD(aadText.getBytes(StandardCharsets.UTF_8));
        }

        byte[] plaintext = cipher.doFinal(ciphertext);
        return new String(plaintext, StandardCharsets.UTF_8);
    }

    public static String toBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static byte[] fromBase64(String b64) {
        return Base64.getDecoder().decode(b64);
    }

    public static PublicKey decodeEd25519PublicKey(String b64) throws Exception {
        byte[] encoded = fromBase64(b64);
        KeyFactory kf = KeyFactory.getInstance("Ed25519");
        return kf.generatePublic(new X509EncodedKeySpec(encoded));
    }

    public static PublicKey decodeX25519PublicKey(String b64) throws Exception {
        byte[] encoded = fromBase64(b64);
        KeyFactory kf = KeyFactory.getInstance("X25519");
        return kf.generatePublic(new X509EncodedKeySpec(encoded));
    }

    private static byte[] sha256(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(data);
    }

    private static byte[] hkdfExtract(byte[] salt, byte[] ikm) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec key = new SecretKeySpec(salt, "HmacSHA256");
        mac.init(key);
        return mac.doFinal(ikm);
    }

    private static byte[] hkdfExpand(byte[] prk, byte[] info, int length) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec key = new SecretKeySpec(prk, "HmacSHA256");
        mac.init(key);

        byte[] okm = new byte[length];
        byte[] previous = new byte[0];
        int bytesGenerated = 0;
        int counter = 1;

        while (bytesGenerated < length) {
            mac.reset();
            mac.init(key);
            mac.update(previous);
            mac.update(info);
            mac.update((byte) counter);

            previous = mac.doFinal();

            int bytesToCopy = Math.min(previous.length, length - bytesGenerated);
            System.arraycopy(previous, 0, okm, bytesGenerated, bytesToCopy);

            bytesGenerated += bytesToCopy;
            counter++;
        }

        return okm;
    }

    /**
     * Small holder object for AES-GCM output.
     */
    public static class EncryptedPacket {
        private final byte[] nonce;
        private final byte[] ciphertext;

        public EncryptedPacket(byte[] nonce, byte[] ciphertext) {
            this.nonce = nonce;
            this.ciphertext = ciphertext;
        }

        public byte[] getNonce() {
            return nonce;
        }

        public byte[] getCiphertext() {
            return ciphertext;
        }
    }
}