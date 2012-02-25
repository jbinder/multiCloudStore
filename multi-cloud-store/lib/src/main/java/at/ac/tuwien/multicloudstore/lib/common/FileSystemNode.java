package at.ac.tuwien.multicloudstore.lib.common;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Represents a file system note, e.g. file, directory.
 */
public class FileSystemNode implements Comparable<FileSystemNode> {

    public static final int TYPE_DIRECTORY = 0;
    public static final int TYPE_FILE = 1;
    public static final int TYPE_OTHER = -1;

    public static final int STATUS_IN_SYNC = 0;
    public static final int STATUS_NEW = 1;

    public static final String PARENT_FOLDER_NAME = "..";

    private String name = "";
    private List<FileSystemNode> childs = new ArrayList<FileSystemNode>();
    private FileSystemNode parent = null;
    private int type = TYPE_OTHER;

    private int status = STATUS_NEW;

    public FileSystemNode() {
    }

    public FileSystemNode(String name, int type) {
        this.name = name;
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setChilds(List<FileSystemNode> childs) {
        this.childs = childs;
    }

    public List<FileSystemNode> getChilds() {
        return childs;
    }


    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public ArrayList<HashMap<String,String>> getChildsWithInfo() {
        ArrayList<HashMap<String,String>> list = new ArrayList<HashMap<String,String>>();
        for (FileSystemNode child : childs) {
            HashMap<String,String> temp = new HashMap<String,String>();
            temp.put("name", child.getName() + (child.getStatus() == FileSystemNode.STATUS_NEW ? " (*)" : ""));
            list.add(temp);
        }
        return list;
    }

    public FileSystemNode getChild(String name) {
        for (FileSystemNode child : childs) {
            if (child.getName().equals(name)) return child;
        }
        return null;
    }

    public List<FileSystemNode> getChilds(int typeFilter) {
        List<FileSystemNode> directories = new ArrayList<FileSystemNode>();
        for (FileSystemNode node : childs) {
            if (node.getType() != typeFilter) continue;
            directories.add(node);
        }
        return directories;
    }

    public void setType(int value) {
        type = value;
    }

    public int getType() {
        return type;
    }

    public void addChild(FileSystemNode node) {
        node.parent = this;
        childs.add(node);
    }

    public String toString() {
        String statusStr = (status == STATUS_NEW) ? " (*)" : "";
        return name + statusStr;
    }

    public String getFullPath() {
        String separator = (type == TYPE_DIRECTORY) ? File.separator : "";
        if (parent == null) return name + separator;
        return parent.getFullPath() + name + separator;
    }

    public FileSystemNode getParent() {
        return parent;
    }

    public FileSystemNode getFile(String fileName) {
        String[] path = fileName.split(File.separator);
        FileSystemNode result = this;
        for (String node : path) {
            result = result.getChild(node);
        }
        return result;
    }

    public void merge(FileSystemNode fileSystemNode) throws IncompatibleFileSystemsException {
        if (!equals(fileSystemNode)) throw new IncompatibleFileSystemsException();
        int status = FileSystemNode.STATUS_IN_SYNC;
        for (FileSystemNode child : fileSystemNode.childs) {
            if (childs.contains(child)) {
                childs.get(childs.indexOf(child)).merge(child);
                if (childs.get(childs.indexOf(child)).getStatus() != STATUS_IN_SYNC) status = STATUS_NEW;
            } else {
                child.setStatus(FileSystemNode.STATUS_NEW);
                status = FileSystemNode.STATUS_NEW;
                addChild(child);
            }
        }
        if (status == FileSystemNode.STATUS_IN_SYNC && (!(childs.size() == fileSystemNode.childs.size()))) {
            status = FileSystemNode.STATUS_NEW;
        }
        setStatus(status);
    }

    public void mergeChild(FileSystemNode fileSystemNode) throws IncompatibleFileSystemsException {
        int childPos = childs.indexOf(fileSystemNode);
        if (childPos > -1) childs.get(childPos).merge(fileSystemNode);
        else childs.add(fileSystemNode);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FileSystemNode)) return false;
        FileSystemNode node = (FileSystemNode) obj;
        // TODO: check for unique attributes
        return this.name.equals(node.name) && this.type == node.type;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (childs != null ? childs.hashCode() : 0);
        result = 31 * result + (parent != null ? parent.hashCode() : 0);
        result = 31 * result + type;
        return result;
    }

    public void sortByName() {
        Collections.sort(childs);
        for (FileSystemNode child: childs) {
            child.sortByName();
        }
    }

    public int compareTo(FileSystemNode fileSystemNode) {
        if (name.equals(PARENT_FOLDER_NAME)) return -1;
        return name.compareTo(fileSystemNode.name);
    }
}
