package org.yby.ecoc2017.defragAlgorithm.patel;

import org.yby.ecoc2017.net.EonEdge;
import org.yby.ecoc2017.traffic.ServiceAssignment;

import java.util.Comparator;

/**
 * sort serviceAssignments by comparing the max slot index occupied by descend.
 * @author yby
 */
public class MaxSlotDescendComparator<E extends EonEdge> implements Comparator<ServiceAssignment<E>> {

    @Override
    public int compare(ServiceAssignment<E> o1, ServiceAssignment<E> o2) {
        int maxSlotNum1 = o1.getStartIndex()+o1.getService().getRequiredSlotNum() - 1;
        int maxSlotNum2 = o2.getStartIndex()+o2.getService().getRequiredSlotNum() - 1;
        if (maxSlotNum1 > maxSlotNum2) {
            return -1;
        } else if (maxSlotNum1 < maxSlotNum2) {
            return 1;
        } else {
            return 0;
        }
    }
}
