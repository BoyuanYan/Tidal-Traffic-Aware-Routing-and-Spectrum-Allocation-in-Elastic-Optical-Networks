package org.yby.ecoc2017.defragAlgorithm.icocn2017_2;

import org.jgrapht.graph.SimpleWeightedGraph;
import org.yby.ecoc2017.RSAAlgorithm.BasicRSAAlg;
import org.yby.ecoc2017.defragAlgorithm.patel.ShortestPathDefragmentationAlg;
import org.yby.ecoc2017.net.EonEdge;
import org.yby.ecoc2017.net.EonVertex;
import org.yby.ecoc2017.traffic.Service;
import org.yby.ecoc2017.traffic.ServiceAssignment;

import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by yby on 2017/5/24.
 */
public class LEAlg<E extends EonEdge>  extends ShortestPathDefragmentationAlg<E> {

    public LEAlg(SimpleWeightedGraph<EonVertex, E> graph, TreeMap<Integer, ServiceAssignment<E>> currentServices) {
        super(graph, currentServices);
        serviceShiftTime = 0;
        checkedServiceNum = 0;
    }

    int serviceShiftTime;
    int checkedServiceNum;

    /**
     *
     * @param lastService 不是被阻塞的业务，而是触发前处理的最后一个离去的业务，所以时间应该从入参业务的离去时间算起。
     */
    @Override
    public void defragment(Service lastService) {
        // 获取被重构业务的个数
        int num = getCurrentServices().size();
        // Step 1
        // 按照被占用slot的降序排序
        List<ServiceAssignment<E>> orderedSA = maxSlotDescendServices();
        Iterator<ServiceAssignment<E>> iteratorLong = orderedSA.iterator();
        Iterator<ServiceAssignment<E>> iteratorShort = orderedSA.iterator();

        checkedServiceNum = num;
        Calendar seperationTime = Calendar.getInstance();
        // 计算阈值时间分界点。
        seperationTime.setTimeInMillis(
                lastService.getEndTime().getTimeInMillis() + Configuration.restTimeInMs);

        // 两次while循环，依次处理留存时间超过/没有超过restTimeInMs的业务。
        while (iteratorLong.hasNext()) {
            ServiceAssignment<E> serviceAssignment = iteratorLong.next();
            if (serviceAssignment.getService().getEndTime().after(seperationTime)) {
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
                    // 如果被搬移业务超过一定数量，就返回，不再搬移
                    if (serviceShiftTime == Configuration.totalPerDefrag) {
                        return;
                    }
                } else {
                    // if no available choice, then restore
                    BasicRSAAlg.tempAllocateService(serviceAssignment);
                }
            }
        }

        // 第二次循环
        while (iteratorShort.hasNext()) {
            ServiceAssignment<E> serviceAssignment = iteratorShort.next();
            if (!serviceAssignment.getService().getEndTime().after(seperationTime)) {
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
                    // 如果被搬移业务超过一定数量，就返回，不再搬移
                    if (serviceShiftTime == Configuration.totalPerDefrag) {
                        return;
                    }
                } else {
                    // if no available choice, then restore
                    BasicRSAAlg.tempAllocateService(serviceAssignment);
                }
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
