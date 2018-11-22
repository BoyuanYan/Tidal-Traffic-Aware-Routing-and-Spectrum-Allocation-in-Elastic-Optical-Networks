package org.yby.ecoc2017.net;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.yby.ecoc2017.boot.EonNetParams;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Edge class in EON including
 * Created by yby on 2017/3/31.
 */
public class EonEdge extends DefaultWeightedEdge implements Serializable{
    private ArrayList<EonSlot> slots;

    public ArrayList<EonSlot> getSlots() {
        return slots;
    }

    public EonVertex getSource() {
        return (EonVertex) super.getSource();
    }

    public EonVertex getDestination() {
        return (EonVertex) super.getTarget();
    }

    /**
     * calculate the number of occupied slots.
     * @return
     */
    public int occupiedSlotsNum() {
        int count = 0;
        for(EonSlot slot : slots) {
            if (slot.isOccupied()) {
                count++;
            }
        }
        return count;
    }

    /**
     * get the set of available slots index.
     * The iterator of class Treeset is ordered by ascend.
     * @return set
     */
    public TreeSet<Integer> availableSlotsIndexSet() {
        TreeSet<Integer> availableSlotsIndex = Sets.newTreeSet();

        for (EonSlot slot : slots) {
            if (!slot.isOccupied()) {
                availableSlotsIndex.add(slot.getSlotIndex());
            }
        }
        return availableSlotsIndex;
    }

    /**
     * get the array list of available slots index by ascend.
     * @return set
     */
    public ArrayList<Integer> availableSlotsIndexList() {
        ArrayList<Integer> availableSlotsIndex = Lists.newArrayList();
        for (EonSlot slot : slots) {
            if (!slot.isOccupied()) {
                availableSlotsIndex.add(slot.getSlotIndex());
            }
        }
        return availableSlotsIndex;
    }

    /**
     * get the occupied condition per slot.
     * @return list of occupied slots index
     */
    public ArrayList<Integer> occupiedSlotsIndex() {
        ArrayList<Integer> list = Lists.newArrayList();
        for(EonSlot slot : slots) {
            if (slot.isOccupied()) {
                list.add(slot.getOccupiedServiceIndex());
            }
        }
        return list;
    }

    /**
     * This non-parameter constructor will be called by default When Graph.addEdge() is called in JgrapT package.
     */
    public EonEdge() {
        slots = Lists.newArrayListWithCapacity(EonNetParams.getInstance().slotNum);
        for (int i=0;i<EonNetParams.getInstance().slotNum;i++) {
            // slot index starts from 1.
            slots.add(new EonSlot(i+1));
        }
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("source", getSource())
                .add("destination", getTarget())
                .toString();
    }

//    // copy deny.
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof EonEdge)) return false;
//        EonEdge edge = (EonEdge) o;
//        //TODO slots must be located at the same address.
//        return Objects.equal(getSlots(), edge.getSlots());
//    }
//
//    // TODO must verify this !!!
//    @Override
//    public int hashCode() {
//        return this.getSource().hashCode() << 16 + this.getTarget().hashCode();
//    }

}
