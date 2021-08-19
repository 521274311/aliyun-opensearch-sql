package club.kingon.sql.opensearch.entry;

import java.io.Serializable;
import java.util.List;

/**
 * @author dragons
 * @date 2021/8/18 17:57
 */
public abstract class OpenSearchBaseResult<T> implements Serializable {
    private static final long serialVersionUID = -5809782578272943999L;

    private String status;

    private String requestId;

    private T result;

    private List<Error> errors;

    private String tracer;

    private String opsRequestMisc;

    public OpenSearchBaseResult() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

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

    public List<Error> getErrors() {
        return errors;
    }

    public void setErrors(List<Error> errors) {
        this.errors = errors;
    }

    public String getTracer() {
        return tracer;
    }

    public void setTracer(String tracer) {
        this.tracer = tracer;
    }

    public String getOpsRequestMisc() {
        return opsRequestMisc;
    }

    public void setOpsRequestMisc(String opsRequestMisc) {
        this.opsRequestMisc = opsRequestMisc;
    }

    @Override
    public String toString() {
        return "OpenSearchBaseResult{" +
            "status='" + status + '\'' +
            ", requestId='" + requestId + '\'' +
            ", result=" + result +
            ", errors=" + errors +
            ", tracer='" + tracer + '\'' +
            ", opsRequestMisc='" + opsRequestMisc + '\'' +
            '}';
    }
}
