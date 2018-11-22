package org.yby.ecoc2017.traffic;

import java.io.Serializable;
import java.util.Calendar;

/**
 * Created by yby on 2017/4/3.
 */
public class Timestamp implements Serializable {
    private Calendar time;
    // true - startTime, false - endTime
    private boolean isStartTime;
    // service index.
    private int serviceIndex;

    public Timestamp(Calendar time, boolean isStartTime, int serviceIndex) {
        this.time = time;
        this.isStartTime = isStartTime;
        this.serviceIndex = serviceIndex;
    }

    public Calendar getTime() {
        return time;
    }

    public void setTime(Calendar time) {
        this.time = time;
    }

    public boolean isStartTime() {
        return isStartTime;
    }

    public void setIsStartTime(boolean isStartTime) {
        this.isStartTime = isStartTime;
    }


    public int getServiceIndex() {
        return serviceIndex;
    }

    public void setServiceIndex(final int serviceIndex) {
        this.serviceIndex = serviceIndex;
    }
}
