package org.yby.ecoc2017.traffic;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yby.ecoc2017.utils.Debug;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * @author yby
 */
public class SineServiceGenerator {


    private static final Logger log = LoggerFactory.getLogger(SineServiceGenerator.class);

    protected static final int MIN_IN_ONE_DAY = 24*60;

    protected static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    // rou = rou_bias + rou_peak*(1+sin(-pi/2+pi/12*t), if in business area
    // rou = rou_bias + rou_peak*(1+cos(pi/12*t), if in residential area

    // the unit of rou and miu is /min.

    // time interval to change rou as equation shown before with unit minute.
    protected int timeInterval;
    // bias arrival rate for business area and residential area, one for source ,one for destination.
    private double rouBias;
    // peak arrival rate for business area
    private double rouBusinessPeak;
    // peak arrival rate for residential area
    private double rouResidentialPeak;

    // leave rate
    protected double miu;

    // minimum required slot number per service
    protected int minRequiredSlotNum;
    // maximum required slot number per service
    protected int maxRequiredSlotNum;

    private ArrayList<Integer> businessArea;
    private ArrayList<Integer> residentialArea;

    protected ServiceQueue serviceQueue;

    protected Calendar startTime;
    protected int days;

    /**
     * Notification: timeInterval must can be divided exactly by 60*24.
     * @param timeInterval
     * @param rouBias
     * @param rouBusinessPeak
     * @param rouResidentialPeak
     * @param miu
     * @param minRequiredSlotNum
     * @param maxRequiredSlotNum
     * @param businessArea
     * @param residentialArea
     * @param vertexNum
     * @param startTime
     * @param days
     */
    public SineServiceGenerator(int timeInterval, double rouBias, double rouBusinessPeak, double rouResidentialPeak, double miu,
                                int minRequiredSlotNum, int maxRequiredSlotNum, ArrayList<Integer> businessArea,
                                ArrayList<Integer> residentialArea, int vertexNum, Calendar startTime, int days) {
        this.timeInterval = timeInterval;
        this.rouBias = rouBias;
        this.rouBusinessPeak = rouBusinessPeak;
        this.rouResidentialPeak = rouResidentialPeak;
        this.miu = miu;
        this.minRequiredSlotNum = minRequiredSlotNum;
        this.maxRequiredSlotNum = maxRequiredSlotNum;
        this.businessArea = businessArea;
        this.residentialArea = residentialArea;

        this.startTime = startTime;
        this.days = days;
        this.serviceQueue = new ServiceQueue();
    }

    private ArrayList<Integer> combineArea(ArrayList<Integer> one, ArrayList<Integer> theOther) {
        ArrayList<Integer> rtn = Lists.newArrayListWithExpectedSize(one.size()+theOther.size());
        for (int i : one) {
            rtn.add(i);
        }
        for (int j : theOther) {
            rtn.add(j);
        }
        return rtn;
    }

