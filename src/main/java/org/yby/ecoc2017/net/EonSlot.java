package org.yby.ecoc2017.net;

import java.io.Serializable;

/**
 * The slot resource in edges.
 * Created by yby on 2017/4/1.
 */
public class EonSlot implements Serializable {

    public static final int AVAILABLE = 0;
    private int occupiedNum; // the number of being occupied by different services
    private int occupiedServiceIndex; // the index of service if occupied now, otherwise, equals 0
    private int slotIndex;

    public boolean isOccupied() {
        return occupiedServiceIndex != AVAILABLE;
    }

    /**
     * occupiedNum plus 1.
     */
    public void accumulate() {
        occupiedNum++;
    }

    public int getOccupiedNum() {
        return occupiedNum;
    }

    public int getOccupiedServiceIndex() {
        return occupiedServiceIndex;
    }

    public void setOccupiedServiceIndex(int occupiedServiceIndex) {
        this.occupiedServiceIndex = occupiedServiceIndex;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public EonSlot(final int slotIndex) {
        occupiedNum = 0;
        occupiedServiceIndex = AVAILABLE;
        this.slotIndex = slotIndex;
    }
}
