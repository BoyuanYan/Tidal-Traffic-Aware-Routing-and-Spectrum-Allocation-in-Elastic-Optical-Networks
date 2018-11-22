package org.yby.ecoc2017.defragAlgorithm.boyuan;

import com.google.common.collect.Lists;
import org.jgrapht.WeightedGraph;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yby.ecoc2017.RSAAlgorithm.BasicRSAAlg;
import org.yby.ecoc2017.defragAlgorithm.patel.MaxSlotDescendComparator;
import org.yby.ecoc2017.defragAlgorithm.patel.ShortestPathDefragmentationAlg;
import org.yby.ecoc2017.net.EonEdge;
import org.yby.ecoc2017.net.EonSlot;
import org.yby.ecoc2017.net.EonVertex;
import org.yby.ecoc2017.traffic.Service;
import org.yby.ecoc2017.traffic.ServiceAssignment;
import org.yby.ecoc2017.utils.Debug;

import java.util.*;

/**
 * 1、通过机器学习得到的预测结果的处理，对所有当前业务进行排序。
 *     1.1 得到以前一个月的每一天内固定时间间隔的网络流量分布情况oldData，以及指定时间间隔内失败的业务。
 *     1.2 假设频谱资源足够，对失败的业务进行最短路算路，并进行频谱分配，频谱编号统统从1号频谱开始。将这个分配结果virtualData叠加到oldData上去，
 *         得到频谱资源无限情况下的每条链路上的占用频谱数的分配情况trainingData。
 *     1.3 根据trainingData，使用<b>线性回归模型</b>训练数据，得到指定链路在一天的某个时间段内的预测占用频谱数分配情况，
 *         记为predictedOccupiedSlotNum。
 *     1.4 将predictedOccupiedSlotNum作为边权值赋给图Graph，每条边l上的边权记为weight(l)，简称为wei。
 *     1.5 记业务s在链路l上占用的最高频谱编号为maxSlotIndex(s,l)，简称为msi;占用的频谱宽度为slotWidth(s,l)，简称为sw;每条链路上的频谱总数为N。
 *         则综合考虑频谱分布现状和将来流量需求，设置一个名字待定的值x = msi/N + lambda*sigma(wei)/N。其中除以N表示归一化，第二项乘以lambda表示
 *         最高频谱编号和将来流量需求之间的权衡系数。
 *     1.6 根据当前网络中存在的每条业务计算其x值，然后按照x的降序对业务集合进行排序。
 * 2、从有序集合中选择第一个业务S，并记录其占用的最低波长编号，记为P。
 * 3、以最少跳数为准计算最短路径。如果在该最短路径上，从最低波长编号开始有足够的可用波长资源，则将该业务重构到该最短路径上。如果没有足够的可用波长
 *    资源，并且当前的最低波长编号还没有业务S占用的最低的波长编号高，则将P=P+1;如果两者已经相等，则不对该业务进行重构。并进行步骤4。
 * 4、从有序业务集合中选择下一个业务，重复步骤2和步骤3。
 * @author yby
 */
public class MLSPDAlg<E extends EonEdge> extends ShortestPathDefragmentationAlg<E> {

    private static final Logger log = LoggerFactory.getLogger(MLSPDAlg.class);
    // collect from orderedList, or set by input parameters.
    private Map<E, Double> occupiedSlotsNum;


    public MLSPDAlg(SimpleWeightedGraph<EonVertex, E> graph, TreeMap<Integer, ServiceAssignment<E>> currentServices,
                    Map<E, Double> occupiedSlotsNum) {
        super(graph, currentServices);
        this.occupiedSlotsNum = occupiedSlotsNum;
    }


//    @Override
//    public void defragment(Service blockedService) {
//        // sort
//        LinkedList<ServiceAssignment<E>> edgeWeightDescendList = Lists.newLinkedList();
//        for (ServiceAssignment<E> serviceAssignment : getCurrentServices().values()) {
//            edgeWeightDescendList.add(serviceAssignment);
//        }
//        // no rerouting, so no need to change link weight in graph.
//        edgeWeightDescendList.sort(new EdgeWeightDescendComparator(getOccupiedSlotsNum()));
////        List<ServiceAssignment<E>> orderedSA = maxSlotDescendServices();
//
//        int count=0;
//        // defragment
////        Iterator<ServiceAssignment<E>> iterator = orderedSA.iterator();
//        Iterator<ServiceAssignment<E>> iterator = edgeWeightDescendList.iterator();
//        while (iterator.hasNext()) {
//            ServiceAssignment<E> serviceAssignment = iterator.next();
//            int minOccupiedSlotIndex = serviceAssignment.getStartIndex();
//            List<E> path = serviceAssignment.getPath();
//            BasicRSAAlg.tempReleaseService(serviceAssignment);
//            int shiftSlotStartIndex = BasicRSAAlg.findFirstAvailableSlot(path,
//                    serviceAssignment.getService().getRequiredSlotNum());
//            if (shiftSlotStartIndex != BasicRSAAlg.UNAVAILABLE &&
//                    shiftSlotStartIndex < minOccupiedSlotIndex) {
//                count++;
//                // if there is enough resource for shifting.
//                BasicRSAAlg.shiftService(serviceAssignment, shiftSlotStartIndex);
//                if (blockedService != null) {// 这一步判断是为了阈值重构触发
//                    addServiceShiftTime(serviceAssignment.getPath().size(),
//                            blockedService.getStartTime());
//                }
//            } else {
//                // if no available choice, then restore
//                BasicRSAAlg.tempAllocateService(serviceAssignment);
//            }
//        }
////        log.info("number of shifting services is {}." , count);
//
//    }

