package org.yby.ecoc2017.traffic;

import java.io.Serializable;
import java.util.Calendar;

/**
 * Service.
 * Created by yby on 2017/4/1.
 */
public class Service implements Serializable {
    //
    private int index;

    private Calendar startTime;

    private Calendar endTime;

    private int requiredSlotNum;

    private int source;

    private int destination;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Service service = (Service) o;

        return index == service.index;

    }

    @Override
    public int hashCode() {
        return index;
    }


    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Calendar getStartTime() {
        return startTime;
    }

    public void setStartTime(Calendar startTime) {
        this.startTime = startTime;
    }

    public Calendar getEndTime() {
        return endTime;
    }

    public void setEndTime(Calendar endTime) {
        this.endTime = endTime;
    }

    public int getRequiredSlotNum() {
        return requiredSlotNum;
    }

    public void setRequiredSlotNum(int retuiredSlotNum) {
        this.requiredSlotNum = retuiredSlotNum;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public int getDestination() {
        return destination;
    }

    public void setDestination(int destination) {
        this.destination = destination;
    }

    public Service(int index, Calendar startTime, Calendar endTime, int retuiredSlotNum, int source, int destination) {
        this.index = index;
        this.startTime = startTime;
        this.endTime = endTime;
        this.requiredSlotNum = retuiredSlotNum;
        this.source = source;
        this.destination = destination;
    }


}
