package micdm.btce.misc;

import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;

import java.util.ArrayList;
import java.util.Iterator;

public class CommonFunctions {

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
