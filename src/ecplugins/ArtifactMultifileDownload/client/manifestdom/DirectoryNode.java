package ecplugins.ArtifactMultifileDownload.client.manifestdom;

public class DirectoryNode extends FileSystemTreeNode {

	public DirectoryNode(String nodeId, String name, boolean isOpen) {
		super(nodeId, name, isOpen);
		this.setIcon("[SKIN]/folder_file.png");
	}

}
