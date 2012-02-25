package at.ac.tuwien.multicloudstore.lib.distribution;

import at.ac.tuwien.multicloudstore.lib.common.FileSystemNode;
import at.ac.tuwien.multicloudstore.lib.common.IncompatibleFileSystemsException;
import at.ac.tuwien.multicloudstore.lib.storageservices.StorageService;
import at.ac.tuwien.multicloudstore.lib.storageservices.StorageServiceOperationException;
import at.ac.tuwien.multicloudstore.lib.transfer.NoStorageServiceAvailableException;

import java.util.Random;

public class MirroredFileDistributor extends AbstractFileDistributor {

    private Random random = new Random();

    public void download(String localFileName, String remoteFileName) throws StorageServiceOperationException, NoStorageServiceAvailableException {
        if (storageServices.size() < 1) throw new NoStorageServiceAvailableException();
        ((StorageService) storageServices.values().toArray()[random.nextInt(storageServices.size())]).
                download(localFileName, remoteFileName);
    }

    public void upload(String localFileName, String remoteFileName) throws StorageServiceOperationException, NoStorageServiceAvailableException {
        if (storageServices.size() < 1) throw new NoStorageServiceAvailableException();
        for (StorageService service : storageServices.values()) {
            service.upload(localFileName, remoteFileName);
        }
    }

    public void delete(String fileName) throws StorageServiceOperationException, NoStorageServiceAvailableException {
        if (storageServices.size() < 1) throw new NoStorageServiceAvailableException();
        for (StorageService service : storageServices.values()) {
            service.delete(fileName);
        }
    }

    public FileSystemNode listAll() throws StorageServiceOperationException, NoStorageServiceAvailableException {
        if (storageServices.size() < 1) throw new NoStorageServiceAvailableException();
        FileSystemNode root = new FileSystemNode();
        for (StorageService service : storageServices.values()) {
            try {
                root.merge(service.listAll());
            } catch (IncompatibleFileSystemsException e) {
                throw new StorageServiceOperationException(e);
            }
        }
        root.sortByName();
        return root;
    }
}
