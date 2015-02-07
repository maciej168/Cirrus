package pl.mmajewski.cirrus.main.coreevents.storage;

import pl.mmajewski.cirrus.common.event.CirrusEvent;
import pl.mmajewski.cirrus.common.model.ContentMetadata;
import pl.mmajewski.cirrus.common.model.ContentPiece;
import pl.mmajewski.cirrus.common.persistance.ContentStorage;
import pl.mmajewski.cirrus.main.CirrusCore;

/**
 * Created by Maciej Majewski on 2015-02-03.
 */
public class BalanceAndDiffuseStorageCirrusEvent extends CirrusEvent<CirrusCore.CoreEventHandler> {

    private ContentStorage toCommit;

    public void setStorageToCommit(ContentStorage toCommit) {
        this.toCommit = toCommit;
    }

    @Override
    public void event(CirrusCore.CoreEventHandler handler) {
        ContentStorage storage = handler.getContentStorage();
        storage.updateContentMetadata(toCommit.getAllContentMetadata());
        for(ContentMetadata metadata : toCommit.getAllContentMetadata()) {
            for (ContentPiece piece : toCommit.getAvailablePieces(metadata)) {
                storage.storeContentPiece(piece);
            }
        }

        //TODO push new content to memory storage
        //TODO generate events for content updates
        //TODO initiate content diffusion
    }
}