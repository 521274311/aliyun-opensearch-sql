package club.kingon.sql.opensearch;

import java.io.Serializable;

/**
 * @author dragons
 * @date 2020/12/24 10:28
 */
public class OpenSearchDqlException extends RuntimeException implements Serializable {
    private static final long serialVersionUID = 0L;
    private Throwable cause;

    public OpenSearchDqlException(String message) {
        super(message);
    }

    public OpenSearchDqlException(Throwable cause) {
        super(cause.getMessage());
        this.cause = cause;
    }

    public OpenSearchDqlException(String message, Throwable cause) {
        super(message);
        this.cause = cause;
    }

    @Override
    public Throwable getCause() {
        return this.cause;
    }
}
