package pl.mmajewski.cirrus.tests;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import pl.mmajewski.cirrus.common.event.CirrusEventHandler;
import pl.mmajewski.cirrus.common.model.ContentMetadata;
import pl.mmajewski.cirrus.common.persistance.ContentStorage;
import pl.mmajewski.cirrus.content.ContentAccessor;
import pl.mmajewski.cirrus.content.ContentAdapter;
import pl.mmajewski.cirrus.impl.content.accessors.ContentAccessorImplPlainBQueue;
import pl.mmajewski.cirrus.impl.content.adapters.ContentAdapterImplPlainFile;
import pl.mmajewski.cirrus.main.CirrusBasicApp;
import pl.mmajewski.cirrus.main.appevents.CommitContentCirrusAppEvent;

import java.io.File;
import java.io.FileOutputStream;

import static org.testng.Assert.*;

public class ContentAccessorImplPlainBQueueTest {

    private CirrusBasicApp app = new CirrusBasicApp();


    @Parameters({"testFile","dumpFile"})
    @Test
    public void testSaveAsFile(String testFile, String dumpFile) throws Exception {
        File inFile = new File(testFile);
        File outFile = new File(dumpFile);
        Assert.assertTrue(outFile.canWrite()||!outFile.exists());


        ContentAdapter adapter = new ContentAdapterImplPlainFile(app.getAppEventHandler());
        adapter.adapt(testFile);

        app.getAppEventHandler().standby();

        ContentStorage storage = app.getAppEventHandler().getContentStorage();
        CirrusEventHandler coreEventHandler = app.getAppEventHandler().getCoreEventHandler();
        Assert.assertNotNull(storage);
        Assert.assertNotNull(storage.getAllContentMetadata());
        Assert.assertNotNull(coreEventHandler);
        Assert.assertFalse(storage.getAllContentMetadata().isEmpty());

        Object[] metadatas = storage.getAllContentMetadata().toArray();
        Assert.assertNotNull(metadatas);
        Assert.assertEquals(metadatas.length, 1);
        Assert.assertNotNull(metadatas[0]);



        CommitContentCirrusAppEvent evt = new CommitContentCirrusAppEvent();
        evt.init();
        app.getAppEventHandler().accept(evt);

        app.getAppEventHandler().standby();
        app.getAppEventHandler().getCoreEventHandler().standby();



        ContentAccessor accessor = new ContentAccessorImplPlainBQueue((ContentMetadata) metadatas[0],coreEventHandler);
        accessor.saveAsFile(dumpFile);

        app.stopProcessingEvents();
        app.getAppEventHandler().standby();
        app.getAppEventHandler().getCoreEventHandler().standby();

        Assert.assertTrue(FileUtils.contentEquals(inFile,outFile));

    }
}