package ecplugins.ArtifactMultifileDownload.client;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.electriccloud.commander.client.ChainedCallback;
import com.electriccloud.commander.client.domain.Property;
import com.electriccloud.commander.client.requests.GetArtifactVersionsRequest;
import com.electriccloud.commander.client.requests.GetArtifactsRequest;
import com.electriccloud.commander.client.requests.GetPropertyRequest;
import com.electriccloud.commander.client.requests.GetRepositoriesRequest;
import com.electriccloud.commander.client.requests.SetPropertyRequest;
import com.electriccloud.commander.client.responses.CommanderError;
import com.electriccloud.commander.client.responses.DefaultArtifactListCallback;
import com.electriccloud.commander.client.responses.DefaultArtifactVersionListCallback;
import com.electriccloud.commander.client.responses.DefaultPropertyCallback;
import com.electriccloud.commander.client.responses.DefaultRepositoryListCallback;
import com.electriccloud.commander.client.responses.PropertyCallback;
import com.electriccloud.commander.gwt.client.ComponentBase;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.xml.client.DOMException;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.XMLParser;
import com.smartgwt.client.widgets.tree.Tree;

import ecplugins.ArtifactMultifileDownload.client.manifestdom.DirectoryNode;
import ecplugins.ArtifactMultifileDownload.client.manifestdom.FileNode;
import ecplugins.ArtifactMultifileDownload.client.manifestdom.FileSystemTreeNode;
import ecplugins.ArtifactMultifileDownload.client.manifestdom.ManifestNode;

public class Utils {
	private ComponentBase component;

	Utils(ComponentBase component) {
		this.component = component;
	}

	/**
	 * get url parameters from the current page
	 * 
	 * @param param
	 *            the parameter name
	 * @return the value of the parameter
	 */
	public String getUrlParameter(String param) {

		String value = "";
		Map<String, List<String>> getParameters = Window.Location.getParameterMap();
		for (Entry<String, List<String>> stringListEntry : getParameters.entrySet()) {
			String name = stringListEntry.getKey();
			List<String> values = stringListEntry.getValue();
			value = values.isEmpty() ? "" : values.get(0);
			if (name.contentEquals(param)) {
				component.getLog().debug("URL Parameters: name = " + name + ", value = " + value);
				break;
			}
		}
		return value;
	}

	/**
	 * set the property value to the specified property path.
	 * 
	 * @param propertyPath
	 *            the path of the property
	 * @param propertyValue
	 *            the value to be set
	 */
	public void setProperty(String propertyPath, String propertyValue) {

		SetPropertyRequest setPropReq = component.getRequestFactory().createSetPropertyRequest();

		setPropReq.setPropertyName(propertyPath);

		setPropReq.setValue(propertyValue);

		PropertyCallback propertyRequestCallback = new DefaultPropertyCallback(component) {
			@Override
			public void handleError(CommanderError error) {
				component.getLog().debug("Error code=" + error.getCode() + ", Error message=" + error.getMessage());
			}

			@Override
			public void handleResponse(Property property) {
			}
		};

		setPropReq.setCallback(propertyRequestCallback);

		component.doRequest(new ChainedCallback() {

			@Override
			public void onComplete()

			{
				component.getLog().debug("SetProperty completed!");
			}

		}, setPropReq);

	}

	/**
	 * build the getPropertyRequest
	 * 
	 * @param propertyName
	 *            the property name to request
	 * @param propertyCallback
	 *            the callback to handle the response
	 * @return
	 */
	public GetPropertyRequest buildGetPropertyRequest(String propertyName, DefaultPropertyCallback propertyCallback) {
		GetPropertyRequest getPropertyRequest = component.getRequestFactory().createGetPropertyRequest();

		getPropertyRequest.setCallback(propertyCallback);

		getPropertyRequest.setPropertyName(propertyName);

		return getPropertyRequest;

	}

