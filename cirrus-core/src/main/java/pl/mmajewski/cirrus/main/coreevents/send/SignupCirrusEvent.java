package pl.mmajewski.cirrus.main.coreevents.send;

import pl.mmajewski.cirrus.common.exception.EventHandlerClosingCirrusException;
import pl.mmajewski.cirrus.common.model.Host;
import pl.mmajewski.cirrus.impl.client.BroadcastPropagationStrategy;
import pl.mmajewski.cirrus.main.coreevents.ActionFailureCirrusEvent;
import pl.mmajewski.cirrus.main.coreevents.network.MetadataPropagationCirrusEvent;
import pl.mmajewski.cirrus.network.client.CirrusEventPropagationStrategy;
import pl.mmajewski.cirrus.network.client.ClientEventConnection;
import pl.mmajewski.cirrus.main.coreevents.network.FetchContentMetadataCirrusEvent;
import pl.mmajewski.cirrus.main.coreevents.network.HostUpdateCirrusEvent;
import pl.mmajewski.cirrus.network.exception.NetworkCirrusException;
import pl.mmajewski.cirrus.network.server.ServerCirrusEventHandler;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Maciej Majewski on 21/11/15.
 */
public class SignupCirrusEvent extends HostUpdateCirrusEvent {

    private Set<Host> joiningHosts = null;

    public void setJoiningHosts(Set<Host> joiningHosts) {
        this.joiningHosts = joiningHosts;
    }

    @Override
    public void event(ServerCirrusEventHandler handler) {
        super.event(handler);

        HostUpdateCirrusEvent hostShareEvent = new HostUpdateCirrusEvent();
        hostShareEvent.setSharedHosts(handler.getHostStorage().fetchAllHosts());
        hostShareEvent.addTrace(handler.getLocalCirrusId());

        FetchContentMetadataCirrusEvent metadataShareEvent = new FetchContentMetadataCirrusEvent();
        metadataShareEvent.setSharedMetadata(handler.getContentStorage().getAllContentMetadata());
        metadataShareEvent.addTrace(handler.getLocalCirrusId());

        Set<Host> joinedHosts = new HashSet<>();

        for(Host host : joiningHosts) {
            try {
                handler.getConnectionPool().addHost(host);
                ClientEventConnection connection = handler.getConnectionPool().fetchConnection(host);
                connection.sendEvent(hostShareEvent);
                connection.sendEvent(metadataShareEvent);

                joinedHosts.add(host);
            } catch (NetworkCirrusException e) {
                ActionFailureCirrusEvent failureEvent = new ActionFailureCirrusEvent();
                failureEvent.setException(e);
                failureEvent.setMessage(e.getMessage());
                try {
                    handler.accept(failureEvent);
                } catch (EventHandlerClosingCirrusException e1) {
                    e.printStackTrace();
                }
            }
        }

        HostUpdateCirrusEvent broadcastJoinedHosts = new HostUpdateCirrusEvent();
//        broadcastJoinedHosts.addTrace(this.getTrace());
        broadcastJoinedHosts.addTrace(handler.getLocalCirrusId());
        broadcastJoinedHosts.setSharedHosts(joinedHosts);

        CirrusEventPropagationStrategy propagationStrategy = new BroadcastPropagationStrategy<HostUpdateCirrusEvent>();
        Set<Host> targets = propagationStrategy.getTargets(handler.getHostStorage(), broadcastJoinedHosts);

        for(Host jh : joinedHosts) {
            broadcastJoinedHosts.addTrace(jh.getCirrusId());
        }

        for(Host host : targets){
            try {
                ClientEventConnection connection = handler.getConnectionPool().fetchConnection(host);
                connection.sendEvent(broadcastJoinedHosts);
            } catch (NetworkCirrusException e) {
                ActionFailureCirrusEvent failureEvent = new ActionFailureCirrusEvent();
                failureEvent.setException(e);
                failureEvent.setMessage(e.getMessage());
                try {
                    handler.accept(failureEvent);
                } catch (EventHandlerClosingCirrusException e1) {
                    e.printStackTrace();
                }
            }
        }

        handler.getHostStorage().updateHosts(joinedHosts);
    }
}
