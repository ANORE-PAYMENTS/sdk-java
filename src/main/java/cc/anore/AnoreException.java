package cc.anore;

/** Base class for everything the anore SDK throws. */
public class AnoreException extends RuntimeException {
    public AnoreException(String message) {
        super(message);
    }

    public AnoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
