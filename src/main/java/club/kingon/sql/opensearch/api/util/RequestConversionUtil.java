package club.kingon.sql.opensearch.api.util;

import club.kingon.sql.opensearch.api.AbstractOpenSearchManageAppNameApiRequest;
import club.kingon.sql.opensearch.api.AliyunApiClient;
import club.kingon.sql.opensearch.api.AliyunApiRequest;
import com.aliyuncs.CommonRequest;

import java.util.Map;

/**
 * @author dragons
 * @date 2021/8/12 10:20
 */
public class RequestConversionUtil {

    public static <P extends AliyunApiRequest>CommonRequest aliyunApiRequestToCommonRequest(P p) {
        CommonRequest request = new CommonRequest();
        request.setSysDomain(p.getDomain());
        request.setSysMethod(p.getHttpMethod());
        request.setSysVersion(p.getVersion());
        request.setSysUriPattern(p.getUri());
        p.getHeaders().forEach(request::putHeadParameter);
        request.setHttpContent(p.getBody().getBytes(), p.getEncoding(), p.getFormat());
        return request;
    }
}