    /**
     * ʵ��ҵ�����ɣ����ǲ��ܷ�������ֻ��עҵ��ĳ���ʱ������
     * @return
     */
    public ArrayList<Service> generateGlobalServices() {
        int startIndex = 1;
        double elephantRatio = 0.1;
        double shortMiu = 0.5;
        double elephantMiu = 0.1;

        // get the start time of days, such as 2016-04-07 00:00:00.000
        ArrayList<Calendar> eachDayBegin = dayBegin(startTime, days);
        ArrayList<Integer> wholeArea = combineArea(businessArea, residentialArea);
        for (Calendar startPerDay : eachDayBegin) {
            // get every point to split one day to multiple parts.
            ArrayList<Calendar> intervalPoint = minutesBegin(startPerDay, timeInterval);
            for (int i = 1; i < intervalPoint.size(); i++) {
                Calendar tmpStartTime = intervalPoint.get(i - 1);
                Calendar tmpEndTime = intervalPoint.get(i);
                ServiceGenerator elephantGenerator = new ServiceGenerator(wholeArea, rouBias*elephantRatio, elephantMiu,
                        tmpStartTime,tmpEndTime, minRequiredSlotNum, maxRequiredSlotNum, startIndex);
                ArrayList<Service> elephantServices = elephantGenerator.generateServices();
                if (Debug.ENABLE_DEBUG && elephantServices.isEmpty()) {
                    log.warn("elephant: no service arrival and leave between {} and {} with rou equals {}.",
                            format.format(tmpStartTime.getTime()),
                            format.format(tmpEndTime.getTime()),
                            rouBias);
                }
                serviceQueue.addServiceList(elephantServices);
                startIndex = startIndex + elephantServices.size();

                // short service
                ArrayList<Service> shortServices;
                ServiceGenerator residentialGenerator = new ServiceGenerator(wholeArea, rouBias*(1-elephantRatio),
                        shortMiu,tmpStartTime, tmpEndTime, minRequiredSlotNum, maxRequiredSlotNum, startIndex);
                shortServices = residentialGenerator.generateServices();
                if (Debug.ENABLE_DEBUG && shortServices.isEmpty()) {
                    log.warn("short: no service arrival and leave between {} and {} with rou equals {}.",
                            format.format(tmpStartTime.getTime()),
                            format.format(tmpEndTime.getTime()),
                            rouBias);
                }

                serviceQueue.addServiceList(shortServices);
                startIndex = startIndex + shortServices.size();
            }
        }
        return serviceQueue.combineServiceList();
    }


    /**
     * generate services
     * @return
     */
    public ArrayList<Service> generateServices() {
        int startIndex = 1;

        // get the start time of days, such as 2016-04-07 00:00:00.000
        ArrayList<Calendar> eachDayBegin = dayBegin(startTime, days);
        for (Calendar startPerDay : eachDayBegin) {
            // get every point to split one day to multiple parts.
            ArrayList<Calendar> intervalPoint = minutesBegin(startPerDay, timeInterval);
            for (int i=1; i<intervalPoint.size(); i++) {
                Calendar tmpStartTime = intervalPoint.get(i-1);
                Calendar tmpEndTime = intervalPoint.get(i);

                double t = getDoubleTime(tmpStartTime, tmpEndTime);
                double tmpBusinessRou = rouBusinessPeak*(1.0d+Math.sin(-Math.PI/2.0d + Math.PI/12.0d * t));
                double tmpResidentialRou = rouResidentialPeak*(1.0d+Math.cos(Math.PI/12.0d * t));
                // generate three service queues for business area, residential area, and cross area.
                // business area

                ServiceGenerator businessGenerator = new ServiceGenerator(businessArea, tmpBusinessRou, miu, tmpStartTime,
                        tmpEndTime, minRequiredSlotNum, maxRequiredSlotNum, startIndex);
                ArrayList<Service> businessServices = businessGenerator.generateServices();
                if (Debug.ENABLE_DEBUG && businessServices.isEmpty()) {
                    log.warn("business: no service arrival and leave between {} and {} with rou equals {}.",
                            format.format(tmpStartTime.getTime()),
                            format.format(tmpEndTime.getTime()),
                            tmpBusinessRou);
                }
                serviceQueue.addServiceList(businessServices);
                startIndex = startIndex + businessServices.size();
                // residential area
                ArrayList<Service> residentialServices;
                if (tmpResidentialRou != 0) {
                    ServiceGenerator residentialGenerator = new ServiceGenerator(residentialArea, tmpResidentialRou, miu,
                            tmpStartTime, tmpEndTime, minRequiredSlotNum, maxRequiredSlotNum, startIndex);
                    residentialServices = residentialGenerator.generateServices();
                    if (Debug.ENABLE_DEBUG && residentialServices.isEmpty()) {
                        log.warn("residential: no service arrival and leave between {} and {} with rou equals {}.",
                                format.format(tmpStartTime.getTime()),
                                format.format(tmpEndTime.getTime()),
                                tmpResidentialRou);
                    }
                } else {
                    residentialServices = Lists.newArrayList();
                }

                serviceQueue.addServiceList(residentialServices);
                startIndex = startIndex + residentialServices.size();
                // cross area
                ServiceGenerator crossGenerator = new ServiceGenerator(businessArea, residentialArea, rouBias, miu,
                        tmpStartTime, tmpEndTime, minRequiredSlotNum, maxRequiredSlotNum, startIndex);
                ArrayList<Service> crossServices = crossGenerator.generateServices();
                if (Debug.ENABLE_DEBUG && crossServices.isEmpty()) {
                    log.warn("cross: no service arrival and leave between {} and {} with rou equals {}.",
                            format.format(tmpStartTime.getTime()),
                            format.format(tmpEndTime.getTime()),
                            rouBias);
                }
                serviceQueue.addServiceList(crossServices);
                startIndex = startIndex + crossServices.size();
            }
        }

        return serviceQueue.combineServiceList();
    }