    /**
     * 从path中找出落在business区域内部的子路径。如果原/宿节点在business area边缘的话，有可能为空。
     * @param path
     * @return
     */
    private List<E> filterBusinessPath(List<E> path) {
        List<E> rtn = Lists.newArrayList();
        for (E e : path) {
            if (judgeArea(e) != 3) {
                // 非business area内部的link统统被认为是residential business的。
                // 啥都不做
            } else {
                rtn.add(e);
            }
        }
        return rtn;
    }

    private List<E> filterResidentialPath(List<E> path ) {
        List<E> rtn = Lists.newArrayList();
        for (E e : path) {
            if (judgeArea(e) != 3) {
                rtn.add(e);
            } else {
                // 如果是business area内部的link不算在内
            }
        }
        return rtn;
    }


    private int judgeArea(E e) {
        boolean sourceInBusiness = EdgeWeightDescendComparator.businessAreaSet.contains(e.getSource().getIndex());
        boolean destInBusiness = EdgeWeightDescendComparator.businessAreaSet.contains(e.getDestination().getIndex());
        if (sourceInBusiness) {
            if (destInBusiness) {
                return 3;
            } else {
                return 2;
            }
        } else {
            if (destInBusiness) {
                return 2;
            } else {
                return 1;
            }
        }
    }

    /**
     * 计算在path中各个link里，位于minOccupiedSLotIndex编号的slot是否被占用，
     * 如果被占用而且仅仅被一个业务占用，则返回该业务；否则，返回null
     * 注意：这里没有对如果没有被占用的情况作区分。因为如果调整失败，
     * 紧接着还有一次正常调整的流程，那个肯定可以调整成功。
     * @param path
     * @param minOccupiedSlotIndex
     * @return
     */
    private ServiceAssignment<E> checkInfluencedService(List<E> path, int minOccupiedSlotIndex) {
        ServiceAssignment<E> rtn = null;
        for (E e : path) {
            // must -1
            int occupiedServiceIndex  = e.getSlots().get(minOccupiedSlotIndex-1).getOccupiedServiceIndex();
            if (occupiedServiceIndex != EonSlot.AVAILABLE) {
                ServiceAssignment<E> tmp = getCurrentServices().get(occupiedServiceIndex);
                if (tmp == null) {
                    throw new RuntimeException("no such service in current services!");
                } else {
                    if (rtn != tmp) {
                        if (rtn == null) {
                            rtn = tmp;
                        } else {
                            // 如果存在第二个被影响的业务，直接返回null
                            return null;
                        }
                    } else {
                        // 相等说明是同一个业务，无需改动
                    }
                }
            }
        }

        return rtn;
    }

