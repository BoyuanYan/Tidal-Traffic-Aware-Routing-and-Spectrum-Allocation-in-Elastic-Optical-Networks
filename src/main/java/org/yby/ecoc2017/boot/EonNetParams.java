package org.yby.ecoc2017.boot;

import java.util.List;

/**
 * Parsing parameters about EON networks.
 * Notification: There may be more than one network for simulation.
 * Created by yby on 2017/4/1.
 */
public class EonNetParams {
    private static EonNetParams ourInstance = new EonNetParams();

    public static EonNetParams getInstance() {
        return ourInstance;
    }

    public int slotNum;
    public List<SingleEonNet> networks;

    // the unit of rou and miu is /min.
    // rou = rou_bias + rou_peak*(1+sin(-pi/2+pi/12*t)
    // time interval to change rou as equation shown before with unit minute.
    public int timeInterval;
    // bias arrival rate for cross vertexes of business area and residential area,
    // source for business, destination for residential
    public double rouBias;
    // peak arrival rate for business area
    public double rouBusinessPeak;
    // peak arrival rate for residential area
    public double rouResidentialPeak;

    // leave rate
    public double miu;


    // minimum required slot number per service
    public int minRequiredSlotNum;
    // maximum required slot number per service
    public int maxRequiredSlotNum;

    private EonNetParams() {

    }
}
