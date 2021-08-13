package club.kingon.sql.opensearch.api;

/**
 * 阿里云接入点枚举
 * @author dragons
 * @date 2021/8/12 9:34
 */
public enum Endpoint {
    QINGDAO("cn-qingdao"),
    BEIJING("cn-beijing"),
    HANGZHOU("cn-hangzhou"),
    SHANGHAI("cn-shanghai"),
    SHENZHEN("cn-shenzhen"),
    SINGAPORE("ap-southeast-1"),
    ZHANGJIAKOU("cn-zhangjiakou"),
    FRANKFURT("eu-central-1");

    private String regionId;

    Endpoint(String regionId) {
        this.regionId = regionId;
    }

    public String getRegionId() {
        return regionId;
    }
}