	/**
	 * build the getRepositoriesRequest
	 * 
	 * @param repositoryListCallback
	 *            the call back to handle the response
	 * @return
	 */
	public GetRepositoriesRequest buildGetRepositoriesRequest(DefaultRepositoryListCallback repositoryListCallback) {
		GetRepositoriesRequest getRepositoriesRequest = component.getRequestFactory().createGetRepositoriesRequest();
		getRepositoriesRequest.setCallback(repositoryListCallback);
		return getRepositoriesRequest;
	}

	public GetArtifactsRequest buildGetArtifactsRequest(DefaultArtifactListCallback defaultArtifactListCallback) {
		GetArtifactsRequest getArtifactsRequest = component.getRequestFactory().createGetArtifactsRequest();
		getArtifactsRequest.setCallback(defaultArtifactListCallback);
		return getArtifactsRequest;
	}

	public GetArtifactVersionsRequest buildGetArtifactVersionsRequest(String artifactName,
			DefaultArtifactVersionListCallback defaultArtifactVersionListCallback) {
		GetArtifactVersionsRequest getArtifactVersionsRequest = component.getRequestFactory()
				.createGetArtifactVersionsRequest();
		getArtifactVersionsRequest.setCallback(defaultArtifactVersionListCallback);
		getArtifactVersionsRequest.setArtifactName(artifactName);
		return getArtifactVersionsRequest;
	}

	public void updateTreeWithNewManifest(String manifestXml, Tree fileSystemTree) {

		try {
			// parse the XML document into a DOM
			Document messageDom = XMLParser.parse(manifestXml);

			// find the manifest root node.
			Node manifestXmlNode = messageDom.getElementsByTagName("manifest").item(0);

			String version = ((Element) manifestXmlNode).getAttribute("version");
			String format = ((Element) manifestXmlNode).getAttribute("format");
			String sha1 = ((Element) manifestXmlNode).getAttribute("sha1");

			String manifestId = DOM.createUniqueId();

			FileSystemTreeNode manifestTreeNode = new ManifestNode(manifestId, ".", version, format, sha1, true);
			fileSystemTree.setRoot(manifestTreeNode);
			fileSystemTree.add(manifestTreeNode);

			parseFoldersAndFiles(fileSystemTree, manifestTreeNode, manifestXmlNode);
			// add one extra dummy node, we found that the last node will always not be shown.
			fileSystemTree.add(new FileSystemTreeNode(DOM.createUniqueId(), "dummy", false), manifestTreeNode);

		} catch (DOMException e) {
			FileSystemTreeNode manifestTreeNode = new ManifestNode("001", ".", "01", "tar", "sha1", true);
			fileSystemTree.setRoot(manifestTreeNode);
			fileSystemTree.add(manifestTreeNode);
			fileSystemTree.add(new DirectoryNode(DOM.createUniqueId(), "Couldn't find artifact version in repository.", false), manifestTreeNode);
			// add one extra dummy node, we found that the last node will always not be shown.
			fileSystemTree.add(new FileSystemTreeNode(DOM.createUniqueId(), "dummy", false), manifestTreeNode);
			component.getLog().debug("Could not parse manifest XML document");
			
		} catch (Exception ex) {
			component.getLog().debug("Could not parse manifest XML document ex" + ex.getMessage());

		}
	}

	private void parseFoldersAndFiles(Tree fileSystemTree, FileSystemTreeNode parentTreeNode, Node parentXmlNode) {
		// parse all child nodes
		NodeList childNodes = parentXmlNode.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			String nodeName = childNode.getNodeName();
			if (nodeName.equalsIgnoreCase("directory")) {
				String directoryId = DOM.createUniqueId();
				DirectoryNode directoryNode = new DirectoryNode(directoryId,
						((Element) childNode).getAttribute("name"), true);
				fileSystemTree.add(directoryNode, parentTreeNode);
				parseFoldersAndFiles(fileSystemTree, directoryNode, childNode);
			} else if (nodeName.equalsIgnoreCase("file")) {
				String fileId = DOM.createUniqueId();
				FileNode fileNode = new FileNode(fileId, ((Element) childNode).getAttribute("name"),
						((Element) childNode).getAttribute("size"), ((Element) childNode).getAttribute("sha1"), true);
				fileSystemTree.add(fileNode, parentTreeNode);
			}
		}
	}
}
