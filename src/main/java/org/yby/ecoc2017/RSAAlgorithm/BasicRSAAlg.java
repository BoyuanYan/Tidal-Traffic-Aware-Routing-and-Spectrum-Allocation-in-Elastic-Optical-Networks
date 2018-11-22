package org.yby.ecoc2017.RSAAlgorithm;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yby.ecoc2017.net.EonEdge;
import org.yby.ecoc2017.net.EonSlot;
import org.yby.ecoc2017.net.EonVertex;
import org.yby.ecoc2017.traffic.Service;
import org.yby.ecoc2017.traffic.ServiceAssignment;
import org.yby.ecoc2017.traffic.Timestamp;
import org.yby.ecoc2017.utils.Debug;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Basic RSA Algorithm.
 * @author yby
 */
public abstract class BasicRSAAlg<V, E extends EonEdge> {

    private static final Logger log = LoggerFactory.getLogger(BasicRSAAlg.class);

    private SimpleWeightedGraph<V, E> graph;

    // services queue with service index ascend
    private ArrayList<Service> servicesQueue;

    // services queue with service startTime/endTime ascend, so each service will appear twice.
    private ArrayList<Timestamp> servicesOrderedQueue;
    // current services in Graph ordered by ascend. The key represents service index. Be called when new Service arrivals, and old service leaves.
    private TreeMap<Integer, ServiceAssignment<E>> currentServices;
    // blocked services in Graph ordered by ascend. The key represents service index. Be called when new arrived service failures.
    private TreeMap<Integer, Service> blockedServices;
    // passed services in Graph ordered by ascend. The key represents service index. Be called when withdrawing current service.
    private TreeMap<Integer, ServiceAssignment<E>> passedServices;

    public static final int UNAVAILABLE = -1;

    /**
     * allocate spectrum resources to services' demand.
     */
    public abstract void allocate();

    public SimpleWeightedGraph<V, E> getGraph() {
        return graph;
    }

    public void setGraph(final SimpleWeightedGraph<V, E> graph) {
        this.graph = graph;
    }

    public ArrayList<Service> getServicesQueue() {
        return servicesQueue;
    }

    public void setServicesQueue(final ArrayList<Service> servicesQueue) {
        this.servicesQueue = servicesQueue;
    }

    public TreeMap<Integer, ServiceAssignment<E>> getCurrentServices() {
        return currentServices;
    }

    public TreeMap<Integer, Service> getBlockedServices() {
        return blockedServices;
    }

    public ArrayList<Timestamp> getServicesOrderedQueue() {
        return servicesOrderedQueue;
    }

    public TreeMap<Integer, ServiceAssignment<E>> getPassedServices() {
        return passedServices;
    }

    public void setServicesOrderedQueue(final ArrayList<Timestamp> servicesOrderedQueue) {
        this.servicesOrderedQueue = servicesOrderedQueue;
    }

    /**
     * Put new service to current services map.
     * @param serviceAssignment
     */
    public void putCurrentService(ServiceAssignment<E> serviceAssignment) {
        if (currentServices.containsKey(serviceAssignment.getService().getIndex())) {
            throw new RuntimeException("allocate the same services twice.");
        } else {
            currentServices.put(serviceAssignment.getService().getIndex(), serviceAssignment);
        }
    }

    /**
     * remove a service from current service map.
     * @param serviceIndex
     * @return
     */
    public ServiceAssignment<E> removeCurrentService(int serviceIndex) {
        return currentServices.remove(serviceIndex);
    }

    /**
     * put a passed service.
     * @param serviceAssignment
     */
    public void putPassedService(ServiceAssignment<E> serviceAssignment) {
        if (passedServices.containsKey(serviceAssignment.getService().getIndex())) {
            throw new RuntimeException("put duplicate passed service to passedServices");
        } else {
            passedServices.put(serviceAssignment.getService().getIndex(), serviceAssignment);
        }
    }


    public void addBlockedService(Service service) {
        blockedServices.put(service.getIndex(), service);
    }

