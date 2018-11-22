package org.yby.ecoc2017.defragAlgorithm.boyuan;

import org.jgrapht.GraphPath;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.yby.ecoc2017.defragAlgorithm.patel.ShortestPathDefragmentationAlg;
import org.yby.ecoc2017.net.EonEdge;
import org.yby.ecoc2017.net.EonVertex;
import org.yby.ecoc2017.traffic.Service;
import org.yby.ecoc2017.traffic.Timestamp;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yby on 2017/4/19.
 * 01表示先按顺序来,再用机器学习扫一遍
 */
public class Double01 extends MLSPDRSA {
    public Double01(SimpleWeightedGraph<EonVertex, EonEdge> graph, ArrayList<Service> servicesQueue, ArrayList<Timestamp> servicesOrderedQueue) {
        super(graph, servicesQueue, servicesOrderedQueue);
    }

    @Override
    protected void handleAllocationFail(Service blockedService, int index) {
        ShortestPathDefragmentationAlg<EonEdge> spd =
                new ShortestPathDefragmentationAlg<>(getGraph(), getCurrentServices());
        spd.defragment(blockedService);
        List<Timestamp> orderedList = subList(getServicesOrderedQueue(), index);
        MLSPDAlg<EonEdge> mlspd = new MLSPDRAlg<>(
                getGraph(), getCurrentServices(), generateOccupiedSlotsNum(orderedList));
        mlspd.defragment(blockedService);

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
