package club.kingon.sql.opensearch.api;

import com.aliyuncs.http.MethodType;

/**
 * 查询应用详情
 * @author dragons
 * @date 2021/8/12 10:52
 */
public class OpenSearchAppNameDetailQueryApiRequest extends AbstractOpenSearchManageAppNameApiRequest{

    private String appName;

    private Endpoint endpoint;

    public OpenSearchAppNameDetailQueryApiRequest(String appName) {
        this.appName = appName;
        endpoint = Endpoint.SHENZHEN;
    }

    public OpenSearchAppNameDetailQueryApiRequest(String appName, Endpoint endpoint) {
        this(appName);
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
        return "/v4/openapi/app-groups/" + appName;
    }

}
