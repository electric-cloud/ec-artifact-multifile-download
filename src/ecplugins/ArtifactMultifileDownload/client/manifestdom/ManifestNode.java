package ecplugins.ArtifactMultifileDownload.client.manifestdom;

public class ManifestNode extends FileSystemTreeNode {

	public ManifestNode(String nodeId, String name, String version,
			String format, String sha1, boolean isOpen) {
		super(nodeId, name, isOpen);
		setAttribute("Version", version);
		setAttribute("Format", format);
		setAttribute("Sha1", sha1);
	}

}
