package org.yby.ecoc2017.RSAAlgorithm;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yby.ecoc2017.net.EonEdge;
import org.yby.ecoc2017.net.EonSlot;
import org.yby.ecoc2017.net.EonVertex;
import org.yby.ecoc2017.traffic.Service;
import org.yby.ecoc2017.traffic.ServiceAssignment;
import org.yby.ecoc2017.traffic.Timestamp;

import java.util.*;

/**
 * The algorithm follows this paper <b></b>.
 * Chinese version:
 * 1、按照时间顺序从业务队列中取出业务，为其计算最短路径
 * 2、在最短路径上按照波长资源编号升序，查看是否有足够的满足约束的波长资源，如果有，则分配;如果没有，则将该业务阻塞。
 * 3、重复步骤1-2。
 * @author yby
 */
public class FirstFitRSAAlg extends BasicRSAAlg<EonVertex, EonEdge> {

    private static final Logger log = LoggerFactory.getLogger(FirstFitRSAAlg.class);
    private DijkstraShortestPath<EonVertex, EonEdge> dijkstraShortestPath;

    public FirstFitRSAAlg(SimpleWeightedGraph<EonVertex, EonEdge> graph, ArrayList<Service> servicesQueue,
                          ArrayList<Timestamp> servicesOrderedQueue) {
        super(graph, servicesQueue, servicesOrderedQueue);
        dijkstraShortestPath = new DijkstraShortestPath<>(graph);
    }


    @Override
    public void allocate() {
        for (int index=0; index<getServicesOrderedQueue().size(); index++) {
            Timestamp timestamp = getServicesOrderedQueue().get(index);
            Service serviceToBeAssigned = getServicesQueue().get(timestamp.getServiceIndex()-1);
            // if this timestamp is start-time.
            if (timestamp.isStartTime()) {
                EonVertex src = new EonVertex(serviceToBeAssigned.getSource());
                EonVertex dst = new EonVertex(serviceToBeAssigned.getDestination());
                GraphPath<EonVertex, EonEdge> path = dijkstraShortestPath.getPath(src, dst);
                int subscript = findFirstAvailableSlot(path.getEdgeList(), serviceToBeAssigned.getRequiredSlotNum());
                // if there exists enough spectrum resource.
                if (subscript != BasicRSAAlg.UNAVAILABLE) {
                    execAllocation(path.getEdgeList(),
                            subscript,
                            serviceToBeAssigned);
                } else {
                    // if there isn't enough spectrum resource at specific shortest path.
                    handleAllocationFail(serviceToBeAssigned, index);
                }
            } else {
                // if this timestamp is end-time.
                handleServiceLeave(serviceToBeAssigned.getIndex());
            }
        }
    }

    /**
     * handle service leave event.
     * @param leaveServiceIndex the index of leave service
     */
    public void handleServiceLeave(int leaveServiceIndex) {
        if (getCurrentServices().containsKey(leaveServiceIndex)) {
            ServiceAssignment<EonEdge> serviceAssignment = getCurrentServices().get(leaveServiceIndex);
            withdrawSlotsAssignment(serviceAssignment);
            putPassedService(
                    removeCurrentService(leaveServiceIndex));
        } else if (getBlockedServices().containsKey(leaveServiceIndex)){
            // TODO
            // Actually, there is nothing to do here. Just for readability.
        } else {
            throw new RuntimeException("The leave service belongs to neither assigned services, nor blocked services.");
        }
    }

    /**
     * withdraw slots resource assigned to serviceAssignment.
     * @param serviceAssignment service assignment information
     */
    private void withdrawSlotsAssignment(ServiceAssignment<EonEdge> serviceAssignment) {
        List<EonEdge> path = serviceAssignment.getPath();
        int serviceIndex = serviceAssignment.getService().getIndex();

        for (EonEdge edge : path) {
            for (int slotIndex=serviceAssignment.getStartIndex();
                 slotIndex<=serviceAssignment.getStartIndex()+serviceAssignment.getService().getRequiredSlotNum()-1;
                 slotIndex++) {
                // must be slotIndex-1
                EonSlot withdrawSlot = edge.getSlots().get(slotIndex-1);
                // if both slotIndex and occupiedServiceIndex are matched.
                if (withdrawSlot.getSlotIndex() == slotIndex &&
                        withdrawSlot.getOccupiedServiceIndex() == serviceIndex) {
                    withdrawSlot.setOccupiedServiceIndex(EonSlot.AVAILABLE);
                } else {
                    throw new RuntimeException("slotIndex or occupiedServiceIndex doesn't match while withdrawing service.");
                }
            }
        }
    }

    /**
     * handle blocked service, add it into blocked service queue.
     * @param blockedService blocked service
     * @param index the index of ArrayList<Timestamp> of blocked service
     */
    protected void handleAllocationFail(Service blockedService, int index) {
        addBlockedService(blockedService);
    }


    public DijkstraShortestPath<EonVertex, EonEdge> getDijkstraShortestPath() {
        return dijkstraShortestPath;
    }
}
