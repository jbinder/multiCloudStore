package at.ac.tuwien.multicloudstore.lib.naming;

public interface NameConverter {
    /**
     * Converts the filename to a name which is compatible with the service's restrictions.
     */
    String toRemoteName(String name);

    /**
     * Converts a compatible filename back to the original filename
     */
    String toLocalName(String name);
}
