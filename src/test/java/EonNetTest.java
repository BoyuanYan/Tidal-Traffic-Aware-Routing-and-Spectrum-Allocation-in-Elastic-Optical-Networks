import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yby.ecoc2017.boot.Bootstrap;

/**
 * Created by yby on 2017/4/1.
 */
public class EonNetTest {
    private static final Logger log = LoggerFactory.getLogger(EonNetTest.class);

    @Test
    public void netTest() {
        Bootstrap bootstrap = Bootstrap.getInstance();
        log.info(bootstrap.toString());
        log.info(bootstrap.netParams.networks.toString());
    }
}
