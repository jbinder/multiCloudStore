package at.ac.tuwien.multicloudstore.lib.naming;

public class DefaultNameConverter implements NameConverter {

    public String toRemoteName(String name) {
        return name;
    }

    public String toLocalName(String name) {
        return name;
    }
}
