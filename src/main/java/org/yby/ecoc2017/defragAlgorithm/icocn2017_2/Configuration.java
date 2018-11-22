package org.yby.ecoc2017.defragAlgorithm.icocn2017_2;

/**
 * Created by yby on 2017/5/24.
 */
public class Configuration {
    // 剩余时间
    static final long restTimeInMs = 5*60*1000; // 12分钟
    // 离去业务个数触发阈值。
    static final int threshold = 100;
    // 每次重构最多能够重构多少条业务
    static final int totalPerDefrag = 600;
    // 重构的时候计算k条路
    static final int k = 3;
}
