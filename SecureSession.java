import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.SecretKey;

/**
 * SecureSession
 *
 * Stores secure session state for one peer.
 */
public class SecureSession {

    private String peerUsername;
    private String sessionId;

    // My ephemeral X25519 key pair data
    private PrivateKey myEphemeralPrivateKey;
    private PublicKey myEphemeralPublicKey;

    // Peer's ephemeral X25519 public key
    private PublicKey peerEphemeralPublicKey;

    // Derived symmetric keys
    private SecretKey sendKey;
    private SecretKey receiveKey;

    // Whether the handshake is fully complete
    private boolean established;

    public SecureSession(String peerUsername) {
        this.peerUsername = peerUsername;
    }

    public String getPeerUsername() {
        return peerUsername;
    }

    public String getSessionId() {
        return sessionId;
    }

    public PrivateKey getMyEphemeralPrivateKey() {
        return myEphemeralPrivateKey;
    }

    public PublicKey getMyEphemeralPublicKey() {
        return myEphemeralPublicKey;
    }

    public PublicKey getPeerEphemeralPublicKey() {
        return peerEphemeralPublicKey;
    }

    public SecretKey getSendKey() {
        return sendKey;
    }

    public SecretKey getReceiveKey() {
        return receiveKey;
    }

    public boolean isEstablished() {
        return established;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setMyEphemeralPrivateKey(PrivateKey myEphemeralPrivateKey) {
        this.myEphemeralPrivateKey = myEphemeralPrivateKey;
    }

    public void setMyEphemeralPublicKey(PublicKey myEphemeralPublicKey) {
        this.myEphemeralPublicKey = myEphemeralPublicKey;
    }

    public void setPeerEphemeralPublicKey(PublicKey peerEphemeralPublicKey) {
        this.peerEphemeralPublicKey = peerEphemeralPublicKey;
    }

    public void setSendKey(SecretKey sendKey) {
        this.sendKey = sendKey;
    }

    public void setReceiveKey(SecretKey receiveKey) {
        this.receiveKey = receiveKey;
    }

    public void setEstablished(boolean established) {
        this.established = established;
    }
}
