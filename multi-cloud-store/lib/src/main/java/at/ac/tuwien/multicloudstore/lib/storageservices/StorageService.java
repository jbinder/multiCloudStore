package at.ac.tuwien.multicloudstore.lib.storageservices;

import at.ac.tuwien.multicloudstore.lib.common.FileSystemNode;
import at.ac.tuwien.multicloudstore.lib.naming.NameConverter;

public interface StorageService {

    /**
     * Sets the data required to connect to the server.
     * @param host Address of the server.
     * @param userName The username to login with.
     * @param password The password to login with.
     */
    void init(String host, String userName, String password);

    /**
     * Connect and login to a storage server.
     * @return false on errors, true else
     * @throws StorageServiceUnreachableException
     */
    boolean connect() throws StorageServiceUnreachableException;

    /**
     * Closes the currently open connection.
     * @throws StorageServiceOperationException
     */
    void disconnect() throws StorageServiceOperationException;

    /**
     * Transfer a remote file to the local filesystem.
     * @param localFileName The destination file.
     * @param remoteFileName The source file.
     * @throws StorageServiceOperationException
     */
    void download(String localFileName, String remoteFileName) throws StorageServiceOperationException;

    /**
     * Transfer a local file to the remote filesystem.
     * @param localFileName The source file.
     * @param remoteFileName The destination file.
     * @throws StorageServiceOperationException
     */
    void upload(String localFileName, String remoteFileName) throws StorageServiceOperationException;

    /**
     * Removes a file permanently from the filesystem.
     * @param fileName The file to remove.
     * @return false on error, true else
     * @throws StorageServiceOperationException
     */
    void delete(String fileName) throws StorageServiceOperationException;

    /**
     * Creates a tree of FileSystemNodes which represent the directory structure (including files).
     * @return the root node
     * @throws StorageServiceOperationException
     */
    FileSystemNode listAll() throws StorageServiceOperationException;

    /**
     * Sets a service provider specific option.
     * @param key Identification of the option.
     * @param value Value of the option.
     * @throws InvalidOptionException
     */
    void setOption(String key, String value) throws InvalidOptionException;

    /**
     * @param nameConverter The naming converter.
     */
    void setNameConverter(NameConverter nameConverter);

    /**
     * @return The naming converter.
     */
    NameConverter getNameConverter();
}
