package club.kingon.sql.opensearch.api;

import club.kingon.sql.opensearch.api.util.RequestConversionUtil;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;

import java.util.Objects;

/**
 * @author dragons
 * @date 2021/8/11 20:10
 */
public class DefaultAliyunApiClient implements AliyunApiClient {

    private DefaultProfile profile;

    private IAcsClient client;

    public DefaultAliyunApiClient(String accessKey, String secret, Endpoint endpoint) {
        profile = DefaultProfile.getProfile(Objects.requireNonNull(endpoint).getRegionId(), accessKey, secret);
        client = new DefaultAcsClient(profile);
    }

    public DefaultAliyunApiClient(String regionId, String accessKey, String secret, long connectionTimeout, long readTimeout) {
        profile = DefaultProfile.getProfile(regionId, accessKey, secret);
        profile.getHttpClientConfig().setConnectionTimeoutMillis(connectionTimeout);
        profile.getHttpClientConfig().setReadTimeoutMillis(readTimeout);
        client = new DefaultAcsClient(profile);
    }

    @Override
    public <T extends CommonResponse, P extends AliyunApiRequest> T execute(P p) throws ClientException {
        return (T) client.getCommonResponse(RequestConversionUtil.aliyunApiRequestToCommonRequest(p));
    }

    public DefaultProfile getProfile() {
        return profile;
    }
}
