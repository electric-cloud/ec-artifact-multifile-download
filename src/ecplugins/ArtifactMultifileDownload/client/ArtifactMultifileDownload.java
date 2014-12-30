// ArtifactMultifileDownload.java --
//
// ArtifactMultifileDownload.java is part of the ArtifactMultifileDownload plugin.
//
// Copyright (c) 2005-2014 Electric Cloud, Inc.
// All rights reserved.
//

package ecplugins.ArtifactMultifileDownload.client;

import static com.electriccloud.commander.gwt.client.ComponentBaseFactory.getPluginName;

import java.util.LinkedHashMap;
import java.util.List;

import com.electriccloud.commander.client.ChainedCallback;
import com.electriccloud.commander.client.domain.Artifact;
import com.electriccloud.commander.client.domain.ArtifactVersion;
import com.electriccloud.commander.client.domain.Repository;
import com.electriccloud.commander.client.requests.GetArtifactVersionsRequest;
import com.electriccloud.commander.client.requests.GetArtifactsRequest;
import com.electriccloud.commander.client.requests.GetRepositoriesRequest;
import com.electriccloud.commander.client.responses.DefaultArtifactListCallback;
import com.electriccloud.commander.client.responses.DefaultArtifactVersionListCallback;
import com.electriccloud.commander.client.responses.DefaultRepositoryListCallback;
import com.electriccloud.commander.gwt.client.ComponentBase;
import com.electriccloud.commander.gwt.client.util.CommanderUrlBuilder;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;
import com.smartgwt.client.types.FormMethod;
import com.smartgwt.client.types.SelectionAppearance;
import com.smartgwt.client.types.TreeModelType;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.DrawEvent;
import com.smartgwt.client.widgets.events.DrawHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.HiddenItem;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeGridField;
import com.smartgwt.client.widgets.tree.TreeNode;

import ecplugins.ArtifactMultifileDownload.client.manifestdom.FileNode;

/**
 * Basic component that is meant to be cloned and then customized to perform a
 * real function.
 */
public class ArtifactMultifileDownload extends ComponentBase {

	// ~ Methods
	// ----------------------------------------------------------------

	private final DynamicForm artifactsSearchFrom = new DynamicForm();
	private final RadioGroupItem rgRepository = new RadioGroupItem("repositoryName");
	private final ComboBoxItem cbArtifact = new ComboBoxItem();
	private final ComboBoxItem cbArtifactVersion = new ComboBoxItem();
	private final HiddenItem hiArtifactVersion = new HiddenItem("artifactVersion");
	private final HiddenItem hiArtifactFiles = new HiddenItem("artifactFiles");

	private final Tree filesystemTree = new Tree();
	private final TreeGrid filesystemTreeGrid = new TreeGrid();
	private Utils myUtil;

