gs-fx
=====

JavaFX user interface for GraphStream.

Examples can be found in the src/test/java folder.  A quick JavaFX view can be setup as simple as: 

```
Viewer viewer = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
ViewNode view = new ViewNode(viewer);
viewer.addView(view);
viewer.enableAutoLayout();
view.getCamera().setAutoFitView(true);
```