    /**
     * transfer the mid value of start and end to a double value.such as 06:30:00.000 will be transferred to 6.50
     * @param start start time
     * @param end end time
     * @return min value in double format
     */
    protected double getDoubleTime(Calendar start, Calendar end) {
        Calendar midVal = Calendar.getInstance();
        midVal.setTimeInMillis((start.getTimeInMillis()+end.getTimeInMillis())/2);
        int hour = midVal.get(Calendar.HOUR_OF_DAY);
        int minute = midVal.get(Calendar.MINUTE);
        int second = midVal.get(Calendar.SECOND);
        int ms = midVal.get(Calendar.MILLISECOND);
        return hour + minute/60.0d + second/(60.0d*60.0d) + ms/(60.0d*60.0d*1000.0d);
    }


    /**
     * generate a list of every time interval's begin time and end time
     * @param beginDay begin of one day
     * @param interval time interval
     * @return list of every time interval's begin time and end time
     */
    protected ArrayList<Calendar> minutesBegin(Calendar beginDay, int interval) {
        if (MIN_IN_ONE_DAY%interval != 0) {
            throw new RuntimeException("time interval can not be divided exactly by 24*60!");
        }
        int partsNum = MIN_IN_ONE_DAY / interval;
        ArrayList<Calendar> minutes = Lists.newArrayList();
        Calendar starts = Calendar.getInstance();
        starts.setTimeInMillis(beginDay.getTimeInMillis());
        for (int i=0; i<partsNum; i++) {
            Calendar tmp = Calendar.getInstance();
            tmp.setTimeInMillis(starts.getTimeInMillis());
            minutes.add(tmp);

            starts.add(Calendar.MINUTE, interval);
        }
        minutes.add(starts);
        return minutes;
    }

    /**
     * generate a list of every day's begin time.
     * @param startTime start time
     * @param days days
     * @return
     */
    protected ArrayList<Calendar> dayBegin(Calendar startTime, int days) {
        ArrayList<Calendar> list = Lists.newArrayList();
        int year = startTime.get(Calendar.YEAR);
        int month = startTime.get(Calendar.MONTH);
        int day = startTime.get(Calendar.DAY_OF_MONTH);
        Calendar startDay = Calendar.getInstance();
        startDay.clear();
        startDay.set(year, month, day, 0, 0, 0);
        startDay.set(Calendar.MILLISECOND, 0);
        for (int i=0; i<days; i++) {
            Calendar tmp = Calendar.getInstance();
            tmp.setTimeInMillis(startDay.getTimeInMillis());
            list.add(tmp);
            startDay.add(Calendar.DAY_OF_YEAR, 1);
        }
        if (Debug.ENABLE_DEBUG) {
            for (Calendar calendar : list) {
                log.info("past dataset from {} generated.", format.format(calendar.getTime()));
            }
        }
        return list;
    }


    public ArrayList<Timestamp> generateServicesOrder() {
        return serviceQueue.sortQueue();
    }

    public ArrayList<Timestamp> generateServicesOrder(ArrayList<Service> services) {
        return serviceQueue.sortQueue(services);
    }
}
