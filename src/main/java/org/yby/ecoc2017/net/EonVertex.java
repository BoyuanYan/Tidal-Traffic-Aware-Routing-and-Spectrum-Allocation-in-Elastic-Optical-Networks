package org.yby.ecoc2017.net;

import com.google.common.base.Objects;

import java.io.Serializable;

/**
 * Vertex class in EON including index and boolean value judging whether it runs normally.
 * Created by yby on 2017/3/31.
 */
public class EonVertex implements Serializable {
    private int index;
    private boolean isRunning;

    public EonVertex(int index, boolean isRunning) {
        this.index = index;
        this.isRunning = isRunning;
    }

    public EonVertex(int index) {
        this.index = index;
        this.isRunning = true;
    }

    public int getIndex() {
        return index;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setIsRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EonVertex eonVertex = (EonVertex) o;

        return index == eonVertex.index;

    }

    @Override
    public int hashCode() {
        return index;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("index", index)
                .toString();
    }
}
