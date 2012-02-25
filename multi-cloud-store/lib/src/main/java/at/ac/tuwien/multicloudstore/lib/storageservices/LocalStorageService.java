package at.ac.tuwien.multicloudstore.lib.storageservices;

import at.ac.tuwien.multicloudstore.lib.common.FileSystemNode;
import at.ac.tuwien.multicloudstore.lib.naming.NameConverter;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * Allowed options:
 * - rootDirectory: Used as base directory for all operations.
 */
public class LocalStorageService implements StorageService {

    private Map<String, String> options = new HashMap<String, String>();
    private NameConverter nameConverter;

    public void init(String host, String userName, String password) {
        // nothing to be done
    }

    public boolean connect() throws StorageServiceUnreachableException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void disconnect() throws StorageServiceOperationException {
        // nothing to be done
    }

    private void list(FileSystemNode root) throws StorageServiceOperationException {
        File[] files;
        if (root.getName().equals("")) {
            if (options.containsKey("rootDirectory")) {
                files = new File(options.get("rootDirectory")).listFiles();
            } else {
                files = File.listRoots();
            }
        } else {
            files = new File(getRootDirectory() + root.getFullPath()).listFiles();
        }
        buildList(files, root);
        files = null; // memory is quite limited, so allow freeing objects
        for (FileSystemNode node: root.getChilds(FileSystemNode.TYPE_DIRECTORY)) {
            if (node.getName().equals(FileSystemNode.PARENT_FOLDER_NAME)) continue;
            list(node);
        }
    }

    public FileSystemNode listAll() throws StorageServiceOperationException {
        FileSystemNode root = new FileSystemNode();
        root.setName("");
        list(root);
        root.sortByName();
        return root;
    }

    public void setOption(String key, String value) {
        options.put(key, value);
    }


    public void setNameConverter(NameConverter nameConverter) {
        this.nameConverter = nameConverter;
    }

    public NameConverter getNameConverter() {
        return nameConverter;
    }

    private void buildList(File[] list, FileSystemNode root) {
        if (!root.getName().equals("")) {
            FileSystemNode parent = new FileSystemNode();
            parent.setName(FileSystemNode.PARENT_FOLDER_NAME);
            parent.setType(FileSystemNode.TYPE_DIRECTORY);
            root.addChild(parent);
        }
        if (list == null) return;

        for (File file : list) {
            FileSystemNode node = new FileSystemNode();

            if (file.isDirectory()) {
                node.setName(file.getName());
                if (node.getName().equals("")) node.setName(File.separator);
                node.setType(FileSystemNode.TYPE_DIRECTORY);
            } else {
                node.setName(file.getName());
                node.setType(FileSystemNode.TYPE_FILE);
            }
            root.addChild(node);
        }
    }

    /**
     * see http://stackoverflow.com/questions/106770/standard-concise-way-to-copy-a-file-in-java
     */
    public void download(String localFileName, String remoteFileName) throws StorageServiceOperationException {
        File destFile = new File(localFileName);
        File sourceFile = new File(getRootDirectory() + remoteFileName);
        try {
            if (!destFile.exists()) {
                destFile.mkdirs();
                destFile.createNewFile();
            }

            FileChannel source = null;
            FileChannel destination = null;
            try {
                try {
                    source = new FileInputStream(sourceFile).getChannel();
                    destination = new FileOutputStream(destFile).getChannel();
                    destination.transferFrom(source, 0, source.size());
                } catch (FileNotFoundException e) {
                    throw new StorageServiceOperationException(e);
                }
            } finally {
                if (source != null) {
                    source.close();
                }
                if (destination != null) {
                    destination.close();
                }
            }
        } catch (IOException e) {
            throw new StorageServiceOperationException(e);
        }
    }

    public void upload(String localFileName, String remoteFileName) throws StorageServiceOperationException {
        download(remoteFileName, localFileName);
    }

    public void delete(String fileName) throws StorageServiceOperationException {
        if (!((new File(getRootDirectory() + fileName)).delete())) {
            throw new StorageServiceOperationException("Unable to delete file: " + fileName + ".");
        }
    }

    private String getRootDirectory() {
        if (options.containsKey("rootDirectory")) {
            return options.get("rootDirectory") + File.separator;
        }
        return "";
    }
}
