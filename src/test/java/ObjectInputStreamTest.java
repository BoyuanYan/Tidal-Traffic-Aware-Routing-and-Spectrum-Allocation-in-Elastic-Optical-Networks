import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yby.ecoc2017.traffic.Service;
import org.yby.ecoc2017.traffic.Timestamp;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

/**
 * Created by yby on 2017/4/11.
 */
public class ObjectInputStreamTest {
    private static final Logger log = LoggerFactory.getLogger(ObjectInputStreamTest.class);

    @Test
    public void objInputTest() throws Exception {
        File file = new File("USLIKENET_2017-03-01_1days_ServiceTimestamp.data");
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file));
        ArrayList<Timestamp> services = (ArrayList<Timestamp>)inputStream.readObject();

        log.info("The size of services is {}.", services.size());
    }
}