    public BasicRSAAlg(final SimpleWeightedGraph<V, E> graph, final ArrayList<Service> servicesQueue, final ArrayList<Timestamp> servicesOrderedQueue) {
        this.graph = graph;
        this.servicesQueue = servicesQueue;
        this.servicesOrderedQueue = servicesOrderedQueue;
        this.currentServices = Maps.newTreeMap();
        this.blockedServices = Maps.newTreeMap();
        this.passedServices = Maps.newTreeMap();
    }

//    /**
//     * release the slots of new path occupied by specific service temporarily, the new path is different from the
//     * old one included by serviceAssignment.
//     * @param serviceAssignment service assignment information
//     * @param newPath new path
//     * @param <Eg> subclass of EonEdge
//     * @return the intersection of old path and new path
//     */
//    public static <Eg extends EonEdge> ArrayList<Eg> tempReleaseService(
//            final ServiceAssignment<Eg> serviceAssignment, final List<Eg> newPath) {
//        List<Eg> oldPath = serviceAssignment.getPath();
//        ArrayList<Eg> intersection = intersectionWithNewList(oldPath, newPath);
//        if (!intersection.isEmpty()) {
//            if (Debug.ENABLE_DEBUG) {
//                log.info("intersection of old path and new path is not empty");
//            }
//            int startSlotIndex = serviceAssignment.getStartIndex();
//            int endSlotIndex = startSlotIndex + serviceAssignment.getService().getRequiredSlotNum()-1;
//            for (Eg edge : intersection) {
//                List<EonSlot> slots = edge.getSlots();
//                for (int index = startSlotIndex; index <= endSlotIndex; index++) {
//                    slots.get(index-1).setOccupiedServiceIndex(EonSlot.AVAILABLE);
//                }
//            }
//        }
//        return intersection;
//
//    }

    private static <Eg extends EonEdge> ArrayList<Eg> intersectionWithNewList(List<Eg> oldPath, List<Eg> newPath) {
        ArrayList<Eg> intersection = Lists.newArrayList();
        for (Eg oldEdge : oldPath) {
            for (Eg newEdge : newPath) {
                // bacause there is the same edge instance all the way, so if the memory addresses of oldEdge and
                // newEdge are the same, they are the same edge.
                if (oldEdge == newEdge) {
                    intersection.add(newEdge);
                }
            }
        }
        return intersection;
    }

    /**
     * release the slots occupied by specific service temporarily to avoid original service affecting shift operation.
     * Notification: this operation will not affect anything except edges occupied by specific service.
     */
    public static <Eg extends EonEdge> void tempReleaseService(ServiceAssignment<Eg> serviceAssignment) {
        List<Eg> path = serviceAssignment.getPath();
        int minIndex = serviceAssignment.getStartIndex();
        int maxIndex = serviceAssignment.getStartIndex()+serviceAssignment.getService().getRequiredSlotNum() - 1;
        for (Eg edge : path) {
            ArrayList<EonSlot> slots = edge.getSlots();
            for (int index = minIndex; index <= maxIndex; index++) {
                if (Debug.ENABLE_DEBUG) {
                    if (slots.get(index - 1).getOccupiedServiceIndex() != serviceAssignment.getService().getIndex()) {
                        throw new RuntimeException("release wrong slot that isn't occupied by this service.");
                    }
                }
                slots.get(index-1).setOccupiedServiceIndex(EonSlot.AVAILABLE);
            }
        }
    }


    /**
     * allocate/restore the slots to specific service.
     * Notification: this method is generally called after tempReleaseService. And it will not affect the statistic data.
     * @param serviceAssignment information of service assignment
     * @param <Eg> Class extends EonEdge
     */
    public static <Eg extends EonEdge> void tempAllocateService(ServiceAssignment<Eg> serviceAssignment) {
        List<Eg> path = serviceAssignment.getPath();
        int minIndex = serviceAssignment.getStartIndex();
        int maxIndex = serviceAssignment.getStartIndex()+serviceAssignment.getService().getRequiredSlotNum() - 1;
        for (Eg edge : path) {
            ArrayList<EonSlot> slots = edge.getSlots();
            for (int index = minIndex; index <= maxIndex; index++) {
                slots.get(index-1)
                        .setOccupiedServiceIndex(
                            serviceAssignment.getService().getIndex());
            }
        }
    }

    /**
     * find first available slots set in specific link
     * @param edge edge
     * @param requiredSlotNum required slot number
     * @param <Eg> subclass of EonEdge
     * @param <Vx> subclass of EonVertex
     * @return the index of available slots if success, -1 if fail.
     */
    public static <Eg extends EonEdge, Vx extends EonVertex> int findFirstAvailableSlot(
            Eg edge, int requiredSlotNum) {
        ArrayList<Eg> egs = Lists.newArrayListWithCapacity(1);
        egs.add(edge);
        return findFirstAvailableSlot(egs, requiredSlotNum);
    }


