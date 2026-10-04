package cc.anore;

public class AnoreException extends RuntimeException {
    public AnoreException(String message) {
        super(message);
    }

    public AnoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
