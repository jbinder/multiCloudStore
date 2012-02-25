package at.ac.tuwien.multicloudstore.lib.distribution;

import at.ac.tuwien.multicloudstore.lib.storageservices.StorageService;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractFileDistributor implements FileDistributor {

    protected Map<String, StorageService> storageServices = new HashMap<String, StorageService>();

    public void addStorageService(String name, StorageService storageService) {
        storageServices.put(name, storageService);
    }

    public void removeStorageService(String name) {
        storageServices.remove(name);
    }

    public void clearStorageServices() {
        storageServices.clear();
    }

}
