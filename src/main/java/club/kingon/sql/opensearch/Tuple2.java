package club.kingon.sql.opensearch;

/**
 * @author dragons
 * @date 2020/12/22 18:42
 */
public class Tuple2<T, P> implements Cloneable {
    public T t1;
    public P t2;

    private Tuple2() {
    }

    private Tuple2(T t1, P t2) {
        this.t1 = t1;
        this.t2 = t2;
    }

    public static <T, P>Tuple2<T, P> of(T t, P p) {
        return new Tuple2<>(t, p);
    }

    @Override
    public String toString() {
        return "t1:" + t1 + ", " + t2;
    }
}
