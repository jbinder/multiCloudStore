package at.ac.tuwien.multicloudstore.lib.transfer;

import at.ac.tuwien.multicloudstore.lib.common.FileSystemNode;
import at.ac.tuwien.multicloudstore.lib.naming.NameConverter;
import at.ac.tuwien.multicloudstore.lib.storageservices.InvalidOptionException;
import at.ac.tuwien.multicloudstore.lib.storageservices.StorageService;
import at.ac.tuwien.multicloudstore.lib.storageservices.StorageServiceOperationException;
import at.ac.tuwien.multicloudstore.lib.storageservices.StorageServiceUnreachableException;

import java.util.logging.Logger;

public class UnreliableStorageServiceDecorator implements StorageService {

    private final Logger log = Logger.getLogger(UnreliableStorageServiceDecorator.class.getCanonicalName());
    private static final int timeout = 10 * 1000;
    private static final int retryCount = 5;

    private StorageService storageService;

    class ListAllRunnable implements TimedRunnable {

        private FileSystemNode root;

        public void run() {
            try {
                root = storageService.listAll();
            } catch (StorageServiceOperationException e) {
                log.severe(e.getLocalizedMessage());
            }
        }

        public FileSystemNode getRootNode() {
            return root;
        }

        public boolean isComplete() {
            return root != null;
        }
    }

    class TransferRunnable implements TimedRunnable {

        private String localFileName;
        private String remoteFileName;
        private Boolean done = false;
        private boolean upload = false;

        public TransferRunnable(String localFileName, String remoteFileName, boolean upload) {
            this.localFileName = localFileName;
            this.remoteFileName = remoteFileName;
            this.upload = upload;
        }

        public synchronized void run() {
            if (done) return;
            try {
                if (!upload) storageService.download(localFileName, remoteFileName);
                else storageService.upload(localFileName, remoteFileName);
                done = true;
            } catch (StorageServiceOperationException e) {
                log.severe(e.getLocalizedMessage());
            }
        }

        public boolean isComplete() {
            return done;
        }
    }

    class DeleteRunnable implements TimedRunnable {

        private String fileName;
        private Boolean done = false;

        public DeleteRunnable(String fileName) {
            this.fileName = fileName;
        }

        public synchronized void run() {
            if (done) return;
            try {
                storageService.delete(fileName);
                done = true;
            } catch (StorageServiceOperationException e) {
                log.severe(e.getLocalizedMessage());
            }
        }

        public boolean isComplete() {
            return done;
        }
    }

    public UnreliableStorageServiceDecorator(StorageService storageService) {
        this.storageService = storageService;
    }

    public void init(String host, String userName, String password) {
        storageService.init(host, userName, password);
    }

    public boolean connect() throws StorageServiceUnreachableException {
        return storageService.connect();
    }

    public void disconnect() throws StorageServiceOperationException {
        storageService.disconnect();
    }

    public void download(String localFileName, String remoteFileName) throws StorageServiceOperationException {
        if (!runMultipleTimes(new TransferRunnable(localFileName, remoteFileName, false))) {
            throw new StorageServiceOperationException("Unable to download file: " + remoteFileName + ".");
        }
    }

    public void upload(String localFileName, String remoteFileName) throws StorageServiceOperationException {
        if (!runMultipleTimes(new TransferRunnable(localFileName, remoteFileName, true))) {
            throw new StorageServiceOperationException("Unable to upload file: " + localFileName + ".");
        }
    }

    public void delete(String fileName) throws StorageServiceOperationException {
        if (!runMultipleTimes(new DeleteRunnable(fileName))) {
            throw new StorageServiceOperationException("Unable to delete file: " + fileName + ".");
        }
    }

    public FileSystemNode listAll() throws StorageServiceOperationException {
        ListAllRunnable runnable = new ListAllRunnable();
        if (!runMultipleTimes(runnable) || runnable.getRootNode() == null) {
            throw new StorageServiceOperationException("Unable to get file list.");
        }
        return runnable.getRootNode();
    }

    public void setOption(String key, String value) throws InvalidOptionException {
        storageService.setOption(key, value);
    }

    public void setNameConverter(NameConverter nameConverter) {
        storageService.setNameConverter(nameConverter);
    }

    public NameConverter getNameConverter() {
        return storageService.getNameConverter();
    }

    private boolean runMultipleTimes(TimedRunnable task) {
        for (int i = 0; i < retryCount; ++i) {
            Thread thread = new Thread(task);
            thread.start();
            try {
                thread.join(timeout);
                if (task.isComplete()) return true;
                else log.info("Timeout occurred while executing task.");
            } catch (InterruptedException e) {
                log.severe(e.getLocalizedMessage());
            }
        }
        return false;
    }
}