    /**
     * 不返回null，只会返回empty
     * @param edges
     * @param requiredSlotNum
     * @param <Eg>
     * @return
     */
    public static <Eg extends EonEdge> TreeSet<Integer> findAvailableSlotsSet
            (List<Eg> edges, int requiredSlotNum) {
        TreeSet<Integer> avaiIntersectionSet;
        if (edges.isEmpty()) {
            return Sets.newTreeSet();
        } else if (edges.size() == 1){
            avaiIntersectionSet = edges.get(0).availableSlotsIndexSet();
            if (avaiIntersectionSet.size() < requiredSlotNum) {
                return Sets.newTreeSet();
            }
        } else {
            avaiIntersectionSet = edges.get(0).availableSlotsIndexSet();
            for (int i=1; i<edges.size(); i++) {
                TreeSet<Integer> specificEdgeAvailSet = edges.get(i).availableSlotsIndexSet();
                // intersection
                intersection(avaiIntersectionSet, specificEdgeAvailSet);
                // if the size of intersection set is small than requiredSlotNum, return -1.
                if (avaiIntersectionSet.size() < requiredSlotNum) {
                    return Sets.newTreeSet();
                }
            }
        }
        return avaiIntersectionSet;
    }

    /**
     * 不返回null，只返回empty
     * @param edges
     * @param requiredSlotNum
     * @param <Eg>
     * @return
     */
    public static <Eg extends EonEdge> ArrayList<Integer> findAvailableSlotsList
                                                                (List<Eg> edges, int requiredSlotNum) {
        // this list is ordered by ascend.
        ArrayList<Integer> avaiIntersectionList = Lists.newArrayList();
        TreeSet<Integer> avaiIntersectionSet = findAvailableSlotsSet(edges, requiredSlotNum);
        if (!avaiIntersectionSet.isEmpty()) {
            avaiIntersectionList = treesetToArraylist(avaiIntersectionSet);
        }
        return avaiIntersectionList;
    }

    /**
     * find first available slots set in specific path
     * @param edges edges
     * @param requiredSlotNum required slot Num
     * @return the index of available slots if success, -1 if fail.
     */
    public static <Eg extends EonEdge, Vx extends EonVertex> int findFirstAvailableSlot(
            List<Eg> edges, int requiredSlotNum) {
        // this list is ordered by ascend.
        ArrayList<Integer> avaiIntersectionList;
        if (edges.isEmpty()) {
            return UNAVAILABLE;
        } else if (edges.size() == 1){
            avaiIntersectionList = edges.get(0).availableSlotsIndexList();
            if (avaiIntersectionList.size() < requiredSlotNum) {
                return UNAVAILABLE;
            }
        } else {
            TreeSet<Integer> avaiIntersectionSet = edges.get(0).availableSlotsIndexSet();
            for (int i=1; i<edges.size(); i++) {
                TreeSet<Integer> specificEdgeAvailSet = edges.get(i).availableSlotsIndexSet();
                // intersection
                intersection(avaiIntersectionSet, specificEdgeAvailSet);
                // if the size of intersection set is small than requiredSlotNum, return -1.
                if (avaiIntersectionSet.size() < requiredSlotNum) {
                    return UNAVAILABLE;
                }
            }
            // transform set to list.
            // In TreeSet, over the elements in this set in ascending order.
            avaiIntersectionList = treesetToArraylist(avaiIntersectionSet);
        }

        return searchLowestAvaiIndex(avaiIntersectionList, requiredSlotNum);
    }


    /**
     * search the continuous block from ordered list.
     * @param list list
     * @param requiredNum required slot number
     * @return -1 if false
     */
    private static int searchLowestAvaiIndex(ArrayList<Integer> list, int requiredNum) {
        if (requiredNum == 1) {
            return list.get(0);
        }
        int startIndex = list.get(0);
        int continuousNum = 1;
        for (int i=1; i<list.size(); i++) {
            if (list.get(i) == startIndex+continuousNum) {
                continuousNum++;
            } else {
                startIndex = list.get(i);
                continuousNum = 1;
            }
            // if find the first spectrum block which is satisfied.
            if (continuousNum == requiredNum) {
                return startIndex;
            }
        }

        return UNAVAILABLE;
    }


    /**
     * transform TreeSet to ArrayList
     * @param treeSet tree set
     * @return array list
     */
    private static ArrayList<Integer> treesetToArraylist(TreeSet<Integer> treeSet) {
        Iterator<Integer> iterator = treeSet.iterator();
        ArrayList<Integer> list = Lists.newArrayList();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        return list;
    }

