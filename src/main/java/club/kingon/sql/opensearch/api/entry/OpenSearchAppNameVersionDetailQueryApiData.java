package club.kingon.sql.opensearch.api.entry;

import java.io.Serializable;

/**
 * 应用版本详情数据
 * @author dragons
 * @date 2021/8/12 12:50
 */
public class OpenSearchAppNameVersionDetailQueryApiData extends BaseData<OpenSearchAppNameVersionDetailQueryApiData.Result> {

    public static class Result implements Serializable {

        private static final long serialVersionUID = -5809782578272943999L;

        private Schema schema;

        public Schema getSchema() {
            return schema;
        }

        public void setSchema(Schema schema) {
            this.schema = schema;
        }
    }
}
