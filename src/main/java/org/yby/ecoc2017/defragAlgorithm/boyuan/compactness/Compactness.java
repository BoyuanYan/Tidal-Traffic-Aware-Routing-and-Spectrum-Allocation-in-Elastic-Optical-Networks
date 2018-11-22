package org.yby.ecoc2017.defragAlgorithm.boyuan.compactness;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yby.ecoc2017.net.EonEdge;
import org.yby.ecoc2017.net.EonSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by yby on 2017/4/17.
 * 把频谱归整度的概念引入进来，是要设定一个阈值，通过对频谱归整度的判断，
 */
public final class Compactness {

    public static final double THRESHOLD = 0.15;
    private static final Logger log = LoggerFactory.getLogger(Compactness.class);

    /**
     * 计算一条边的SC值.
     * EonEdge里面的EonSlot的list都是按照slotIndex的升序排列的。
     * @param link
     * @param <E>
     * @return
     */
    public static <E extends EonEdge> double linkSC(E link) {
        ArrayList<EonSlot> slots = link.getSlots();
        double lambda_max=0,lambda_min=Double.MAX_VALUE,k=0,sigma_b=0;
        boolean obtainMin = false;

        for (int index=0; index<slots.size(); index++) {
            EonSlot slot = slots.get(index);
            if (slot.isOccupied()) {
                if (!obtainMin) {
                    lambda_min = slot.getSlotIndex();
                    obtainMin = true;
                }
                lambda_max = slot.getSlotIndex();
                sigma_b++;
            }
        }
        if (lambda_min == Double.MAX_VALUE) {
            return Double.MAX_VALUE;
        } else {
            // 翻转次数一定是偶数倍
            int tranverse = 0;
            for (int index = (int) lambda_min - 1; index < lambda_max - 1; index++) {
                // 如果相邻两个slot的被占用情况不同，则表示符号翻转，加一
                if (slots.get(index).isOccupied() != slots.get(index+1).isOccupied()) {
                    tranverse++;
                }
            }
            log.info("tranverse is {}.", tranverse);
            k = tranverse/2;
            double sc = (lambda_max - lambda_min + 1) / sigma_b * (1 / k);
            return sc;
        }
    }

    private static <E extends EonEdge> Param getParam(E link) {
        ArrayList<EonSlot> slots = link.getSlots();
        double lambda_max=0,lambda_min=Double.MAX_VALUE,k=0,sigma_b=0;
        boolean obtainMin = false;

        for (int index=0; index<slots.size(); index++) {
            EonSlot slot = slots.get(index);
            if (slot.isOccupied()) {
                if (!obtainMin) {
                    lambda_min = slot.getSlotIndex();
                    obtainMin = true;
                }
                lambda_max = slot.getSlotIndex();
                sigma_b++;
            }
        }
        if (lambda_min == Double.MAX_VALUE) {
            // 该方法用于网络的SC计算
            return new Param(0,0, Double.MAX_VALUE, 0);
        } else {
            // 翻转次数一定是偶数倍
            int tranverse = 0;
            for (int index = (int) lambda_min - 1; index < lambda_max - 1; index++) {
                // 如果相邻两个slot的被占用情况不同，则表示符号翻转，加一
                if (slots.get(index).isOccupied() != slots.get(index+1).isOccupied()) {
                    tranverse++;
                }
            }
//            log.info("tranverse is {}.", tranverse);
            k = tranverse/2;
            return new Param(lambda_min, lambda_max, k, sigma_b);
        }
    }


    /**
     * 计算一个links的列表的整体的频谱归整度
     * @param links
     * @param <E>
     * @return
     */
    public static <E extends EonEdge> double linksSC(Set<E> links) {
        double sigma_sigma_b=0.0d;
        double sigma_lambda_max=0.0d;
        double sigma_lambda_min=0.0d;
        double sigma_k=0.0d;
        double m = links.size();
        for (E link : links) {
            Param param = getParam(link);
            if (param.k != Double.MAX_VALUE) {
                sigma_sigma_b += param.sigma_b;
                sigma_lambda_max += param.lambda_max;
                sigma_lambda_min += param.lambda_min;
                sigma_k += param.k;
            }
        }
        if (sigma_k==0) {
            return Double.MAX_VALUE;
        } else {
            double sc = (sigma_lambda_max - sigma_lambda_min + m) / sigma_sigma_b * (m / sigma_k);
            return sc;
        }
    }
}

final class Param {
    double lambda_min;
    double lambda_max;
    double k;
    double sigma_b;

    public Param(double lambda_min, double lambda_max, double k, double sigma_b) {
        this.lambda_min = lambda_min;
        this.lambda_max = lambda_max;
        this.k = k;
        this.sigma_b = sigma_b;
    }
}