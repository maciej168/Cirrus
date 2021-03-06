package pl.mmajewski.cirrus.impl.persistance;

import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.index.compound.CompoundIndex;
import com.googlecode.cqengine.persistence.offheap.OffHeapPersistence;
import com.googlecode.cqengine.query.Query;
import pl.mmajewski.cirrus.common.model.ContentAvailability;
import pl.mmajewski.cirrus.common.persistance.AvailabilityStorage;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static com.googlecode.cqengine.query.QueryFactory.and;
import static com.googlecode.cqengine.query.QueryFactory.equal;

/**
 * Created by Maciej Majewski on 15/09/15.
 */
public class MemoryAvailabilityStorage implements AvailabilityStorage {
    private final IndexedCollection<ContentAvailability> availabilty =
            new ConcurrentIndexedCollection<ContentAvailability>(
                OffHeapPersistence.onPrimaryKey(ContentAvailability.IDX_UNIQUE_ID)){{
                    addIndex(CompoundIndex.onAttributes(
                            ContentAvailability.IDX_CONTENT_ID,
                            ContentAvailability.IDX_AVAILABLE_PIECES));
            }};

    @Override
    public void updateAvailability(Set<ContentAvailability> availabilities) {
        removeAvailability(availabilities);
        availabilty.addAll(availabilities);
    }

    @Override
    public void removeAvailability(Set<ContentAvailability> availabilities) {
        Set<ContentAvailability> toDel = new HashSet<>();
        for(ContentAvailability av : availabilities){
            Query<ContentAvailability> query = and(
                    equal(ContentAvailability.IDX_CONTENT_ID, av.getContentId()),
                    equal(ContentAvailability.IDX_HOLDER_CIRRUS_ID, av.getHolderCirrusId())
            );
            for(ContentAvailability avToDel : availabilty.retrieve(query)){
                toDel.add(avToDel);
            }
        }
        availabilty.removeAll(toDel);
    }

    @Override
    public Iterable<ContentAvailability> getContentAvailability(String contentId) {
        Query<ContentAvailability> query = equal(ContentAvailability.IDX_CONTENT_ID, contentId);
        return availabilty.retrieve(query);
    }

    @Override
    public Iterable<ContentAvailability> getHostContentAvailability(String cirrusId) {
        Query<ContentAvailability> query = equal(ContentAvailability.IDX_HOLDER_CIRRUS_ID, cirrusId);
        return availabilty.retrieve(query);
    }

    @Override
    public Set<Integer> getHostContentAvailabilityPieces(String cirrusId, String contentId) {
        Query<ContentAvailability> query = and(
                equal(ContentAvailability.IDX_HOLDER_CIRRUS_ID, cirrusId),
                equal(ContentAvailability.IDX_HOLDER_CIRRUS_ID, contentId));
        Set<Integer> pieces = new TreeSet<>();
        for(ContentAvailability querriedAvailability : availabilty.retrieve(query)){
            pieces.addAll(querriedAvailability.getPiecesSequenceNumbers());
        }
        return pieces;
    }

}
