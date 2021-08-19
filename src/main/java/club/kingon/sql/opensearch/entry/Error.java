package club.kingon.sql.opensearch.entry;

/**
 * @author dragons
 * @date 2021/8/18 18:04
 */
public class Error {

    private Integer code;

    private String message;

    public Error() {
    }

    public Error(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Error{" +
            "code=" + code +
            ", message='" + message + '\'' +
            '}';
    }
}
