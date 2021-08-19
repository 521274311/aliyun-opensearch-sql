package club.kingon.sql.opensearch.parser;

/**
 * @author dragons
 * @date 2021/8/13 14:39
 */
public class SQLParserException extends RuntimeException {

    public SQLParserException(String msg) {
        super(msg);
    }

    public SQLParserException(Throwable cause) {
        super(cause);
    }

    public SQLParserException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
