package at.ac.tuwien.multicloudstore.lib.distribution;

import at.ac.tuwien.multicloudstore.lib.common.FileSystemNode;
import at.ac.tuwien.multicloudstore.lib.storageservices.StorageService;
import at.ac.tuwien.multicloudstore.lib.storageservices.StorageServiceOperationException;
import at.ac.tuwien.multicloudstore.lib.transfer.NoStorageServiceAvailableException;

public interface FileDistributor {

    /**
     * Add a storage service.
     * @param name the name
     * @param storageService the storage service
     */
    void addStorageService(String name, StorageService storageService);

    /**
     * Removes a storage service.
     * @param name the name of the storage service to remove
     */
    void removeStorageService(String name);

    /**
     * Removes all storage services.
     */
    void clearStorageServices();

    /**
     * Transfer a remote file to the local filesystem.
     * @param localFileName The destination file.
     * @param remoteFileName The source file.
     * @throws at.ac.tuwien.multicloudstore.lib.storageservices.StorageServiceOperationException
     */
    void download(String localFileName, String remoteFileName) throws StorageServiceOperationException, NoStorageServiceAvailableException;

    /**
     * Transfer a local file to the remote filesystem.
     * @param localFileName The source file.
     * @param remoteFileName The destination file.
     * @throws StorageServiceOperationException
     */
    void upload(String localFileName, String remoteFileName) throws StorageServiceOperationException, NoStorageServiceAvailableException;

    /**
     * Removes a file permanently from the filesystem.
     * @param fileName The file to remove.
     * @return false on error, true else
     * @throws StorageServiceOperationException
     */
    void delete(String fileName) throws StorageServiceOperationException, NoStorageServiceAvailableException;

    /**
     * Creates a tree of FileSystemNodes which represent the directory structure (including files).
     * @return the root node
     * @throws StorageServiceOperationException
     */
    FileSystemNode listAll() throws StorageServiceOperationException, NoStorageServiceAvailableException;
}
