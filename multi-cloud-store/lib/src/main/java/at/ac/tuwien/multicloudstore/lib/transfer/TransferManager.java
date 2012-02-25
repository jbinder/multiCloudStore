package at.ac.tuwien.multicloudstore.lib.transfer;

import at.ac.tuwien.multicloudstore.lib.common.FileSystemNode;
import at.ac.tuwien.multicloudstore.lib.distribution.FileDistributor;
import at.ac.tuwien.multicloudstore.lib.distribution.MirroredFileDistributor;
import at.ac.tuwien.multicloudstore.lib.storageservices.*;

import java.util.logging.Level;
import java.util.logging.Logger;

public class TransferManager {
    public static final int TYPE_LOCAL = 0;
    public static final int TYPE_FTP = 1;
    public static final int TYPE_AMAZON_S3 = 2;

    private StorageService local = new LocalStorageService();
    private FileDistributor remote = new MirroredFileDistributor();

    public void addRemoteStorageService(int type, String address, String userName, String password) throws InvalidServiceTypeException {
        StorageService storageService = null;
        switch (type) {
            case TYPE_FTP:
                storageService = createFtpStorageService(address, userName, password);
                break;
            case TYPE_AMAZON_S3:
                storageService = createS3StorageService(address, userName, password);
                break;
            default: throw new InvalidServiceTypeException();
        }
        remote.addStorageService(
            createName(address, userName),
            new UnreliableStorageServiceDecorator(storageService));
    }

    private String createName(String address, String userName) {
        return userName + "@" + address;
    }

    public void clearRemoteStorageServices() {
        remote.clearStorageServices();
    }

    public void download(String localFileName, String remoteFileName) throws StorageServiceOperationException, NoStorageServiceAvailableException {
        remote.download(localFileName, remoteFileName);
    }

    /**
     * Transfer a local file to the remote filesystem.
     * @param localFileName The source file.
     * @param remoteFileName The destination file.
     * @throws StorageServiceOperationException
     * @throws NoStorageServiceAvailableException
     */
    public void upload(String localFileName, String remoteFileName) throws StorageServiceOperationException, NoStorageServiceAvailableException {
        remote.upload(localFileName, remoteFileName);
    }

    /**
     * Removes a file permanently from the filesystem.
     * @param fileName The file to remove.
     * @return false on error, true else
     * @throws StorageServiceOperationException
     * @throws NoStorageServiceAvailableException
     */
    public void deleteRemote(String fileName) throws StorageServiceOperationException, NoStorageServiceAvailableException {
        remote.delete(fileName);
    }

    /**
     * Creates a tree of FileSystemNodes which represent the directory structure (including files).
     * @return the root node
     * @throws StorageServiceOperationException
     * @throws at.ac.tuwien.multicloudstore.lib.storageservices.StorageServiceUnreachableException
     * @throws NoStorageServiceAvailableException
     */
    public FileSystemNode listRemote() throws StorageServiceOperationException, NoStorageServiceAvailableException, StorageServiceUnreachableException {
        return remote.listAll();
    }

    /**
     * Removes a file permanently from the filesystem.
     * @param fileName The file to remove.
     * @return false on error, true else
     * @throws StorageServiceOperationException
     */
    public void deleteLocal(String fileName) throws StorageServiceOperationException {
        local.delete(fileName);
    }

    /**
     * Creates a tree of FileSystemNodes which represent the directory structure (including files).
     * @return the root node
     * @throws StorageServiceOperationException
     */
    public FileSystemNode listLocal() throws StorageServiceOperationException {
        return local.listAll();
    }

    private StorageService createFtpStorageService(String address, String userName, String password) {
        StorageService service = new FtpStorageService();
        service.init(address, userName, password);
        return service;
    }

    private StorageService createS3StorageService(String address, String userName, String password) {
        StorageService service = new S3StorageService();
        service.init(address, userName, password);
        return service;
    }

    public void setLocalRootDirectory(String rootPath) {
        try {
            local.setOption("rootDirectory", rootPath);
        } catch (InvalidOptionException e) {
            Logger.getLogger(TransferManager.class.getName()).log(Level.SEVERE, e.getMessage());
        }
    }
}
