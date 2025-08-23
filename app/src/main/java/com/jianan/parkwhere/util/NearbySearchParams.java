package com.jianan.parkwhere.util;

import android.location.Location;

import java.util.Objects;

/**
 * Immutable container for nearby search parameters used to query nearby car parks
 *
 * Holds a {@link android.location.Location} and a search radius in metres
 *
 * Commonly used as the value type for LiveData that applies distinctUntilChanged to avoid
 * redundant searches when parameters have not changed
 */
public class NearbySearchParams {
    private final Location location;
    private final float radiusMeters;

    public NearbySearchParams(Location location, float radiusMeters) {
        this.location = location;
        this.radiusMeters = radiusMeters;
    }

    public Location getLocation() {
        return location;
    }

    public float getRadiusMeters() {
        return radiusMeters;
    }

    // Internally used by LiveData.distinctUntilChanged()
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()){
            return false;
        }
        NearbySearchParams that = (NearbySearchParams) o;
        return Float.compare(that.radiusMeters, radiusMeters) == 0 &&
                Objects.equals(location, that.location);
    }

    // Though not used, this method has been overridden to maintain the equals-hashCode contract
    @Override
    public int hashCode() {
        return Objects.hash(location, radiusMeters);
    }
}
