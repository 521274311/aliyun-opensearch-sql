package club.kingon.sql.opensearch.api;

import com.aliyuncs.http.MethodType;

/**
 * 查询应用版本详
 * @author dragons
 * @date 2021/8/12 10:06
 */
public class OpenSearchAppNameVersionDetailQueryApiRequest extends AbstractOpenSearchManageAppNameApiRequest {

    private Endpoint endpoint;

    private String appName;

    private String appId;

    public OpenSearchAppNameVersionDetailQueryApiRequest(String appName, String appId) {
        this.appName = appName;
        this.appId = appId;
        this.endpoint = Endpoint.SHENZHEN;
    }

    public OpenSearchAppNameVersionDetailQueryApiRequest(String appName, String appId, Endpoint endpoint) {
        this(appName, appId);
        this.endpoint = endpoint;
    }

    @Override
    public Endpoint getEndpoint() {
        return endpoint;
    }

    @Override
    public String getAppName() {
        return appName;
    }

    @Override
    public MethodType getHttpMethod() {
        return MethodType.GET;
    }

    @Override
    public String getUri() {
        return "/v4/openapi/app-groups/" + appName + "/apps/" + appId;
    }
}
