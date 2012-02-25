package at.ac.tuwien.multicloudstore.lib.storageservices;

public class StorageServiceOperationException extends Exception {

    private static final long serialVersionUID = -3170214431000873833L;

    public StorageServiceOperationException(Exception e) {
        super(e);
    }

    public StorageServiceOperationException(String s) {
        super(s);
    }

}
