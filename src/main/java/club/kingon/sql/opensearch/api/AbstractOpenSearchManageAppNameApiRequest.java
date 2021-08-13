package club.kingon.sql.opensearch.api;

/**
 * @author dragons
 * @date 2021/8/12 10:04
 */
public abstract class AbstractOpenSearchManageAppNameApiRequest extends AbstractOpenSearchManageApiRequest {

    public AbstractOpenSearchManageAppNameApiRequest() {
    }

    public abstract String getAppName();
}
