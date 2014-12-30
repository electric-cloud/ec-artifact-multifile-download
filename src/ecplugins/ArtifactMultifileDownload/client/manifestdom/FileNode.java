package ecplugins.ArtifactMultifileDownload.client.manifestdom;

public class FileNode extends FileSystemTreeNode {

	public FileNode(String nodeId, String name, String size, String sha1,
			boolean isOpen) {
		super(nodeId, name, isOpen);
		setAttribute("Size", size);
		setAttribute("Sha1", sha1);
		this.setIcon("[SKIN]/file.png");
	}
}
