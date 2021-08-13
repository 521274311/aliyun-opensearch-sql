package club.kingon.sql.opensearch.api;

import com.aliyuncs.http.FormatType;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author dragons
 * @date 2021/8/12 9:52
 */
public abstract class AbstractAliyunApiRequest implements AliyunApiRequest, Serializable {

    private String version = "2017-12-25";

    private String body = "{}";

    private Map<String, String> headers = new HashMap<>();

    private String encoding = "utf-8";

    private FormatType formatType = FormatType.JSON;

    public AbstractAliyunApiRequest() {

    }

    @Override
    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void putHeaderParameter(String name, String value) {
        headers.put(name, value);
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    @Override
    public FormatType getFormat() {
        return null;
    }

    public void setFormatType(FormatType formatType) {
        this.formatType = formatType;
    }
}
