
package ecplugins.ArtifactMultifileDownload.client;

import com.electriccloud.commander.gwt.client.ComponentContext;

import com.electriccloud.commander.gwt.client.Component;
import com.electriccloud.commander.gwt.client.ComponentBaseFactory;

/**
 * This factory is responsible for providing instances of the ArtifactMultifileDownload
 * class.
 */
public class ArtifactMultifileDownloadFactory
    extends ComponentBaseFactory
{
 
    //~ Methods ----------------------------------------------------------------

    @Override public Component createComponent(ComponentContext jso)
    {
        return new ArtifactMultifileDownload();
    }
}
