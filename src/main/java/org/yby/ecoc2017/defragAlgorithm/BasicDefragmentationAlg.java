package org.yby.ecoc2017.defragAlgorithm;

import com.google.common.collect.Lists;
import org.jgrapht.Graph;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yby.ecoc2017.defragAlgorithm.patel.MaxSlotDescendComparator;
import org.yby.ecoc2017.net.EonEdge;
import org.yby.ecoc2017.net.EonVertex;
import org.yby.ecoc2017.traffic.Service;
import org.yby.ecoc2017.traffic.ServiceAssignment;
import org.yby.ecoc2017.utils.Debug;

import java.util.*;

/**
 * Basic function and member that Defragmentation Algorithm should have.
 * @author yby
 */
public abstract class BasicDefragmentationAlg<V extends EonVertex, E extends EonEdge> {

    private static final Logger log = LoggerFactory.getLogger(BasicDefragmentationAlg.class);

    // graph with shallow copy
    private SimpleWeightedGraph<V, E> graph;

    private TreeMap<Integer, ServiceAssignment<E>> currentServices;

    // key of Pair represents time of spectrum migration, value of Pair represents number of spectrum migration.
    // There is no need to use Map structure.
    public static ArrayList<Pair<Calendar, Integer>> serviceShiftTimeRecords = Lists.newArrayList();

    public static int serviceShiftTime = 0;

    public void addServiceShiftTime(int time, Calendar happenedTime) {
        serviceShiftTime = serviceShiftTime + time;
        serviceShiftTimeRecords.add(new Pair<>(happenedTime, time));
    }

    // due to blocked-trigger, it is necessary to hold the handler of blockedService
    private Service blockedService;

//    protected void addServiceShiftTime() {=
//        serviceShiftTime++;
//    }

    /**
     * trigger defragmentation.
     * @param blockedService blocked service
     * @return true if defragmentation succeeds, false else.
     */
    public abstract void defragment(Service blockedService);

    protected BasicDefragmentationAlg(final SimpleWeightedGraph<V, E> graph, final TreeMap<Integer, ServiceAssignment<E>> currentServices) {
        this.graph = graph;
        this.currentServices = currentServices;
//        this.serviceShiftTime = 0;
//        this.serviceShiftTimeRecords = Lists.newArrayList();
    }

    public SimpleWeightedGraph<V, E> getGraph() {
        return graph;
    }

    public void setGraph(SimpleWeightedGraph<V, E> graph) {
        this.graph = graph;
    }

    public TreeMap<Integer, ServiceAssignment<E>> getCurrentServices() {
        return currentServices;
    }

    public void setCurrentServices(TreeMap<Integer, ServiceAssignment<E>> currentServices) {
        this.currentServices = currentServices;
    }

    /**
     * Step 1: order the set of current service assignment by max occupied slot descend.
     * @return ordered list of serviceAssignment
     */
    protected List<ServiceAssignment<E>> maxSlotDescendServices() {
        LinkedList<ServiceAssignment<E>> maxSlotDescendServices = Lists.newLinkedList();
        Iterator<ServiceAssignment<E>> orderedIterator = getCurrentServices().values().iterator();
        while (orderedIterator.hasNext()) {
            ServiceAssignment<E> serviceAssignment = orderedIterator.next();
            maxSlotDescendServices.add(serviceAssignment);
        }
        maxSlotDescendServices.sort(new MaxSlotDescendComparator<E>());

        if (Debug.ENABLE_DEBUG) {
            if (maxSlotDescendServices.size() >1) {
                ServiceAssignment<E> first = maxSlotDescendServices.getFirst();
                ServiceAssignment<E> second = maxSlotDescendServices.get(1);
                int firstMax = first.getStartIndex() + first.getService().getRequiredSlotNum() - 1;
                int secondMax = second.getStartIndex() + second.getService().getRequiredSlotNum() - 1;
                if (firstMax >= secondMax) {
//                log.warn("remove check code for testing ordered list");
                } else {
                    throw new RuntimeException("wrong order.");
                }
            }
        }

        return maxSlotDescendServices;
    }

    public static ArrayList<Pair<Calendar, Integer>> getServiceShiftTimeRecords() {
        return serviceShiftTimeRecords;
    }

    public static int getServiceShiftTime() {
        return serviceShiftTime;
    }
}
