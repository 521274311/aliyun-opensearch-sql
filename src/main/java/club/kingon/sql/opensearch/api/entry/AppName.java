package club.kingon.sql.opensearch.api.entry;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * @author dragons
 * @date 2021/8/12 12:15
 */
public class AppName implements Serializable {

    private static final long serialVersionUID = -5809782578272943999L;

    private String id;

    private String instanceId;

    private String commodityCode;

    private String resourceGroupId;

    private String name;

    private String currentVersion;

    private Long switchedTime;

    private Quota quota;

    private Integer chargingWay;

    private String type;

    private List<String> versions;

    private String chargeType;

    private String expireOn;

    private String description;

    private Integer produced;

    private Integer hasPendingQuotaReviewTask;

    private Long created;

    private Long updated;

    private Integer beaconAppCreated;

    private String lockMode;

    private String status;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getCommodityCode() {
        return commodityCode;
    }

    public void setCommodityCode(String commodityCode) {
        this.commodityCode = commodityCode;
    }

    public String getResourceGroupId() {
        return resourceGroupId;
    }

    public void setResourceGroupId(String resourceGroupId) {
        this.resourceGroupId = resourceGroupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public Long getSwitchedTime() {
        return switchedTime;
    }

    public void setSwitchedTime(Long switchedTime) {
        this.switchedTime = switchedTime;
    }

    public Quota getQuota() {
        return quota;
    }

    public void setQuota(Quota quota) {
        this.quota = quota;
    }

    public Integer getChargingWay() {
        return chargingWay;
    }

    public void setChargingWay(Integer chargingWay) {
        this.chargingWay = chargingWay;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getVersions() {
        return versions;
    }

    public void setVersions(List<String> versions) {
        this.versions = versions;
    }

    public String getChargeType() {
        return chargeType;
    }

    public void setChargeType(String chargeType) {
        this.chargeType = chargeType;
    }

    public String getExpireOn() {
        return expireOn;
    }

    public void setExpireOn(String expireOn) {
        this.expireOn = expireOn;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getProduced() {
        return produced;
    }

    public void setProduced(Integer produced) {
        this.produced = produced;
    }

    public Integer getHasPendingQuotaReviewTask() {
        return hasPendingQuotaReviewTask;
    }

    public void setHasPendingQuotaReviewTask(Integer hasPendingQuotaReviewTask) {
        this.hasPendingQuotaReviewTask = hasPendingQuotaReviewTask;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public Long getUpdated() {
        return updated;
    }

    public void setUpdated(Long updated) {
        this.updated = updated;
    }

    public Integer getBeaconAppCreated() {
        return beaconAppCreated;
    }

    public void setBeaconAppCreated(Integer beaconAppCreated) {
        this.beaconAppCreated = beaconAppCreated;
    }

    public String getLockMode() {
        return lockMode;
    }

    public void setLockMode(String lockMode) {
        this.lockMode = lockMode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppName appName = (AppName) o;
        return Objects.equals(id, appName.id) && Objects.equals(instanceId, appName.instanceId) && Objects.equals(commodityCode, appName.commodityCode) && Objects.equals(resourceGroupId, appName.resourceGroupId) && Objects.equals(name, appName.name) && Objects.equals(currentVersion, appName.currentVersion) && Objects.equals(switchedTime, appName.switchedTime) && Objects.equals(quota, appName.quota) && Objects.equals(chargingWay, appName.chargingWay) && Objects.equals(type, appName.type) && Objects.equals(versions, appName.versions) && Objects.equals(chargeType, appName.chargeType) && Objects.equals(expireOn, appName.expireOn) && Objects.equals(description, appName.description) && Objects.equals(produced, appName.produced) && Objects.equals(hasPendingQuotaReviewTask, appName.hasPendingQuotaReviewTask) && Objects.equals(created, appName.created) && Objects.equals(updated, appName.updated) && Objects.equals(beaconAppCreated, appName.beaconAppCreated) && Objects.equals(lockMode, appName.lockMode) && Objects.equals(status, appName.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, instanceId, commodityCode, resourceGroupId, name, currentVersion, switchedTime, quota, chargingWay, type, versions, chargeType, expireOn, description, produced, hasPendingQuotaReviewTask, created, updated, beaconAppCreated, lockMode, status);
    }
}