	/**
	 * This function is called by SDK infrastructure to initialize the UI parts
	 * of this component.
	 *
	 * @return A widget that the infrastructure should place in the UI; usually
	 *         a panel.
	 */
	@Override
	public Widget doInit() {

		myUtil = new Utils(this);
		// Simple caption-panel that declares the plugin name and
		// component name.
		VLayout layout = new VLayout();
		layout.setMembersMargin(10);
		layout.setWidth100();
		layout.setHeight100();

		artifactsSearchFrom.setAction(CommanderUrlBuilder.getBase() + "plugins/" + getPluginName()
				+ "/cgi-bin/downloadArtifactFiles.cgi");
		artifactsSearchFrom.setTarget("_self");
		artifactsSearchFrom.setMethod(FormMethod.POST);
		artifactsSearchFrom.setWidth("95%");
		artifactsSearchFrom.setPadding(10);
		artifactsSearchFrom.setColWidths(120, "*");
		artifactsSearchFrom.setIsGroup(true);
		artifactsSearchFrom.setGroupTitle("Artifacts Search");

		cbArtifact.setTitle("Artifacts");
		cbArtifact.setWidth(300);
		cbArtifact.setHint("<nobr>Select the Artifacts</nobr>");
		cbArtifact.setType("comboBox");
		cbArtifact.addChangedHandler(new ChangedHandler() {

			@Override
			public void onChanged(ChangedEvent event) {
				cbArtifactVersion.clearValue();
				updateArtifactVersions(cbArtifact.getValue().toString(), "");
				filesystemTree.removeList(filesystemTree.getAllNodes());
			}
		});

		cbArtifactVersion.setTitle("Artifact Version");
		cbArtifactVersion.setWidth(300);
		cbArtifactVersion.setHint("<nobr>Select the artifact version</nobr>");
		cbArtifactVersion.setType("comboBox");
		cbArtifactVersion.setValue("");
		cbArtifactVersion.addChangedHandler(new ChangedHandler() {

			@Override
			public void onChanged(ChangedEvent event) {
				rgRepository.setValue("");
				filesystemTree.removeList(filesystemTree.getAllNodes());
			}

		});

		rgRepository.setTitle("Repository");
		rgRepository.setWidth(300);
		rgRepository.setVertical(false);
		cbArtifact.setHint("<nobr>Select the Repository</nobr>");
		rgRepository.addChangedHandler(new ChangedHandler() {
			
			@Override
			public void onChanged(ChangedEvent event) {
				try {
					filesystemTree.removeList(filesystemTree.getAllNodes());

					String fetchManifestUrl = Window.Location.getProtocol() + "//" + Window.Location.getHost()
							+ CommanderUrlBuilder.getBase() + "plugins/" + getPluginName()
							+ "/cgi-bin/fetchManifest.cgi?artifactVersion=" + cbArtifact.getValueAsString() + ":"
							+ cbArtifactVersion.getValueAsString() + "&repositoryName=" + rgRepository.getValueAsString();
					
					getLog().debug("Fetch Manifest URL:" + fetchManifestUrl);

					RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(fetchManifestUrl));

					try {
						builder.sendRequest(null, new RequestCallback() {
							public void onError(Request request, Throwable exception) {
								// Couldn't connect to server (could be
								// timeout, SOP violation, etc.)
								getLog().debug("request manifest error.");
							}

							public void onResponseReceived(Request request, Response response) {
								if (200 == response.getStatusCode()) {
									// Process the response in
									// response.getText()
									myUtil.updateTreeWithNewManifest(response.getText(), filesystemTree);
									filesystemTree.closeAll();
								} else {
									// Handle the error. Can get the
									// status text from
									getLog().debug("request manifest error.");
								}
							}
						});
					} catch (RequestException e) {
						// Couldn't connect to server
						getLog().debug("request manifest error.");
					}

				} catch (Exception ex) {
					getLog().debug("file tree update error.");
				}
			}
		});
		
		
		VLayout treeVLayout = new VLayout();
		treeVLayout.setWidth("95%");
		treeVLayout.setHeight(370);

		Label filesTreeLabel = new Label("Files:");
		filesTreeLabel.setHeight(30);

		filesystemTree.setModelType(TreeModelType.CHILDREN);
		filesystemTree.setRootValue(1);
		filesystemTree.setNameProperty("Name");
		filesystemTree.setIdField("nodeId");
		filesystemTree.setOpenProperty("isOpen");

		filesystemTreeGrid.setWidth100();
		filesystemTreeGrid.setHeight(350);
		filesystemTreeGrid.setShowOpenIcons(false);
		filesystemTreeGrid.setShowDropIcons(false);
		filesystemTreeGrid.setClosedIconSuffix("");
		filesystemTreeGrid.setData(filesystemTree);
		filesystemTreeGrid.setSelectionAppearance(SelectionAppearance.CHECKBOX);
		filesystemTreeGrid.setShowSelectedStyle(false);
		filesystemTreeGrid.setShowPartialSelection(true);
		filesystemTreeGrid.setCascadeSelection(true);

		TreeGridField nameField = new TreeGridField("Name");
		TreeGridField sha1Field = new TreeGridField("Sha1");
		TreeGridField sizeField = new TreeGridField("Size");
		filesystemTreeGrid.setFields(nameField, sha1Field, sizeField);

		filesystemTreeGrid.addDrawHandler(new DrawHandler() {
			public void onDraw(DrawEvent event) {
				filesystemTreeGrid.getTree().openAll();
			}
		});

		treeVLayout.addMember(filesTreeLabel);
		treeVLayout.addMember(filesystemTreeGrid);

