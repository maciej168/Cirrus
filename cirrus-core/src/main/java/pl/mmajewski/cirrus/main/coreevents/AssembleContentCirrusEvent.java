package pl.mmajewski.cirrus.main.coreevents;

import pl.mmajewski.cirrus.common.event.CirrusEvent;
import pl.mmajewski.cirrus.common.event.GenericCirrusEventThread;
import pl.mmajewski.cirrus.common.exception.EventHandlerClosingCirrusException;
import pl.mmajewski.cirrus.common.model.ContentMetadata;
import pl.mmajewski.cirrus.common.model.ContentPiece;
import pl.mmajewski.cirrus.common.model.Host;
import pl.mmajewski.cirrus.common.persistance.ContentStorage;
import pl.mmajewski.cirrus.common.util.CirrusBlockingSequence;
import pl.mmajewski.cirrus.impl.client.LowLatencyMissingPiecesRequestingStrategy;
import pl.mmajewski.cirrus.main.CirrusCoreEventHandler;
import pl.mmajewski.cirrus.main.coreevents.network.RequestContentCirrusEvent;
import pl.mmajewski.cirrus.network.ConnectionPool;
import pl.mmajewski.cirrus.network.client.CirrusContentRequestingStrategy;
import pl.mmajewski.cirrus.network.client.ClientEventConnection;
import pl.mmajewski.cirrus.network.exception.NetworkCirrusException;
import pl.mmajewski.cirrus.network.server.ServerCirrusEventHandler;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by Maciej Majewski on 2015-02-06.
 */
public class AssembleContentCirrusEvent extends CirrusEvent<ServerCirrusEventHandler> {
    private ContentMetadata metadata;

    public void setMetadata(ContentMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public void event(ServerCirrusEventHandler handler) {
        ContentStorage storage = handler.getContentStorage();
        ArrayList<ContentPiece> pieces = storage.getAvailablePieces(metadata);

        ConnectionPool connectionPool = handler.getConnectionPool();

        CirrusContentRequestingStrategy<RequestContentCirrusEvent> requestingStrategy = new LowLatencyMissingPiecesRequestingStrategy();
        Map<Host, RequestContentCirrusEvent> targets = requestingStrategy.getTargets(handler.getHostStorage(), handler.getContentStorage(), metadata);
        for(Host target : targets.keySet()){
            try {
                ClientEventConnection connection = connectionPool.fetchConnection(target);
                RequestContentCirrusEvent requestingEvent = targets.get(target);
                requestingEvent.addTrace(this.getTrace());
                requestingEvent.addTrace(handler.getLocalCirrusId());
                connection.sendEvent(requestingEvent);
            } catch (NetworkCirrusException e) {
                ActionFailureCirrusEvent failureEvent = new ActionFailureCirrusEvent();
                failureEvent.setException(e);
                failureEvent.setMessage(e.getMessage());
                e.printStackTrace();
                try {
                    handler.accept(failureEvent);
                } catch (EventHandlerClosingCirrusException e1) {
                    e.printStackTrace();
                }
            }
        }

        for(ContentPiece piece : pieces) {
            handler.getContentPieceSink(metadata).push(piece.getSequence(), piece);
        }
    }

}