package club.kingon.sql.opensearch.api;

/**
 * @author dragons
 * @date 2021/8/12 9:54
 */
public abstract class AbstractOpenSearchManageApiRequest extends AbstractAliyunApiRequest {

    public AbstractOpenSearchManageApiRequest() {
        // 设置默认json
        putHeaderParameter("Content-Type", "application/json");
    }

    @Override
    public String getDomain() {
        return "opensearch." + getEndpoint().getRegionId() + ".aliyuncs.com";
    }

    public abstract Endpoint getEndpoint();
}
