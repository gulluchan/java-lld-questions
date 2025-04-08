import java.util.*;

class FileSystem {
    class FileNode {
        boolean isFile = false;
        Map<String, FileNode> children = new HashMap<>();
        String content = "";
        int size = 0;
        Map<String, Permissions> permissions = new HashMap<>(); // User -> Permissions

        public FileNode() {
        }
    }

    class Permissions {
        boolean read;
        boolean write;
        boolean execute;

        public Permissions(boolean read, boolean write, boolean execute) {
            this.read = read;
            this.write = write;
            this.execute = execute;
        }
    }

    FileNode root;

    public FileSystem() {
        root = new FileNode();
    }

    private FileNode getNode(String path) {
        FileNode current = root;
        if (!path.equals("/")) {
            String[] parts = path.split("/");
            for (int i = 1; i < parts.length; i++) {
                if (!current.children.containsKey(parts[i])) {
                    return null;
                }
                current = current.children.get(parts[i]);
            }
        }
        return current;
    }

    public List<String> ls(String path) {
        FileNode node = getNode(path);
        List<String> result = new ArrayList<>();
        if (node == null) {
            return result; // Or throw an exception
        }

        if (node.isFile) {
            String[] parts = path.split("/");
            result.add(parts[parts.length - 1]);
        } else {
            result.addAll(node.children.keySet());
            Collections.sort(result);
        }
        return result;
    }

    public void mkdir(String path) {
        FileNode current = root;
        String[] parts = path.split("/");
        for (int i = 1; i < parts.length; i++) {
            if (!current.children.containsKey(parts[i])) {
                current.children.put(parts[i], new FileNode());
            }
            current = current.children.get(parts[i]);
        }
    }

    public void addContentToFile(String filePath, String content) {
        FileNode current = root;
        String[] parts = filePath.split("/");
        for (int i = 1; i < parts.length - 1; i++) {
            if (!current.children.containsKey(parts[i])) {
                current.children.put(parts[i], new FileNode());
            }
            current = current.children.get(parts[i]);
        }
        String fileName = parts[parts.length - 1];
        if (!current.children.containsKey(fileName)) {
            current.children.put(fileName, new FileNode());
        }
        FileNode fileNode = current.children.get(fileName);
        fileNode.isFile = true;
        fileNode.content += content;
        fileNode.size = fileNode.content.length();
    }

    public String readContentFromFile(String filePath) {
        FileNode node = getNode(filePath);
        if (node != null && node.isFile) {
            return node.content;
        }
        return ""; // Or throw an exception
    }

    public void rm(String path) {
        if (path.equals("/")) {
            return; // Cannot remove root
        }
        String[] parts = path.split("/");
        String fileName = parts[parts.length - 1];
        String parentPath = "";
        if (parts.length > 1) {
            parentPath = String.join("/", Arrays.copyOfRange(parts, 1, parts.length - 1));
            if (!parentPath.isEmpty()) {
                parentPath = "/" + parentPath;
            } else {
                parentPath = "/";
            }
        } else {
            parentPath = "/";
        }

        FileNode parentNode = getNode(parentPath);
        if (parentNode == null || !parentNode.children.containsKey(fileName)) {
            return; // Path not found
        }

        FileNode nodeToRemove = parentNode.children.get(fileName);
        if (!nodeToRemove.isFile && !nodeToRemove.children.isEmpty()) {
            System.out.println("Cannot remove non-empty directory: " + path);
            return;
        }

        parentNode.children.remove(fileName);
    }

    public void cp(String sourcePath, String destinationPath) {
        FileNode sourceNode = getNode(sourcePath);
        if (sourceNode == null) {
            System.out.println("Source path not found: " + sourcePath);
            return;
        }

        String[] destParts = destinationPath.split("/");
        String destName = destParts[destParts.length - 1];
        String destParentPath = "";
        if (destParts.length > 1) {
            destParentPath = String.join("/", Arrays.copyOfRange(destParts, 1, destParts.length - 1));
            if (!destParentPath.isEmpty()) {
                destParentPath = "/" + destParentPath;
            } else {
                destParentPath = "/";
            }
        } else {
            destParentPath = "/";
        }

        FileNode destParentNode = getNode(destParentPath);
        if (destParentNode == null) {
            mkdir(destParentPath);
            destParentNode = getNode(destParentPath);
            if (destParentNode == null) {
                System.out.println("Failed to create destination parent directory: " + destParentPath);
                return;
            }
        }

        if (sourceNode.isFile) {
            FileNode newFile = new FileNode();
            newFile.isFile = true;
            newFile.content = sourceNode.content;
            newFile.size = sourceNode.size;
            destParentNode.children.put(destName, newFile);
        } else {
            // Deep copy of the directory
            FileNode newDir = copyDirectory(sourceNode);
            destParentNode.children.put(destName, newDir);
        }
    }

    private FileNode copyDirectory(FileNode sourceDir) {
        FileNode newDir = new FileNode();
        for (Map.Entry<String, FileNode> entry : sourceDir.children.entrySet()) {
            String name = entry.getKey();
            FileNode child = entry.getValue();
            if (child.isFile) {
                FileNode newFile = new FileNode();
                newFile.isFile = true;
                newFile.content = child.content;
                newFile.size = child.size;
                newDir.children.put(name, newFile);
            } else {
                newDir.children.put(name, copyDirectory(child));
            }
        }
        return newDir;
    }

