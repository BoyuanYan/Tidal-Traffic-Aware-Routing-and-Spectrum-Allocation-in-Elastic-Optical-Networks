package org.yby.ecoc2017.traffic;

import com.google.common.collect.Lists;
import org.jgrapht.alg.util.Pair;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.TreeMap;

import static com.google.common.base.Preconditions.checkNotNull;
/**
 * sort the service list by time, every service will appears twice, one for happening, the other for leaving.
 * Created by yby on 2017/4/1.
 */
public class ServiceQueue {
    // services lists from multiple services list
    private ArrayList<ArrayList<Service>> servicesLists = Lists.newArrayList();

//    /**
//     * add new service list to service queue as a element of a list.
//     * @param services
//     * @return
//     */
//    public void addServiceList(ArrayList<Service> services) throws WrongOrderException{
//        checkNotNull(services);
//        if (servicesLists.isEmpty()) {
//            servicesLists.add(services);
//        } else {
//            ArrayList<Service> last = servicesLists.get(servicesLists.size() - 1);
//            Calendar lastTime = last.get(last.size()-1).getStartTime();
//            if (services.get(0).getStartTime().after(lastTime)) {
//                servicesLists.add(services);
//            } else {
//                throw new WrongOrderException();
//            }
//        }
//    }

//    /**
//     * combine ArrayList<ArrayList<Service>> to ArrayList<Service> by order.
//     * @return list of services
//     */
//    public ArrayList<Service> combineServiceList() {
//        ArrayList<Service> services = new ArrayList<>();
//        for (int i=0; i< servicesLists.size(); i++) {
//            if (!services.addAll(servicesLists.get(i))) {
//                return null;
//            }
//        }
//        return services;
//    }

    /**
     * add new service list to service queue as a element of a list.
     * Notification:
     * 1. the indexes of added service list must be larger than current servicesLists.
     * 2. the indexes of added service must be ordered by ascend itself.
     * 3. All services in servicesLists are start from 1 with step value 1.
     * @param services
     * @return
     */
    public void addServiceList(ArrayList<Service> services){
        checkNotNull(services);
        if (!services.isEmpty()) {
            checkAscendOrder(services);
            checkServiceListOrder(services);
            servicesLists.add(services);
        }
    }

    /**
     * check whether the indexes of services are ordered by ascend with step value 1.
     * @param services
     */
    private void checkAscendOrder(ArrayList<Service> services) {
        int expectedOrder = services.get(0).getIndex();
        for (Service service : services) {
            if (expectedOrder != service.getIndex()) {
                throw new RuntimeException("The index of service that equals "+service.getIndex()+
                        " doesn't match with expected value"+expectedOrder);
            } else {
                expectedOrder++;
            }
        }
    }

    private void checkServiceListOrder(ArrayList<Service> services) {
        int preIndex = 0;
        if (!servicesLists.isEmpty()) {
            ArrayList<Service> pre = servicesLists.get(servicesLists.size()-1);
            preIndex = pre.get(pre.size()-1).getIndex();
        }
        if (services.get(0).getIndex() != preIndex+1) {
            throw new RuntimeException("The added service list is not behind existed servicesLists.");
        }
    }

    /**
     * combine ArrayList<ArrayList<Service>> to ArrayList<Service>.
     * Notification: the service index of return service list is ordered by ascend with step value 1 starting from 1.
     * @return list of services
     */
    public ArrayList<Service> combineServiceList() {
        ArrayList<Service> services = Lists.newArrayList();
        for (int i=0; i< servicesLists.size(); i++) {
            services.addAll(servicesLists.get(i));
        }
        return services;
    }

    /**
     *
     * @return
     */
    public ArrayList<Timestamp> sortQueue() {
        ArrayList<Service> services = combineServiceList();
        ArrayList<Timestamp> timestamps = Lists.newArrayListWithCapacity(services.size()*2);
        for (Service service : services) {
            timestamps.add(new Timestamp(service.getStartTime(), true, service.getIndex()));
            timestamps.add(new Timestamp(service.getEndTime(), false, service.getIndex()));
        }
        timestamps.sort(new TimeComparator());
        return timestamps;
    }

    /**
     *
     * @return
     */
    public ArrayList<Timestamp> sortQueue(ArrayList<Service> services) {
        ArrayList<Timestamp> timestamps = Lists.newArrayListWithCapacity(services.size()*2);
        for (Service service : services) {
            timestamps.add(new Timestamp(service.getStartTime(), true, service.getIndex()));
            timestamps.add(new Timestamp(service.getEndTime(), false, service.getIndex()));
        }
        timestamps.sort(new TimeComparator());
        return timestamps;
    }


    /**
     * the input parameter is ordered by ascend by default.
     * @param services
     * @return
     */
    private Pair<Calendar, Calendar> getTimeScope(ArrayList<Service> services) {
        return new Pair<>(services.get(0).getStartTime(), services.get(services.size()-1).getStartTime());
    }
}


//class WrongOrderException extends Exception {
//    @Override
//    public void printStackTrace() {
//        System.out.println("The inserted ArrayList<Service> instance can not be put on the tail of servicesLists.");
//        super.printStackTrace();
//    }
//}