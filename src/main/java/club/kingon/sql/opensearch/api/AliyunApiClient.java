package club.kingon.sql.opensearch.api;

import com.aliyuncs.CommonResponse;
import com.aliyuncs.exceptions.ClientException;

/**
 * @author dragons
 * @date 2021/8/11 20:07
 */
public interface AliyunApiClient {
    <T extends CommonResponse, P extends AliyunApiRequest>T execute(P p) throws ClientException;
}
