package at.ac.tuwien.multicloudstore.lib.storageservices;

import at.ac.tuwien.multicloudstore.lib.common.FileSystemNode;
import at.ac.tuwien.multicloudstore.lib.common.IncompatibleFileSystemsException;
import at.ac.tuwien.multicloudstore.lib.naming.NameConverter;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;
import org.opencredo.cloud.storage.BlobDetails;
import org.opencredo.cloud.storage.ContainerStatus;
import org.opencredo.cloud.storage.StorageException;
import org.opencredo.cloud.storage.StorageOperations;
import org.opencredo.cloud.storage.s3.AwsCredentials;
import org.opencredo.cloud.storage.s3.S3Template;

import java.io.File;
import java.util.List;

/**
 * Allowed options:
 * none
 */
public class S3StorageService implements StorageService {

    private StorageOperations template;
    private S3Service s3Service;
    private String host;
    private String userName;
    private String password;
    private NameConverter nameConverter;
    private boolean reconnect;

    public void init(String host, String userName, String password) {
        this.host = host;
        this.userName = userName;
        this.password = password;
        this.reconnect = true;
    }

    public boolean connect() throws StorageServiceUnreachableException {
        if (template != null && !reconnect) return true;

        try {
            if (reconnect) {
                disconnect();
                reconnect = false;
            }
        } catch (Exception e) {
            throw new StorageServiceUnreachableException(e);
        }

        AwsCredentials awsCredentials = new AwsCredentials(userName, password);
        String containerName = host;
        try {
            template = new S3Template(awsCredentials, containerName);
            if (template.checkContainerStatus(containerName) == ContainerStatus.DOES_NOT_EXIST) {
                throw new StorageServiceUnreachableException(new Exception("Bucket not found."));
            /* TODO: create bucket if not exists?
                template.createContainer(containerName);
            */
            }
            s3Service = new RestS3Service(new AWSCredentials(userName, password));
        } catch (StorageException e) {
            throw new StorageServiceUnreachableException(e);
        } catch (S3ServiceException e) {
            throw new StorageServiceUnreachableException(e);
        }

        return true;
    }

    public void disconnect() throws StorageServiceOperationException {
        // template = null;
    }

    private void list(FileSystemNode root) throws StorageServiceOperationException {
        try {
            List<BlobDetails> list = template.listContainerObjectDetails();
            buildList(list, root);
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
        FileSystemNode fsList = new FileSystemNode();
        fsList.setName("");
        list(fsList);
        disconnect();
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

    private void buildList(List<BlobDetails> list, FileSystemNode root) throws IncompatibleFileSystemsException {
        for (BlobDetails file : list) {
            String[] path = file.getName().split("/");
            if (path.length > 1) {
                FileSystemNode last = null;
                FileSystemNode first = null;
                for (int i = 0; i < path.length - 1; ++i) {
                    FileSystemNode child = new FileSystemNode(path[i], FileSystemNode.TYPE_DIRECTORY);
                    if (last != null) {
                        last.addChild(child);
                    }
                    if (first == null) first = child;
                    last = child;
                }
                last.addChild(new FileSystemNode(path[path.length - 1], FileSystemNode.TYPE_FILE));
                root.mergeChild(first);
            } else {
                root.mergeChild(new FileSystemNode(file.getName(), FileSystemNode.TYPE_FILE));
            }
        }
        addParentFolders(root);
        root.sortByName();
    }

    private void addParentFolders(FileSystemNode root) {
        if (root.getName().equals(FileSystemNode.PARENT_FOLDER_NAME)) return;

        if (!root.getName().equals("")) {
            root.addChild(new FileSystemNode(FileSystemNode.PARENT_FOLDER_NAME, FileSystemNode.TYPE_DIRECTORY));
        }
        for (FileSystemNode child: root.getChilds()) {
            addParentFolders(child);
        }
    }

    public void download(String localFileName, String remoteFileName) throws StorageServiceOperationException {
        reconnect();
        try {
            new File(localFileName.substring(0, localFileName.lastIndexOf(File.separator))).mkdirs(); // TODO: check this
            template.receiveAndSaveToFile(remoteFileName, new File(localFileName));
        } catch (Exception e) {
            throw new StorageServiceOperationException(e);
        } finally {
            disconnect();
        }
    }

    public void upload(String localFileName, String remoteFileName) throws StorageServiceOperationException {
        reconnect();
        try {
            File fileToUpload = new File(localFileName);
            template.send(fileToUpload);
            s3Service.renameObject(host, fileToUpload.getName(), new S3Object(remoteFileName));
        } catch (Exception e) {
            throw new StorageServiceOperationException(e);
        } finally {
            disconnect();
        }
    }

    public void delete(String fileName) throws StorageServiceOperationException {
        reconnect();
        try {
            template.deleteObject(fileName);
        } catch (Exception e) {
            throw new StorageServiceOperationException(e);
        } finally {
            disconnect();
        }
    }

}
