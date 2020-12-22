package club.kingon.sql.opensearch.util;

/**
 * @author dragons
 * @date 2020/12/22 18:42
 */
class Tuple2<T, P> {
    T t1;
    P t2;

    private Tuple2() {
    }

    private Tuple2(T t1, P t2) {
        this.t1 = t1;
        this.t2 = t2;
    }

    static <T, P>Tuple2<T, P> of(T t, P p) {
        return new Tuple2<>(t, p);
    }

    @Override
    public String toString() {
        return "t1:" + t1 + ", " + t2;
    }
}
