package club.kingon.sql.opensearch.support;

import club.kingon.sql.opensearch.OpenSearchManager;
import club.kingon.sql.opensearch.api.AliyunApiClient;
import club.kingon.sql.opensearch.api.DefaultAliyunApiClient;
import club.kingon.sql.opensearch.api.Endpoint;
import com.aliyun.opensearch.OpenSearchClient;
import com.aliyun.opensearch.sdk.generated.OpenSearch;

/**
 * @author dragons
 * @date 2021/8/12 13:58
 */
public abstract class AbstractOpenSearchClientManager implements OpenSearchManager {

    private String accessKey;

    private String secret;

    private Endpoint endpoint;

    private boolean intranet;

    protected AliyunApiClient aliyunApiClient;

    protected OpenSearchClient openSearchClient;

    public AbstractOpenSearchClientManager(String accessKey, String secret, Endpoint endpoint) {
        this(accessKey, secret, endpoint, false);
    }

    public AbstractOpenSearchClientManager(String accessKey, String secret, Endpoint endpoint, boolean intranet) {
        this.accessKey = accessKey;
        this.secret = secret;
        this.endpoint = endpoint;
        this.intranet = intranet;
        aliyunApiClient = new DefaultAliyunApiClient(accessKey, secret, endpoint);
        String openSearchServerUrl = "http://" + (intranet ? "intranet." : "") + "opensearch-" + endpoint.getRegionId() + ".aliyuncs.com";
        openSearchClient = new OpenSearchClient(new OpenSearch(accessKey, secret, openSearchServerUrl));
    }

    @Override
    public String getAccessKey() {
        return accessKey;
    }

    @Override
    public String getSecret() {
        return secret;
    }

    @Override
    public Endpoint getEndpoint() {
        return endpoint;
    }

    @Override
    public AliyunApiClient getAliyunApiClient() {
        return aliyunApiClient;
    }

    @Override
    public OpenSearchClient getOpenSearchClient() {
        return openSearchClient;
    }
}
