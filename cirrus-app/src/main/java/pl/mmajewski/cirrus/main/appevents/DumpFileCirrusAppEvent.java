package pl.mmajewski.cirrus.main.appevents;

import pl.mmajewski.cirrus.common.exception.EventHandlerClosingCirrusException;
import pl.mmajewski.cirrus.common.model.ContentMetadata;
import pl.mmajewski.cirrus.content.ContentAccessor;
import pl.mmajewski.cirrus.event.CirrusAppEvent;
import pl.mmajewski.cirrus.exception.EventCancelledCirrusException;
import pl.mmajewski.cirrus.impl.content.accessors.ContentAccessorImplPlainBQueue;
import pl.mmajewski.cirrus.main.CirrusBasicApp;

import java.io.FileNotFoundException;
import java.util.logging.Logger;

/**
 * Created by Maciej Majewski on 2015-02-06.
 */
public class DumpFileCirrusAppEvent extends CirrusAppEvent<CirrusBasicApp.AppEventHandler> {

    private static Logger logger = Logger.getLogger(DumpFileCirrusAppEvent.class.getName());

    private ContentMetadata metadata;
    private String file;

    public void setMetadata(ContentMetadata metadata) {
        this.metadata = metadata;
    }

    public void setFile(String file) {
        this.file = file;
    }

    @Override
    public void appEvent(CirrusBasicApp.AppEventHandler handler) {
        ContentAccessor fileDumper = new ContentAccessorImplPlainBQueue(metadata,handler.getCoreEventHandler());
        try {
            fileDumper.saveAsFile(file);
        } catch (EventHandlerClosingCirrusException|FileNotFoundException e) {
            logger.warning(e.getMessage());
            throw new EventCancelledCirrusException(e);
        }
    }
}
