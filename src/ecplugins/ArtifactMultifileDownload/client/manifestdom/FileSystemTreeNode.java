package ecplugins.ArtifactMultifileDownload.client.manifestdom;

import com.smartgwt.client.widgets.tree.TreeNode;

public class FileSystemTreeNode extends TreeNode {
	public FileSystemTreeNode(String nodeId, String name, boolean isOpen) {
		setAttribute("nodeId", nodeId);
		setAttribute("Name", name);
		setAttribute("isOpen", isOpen);
	}
}