		IButton downloadButton = new IButton("Download");
		downloadButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				// collect selected files and post it to backend and download
				StringBuilder builder = new StringBuilder();
				ListGridRecord[] selectedRecords = filesystemTreeGrid.getSelectedRecords();
				for (ListGridRecord listGridRecord : selectedRecords) {
					TreeNode node = Tree.nodeForRecord(listGridRecord);
					if (node instanceof FileNode) {
						builder.append(filesystemTree.getPath(node));
						builder.append(",");
					}
				}
				hiArtifactVersion.setValue(cbArtifact.getValueAsString() + ":" + cbArtifactVersion.getValueAsString());
				hiArtifactFiles.setValue(builder.toString());
				
				if(builder.toString().length() == 0){
					Window.alert("Please select at least one file to proceed.");
				} else{
					getLog().debug("form submitted: " + cbArtifact.getValueAsString() + ":" + cbArtifactVersion.getValueAsString() + ":" + builder.toString());
					artifactsSearchFrom.submitForm();
				}
			}
		});

		artifactsSearchFrom.setItems(cbArtifact, cbArtifactVersion, rgRepository, hiArtifactFiles, hiArtifactVersion);
		layout.addMember(artifactsSearchFrom);
		layout.addMember(treeVLayout);
		layout.addMember(downloadButton);

		getInitialRepositoriesAndArtifacts();

		return layout;
	}

	private void updateArtifactVersions(String artifactName, final String defaultValue) {
		final LinkedHashMap<String, String> artifactVersionMap = new LinkedHashMap<String, String>();

		GetArtifactVersionsRequest getArtifactVersionsRequest = myUtil.buildGetArtifactVersionsRequest(artifactName,
				new DefaultArtifactVersionListCallback(this) {

					@Override
					public void handleResponse(List<ArtifactVersion> response) {
						artifactVersionMap.clear();
						for (ArtifactVersion artifactVersion : response) {
							getLog().debug("Repository:" + artifactVersion.getRepositoryName());
							artifactVersionMap.put(artifactVersion.getVersion(), artifactVersion.getVersion());
						}
					}
				});

		doRequest(new ChainedCallback() {

			@Override
			public void onComplete() {
				cbArtifactVersion.setValueMap(artifactVersionMap);
				if (!defaultValue.equals("") && artifactVersionMap.containsKey(defaultValue)) {
					cbArtifactVersion.setValue(defaultValue);
					cbArtifactVersion.fireEvent(new ChangedEvent(null));
				}
			}
		}, getArtifactVersionsRequest);
	}

	private void getInitialRepositoriesAndArtifacts() {
		final LinkedHashMap<String, String> repositoryMap = new LinkedHashMap<String, String>();
		final LinkedHashMap<String, String> artifactsMap = new LinkedHashMap<String, String>();

		GetRepositoriesRequest getRepositoriesRequest = myUtil
				.buildGetRepositoriesRequest(new DefaultRepositoryListCallback(this) {

					@Override
					public void handleResponse(List<Repository> response) {
						repositoryMap.clear();
						for (Repository repository : response) {
							repositoryMap.put(repository.getName(), repository.getName());
						}

					}
				});

		GetArtifactsRequest getArtifactsRequest = myUtil
				.buildGetArtifactsRequest(new DefaultArtifactListCallback(this) {

					@Override
					public void handleResponse(List<Artifact> response) {
						artifactsMap.clear();
						for (Artifact artifact : response) {
							artifactsMap.put(artifact.getGroupId() + ":" + artifact.getArtifactKey(),
									artifact.getGroupId() + ":" + artifact.getArtifactKey());
						}
					}
				});

		doRequest(new ChainedCallback() {

			@Override
			public void onComplete() {
				rgRepository.setValueMap(repositoryMap);
				String repository = myUtil.getUrlParameter("repository");
				if (!repository.equals("") && repositoryMap.containsKey(repository)) {
					rgRepository.setValue(repository);
				}
				cbArtifact.setValueMap(artifactsMap);
				String artifact = myUtil.getUrlParameter("artifact");
				if (!artifact.equals("") && artifactsMap.containsKey(artifact)) {
					cbArtifact.setValue(artifact);
					String artifactVersion = myUtil.getUrlParameter("artifactVersion");
					updateArtifactVersions(artifact, artifactVersion);
				}

			}
		}, getRepositoriesRequest, getArtifactsRequest);

	}
}
