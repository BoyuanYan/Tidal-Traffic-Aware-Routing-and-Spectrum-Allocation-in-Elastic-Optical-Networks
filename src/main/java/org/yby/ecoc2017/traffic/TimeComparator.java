package org.yby.ecoc2017.traffic;

import java.util.Comparator;

/**
 * Time Comparator ordered by timestamp ascend.
 * Created by yby on 2017/4/3.
 */
public class TimeComparator implements Comparator<Timestamp> {

    @Override
    public int compare(Timestamp o1, Timestamp o2) {
        if (o1.getTime().after(o2.getTime())) {
            return 1;
        } else {
            return -1;
        }
    }
}
