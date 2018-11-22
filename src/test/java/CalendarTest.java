import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * @author yby
 */
public class CalendarTest {

    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
    private static final Logger log = LoggerFactory.getLogger(CalendarTest.class);

    @Test
    public void calendarTest() {
        Calendar startTime = Calendar.getInstance();
        startTime.clear();
        startTime.set(2017, 2, 1, 6, 30, 21);
        log.info(format.format(startTime.getTime()));

        int year = startTime.get(Calendar.YEAR);
        int month = startTime.get(Calendar.MONTH);
        int day = startTime.get(Calendar.DAY_OF_MONTH);
        Calendar startDay = Calendar.getInstance();
//        startDay.clear();
        startDay.set(year, month, day, 0, 0, 0);
        startDay.set(Calendar.MILLISECOND, 0);
        log.info(format.format(startDay.getTime()));
    }
}