    private static int successNum = 0;
    /**
     * 注意：此方法默认每个业务要求的slots数目为1。如果不符合，则会出错。
     * 注意：无论是是否需要搬移residential业务，只要最后符合搬移条件，就返回搬移的slot的编号。
     * 注意：如果没有符合条件的，就返回-1。
     * @param serviceAssignment
     * @return
     */
    protected int getCrossShiftStartIndex(ServiceAssignment<E> serviceAssignment, Service blockedService) {
        List<E> path = serviceAssignment.getPath();
        int requiredSlotNum = serviceAssignment.getService().getRequiredSlotNum();
        int startIndex = serviceAssignment.getStartIndex();
        List<E> businessPath = filterBusinessPath(path);
        List<E> residentialPath = filterResidentialPath(path);
        if (Debug.ENABLE_DEBUG && businessPath.size()+ residentialPath.size() != path.size()) {
            throw new RuntimeException("path doesn't consist of business and residential");
        }
        // businessAvaiSlots和residentialAvaiSlots都可能为empty，但是绝对不可能为null！
        ArrayList<Integer> businessAvaiSlots = BasicRSAAlg.findAvailableSlotsList(businessPath, requiredSlotNum);
        TreeSet<Integer> residentialAvaiSlots = BasicRSAAlg.findAvailableSlotsSet(residentialPath, requiredSlotNum);

        if (Debug.ENABLE_DEBUG) {
            for (int i=0; i<businessAvaiSlots.size()-1; i++) {
                if (businessAvaiSlots.get(i) >= businessAvaiSlots.get(i+1) ) {
                    throw new RuntimeException("wrong ascend order");
                }
            }
        }
        // 如果businessPath为empty,需要额外处理.
        if (businessPath.isEmpty()) {
            int rtn = BasicRSAAlg.findFirstAvailableSlot(residentialPath, requiredSlotNum);
            if (rtn != BasicRSAAlg.UNAVAILABLE && rtn < serviceAssignment.getStartIndex()) {
                return rtn;
            } else {
                return BasicRSAAlg.UNAVAILABLE;
            }
        }
        for (int index=0; index<businessAvaiSlots.size(); index++) {
            int subscript = businessAvaiSlots.get(index);
            if (subscript < startIndex) {
                if (residentialAvaiSlots.contains(subscript)) {
                    if (Debug.ENABLE_DEBUG) {
                        if (subscript > startIndex) {
                            throw new RuntimeException("doesn't match 2");
                        }
                    }
                    // 如果在residential中本来就是空闲的，则直接分配
                    return subscript;
                } else {
                    // 如果在residential中指定slot已经被占用，则判断是否是和单一residential业务冲突。
                    // residentialPath无论如何不可能为empty
                    int residentialServiceIndex = -1;
                    for (E e : residentialPath) {
                        // must subscript-1
                        EonSlot slot = e.getSlots().get(subscript-1);
                        if (slot.isOccupied()) {
                            if (residentialServiceIndex == -1) {
                                residentialServiceIndex = slot.getOccupiedServiceIndex();
                            } else {
                                if (residentialServiceIndex != slot.getOccupiedServiceIndex()) {
                                    // 如果存在至少两个被影响的业务,则无法对这个slot进行搬移
                                    residentialServiceIndex = -1;
                                    break;
                                } else {
                                    // 如果是同一个service,则什么都不做
                                }
                            }
                        } else {
                            // 如果e上的这个slot没有被占用,则什么都不做
                        }
                    }
                    // 如果为-1,表示至少存在两个被影响的业务.不可能出现没有被影响的业务,
                    // 因为这个可能性已经在上面通过contains方法排除了
                    if (residentialServiceIndex != -1) {
                        ServiceAssignment<E> residentialService = getCurrentServices().get(residentialServiceIndex);
                        int privilege = EdgeWeightDescendComparator.calPrivilege(residentialService);
                        // 确保被转移的是residential内部的业务,且跳数不超过4跳
                        if (privilege == 1) {
                            // 当且仅当需要调整以便留出空闲slot的业务是residential内部的业务的时候,才可能进行调整
                            // 可转移的目标slot不能是跟原来一样编号的slot,因此,不调用tmpReleaseService进行预空闲处理
                            int residentialShiftIndex = BasicRSAAlg.findFirstAvailableSlot(
                                                            residentialService.getPath(),
                                                            residentialService.getService().getRequiredSlotNum());
                            if (residentialShiftIndex != BasicRSAAlg.UNAVAILABLE) {
                                successNum++;
//                                log.info("{} time success", successNum);
                                // TODO 此处有可能出现residentialService要转移的频谱刚好是serviceAssignment临时让出来的频谱
                                // TODO 这种情况下就会在checkTempRelease的时候抛出错误了.
                                BasicRSAAlg.tempReleaseService(residentialService);
                                BasicRSAAlg.shiftService(residentialService, residentialShiftIndex);
                                addServiceShiftTime(residentialService.getPath().size(),
                                        blockedService.getStartTime());
                                // 此时,可以直接返回了
                                return subscript;
                            }
                        } else {
                            // 否则,无法进行调整.因为不确定调整以后会不会造成更糟糕的情况
                        }
                    }
                }
            } else {
                // 如果相等的话，就没必要再循环下去了。
                break;
            }
        }

        return BasicRSAAlg.UNAVAILABLE;
    }

