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

    private final static int DEFAULT_CONNECTION_TIMEOUT = 10_000;

    private final static int DEFAULT_READ_TIMEOUT = 5_000;

    public AbstractOpenSearchClientManager(String accessKey, String secret, Endpoint endpoint) {
        this(accessKey, secret, endpoint, false);
    }

    public AbstractOpenSearchClientManager(String accessKey, String secret, Endpoint endpoint, boolean intranet) {
        this(accessKey, secret, endpoint, intranet, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    public AbstractOpenSearchClientManager(String accessKey, String secret, Endpoint endpoint, boolean intranet, int connectionTimeout, int readTimeout) {
        this.accessKey = accessKey;
        this.secret = secret;
        this.endpoint = endpoint;
        this.intranet = intranet;
        aliyunApiClient = new DefaultAliyunApiClient(accessKey, secret, endpoint);
        String openSearchServerUrl = "http://" + (intranet ? "intranet." : "") + "opensearch-" + endpoint.getRegionId() + ".aliyuncs.com";
        OpenSearch openSearch = new OpenSearch(accessKey, secret, openSearchServerUrl);
        if (connectionTimeout >= 0) {
            openSearch.setConnectTimeout(connectionTimeout);
        }
        if (readTimeout >= 0) {
            openSearch.setTimeout(readTimeout);
        }
        openSearchClient = new OpenSearchClient(openSearch);

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
