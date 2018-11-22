package org.yby.ecoc2017.defragAlgorithm.boyuan;

import org.yby.ecoc2017.boot.Bootstrap;
import org.yby.ecoc2017.boot.EonNetParams;
import org.yby.ecoc2017.boot.SingleEonNet;
import org.yby.ecoc2017.net.EonEdge;
import org.yby.ecoc2017.traffic.ServiceAssignment;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by yby on 2017/4/10.
 */
public class EdgeWeightDescendComparator<E extends EonEdge> implements Comparator<ServiceAssignment<E>> {

    private Map<E, Double> occupiedSlotsNum;

    public EdgeWeightDescendComparator(Map<E, Double> occupiedSlotsNum) {
        this.occupiedSlotsNum = occupiedSlotsNum;
    }
    public final static Set<Integer> businessAreaSet= Bootstrap.getInstance().netParams.networks.get(0).getBusinessAreaSet();
    public final static Set<Integer> residentialAreaSet= Bootstrap.getInstance().netParams.networks.get(0).getResidentialArea();

    /**
     * 按照service占用的path的权值降序排序，如果权值相同，则按照path跳数的升序排序。
     * @param o1
     * @param o2
     * @return
     */
    @Override
    public int compare(ServiceAssignment<E> o1, ServiceAssignment<E> o2) {
        double firstWeight = 0;
        double secondWeight = 0;
        for (E e: o1.getPath()) {
            firstWeight = firstWeight + occupiedSlotsNum.get(e);
        }
        for (E e : o2.getPath()) {
            secondWeight = secondWeight + occupiedSlotsNum.get(e);
        }

        if (firstWeight > secondWeight) {
            return -1;
        } else if (firstWeight < secondWeight) {
            return 1;
        } else {
            if (o1.getPath().size() > o2.getPath().size()) {
                return 1;
            } else if (o1.getPath().size() < o2.getPath().size()) {
                return -1;
            } else {
                return 0;
            }
        }

//        int pri_1 = calPrivilege(o1);
//        int pri_2 = calPrivilege(o2);
//        if (pri_1 > pri_2) {
//            return -1;
//        } else if (pri_1 < pri_2) {
//            return 1;
//        } else {
//            if (firstWeight > secondWeight) {
//                return -1;
//            } else if (firstWeight < secondWeight) {
//                return 1;
//            } else {
//                if (o1.getStartIndex() > o2.getStartIndex()) {
//                    return -1;
//                } else if (o1.getStartIndex() < o2.getStartIndex()) {
//                    return 1;
//                } else {
//                    return 0;
//                }
//            }
//        }
    }

    public static <E extends EonEdge> int calPrivilege(ServiceAssignment<E> sa) {
        if (businessAreaSet.contains(sa.getService().getSource())) {
            if (businessAreaSet.contains(sa.getService().getDestination())) {
                return 3;
            } else if (residentialAreaSet.contains(sa.getService().getDestination())) {
                return 2;
            } else {
                throw new RuntimeException("node neither belongs to business area, nor residential area.");
            }
        } else if (residentialAreaSet.contains(sa.getService().getSource())) {
            if (businessAreaSet.contains(sa.getService().getDestination())) {
                return 2;
            } else if (residentialAreaSet.contains(sa.getService().getDestination())) {
                return 1;
            } else {
                throw new RuntimeException("node neither belongs to business area, nor residential area.");
            }
        }else {
            throw new RuntimeException("node neither belongs to business area, nor residential area.");
        }
    }
}