    /**
     * 现在的想法是业务排序是{business，cross，residential}，其中在做cross的业务的时候，如果遇到business area内部可以移动，
     * 但是由于residential area的原因不能移动的话，就找占据该slot的业务（假设是一个）。如果该业务是residential内部的业务，那么将其直接搬移。
     * 如果搬移成功，则移动cross业务；如果搬移失败，则寻找下一个。
     * @param blockedService
     */
    @Override
    public void defragment(Service blockedService) {
        // sort
        // 就用原来的降序，保持唯一变量。
        List<ServiceAssignment<E>> orderedSA = maxSlotDescendServices();

        int count=0;
        // defragment
        List<ServiceAssignment<E>> residentialServiceList = Lists.newArrayList();
        for (int index=0; index< orderedSA.size(); index++) {
            ServiceAssignment<E> serviceAssignment = orderedSA.get(index);
            // 3 - business, 2 - cross, 1 - residential
            int privilege = EdgeWeightDescendComparator.calPrivilege(serviceAssignment);
            if (privilege == 2) {
                // TODO 如果是cross业务，就要查验是否需要搬移residential业务以避让出空闲频谱资源来了。
                BasicRSAAlg.tempReleaseService(serviceAssignment);
                int crossShiftSlotStartIndex = getCrossShiftStartIndex(serviceAssignment, blockedService);
                if (false) {
                    int compShiftSlotStartIndex = BasicRSAAlg.findFirstAvailableSlot(serviceAssignment.getPath(),
                            serviceAssignment.getService().getRequiredSlotNum());
                    if (compShiftSlotStartIndex >= serviceAssignment.getStartIndex()) {
                        compShiftSlotStartIndex = BasicRSAAlg.UNAVAILABLE;
                    }
                    if (compShiftSlotStartIndex != crossShiftSlotStartIndex) {
                        throw new RuntimeException("wrong shift slot start index");
                    }
                }
                if (crossShiftSlotStartIndex != BasicRSAAlg.UNAVAILABLE) {
                    // 调整cross业务
                    BasicRSAAlg.shiftService(serviceAssignment, crossShiftSlotStartIndex);
                    addServiceShiftTime(serviceAssignment.getPath().size(),
                                        blockedService.getStartTime());
                } else {
                    BasicRSAAlg.tempAllocateService(serviceAssignment);
                }
            } else if (privilege == 1) {
                // 如果是residential业务，先留存不做处理。因为cross业务的处理会影响到residential业务的排序
                residentialServiceList.add(serviceAssignment);
            } else if (privilege == 3){
                // TODO 如果不是cross业务，该怎么往下压，还怎么往下压。
                int minOccupiedSlotIndex = serviceAssignment.getStartIndex();
                List<E> path = serviceAssignment.getPath();
                BasicRSAAlg.tempReleaseService(serviceAssignment);
                int shiftSlotStartIndex = BasicRSAAlg.findFirstAvailableSlot(path,
                        serviceAssignment.getService().getRequiredSlotNum());
                if (shiftSlotStartIndex != BasicRSAAlg.UNAVAILABLE &&
                        shiftSlotStartIndex < minOccupiedSlotIndex) {
                    count++;
                    // if there is enough resource for shifting.
                    BasicRSAAlg.shiftService(serviceAssignment, shiftSlotStartIndex);
                    if (blockedService != null) {// 这一步判断是为了阈值重构触发
                        addServiceShiftTime(serviceAssignment.getPath().size(),
                                blockedService.getStartTime());
                    }
                } else {
                    // if no available choice, then restore
                    BasicRSAAlg.tempAllocateService(serviceAssignment);
                }
            } else {
                // 不可能是除了1,2,3以外的取值了.
            }
        }
//        log.info("number of shifting services is {}." , count);
        // 首先，进行排序，还是按照占用slot编号的降序进行排序。
        residentialServiceList.sort(new MaxSlotDescendComparator<E>());
        // 开始处理residential业务
        for (int residentialIndex=0; residentialIndex<residentialServiceList.size(); residentialIndex++) {
            ServiceAssignment<E> serviceAssignment = residentialServiceList.get(residentialIndex);
            int minOccupiedSlotIndex = serviceAssignment.getStartIndex();
            List<E> path = serviceAssignment.getPath();
            BasicRSAAlg.tempReleaseService(serviceAssignment);
            int shiftSlotStartIndex = BasicRSAAlg.findFirstAvailableSlot(path,
                    serviceAssignment.getService().getRequiredSlotNum());
            if (shiftSlotStartIndex != BasicRSAAlg.UNAVAILABLE &&
                    shiftSlotStartIndex < minOccupiedSlotIndex) {
                count++;
                // if there is enough resource for shifting.
                BasicRSAAlg.shiftService(serviceAssignment, shiftSlotStartIndex);
                if (blockedService != null) {// 这一步判断是为了阈值重构触发
                    addServiceShiftTime(serviceAssignment.getPath().size(),
                            blockedService.getStartTime());
                }
            } else {
                // if no available choice, then restore
                BasicRSAAlg.tempAllocateService(serviceAssignment);
            }
        }
    }

    public Map<E, Double> getOccupiedSlotsNum() {
        return occupiedSlotsNum;
    }
}
