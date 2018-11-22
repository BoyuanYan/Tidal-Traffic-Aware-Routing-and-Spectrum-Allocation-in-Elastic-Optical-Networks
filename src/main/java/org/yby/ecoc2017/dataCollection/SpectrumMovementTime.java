package org.yby.ecoc2017.dataCollection;

import com.google.common.collect.Lists;
import org.jgrapht.alg.util.Pair;
import org.yby.ecoc2017.defragAlgorithm.BasicDefragmentationAlg;
import org.yby.ecoc2017.net.EonEdge;
import org.yby.ecoc2017.net.EonVertex;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by yby on 2017/4/11.
 * Calculate time of spectrum movement. one time of spectrum movement means that one link of path while one service is defragmented.
 * 频谱搬移次数应该在重构算法里面。
 */
public class SpectrumMovementTime {
    private static SpectrumMovementTime ourInstance = new SpectrumMovementTime();

    public static SpectrumMovementTime getInstance() {
        return ourInstance;
    }

    private SpectrumMovementTime() {
    }

    /**
     * calculate spectrum migrating time from startTime to endTime, and divide it to dividedNum parts.
     * @param startTime start time
     * @param endTime end time
     * @param dividedNum divided time
     * @param defragmentationAlg alg used
     * @param <V> Vertex extends EonVertex
     * @param <E> Edge extends EonEdge
     * @return list of spectrum migrating time
     */
    public <V extends EonVertex, E extends EonEdge> ArrayList<Integer> calculateSMT(
                                                                Calendar startTime, Calendar endTime, int dividedNum,
                                                                BasicDefragmentationAlg<V, E> defragmentationAlg) {
        // moveRecords must be
        ArrayList<Pair<Calendar, Integer>> moveRecords = defragmentationAlg.getServiceShiftTimeRecords();
        // 这里可能出现有余数的情况，不重要
        // unit is ms
        int timeInterval = (int)(endTime.getTimeInMillis() - startTime.getTimeInMillis()) / dividedNum + 1;
        // initialization
        ArrayList<Integer> smt = Lists.newArrayListWithCapacity(dividedNum);
        for (int i=0; i<smt.size(); i++) {
            smt.set(i, 0);
        }
        Calendar tmp = Calendar.getInstance();
        tmp.setTimeInMillis(startTime.getTimeInMillis() + timeInterval);
        int pointer = 0;
        for (int index=0; index<moveRecords.size(); index++) {
            Calendar key = moveRecords.get(index).getFirst();
            int value = moveRecords.get(index).getSecond();
            if (key.after(tmp)) {
                pointer++;
            }
            smt.set(pointer, smt.get(pointer) + value);
        }
        return smt;

    }
}
