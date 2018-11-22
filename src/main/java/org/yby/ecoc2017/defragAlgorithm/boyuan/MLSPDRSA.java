package org.yby.ecoc2017.defragAlgorithm.boyuan;

import com.google.common.collect.Maps;
import org.jgrapht.GraphPath;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yby.ecoc2017.RSAAlgorithm.FirstFitRSAAlg;
import org.yby.ecoc2017.net.EonEdge;
import org.yby.ecoc2017.net.EonVertex;
import org.yby.ecoc2017.traffic.Service;
import org.yby.ecoc2017.traffic.ServiceAssignment;
import org.yby.ecoc2017.traffic.Timestamp;
import org.yby.ecoc2017.utils.Debug;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * @author yby
 */
public class MLSPDRSA extends FirstFitRSAAlg {

    private static final Logger log = LoggerFactory.getLogger(MLSPDRSA.class);

    /**
     * the time window of prediction in minutes.
     * Generally, it should be the same as the time interval we generate data.
     */
    protected static final int predictionIntervalInMins =20;
    protected static final int minToMs = 60*1000;

    public MLSPDRSA(SimpleWeightedGraph<EonVertex, EonEdge> graph, ArrayList<Service> servicesQueue,
                    ArrayList<Timestamp> servicesOrderedQueue) {
        super(graph, servicesQueue, servicesOrderedQueue);

    }

//    @Override
//    public void allocate() {
//        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
//        for (int index=0; index<getServicesOrderedQueue().size(); index++) {
//            Timestamp timestamp = getServicesOrderedQueue().get(index);
//            Service serviceToBeAssigned = getServicesQueue().get(timestamp.getServiceIndex()-1);
//            // if this timestamp is start-time.
//            if (timestamp.isStartTime()) {
//                EonVertex src = new EonVertex(serviceToBeAssigned.getSource());
//                EonVertex dst = new EonVertex(serviceToBeAssigned.getDestination());
//                GraphPath<EonVertex, EonEdge> path = getDijkstraShortestPath().getPath(src, dst);
//                int subscript = findFirstAvailableSlot(path.getEdgeList(), serviceToBeAssigned.getRequiredSlotNum());
//                // if there exists enough spectrum resource.
//                if (subscript != -1) {
//                    execAllocation(path.getEdgeList(),
//                            subscript,
//                            serviceToBeAssigned);
//                } else {
//                    // if there isn't enough spectrum resource at specific shortest path.
//
//                    // TODO ���ڸĳ���ֵ��������ˣ��������ᴥ���ع�
//                    handleAllocationFail(serviceToBeAssigned, index);
////                    addBlockedService(serviceToBeAssigned);
//                }
//            } else {
//                // if this timestamp is end-time.
//                handleServiceLeave(serviceToBeAssigned.getIndex());
//            }
////            // ����SCֵ
////            double sc = Compactness.linksSC(getGraph().edgeSet());
////            if (sc < Compactness.THRESHOLD) {
////                log.info("value of sc is {}.", sc);
////                List<Timestamp> orderedList = subList(getServicesOrderedQueue(), index+1);
////                MLSPDAlg<EonEdge> mlspdAlg = new MLSPDAlg<>(getGraph(), getCurrentServices(),
////                        generateOccupiedSlotsNum(orderedList));
////                mlspdAlg.defragment(null);
////            }
//        }
//    }


    /**
     * handle blocked service, do defragmentation
     * @param blockedService blocked service
     * @param index the index of ArrayList<Timestamp> of blocked service
     */
    @Override
    protected void handleAllocationFail(Service blockedService, int index) {
        List<Timestamp> orderedList = subList(getServicesOrderedQueue(), index);
        MLSPDAlg<EonEdge> mlspdAlg = new MLSPDAlg<>(getGraph(), getCurrentServices(),
                                                    generateOccupiedSlotsNum(orderedList));
        mlspdAlg.defragment(blockedService);

        EonVertex src = new EonVertex(blockedService.getSource());
        EonVertex dst = new EonVertex(blockedService.getDestination());
        GraphPath<EonVertex, EonEdge> path = getDijkstraShortestPath().getPath(src, dst);
        int subscript = findFirstAvailableSlot(path.getEdgeList(), blockedService.getRequiredSlotNum());
        // if there exists enough spectrum resource.
        if (subscript != -1) {
            super.execAllocation(path.getEdgeList(),
                    subscript,
                    blockedService);
        } else {
            // if still no engouth sprctrum resource after defragmentation
//            super.handleAllocationFail(blockedService, index);
            addBlockedService(blockedService);
        }
    }


