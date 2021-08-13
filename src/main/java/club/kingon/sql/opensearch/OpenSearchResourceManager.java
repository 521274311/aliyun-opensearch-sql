package club.kingon.sql.opensearch;

import club.kingon.sql.opensearch.api.AliyunApiClient;
import com.aliyun.opensearch.OpenSearchClient;

/**
 * @author dragons
 * @date 2021/8/13 10:23
 */
public interface OpenSearchResourceManager {

    AliyunApiClient getAliyunApiClient();

    OpenSearchClient getOpenSearchClient();

    void close();
}
