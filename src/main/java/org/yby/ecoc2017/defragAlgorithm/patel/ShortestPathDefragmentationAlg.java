package org.yby.ecoc2017.defragAlgorithm.patel;

import com.google.common.collect.Lists;
import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yby.ecoc2017.RSAAlgorithm.BasicRSAAlg;
import org.yby.ecoc2017.defragAlgorithm.BasicDefragmentationAlg;
import org.yby.ecoc2017.net.EonEdge;
import org.yby.ecoc2017.net.EonVertex;
import org.yby.ecoc2017.traffic.Service;
import org.yby.ecoc2017.traffic.ServiceAssignment;
import org.yby.ecoc2017.utils.Debug;

import java.util.*;

/**
 * The idea of this defragAlgorithm is from <b>201103-OFC-Patel-Defragmentation of Transparent Flexible Optical WDM</b>.
 * Chinese version（前两步和GreedyDefragmentationAlg一样）:
 * 1、以占用波长序列号的降序排列当前存在于网络中的业务。
 * 2、从有序集合中选择第一个业务S，并记录其占用的最低波长编号，记为P。
 * 3、以最少跳数为准计算最短路径。如果在该最短路径上，从最低波长编号开始有足够的可用波长资源，则将该业务重构到该最短路径上。如果没有足够的可用波长
 *    资源，并且当前的最低波长编号还没有业务S占用的最低的波长编号高，则将P=P+1;如果两者已经相等，则不对该业务进行重构。并进行步骤4。
 * 4、从有序业务集合中选择下一个业务，重复步骤2和步骤3。
 * Created by yby on 2017/4/4.
 */
public class ShortestPathDefragmentationAlg<E extends EonEdge> extends BasicDefragmentationAlg<EonVertex, E> {

    private static final Logger log = LoggerFactory.getLogger(ShortestPathDefragmentationAlg.class);

    public ShortestPathDefragmentationAlg(SimpleWeightedGraph<EonVertex,E> graph, TreeMap<Integer, ServiceAssignment<E>> currentServices) {
        super(graph, currentServices);
    }

    @Override
    public void defragment(Service blockedService) {
        // Step 1
        List<ServiceAssignment<E>> orderedSA = maxSlotDescendServices();
        Iterator<ServiceAssignment<E>> iterator = orderedSA.iterator();
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
                addServiceShiftTime(serviceAssignment.getPath().size(),
                                        blockedService.getStartTime());
            } else {
                // if no available choice, then restore
                BasicRSAAlg.tempAllocateService(serviceAssignment);
            }
        }
    }


}