    /**
     * @param total
     * @param index
     * @return
     */
    protected List<Timestamp> subList(ArrayList<Timestamp> total, int index) {

        Calendar tmp = Calendar.getInstance();
        tmp.setTimeInMillis(total.get(index).getTime().getTimeInMillis() + predictionIntervalInMins*minToMs);

        int end = index+1;
        while (total.get(end).getTime().before(tmp)) {
            end++;
            if (end == total.size()) {
                break;
            }
        }
        if (end == index+1) {
            throw new RuntimeException("only one or no service between predictionInterval.");
        }
        /* Returns a view of the portion of this list between the specified
           {@code fromIndex}, inclusive, and {@code toIndex}, exclusive. */
        List<Timestamp> list = total.subList(index+1, end);
        return list;

    }


    /**
     *
     */
    protected Map<EonEdge, Double> generateOccupiedSlotsNum(List<Timestamp> orderedList) {
        // initialize occupiedSlotsNum
        Map<EonEdge, Double> occupiedSlotsNum = Maps.newHashMap();
        for (EonEdge edge : getGraph().edgeSet()) {
            occupiedSlotsNum.put(edge, 0.0d);
        }


        // collect current services
        for (ServiceAssignment<EonEdge> servAssign : getCurrentServices().values()) {
            execVirtualServiceAssignment(occupiedSlotsNum, servAssign.getPath(),
                    true, servAssign.getService().getRequiredSlotNum());
        }

        // collect future services
        // key represents service index, value represents path of services
        Map<Integer, List<EonEdge>> futureService = Maps.newHashMap();
        for (int i=0; i<orderedList.size(); i++) {
            List<EonEdge> path;
            Timestamp timestamp = orderedList.get(i);
            int serviceIndex = timestamp.getServiceIndex();
            if (timestamp.isStartTime()) {
                Service service = getServicesQueue().get(serviceIndex-1);
                path = getDijkstraShortestPath().getPath(
                                            new EonVertex(service.getSource()), new EonVertex(service.getDestination()))
                                         .getEdgeList();
                futureService.put(serviceIndex, path);
                execVirtualServiceAssignment(occupiedSlotsNum, path, true, service.getRequiredSlotNum());
            } else {
                int requiredSlotsNum = 0;
                if (getCurrentServices().containsKey(serviceIndex)) {
                    path = getCurrentServices().get(serviceIndex).getPath();
                    requiredSlotsNum = getServicesQueue().get(serviceIndex-1).getRequiredSlotNum();
                } else if (futureService.containsKey(serviceIndex)) {
                    path = futureService.get(serviceIndex);
                    // must be serviceIndex-1
                    requiredSlotsNum = getServicesQueue().get(serviceIndex-1).getRequiredSlotNum();
                    futureService.remove(serviceIndex);
                } else if (getBlockedServices().containsKey(serviceIndex)) {
                    // Do nothing.
                    path = null;
                } else {
                    throw new RuntimeException(
                            "service which neither belongs to current services nor future services leaves.");
                }
                execVirtualServiceAssignment(occupiedSlotsNum, path, false, requiredSlotsNum);

            }
        }

        if (Debug.ENABLE_DEBUG) {
            checkNoLessThanZero(occupiedSlotsNum);
        }
        return occupiedSlotsNum;
    }


    /**
     * verify all double value in input parameters is not less than 0.
     * @param occupiedSlotsNum condition about occupied slots number
     */
    protected void checkNoLessThanZero(Map<EonEdge, Double> occupiedSlotsNum) {
        for (double d : occupiedSlotsNum.values()) {
            if (d < 0 ) {
                throw new RuntimeException("less than zero!");
            }
        }
    }

    /**
     *
     * @param occupiedSlotsNum
     * @param path
     * @param isPlus
     */
    protected void execVirtualServiceAssignment(Map<EonEdge, Double> occupiedSlotsNum,
                                              List<EonEdge> path, boolean isPlus, int requiredSlotsNum) {
        if (path != null) {
            if (isPlus) {
                for (EonEdge edge : path) {
                    int num = requiredSlotsNum;
                    occupiedSlotsNum.put(edge, occupiedSlotsNum.get(edge) + num);
                }
            } else {
                for (EonEdge edge : path) {
                    int num = requiredSlotsNum;
                    occupiedSlotsNum.put(edge, occupiedSlotsNum.get(edge) - num);
                }
            }
        } else {
            // blocked service leave, do nothing.
        }
    }
}
