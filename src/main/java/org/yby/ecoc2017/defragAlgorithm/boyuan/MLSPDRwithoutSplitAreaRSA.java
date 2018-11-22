package org.yby.ecoc2017.defragAlgorithm.boyuan;

import org.jgrapht.GraphPath;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.yby.ecoc2017.net.EonEdge;
import org.yby.ecoc2017.net.EonVertex;
import org.yby.ecoc2017.traffic.Service;
import org.yby.ecoc2017.traffic.Timestamp;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yby on 2017/4/20.
 */
public class MLSPDRwithoutSplitAreaRSA  extends MLSPDRSA  {
    public MLSPDRwithoutSplitAreaRSA(SimpleWeightedGraph<EonVertex, EonEdge> graph, ArrayList<Service> servicesQueue, ArrayList<Timestamp> servicesOrderedQueue) {
        super(graph, servicesQueue, servicesOrderedQueue);
    }

    /**
     * handle blocked service, do defragmentation
     * @param blockedService blocked service
     * @param index the index of ArrayList<Timestamp> of blocked service
     */
    @Override
    protected void handleAllocationFail(Service blockedService, int index) {

        List<Timestamp> orderedList = subList(getServicesOrderedQueue(), index);
        MLSPDRwithoutSplitAreaAlg<EonEdge> mlspdRwithoutSplitAreaAlg = new MLSPDRwithoutSplitAreaAlg<>(
                getGraph(), getCurrentServices(),
                generateOccupiedSlotsNum(orderedList));
        mlspdRwithoutSplitAreaAlg.defragment(blockedService);

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
}
