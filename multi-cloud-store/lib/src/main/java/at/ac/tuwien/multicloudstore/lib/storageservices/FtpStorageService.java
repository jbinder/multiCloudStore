package at.ac.tuwien.multicloudstore.lib.storageservices;

import at.ac.tuwien.multicloudstore.lib.common.FileSystemNode;
import at.ac.tuwien.multicloudstore.lib.naming.NameConverter;
import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPFile;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Allowed options:
 * none
 */
public class FtpStorageService implements StorageService {

    private FTPClient client = new FTPClient();
    Map<Integer, Integer> typeMap = new HashMap<Integer, Integer>() {
        {
            put(FTPFile.TYPE_DIRECTORY, FileSystemNode.TYPE_DIRECTORY);
            put(FTPFile.TYPE_FILE, FileSystemNode.TYPE_FILE);
            put(FTPFile.TYPE_LINK, FileSystemNode.TYPE_OTHER);
        }
    };
    private String host;
    private String userName;
    private String password;
    private FileSystemNode fsList;
    private NameConverter nameConverter;
    private boolean reconnect;

    public void init(String host, String userName, String password) {
        this.host = host;
        this.userName = userName;
        this.password = password;
        this.reconnect = true;
    }

    public boolean connect() throws StorageServiceUnreachableException {
        if (client.isConnected() && !reconnect) return true;
        try {
            if (reconnect) {
                disconnect();
                reconnect = false;
            }
            client.connect(host);
            client.login(userName, password);
            client.setCompressionEnabled(client.isCompressionSupported());
        } catch (Exception e) {
            throw new StorageServiceUnreachableException(e); // TODOD: rename or throw different exception for login failure
        }
        return true;
    }

    public void disconnect() throws StorageServiceOperationException {
        if (!client.isConnected()) return;

        try {
            client.disconnect(true);
        } catch (Exception e) {
            throw new StorageServiceOperationException(e);
        }
    }

    private void list(FileSystemNode root) throws StorageServiceOperationException {
        FTPFile[] list;
        try {
            client.changeDirectory(root.getName());
            list = client.list();
            buildList(list, root);
            for (FileSystemNode node: root.getChilds(FileSystemNode.TYPE_DIRECTORY)) {
                if (node.getName().equals(FileSystemNode.PARENT_FOLDER_NAME)) continue;
                list(node);
            }
            client.changeDirectoryUp();
        } catch (Exception e) {
            throw new StorageServiceOperationException(e);
        }
    }

    private void reconnect() throws StorageServiceOperationException {
        disconnect();
        try {
            connect();
        } catch (StorageServiceUnreachableException e) {
            throw new StorageServiceOperationException(e);
        }
    }

    public FileSystemNode listAll() throws StorageServiceOperationException {
        reconnect();
        fsList = new FileSystemNode();
        fsList.setName("");
        list(fsList);
        disconnect();
        fsList.sortByName();
        return fsList;
    }

    public void setOption(String key, String value) throws InvalidOptionException {
        // no options
        throw new InvalidOptionException();
    }

    public void setNameConverter(NameConverter nameConverter) {
        this.nameConverter = nameConverter;
    }

    public NameConverter getNameConverter() {
        return nameConverter;
    }

    private void buildList(FTPFile[] list, FileSystemNode root) {
        if (!root.getName().equals("")) {
            FileSystemNode parent = new FileSystemNode(FileSystemNode.PARENT_FOLDER_NAME, FileSystemNode.TYPE_DIRECTORY);
            root.addChild(parent);
        }

        for (FTPFile file : list) {
            FileSystemNode node = new FileSystemNode(file.getName(), typeMap.get(file.getType()));
            root.addChild(node);
        }
    }

    public void download(String localFileName, String remoteFileName) throws StorageServiceOperationException {
        reconnect();
        try {
            new File(localFileName.substring(0, localFileName.lastIndexOf(File.separator))).mkdirs(); // TODO: check this
            client.download(remoteFileName, new File(localFileName));
        } catch (Exception e) {
            throw new StorageServiceOperationException(e);
        } finally {
            disconnect();
        }
    }

    public void upload(String localFileName, String remoteFileName) throws StorageServiceOperationException {
        reconnect();
        String remoteFileNamWithoutPath = last(remoteFileName.split(File.separator));
        String localFileNameWithoutPath = last(localFileName.split(File.separator));
        String tmpDirectory = UUID.randomUUID().toString();
        int status = 0;
        try {
            String[] directories = remoteFileName.split(File.separator);
            for (int i = 0; i < (directories.length - 1); ++i) {
                if (!Arrays.asList(client.listNames()).contains(directories[i])) {
                    client.createDirectory(directories[i]);
                }
                client.changeDirectory(directories[i]);
            }
            client.createDirectory(tmpDirectory);
            client.changeDirectory(tmpDirectory);
            status = 1;
            client.upload(new java.io.File(localFileName));
            status = 2;
            client.rename(localFileNameWithoutPath, ".." + File.separator + remoteFileNamWithoutPath);
            status = 3;
            client.changeDirectoryUp();
        } catch (Exception e) {
            throw new StorageServiceOperationException(e);
        } finally {
            if (0 < status) try {
                client.deleteDirectory(tmpDirectory);
            } catch (Exception e1) {
                // nothing to be done
            }
            disconnect();
        }
    }

    public static <T> T last(T[] array) {
        return array[array.length - 1];
    }

    public void delete(String fileName) throws StorageServiceOperationException {
        if (null == fsList) listAll();
        reconnect();
        FileSystemNode node = fsList;
        String[] subDirectories = fileName.split(File.separator);
        for (int i = 0; i < subDirectories.length; ++i) {
            node = node.getChild(subDirectories[i]);
            if (i >= subDirectories.length - 1) continue;
            try {
                client.changeDirectory(subDirectories[i]);
            } catch (Exception e) {
                throw new StorageServiceOperationException(e);
            }
        }
        try {
            if (node.getType() == FileSystemNode.TYPE_DIRECTORY) {
                client.deleteDirectory(node.getName());
            } else {
                client.deleteFile(node.getName());
            }
        } catch (Exception e) {
            throw new StorageServiceOperationException(e);
        } finally {
            disconnect();
        }
    }

}
