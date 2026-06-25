package cc.anore;

/** Webhook signature did not match the expected value. */
public class SignatureException extends AnoreException {
    public SignatureException(String message) {
        super(message);
    }
}
