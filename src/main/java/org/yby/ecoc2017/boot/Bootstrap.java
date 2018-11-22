package org.yby.ecoc2017.boot;

import com.google.common.collect.Lists;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * The Bootstrap class before simulation for initialization.
 * Created by yby on 2017/3/31.
 */
public class Bootstrap {
    private static Bootstrap ourInstance = new Bootstrap();

    public static Bootstrap getInstance() {
        return ourInstance;
    }
    // root configure file name.
    String confFile = "init.properties";

    /** Parameters about network **/
    public EonNetParams netParams = EonNetParams.getInstance();

    /** Parameters about Traffic generation **/


    /**
     * Simulation related parameters parsing from Properties files.
     */
    private Bootstrap() {
        try {
            Properties init = new Properties();
            init.load(new FileInputStream(confFile));
            netParams.slotNum = Integer.parseInt(init.getProperty("slot_num_per_edge"));
            netParams.timeInterval = Integer.parseInt(init.getProperty("time_interval"));
            netParams.rouBias = Double.parseDouble(init.getProperty("rou_bias"));
            netParams.rouBusinessPeak = Double.parseDouble(init.getProperty("rou_business_peak"));
            netParams.rouResidentialPeak = Double.parseDouble(init.getProperty("rou_residential_peak"));
            netParams.minRequiredSlotNum = Integer.parseInt(init.getProperty("min_required_slot_num"));
            netParams.maxRequiredSlotNum = Integer.parseInt(init.getProperty("max_required_slot_num"));
            netParams.miu = Double.parseDouble(init.getProperty("miu"));
            int netNum = Integer.parseInt(init.getProperty("net_num"));
            netParams.networks = Lists.newArrayListWithCapacity(netNum);
            for (int i=1; i<=netNum; i++) {
                // load specific properties file of network parameters.
                String netFileName = init.getProperty("net_"+i+".file_name");
                Properties netProperties = new Properties();
                netProperties.load(new FileInputStream(netFileName));
                String name = netProperties.getProperty("name");
                int vertexNum = Integer.parseInt(netProperties.getProperty("vertex_num"));
                int edgeNum = Integer.parseInt(netProperties.getProperty("edge_num"));
                List<String> edgesStr = Lists.newArrayListWithCapacity(edgeNum);
                for (int edgeIndex=1; edgeIndex<=edgeNum; edgeIndex++) {
                    edgesStr.add(netProperties.getProperty("edge_"+edgeIndex));
                }

                ArrayList<Integer> businessArea = Lists.newArrayList();
                int businessSize = Integer.parseInt(netProperties.getProperty("business_vertex_num"));
                String[] businessVx = netProperties.getProperty("business_vertexes").split(",");
                if (businessVx.length == businessSize) {
                    for (int j=0; j<businessSize; j++) {
                        businessArea.add(Integer.parseInt(businessVx[j]));
                    }
                } else {
                    throw new RuntimeException("business vertexes configuration error!");
                }

                ArrayList<Integer> residentialArea = Lists.newArrayList();
                int residentSize = Integer.parseInt(netProperties.getProperty("residential_vertex_num"));
                String[] residentVx = netProperties.getProperty("residential_vertexes").split(",");
                if (residentVx.length == residentSize) {
                    for (int j=0; j<residentSize; j++) {
                        residentialArea.add(Integer.parseInt(residentVx[j]));
                    }
                } else {
                    throw new RuntimeException("residential vertexes configuration error!");
                }

                SingleEonNet singleEonNet = new SingleEonNet(businessArea, residentialArea, name, vertexNum, edgeNum, edgesStr);
                netParams.networks.add(singleEonNet);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