    /**
     * calculate intersection by removing the element of original.
     * @param original original tree set
     * @param compared compared tree set
     */
    private static void intersection(TreeSet<Integer> original, TreeSet<Integer> compared) {
        ArrayList<Integer> duplicate = original.stream().filter(index -> !compared.contains(index)).collect(Collectors.toCollection(ArrayList::new));
        duplicate.forEach(original::remove);
    }

    /**
     * move the slots occupied by serviceAssignment to the slots block starting from startIndex in different path.
     * Notification: Must verify that the original occupied slots have been released!
     * Notification: This method should be called after tempReleaseService method.
     * @param serviceAssignment service assignment information
     * @param newStartIndex start index of aimed slots block
     * @param newPath new path
     * @param <Eg> Subclass of EonEdge
     */
    public static <Eg extends EonEdge> void shiftService(
            ServiceAssignment<Eg> serviceAssignment, int newStartIndex, List<Eg> newPath) {
        // TODO
        if (Debug.ENABLE_DEBUG) {
            // 由于getCrossShiftStartIndex的原因,这个检查可能会失败.
//            checkTempRealease(serviceAssignment);
        }
        int serviceIndex = serviceAssignment.getService().getIndex();
        int newEndIndex = newStartIndex + serviceAssignment.getService().getRequiredSlotNum()-1;
        for (Eg edge : newPath) {
            List<EonSlot> slots = edge.getSlots();
            for (int index = newStartIndex; index <= newEndIndex; index++) {
                // must be index-1
                if (!slots.get(index-1).isOccupied()) {
                    slots.get(index-1).setOccupiedServiceIndex(serviceIndex);
                    slots.get(index-1).accumulate();
                } else {
                    throw new RuntimeException("assign a occupied slot while shifting service.");
                }
            }
        }

        // tail-in work
        serviceAssignment.setPath(newPath);
        serviceAssignment.setStartIndex(newStartIndex);
    }


    private static <Eg extends EonEdge> void checkTempRealease(ServiceAssignment<Eg> serviceAssignment) {
        int tmpStartIndex = serviceAssignment.getStartIndex();
        int tmpEndIndex = tmpStartIndex+serviceAssignment.getService().getRequiredSlotNum()-1;
        for (Eg edge : serviceAssignment.getPath()) {
            ArrayList<EonSlot> slots = edge.getSlots();
            for (int index=tmpStartIndex; index<=tmpEndIndex; index++) {
                if (slots.get(index-1).isOccupied()) {
                    throw new RuntimeException("error while releasing service.");
                }
            }
        }
    }

    /**
     * move the slots occupied by serviceAssignment to the slots block starting from startIndex in the same path.
     * Notification: Must verify that the original occupied slots have been released!
     * Notification: This method should be called after tempReleaseService method.
     * @param serviceAssignment service assignment information
     * @param startIndex start index of aimed slots block
     * @param <Eg> Subclass of EonEdge
     */
    public static <Eg extends EonEdge> void shiftService(ServiceAssignment<Eg> serviceAssignment, int startIndex) {
        // TODO
        if (Debug.ENABLE_DEBUG) {
            // 在MLSPDAlg的getCrossShiftStartIndex方法中可能出现异常情况,因此拿掉.
//            checkTempRealease(serviceAssignment);
        }
        serviceAssignment.setStartIndex(startIndex);
        int endIndex = startIndex+serviceAssignment.getService().getRequiredSlotNum()-1;
        int serviceIndex = serviceAssignment.getService().getIndex();
        for (Eg edge : serviceAssignment.getPath()) {
            ArrayList<EonSlot> slots = edge.getSlots();
            for (int index=startIndex; index <= endIndex; index++) {
                // must be index-1
                slots.get(index-1).setOccupiedServiceIndex(serviceIndex);
                slots.get(index-1).accumulate();
            }
        }
    }

    /**
     * execute allocation operation to assign specific spectrums of specific path.
     * @param path route of service
     * @param startIndex start index of slots to be occupied
     * @param service service to be assigned
     */
    protected void execAllocation(List<E> path, int startIndex, Service service) {
        for (E edge : path) {
            ArrayList<EonSlot> slots = edge.getSlots();
            for (int i=startIndex; i<=startIndex+service.getRequiredSlotNum()-1; i++) {
                // must be i-1 rather than i
                EonSlot slot = slots.get(i-1);
                if (slot.isOccupied()) {
                    throw new RuntimeException("assign a occupied slot to arrived service.");
                }
                // slot operation
                slot.setOccupiedServiceIndex(service.getIndex());
                slot.accumulate();
            }
        }
        // service operation
        putCurrentService(new ServiceAssignment<>(service, startIndex, path));
    }
}
