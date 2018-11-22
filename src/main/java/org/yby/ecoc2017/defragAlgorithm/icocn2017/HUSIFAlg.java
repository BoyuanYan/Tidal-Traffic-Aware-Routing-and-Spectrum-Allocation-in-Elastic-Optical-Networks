package org.yby.ecoc2017.defragAlgorithm.icocn2017;

import org.jgrapht.graph.SimpleWeightedGraph;
import org.yby.ecoc2017.RSAAlgorithm.BasicRSAAlg;
import org.yby.ecoc2017.defragAlgorithm.patel.ShortestPathDefragmentationAlg;
import org.yby.ecoc2017.net.EonEdge;
import org.yby.ecoc2017.net.EonVertex;
import org.yby.ecoc2017.traffic.Service;
import org.yby.ecoc2017.traffic.ServiceAssignment;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by yby on 2017/5/12.
 * 201311-ICC-Bandwidth defragmentation in dynamic elastic optical network with minimum traffic disruptions
 * 按照被占用的slot编号的降序选择被重构的业务，然后依次将目前正在使用该slot的业务选出来，一直到达到一定的比例γ。
 */
public class HUSIFAlg<E extends EonEdge> extends ShortestPathDefragmentationAlg<E> {

    public HUSIFAlg(SimpleWeightedGraph<EonVertex, E> graph, TreeMap<Integer, ServiceAssignment<E>> currentServices) {
        super(graph, currentServices);
        serviceShiftTime = 0;
        checkedServiceNum = 0;
    }

    int serviceShiftTime;
    int checkedServiceNum;

    @Override
    public void defragment(Service blockedService) {
        // 获取被重构业务的个数
        int num = (int)Math.ceil(Configuration.gama * getCurrentServices().size());
        // Step 1
        // 按照被占用slot的降序排序
        List<ServiceAssignment<E>> orderedSA = maxSlotDescendServices();
        // 取其前num个进行重构。
        List<ServiceAssignment<E>> subOrderedSA = orderedSA.subList(0, num);
        Iterator<ServiceAssignment<E>> iterator = subOrderedSA.iterator();
        checkedServiceNum = num;
        // Step 2
        while (iterator.hasNext()) {
            ServiceAssignment<E> serviceAssignment = iterator.next();
            int minOccupiedSlotIndex = serviceAssignment.getStartIndex();
            List<E> path = serviceAssignment.getPath();
            BasicRSAAlg.tempReleaseService(serviceAssignment);
            int shiftSlotStartIndex = BasicRSAAlg.findFirstAvailableSlot(path,
                    serviceAssignment.getService().getRequiredSlotNum());
            if (shiftSlotStartIndex != BasicRSAAlg.UNAVAILABLE &&
                    shiftSlotStartIndex < minOccupiedSlotIndex) {
                // if there is enough resource for shifting.
                BasicRSAAlg.shiftService(serviceAssignment, shiftSlotStartIndex);
                serviceShiftTime++;
//                addServiceShiftTime(serviceAssignment.getPath().size(),
//                        blockedService.getStartTime());
            } else {
                // if no available choice, then restore
                BasicRSAAlg.tempAllocateService(serviceAssignment);
            }
        }
    }

    /**
     * 获取本次重构的业务搬移次数
     * @return
     */
    int getShiftTime(){
        return serviceShiftTime;
    }

    /**
     * 获取本次重构，需要进行重构的业务数量，注意，并不是实际上被重构的业务数量。
     * @return
     */
    int getCheckedServiceNum() {
        return checkedServiceNum;
    }
}
