package org.yby.ecoc2017.defragAlgorithm.boyuan;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.KShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.KShortestPaths;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.yby.ecoc2017.RSAAlgorithm.BasicRSAAlg;
import org.yby.ecoc2017.net.EonEdge;
import org.yby.ecoc2017.net.EonVertex;
import org.yby.ecoc2017.traffic.Service;
import org.yby.ecoc2017.traffic.ServiceAssignment;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by yby on 2017/4/20.
 * 没有区域的划分,但是有边权的改变.
 */
public class MLSPDRwithoutSplitAreaAlg <E extends EonEdge> extends MLSPDRAlg<E>  {
    public MLSPDRwithoutSplitAreaAlg(SimpleWeightedGraph<EonVertex, E> graph, TreeMap<Integer, ServiceAssignment<E>> currentServices, Map<E, Double> occupiedSlotsNum) {
        super(graph, currentServices, occupiedSlotsNum);
    }

    @Override
    public void defragment(Service blockedService) {
        changeEdgeWeight();
        // sort
        List<ServiceAssignment<E>> orderedSA = maxSlotDescendServices();

        int k = 2;
        KShortestPathAlgorithm<EonVertex, E> kShortestPathAlgorithm = new KShortestPaths<EonVertex, E>(getGraph(), k);

        for (int index=0; index<orderedSA.size(); index++) {
            ServiceAssignment<E> serviceAssignment = orderedSA.get(index);
            EonVertex source = new EonVertex(serviceAssignment.getService().getSource());
            EonVertex destination = new EonVertex(serviceAssignment.getService().getDestination());
            List<GraphPath<EonVertex, E>> paths = kShortestPathAlgorithm.getPaths(source, destination);
            for (GraphPath<EonVertex, E> graphPath : paths) {
                int minOccupiedSlotIndex = serviceAssignment.getStartIndex();
                List<E> newPath = graphPath.getEdgeList();
                BasicRSAAlg.tempReleaseService(serviceAssignment);
                int shiftSlotStartIndex = BasicRSAAlg.findFirstAvailableSlot(newPath,
                        serviceAssignment.getService().getRequiredSlotNum());
                if (shiftSlotStartIndex != BasicRSAAlg.UNAVAILABLE &&
                        shiftSlotStartIndex < minOccupiedSlotIndex) {
                    // if there is enough resource for shifting.
                    BasicRSAAlg.shiftService(serviceAssignment, shiftSlotStartIndex, newPath);
                    if (blockedService != null) {// 这一步判断是为了阈值重构触发
                        addServiceShiftTime(serviceAssignment.getPath().size(),
                                blockedService.getStartTime());
                    }
                    // 如果调整成功,则跳出paths的循环
                    break;
                } else {
                    // if no available choice, then restore
                    BasicRSAAlg.tempAllocateService(serviceAssignment);
                }
            }
        }

        restoreEdgeWeight();
    }
}
