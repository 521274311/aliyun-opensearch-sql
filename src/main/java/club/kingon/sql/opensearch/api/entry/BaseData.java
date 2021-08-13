package club.kingon.sql.opensearch.api.entry;

import java.io.Serializable;

/**
 * @author dragons
 * @date 2021/8/12 12:13
 */
public abstract class BaseData<T> implements Serializable {

    private static final long serialVersionUID = -5809782578272943999L;

    private String requestId;

    private T result;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }
}
