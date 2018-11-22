package org.yby.ecoc2017.traffic;

import com.google.common.collect.Lists;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.jgrapht.alg.util.Pair;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Service generator within a specific time scope.
 * Created by yby on 2017/4/1.
 */
public class ServiceGenerator {
    // available vertex set in which service will generate
    private ArrayList<Integer> avaiVertexes;

    private ArrayList<Integer> srcVertexesSet;
    private ArrayList<Integer> dstVertexesSet;

    // arrival rate submits to Poisson distribution whose unit is /min
    private double rou;
    // leave rate submits to Poisson distribution whose unit is /min
    private double miu;
    // start time of generated services, for example, 2017-04-01 00:00:000
    private Calendar startTime;
    // end time of generated services, for example, 2017-04-01 06:00:000
    private Calendar endTime;
    // minimum required slots number per service
    private int minRequiredSlotNum;
    // maximum required slots number per service
    private int maxRequiredSlotNum;
    // start index of generated services
    private int startIndex;


    private static final long unitConversion = 1000*60;

    /**
     * generate services list.
     * Notification:
     * @return service list
     */
    public ArrayList<Service> generateServices() {
        ArrayList<Service> services = Lists.newArrayList();
        ExponentialDistribution genExpDistribution = new ExponentialDistribution(1.0/rou);
        ExponentialDistribution leaveExpDistribution = new ExponentialDistribution(1.0/miu);
        double t = timeScope();
        double y = 0;
        ArrayList<Double> happendIntervalTimes = Lists.newArrayList();
        while(y <= t) {
            double delta = genExpDistribution.sample();
            y+=delta;
            happendIntervalTimes.add(delta);
        }
        // remove the last one.
        happendIntervalTimes.remove(happendIntervalTimes.size()-1);

        // generate leave time
        Calendar currentTime = copyOf(startTime);
        for (int i=0; i<happendIntervalTimes.size(); i++) {
            double holdTimeInMin = leaveExpDistribution.sample();
            currentTime.add(Calendar.MILLISECOND, (int)(happendIntervalTimes.get(i)*unitConversion));

            Calendar holdTime = copyOf(currentTime);
            // if generated delta*unitConversion<1, then the result of double-to-int process will be 0.
            // so must be +1.
            holdTime.add(Calendar.MILLISECOND, (int)(holdTimeInMin*unitConversion)+1);
            Pair<Integer, Integer> srcDst = srcDst();

            services.add(
                    new Service(i+startIndex, currentTime, holdTime, requiredSlotNum(), srcDst.getFirst(), srcDst.getSecond()));

            currentTime = copyOf(currentTime);
        }

        return services;
    }

    /**
     * get the deep copy of specific calendar instance.
     * @param calendar calendar being copied
     * @return calendar copying from input
     */
    private Calendar copyOf(Calendar calendar) {
        Calendar rtn = Calendar.getInstance();
        rtn.setTimeInMillis(calendar.getTimeInMillis());
        return rtn;
    }

    /**
     * calculate the difference between startTime and endTime with unit min.
     * @return time scope
     */
    private double timeScope() {
        return (double)((endTime.getTimeInMillis()-startTime.getTimeInMillis())/unitConversion);
    }

    /**
     * generate random required slot number within [minRequiredSlotNum, maxRequiredSlotNum].
     * @return required slot number
     */
    private int requiredSlotNum() {
        // [min, max]
        int scope = maxRequiredSlotNum - minRequiredSlotNum+1;
        return (int)Math.floor(Math.random()*scope)+1;
    }

    /**
     * generate source and destination of one service.
     * @return a pair of source vertex and destination vertex
     */
    private Pair<Integer, Integer> srcDst() {
        if(avaiVertexes != null && srcVertexesSet==null && dstVertexesSet==null) {
            int size = avaiVertexes.size();
            int src = (int) Math.floor(Math.random() * size);
            int dst = (int) Math.floor(Math.random() * size);
            while (src == dst) {
                dst = (int) Math.floor(Math.random() * size);
            }
            return new Pair<>(avaiVertexes.get(src), avaiVertexes.get(dst));
        } else if (avaiVertexes == null && srcVertexesSet!=null && dstVertexesSet!=null) {
            int srcSize = srcVertexesSet.size();
            int dstSize = dstVertexesSet.size();
            int src = (int) Math.floor(Math.random() * srcSize);
            int dst = (int) Math.floor(Math.random() * dstSize);
            return new Pair<>(srcVertexesSet.get(src), dstVertexesSet.get(dst));
        } else {
            throw new RuntimeException(
                    "avaiVertexes and src/dst-VertexesSet can not be null or not empty simultaneously.");
        }
    }

    public ServiceGenerator(ArrayList<Integer> avaiVertexes, double rou, double miu,
                            Calendar startTime, Calendar endTime,
                            int minRequiredSlotNum, int maxRequiredSlotNum, int startIndex) {
        this.avaiVertexes = avaiVertexes;
        this.rou = rou;
        this.miu = miu;
        this.startTime = startTime;
        this.endTime = endTime;
        this.minRequiredSlotNum = minRequiredSlotNum;
        this.maxRequiredSlotNum = maxRequiredSlotNum;
        this.startIndex = startIndex;
    }

    public ServiceGenerator(ArrayList<Integer> srcVertexesSet, ArrayList<Integer> dstVertexesSet, double rou,
                            double miu, Calendar startTime, Calendar endTime, int minRequiredSlotNum,
                            int maxRequiredSlotNum, int startIndex) {
        this.srcVertexesSet = srcVertexesSet;
        this.dstVertexesSet = dstVertexesSet;
        this.rou = rou;
        this.miu = miu;
        this.startTime = startTime;
        this.endTime = endTime;
        this.minRequiredSlotNum = minRequiredSlotNum;
        this.maxRequiredSlotNum = maxRequiredSlotNum;
        this.startIndex = startIndex;
    }

    public ArrayList<Integer> getAvaiVertexes() {
        return avaiVertexes;
    }

    public void setAvaiVertexes(ArrayList<Integer> avaiVertexes) {
        this.avaiVertexes = avaiVertexes;
    }

    public double getRou() {
        return rou;
    }

    public void setRou(double rou) {
        this.rou = rou;
    }

    public double getMiu() {
        return miu;
    }

    public void setMiu(double miu) {
        this.miu = miu;
    }

    public Calendar getStartTime() {
        return startTime;
    }

    public void setStartTime(Calendar startTime) {
        this.startTime = startTime;
    }

    public Calendar getEndTime() {
        return endTime;
    }

    public void setEndTime(Calendar endTime) {
        this.endTime = endTime;
    }

    public int getMinRequiredSlotNum() {
        return minRequiredSlotNum;
    }

    public void setMinRequiredSlotNum(int minRequiredSlotNum) {
        this.minRequiredSlotNum = minRequiredSlotNum;
    }

    public int getMaxRequiredSlotNum() {
        return maxRequiredSlotNum;
    }

    public void setMaxRequiredSlotNum(int maxRequiredSlotNum) {
        this.maxRequiredSlotNum = maxRequiredSlotNum;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public static long getUnitConversion() {
        return unitConversion;
    }
}