    public void mv(String sourcePath, String destinationPath) {
        FileNode sourceNode = getNode(sourcePath);
        if (sourceNode == null) {
            System.out.println("Source path not found: " + sourcePath);
            return;
        }

        String[] sourceParts = sourcePath.split("/");
        String sourceName = sourceParts[sourceParts.length - 1];
        String sourceParentPath = "";
        if (sourceParts.length > 1) {
            sourceParentPath = String.join("/", Arrays.copyOfRange(sourceParts, 1, sourceParts.length - 1));
            if (!sourceParentPath.isEmpty()) {
                sourceParentPath = "/" + sourceParentPath;
            } else {
                sourceParentPath = "/";
            }
        } else {
            sourceParentPath = "/";
        }
        FileNode sourceParentNode = getNode(sourceParentPath);
        if (sourceParentNode == null || !sourceParentNode.children.containsKey(sourceName)) {
            System.out.println("Source parent or node not found.");
            return;
        }

        String[] destParts = destinationPath.split("/");
        String destName = destParts[destParts.length - 1];
        String destParentPath = "";
        if (destParts.length > 1) {
            destParentPath = String.join("/", Arrays.copyOfRange(destParts, 1, destParts.length - 1));
            if (!destParentPath.isEmpty()) {
                destParentPath = "/" + destParentPath;
            } else {
                destParentPath = "/";
            }
        } else {
            destParentPath = "/";
        }

        FileNode destParentNode = getNode(destParentPath);
        if (destParentNode == null) {
            mkdir(destParentPath);
            destParentNode = getNode(destParentPath);
            if (destParentNode == null) {
                System.out.println("Failed to create destination parent directory: " + destParentPath);
                return;
            }
        }

        if (destParentNode.children.containsKey(destName)) {
            System.out.println("Destination already exists: " + destinationPath);
            return;
        }

        destParentNode.children.put(destName, sourceNode);
        sourceParentNode.children.remove(sourceName);
    }

    public int getSize(String path) {
        FileNode node = getNode(path);
        if (node == null) {
            return 0;
        }
        if (node.isFile) {
            return node.size;
        } else {
            int totalSize = 0;
            for (FileNode child : node.children.values()) {
                totalSize += getSizeHelper(child);
            }
            return totalSize;
        }
    }

    private int getSizeHelper(FileNode node) {
        if (node.isFile) {
            return node.size;
        } else {
            int totalSize = 0;
            for (FileNode child : node.children.values()) {
                totalSize += getSizeHelper(child);
            }
            return totalSize;
        }
    }

    public void setPermissions(String path, String user, boolean read, boolean write, boolean execute) {
        FileNode node = getNode(path);
        if (node != null) {
            node.permissions.put(user, new Permissions(read, write, execute));
        }
    }

    public Permissions getPermissions(String path, String user) {
        FileNode node = getNode(path);
        if (node != null && node.permissions.containsKey(user)) {
            return node.permissions.get(user);
        }
        return null; // Or default permissions
    }

    // Example of checking read permission before reading a file
    public String readContentFromFile(String filePath, String user) {
        FileNode node = getNode(filePath);
        if (node != null && node.isFile) {
            Permissions p = getPermissions(filePath, user);
            if (p != null && p.read) {
                return node.content;
            } else if (p == null) {
                return node.content; // Default allow if no specific permissions
            } else {
                System.out.println("Permission denied to read: " + filePath + " for user: " + user);
                return null;
            }
        }
        return null;
    }

    // Example of checking write permission before adding content to a file
    public void addContentToFile(String filePath, String content, String user) {
        FileNode current = root;
        String[] parts = filePath.split("/");
        for (int i = 1; i < parts.length - 1; i++) {
            if (!current.children.containsKey(parts[i])) {
                current.children.put(parts[i], new FileNode());
            }
            current = current.children.get(parts[i]);
        }
        String fileName = parts[parts.length - 1];
        if (!current.children.containsKey(fileName)) {
            current.children.put(fileName, new FileNode());
        }
        FileNode fileNode = current.children.get(fileName);

        Permissions p = getPermissions(filePath, user);
        if (p != null && p.write) {
            fileNode.isFile = true;
            fileNode.content += content;
            fileNode.size = fileNode.content.length();
        } else if (p == null) {
            fileNode.isFile = true;
            fileNode.content += content;
            fileNode.size = fileNode.content.length();
        } else {
            System.out.println("Permission denied to write to: " + filePath + " for user: " + user);
        }
    }

    // Example of checking execute permission (can be used for directories to allow cd)
    public boolean canExecute(String path, String user) {
        FileNode node = getNode(path);
        if (node != null) {
            Permissions p = getPermissions(path, user);
            return p == null || p.execute; // Default allow if no specific permissions
        }
        return false;
    }
}

/**
 * Your FileSystem object will be instantiated and called as such:
 * FileSystem obj = new FileSystem();
 * List<String> param_1 = obj.ls(path);
 * obj.mkdir(path);
 * obj.addContentToFile(filePath,content);
 * String param_4 = obj.readContentFromFile(filePath);
 */
