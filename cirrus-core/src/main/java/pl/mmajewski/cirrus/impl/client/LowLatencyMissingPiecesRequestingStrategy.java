package pl.mmajewski.cirrus.impl.client;

import com.googlecode.cqengine.resultset.ResultSet;
import pl.mmajewski.cirrus.common.model.ContentMetadata;
import pl.mmajewski.cirrus.common.model.Host;
import pl.mmajewski.cirrus.common.persistance.ContentStorage;
import pl.mmajewski.cirrus.common.persistance.HostStorage;
import pl.mmajewski.cirrus.main.coreevents.network.RequestContentCirrusEvent;
import pl.mmajewski.cirrus.network.client.CirrusContentRequestingStrategy;

import java.util.*;

/**
 * Created by Maciej Majewski on 29/11/15.
 */
public class LowLatencyMissingPiecesRequestingStrategy implements CirrusContentRequestingStrategy<RequestContentCirrusEvent> {

    @Override
    public Map<Host, RequestContentCirrusEvent> getTargets(HostStorage hostStorage,
                                                           ContentStorage contentStorage,
                                                           ContentMetadata contentMetadata) {
        Map<Host, RequestContentCirrusEvent> requests = new HashMap<>();
        Set<Integer> missingPieces = contentStorage.getMissingContentPieceSequenceNumbers(contentMetadata);
        Iterable<Host> sharers = hostStorage.fetchSharers(contentMetadata);
        for(Host host : sharers){
            Set<Integer> hostPieces = host.getSharedPieces(contentMetadata.getContentId());
            hostPieces.retainAll(Collections.unmodifiableSet(missingPieces));
            if(!hostPieces.isEmpty()){
                RequestContentCirrusEvent requestEvent = new RequestContentCirrusEvent();
                requestEvent.setContentMetadata(contentMetadata);
                requestEvent.setRequester(hostStorage.fetchLocalHost());
                requestEvent.setNeededPieces(hostPieces);
                requests.put(host, requestEvent);

                missingPieces.removeAll(Collections.unmodifiableSet(hostPieces));
            }
        }
        return requests;
    }

}