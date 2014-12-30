var s_base;
var baseElem = document.getElementById("commanderBase");

if (baseElem == null) {
    s_base = "/commander/";
}
else {
    s_base = baseElem.getAttribute("content");
}
            
var isomorphicDir = s_base +"plugins/ArtifactMultifileDownload/war/ecplugins.ArtifactMultifileDownload.ArtifactMultifileDownload/sc/";