package org.springframework.batch.item;

import org.springframework.core.io.Resource;
import org.springframework.batch.item.file.MultiResourceItemReader;

/**
 * Marker interface indicating that an item should have the Spring {@link Resource} in which it was read from, set on it.
 * The canonical example is within {@link MultiResourceItemReader}, which will set the current resource on any items
 * that implement this interface.
 *
 * @author Lucas Ward
 */
public interface ResourceAware {

    void setResource(Resource resource);
}
