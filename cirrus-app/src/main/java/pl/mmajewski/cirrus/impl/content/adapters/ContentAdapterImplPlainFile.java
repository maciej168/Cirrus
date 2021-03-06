package pl.mmajewski.cirrus.impl.content.adapters;

import pl.mmajewski.cirrus.main.Constants;
import pl.mmajewski.cirrus.common.event.CirrusEventHandler;
import pl.mmajewski.cirrus.common.model.ContentMetadata;
import pl.mmajewski.cirrus.content.ContentAdapter;
import pl.mmajewski.cirrus.exception.ContentAdapterCirrusException;
import pl.mmajewski.cirrus.main.appevents.NewContentPreparedCirrusAppEvent;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;

/**
 * Created by Maciej Majewski on 2015-02-03.
 *
 * Simple implementation loading whole file into memory
 */
public class ContentAdapterImplPlainFile implements ContentAdapter {
    private static Logger logger = Logger.getLogger(ContentAdapterImplPlainFile.class.getName());
    private String contentSource;
    private ByteBuffer[] chunks;
    private boolean eventGenereationSuppressed;
    private CirrusEventHandler eventHandler;

    private volatile int progress = 0;
    private volatile int maxProgress = 100;

    @Override
    public int getProgress() {
        return progress;
    }

    @Override
    public int getMaxProgress() {
        return maxProgress;
    }

    private void setProgress(int progress){
        this.progress = progress;
    }

    private void setProgress(int progress, int maxProgress){
        this.maxProgress = maxProgress;
        this.progress = progress;
    }

    /**
     * Constructor for quiet (non-eventful) processing
     */
    public ContentAdapterImplPlainFile(){
        eventHandler = null;
        eventGenereationSuppressed = true;
    }

    /**
     * Constructor for eventful processing (still can be turned off with suppressEventGeneration method)
     * @param eventHandler handler for generated events
     */
    public ContentAdapterImplPlainFile(CirrusEventHandler eventHandler){
        this.eventHandler = eventHandler;
        this.eventGenereationSuppressed = false;
    }



    @Override
    public String getContentSource() {
        return contentSource;
    }

    @Override
    public String getContentTypeDescription() {
        return "File";
    }

    @Override
    public  boolean isSupported(String contentSource) {
        boolean supported = false;
        try {
            File source = new File(contentSource);
            supported = source.isFile() && source.canRead();
        }catch (Exception e){
            logger.finest(e.getMessage());
        }
        return supported;
    }

    @Override
    public void adapt(String contentSource) throws ContentAdapterCirrusException {
        long startTime = System.currentTimeMillis();//Test metrics
        try{
            this.contentSource = contentSource;
            File source = new File(contentSource);

            FileChannel fileChannel = new FileInputStream(source).getChannel();

            int numberOfChunks = (int) (Math.ceil(((double)source.length()) / Constants.CHUNK_SIZE));
            this.setProgress(0, numberOfChunks);
            int lastChunkSize = 0;
            int numberOfNormalChunks = numberOfChunks;
            if(source.length() % Constants.CHUNK_SIZE > 0){
                lastChunkSize = (int) (Constants.CHUNK_SIZE - ((numberOfChunks * Constants.CHUNK_SIZE) - source.length()));
                numberOfNormalChunks = numberOfChunks - 1;
            }

            chunks = new ByteBuffer[numberOfChunks];

            //Reading all chunks except wierd-sized last
            for (int i = 0; i < numberOfNormalChunks; i++) {
//                ByteBuffer chunk = ByteBuffer.allocateDirect(Constants.CHUNK_SIZE);
                ByteBuffer chunk = ByteBuffer.allocate(Constants.CHUNK_SIZE);
                fileChannel.read(chunk);
                chunk.rewind();
                chunks[i] = chunk;
                setProgress(i);
            }
            //Reading last chunk
            if(lastChunkSize>0){
//                ByteBuffer chunk = ByteBuffer.allocateDirect(lastChunkSize);
                ByteBuffer chunk = ByteBuffer.allocate(lastChunkSize);
                fileChannel.read(chunk);
                chunk.rewind();//make buffer ready for read
                chunks[numberOfChunks-1] = chunk;
            }

            if(!eventGenereationSuppressed) {
                ContentFactory contentFactory = new ContentFactory(source.getName(), numberOfChunks);
                contentFactory.feed(chunks);

                NewContentPreparedCirrusAppEvent evt = new NewContentPreparedCirrusAppEvent();
                evt.init();
                ContentMetadata metadata = contentFactory.getMetadata();
                metadata.setCommiterCirrusId(eventHandler.getLocalCirrusId());
                evt.setMetadata(metadata);
                evt.setPieces(contentFactory.getPieces());
                eventHandler.accept(evt);
            }
            setProgress(getMaxProgress());
        }catch (Exception e){
            throw new ContentAdapterCirrusException(e,this);
        }

        //Test metrics
        long adaptTime = System.currentTimeMillis() - startTime;
        logger.info("[TIME] ADAPT_FILE: "+adaptTime+"ms ("+contentSource+")");
    }

    public ByteBuffer[] getChunks(){
        return chunks;
    }

    public void suppressEventGeneration(boolean suppress){
        if(eventHandler == null && !suppress){
            throw new UnsupportedOperationException("Instance was initialized as non-eventful");
        }
        eventGenereationSuppressed = suppress;
    }

    public CirrusEventHandler getEventHandler(){
        return eventHandler;
    }
}
