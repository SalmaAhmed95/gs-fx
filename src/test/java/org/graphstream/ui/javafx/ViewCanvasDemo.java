package org.graphstream.ui.javafx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.graphstream.algorithm.generator.Generator;
import org.graphstream.algorithm.generator.RandomEuclideanGenerator;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.view.Viewer;

import java.util.UUID;

/**
 * simple demo class
 * <p>
 * User: bowen
 * Date: 8/3/14
 */
public class ViewCanvasDemo extends Application
{
    private Application application;


    public static void main(String[] args)
    {
        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
        launch(args);
    }


    @Override
    public void start(Stage stage) throws Exception
    {
        final ViewNode view = new ViewNode();
        final Graph graph = new SingleGraph(UUID.randomUUID().toString());
        graph.addAttribute("ui.antialias");
        graph.addAttribute("ui.quality");
        graph.addAttribute("ui.stylesheet", "node:selected { fill-color: red; }");
        final Viewer viewer = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);

        viewer.addView(view);
        this.application = view.openInApplication();
        this.application.start(stage);

        final Runnable worker = new Runnable()
        {
            @Override
            public void run()
            {
                Generator generator = new RandomEuclideanGenerator();
                generator.addSink(graph);
                generator.begin();
                for (int i = 0; i < 200; i++)
                {
                    generator.nextEvents();
                    try
                    {
                        Thread.sleep(100);
                    }
                    catch (InterruptedException ex)
                    {
                        ex.printStackTrace();
                    }
                }
                generator.end();
            }
        };
        final Thread thread = new Thread(worker);
        thread.setDaemon(true);
        thread.start();
    }


    public void stop() throws Exception
    {
        this.application.stop();
        this.application = null;
        Platform.exit();
    }
}
