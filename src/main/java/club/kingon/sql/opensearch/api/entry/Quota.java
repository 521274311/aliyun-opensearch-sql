package club.kingon.sql.opensearch.api.entry;

import java.io.Serializable;

/**
 * @author dragons
 * @date 2021/8/12 12:18
 */
public class Quota implements Serializable {

    private static final long serialVersionUID = -5809782578272943999L;

    private Long docSize;

    private Long computeResource;

    private String spec;

    public Long getDocSize() {
        return docSize;
    }

    public void setDocSize(Long docSize) {
        this.docSize = docSize;
    }

    public Long getComputeResource() {
        return computeResource;
    }

    public void setComputeResource(Long computeResource) {
        this.computeResource = computeResource;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }
}
