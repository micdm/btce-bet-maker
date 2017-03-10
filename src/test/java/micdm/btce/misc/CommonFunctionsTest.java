package micdm.btce.misc;

import io.reactivex.Flowable;
import org.junit.Test;

public class CommonFunctionsTest {

    public CommonFunctionsTest() {

    }

    @Test
    public void testGetPrevious() {
        CommonFunctions commonFunctions = new CommonFunctions();
        Flowable.just(1, 2, 3)
            .compose(commonFunctions.getPrevious())
            .test()
            .assertResult(1, 2);
    }

    @Test
    public void testOnly() {
        CommonFunctions commonFunctions = new CommonFunctions();
        commonFunctions.only(1)
            .take(5)
            .test()
            .assertResult(1, 1, 1, 1, 1);
    }
}
