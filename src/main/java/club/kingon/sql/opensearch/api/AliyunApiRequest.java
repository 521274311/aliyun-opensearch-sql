package club.kingon.sql.opensearch.api;

import com.aliyuncs.http.FormatType;
import com.aliyuncs.http.MethodType;

import java.util.Map;

/**
 * @author dragons
 * @date 2021/8/12 9:42
 */
public interface AliyunApiRequest {

    MethodType getHttpMethod();

    String getUri();

    String getVersion();

    String getDomain();

    String getBody();

    Map<String, String> getHeaders();

    String getEncoding();

    FormatType getFormat();
}
