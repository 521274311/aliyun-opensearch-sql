package club.kingon.sql.opensearch;

/**
 * @author dragons
 * @date 2021/8/12 14:57
 */
public class OpenSearchManagerException extends RuntimeException {

    public OpenSearchManagerException(String msg) {
        super(msg);
    }

    public OpenSearchManagerException(Throwable cause) {
        super(cause);
    }

    public OpenSearchManagerException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
