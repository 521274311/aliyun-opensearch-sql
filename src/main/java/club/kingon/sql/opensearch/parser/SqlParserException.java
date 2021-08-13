package club.kingon.sql.opensearch.parser;

/**
 * @author dragons
 * @date 2021/8/13 14:39
 */
public class SqlParserException extends RuntimeException {

    public SqlParserException(String msg) {
        super(msg);
    }

    public SqlParserException(Throwable cause) {
        super(cause);
    }

    public SqlParserException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
