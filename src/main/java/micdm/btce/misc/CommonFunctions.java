package micdm.btce.misc;

import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.functions.Predicate;

import java.util.ArrayList;
import java.util.Iterator;

public class CommonFunctions {

    public boolean toTrue(Object o) {
        return true;
    }

    public boolean toFalse(Object o) {
        return false;
    }

    public boolean isTrue(boolean value) {
        return value;
    }

    public boolean isFalse(boolean value) {
        return !value;
    }

    public <T> Predicate<T> isEqual(T value) {
        return value::equals;
    }

    public <T> FlowableTransformer<T, T> getPrevious() {
        return flowable -> flowable
            .scan(new ArrayList<T>(), (accumulated, value) -> {
                if (accumulated.isEmpty()) {
                    accumulated.add(0, null);
                    accumulated.add(1, value);
                } else {
                    accumulated.set(0, accumulated.get(1));
                    accumulated.set(1, value);
                }
                return accumulated;
            })
            .skip(2)
            .map(list -> list.get(0));
    }

    public <T> Flowable<T> only(T value) {
        return Flowable.fromIterable(() ->
            new Iterator<T>() {
                @Override
                public boolean hasNext() {
                    return true;
                }
                @Override
                public T next() {
                    return value;
                }
            }
        );
    }
}
