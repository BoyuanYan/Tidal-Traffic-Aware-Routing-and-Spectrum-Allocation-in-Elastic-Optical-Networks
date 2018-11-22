package org.yby.ecoc2017.traffic;

import com.google.common.base.Objects;
import org.yby.ecoc2017.net.EonEdge;

import java.io.Serializable;
import java.util.List;

/**
 * store service assignment information.
 * @author yby
 */
public class ServiceAssignment<E extends EonEdge> implements Serializable {
    private Service service;

    private int startIndex;

    private List<E> path;

    public Service getService() {
        return service;
    }

    public void setService(final Service service) {
        this.service = service;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public List<E> getPath() {
        return path;
    }

    public void setPath(List<E> path) {
        this.path = path;
    }

    public ServiceAssignment(Service service, int startIndex, List<E> path) {
        this.service = service;
        this.startIndex = startIndex;
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServiceAssignment)) return false;
        ServiceAssignment that = (ServiceAssignment) o;
        return Objects.equal(service, that.service);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(service);
    }
}
