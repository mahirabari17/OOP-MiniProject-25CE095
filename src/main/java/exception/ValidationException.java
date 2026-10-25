package exception;

/** Thrown when user input fails validation (bad mobile, email, type ...). */
public class ValidationException extends BankException {
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
