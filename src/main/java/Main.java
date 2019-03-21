import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;
import java.net.URL;

public class Main {
    public static void main(String[] args) throws Exception {
        // 可以通过 java -Dport=8899 xxx.jar 指定端口，默认8899
        final int port= Integer.parseInt(System.getProperty("port", "8899"));
        // 可以通过 java -Dpath=/ xxx.jar 指定URL路径，默认使用根目录
        final String path= System.getProperty("path", "/");
        // 可以通过 java -Dwar=xxx.war xxx.jar 指定运行war包，默认与本项目同名，如果不存在就使用jar包自己
        // 指定war包之后，xxx.jar的作用就是一个start.jar的作用，即：启动jetty，并加载xxx.war，开始服务。
        final String war= System.getProperty("war", "hellojetty.war");

        // 获取代码运行位置，在调试阶段可能是一个classes目录，打成jar包之后，是那个jar包的位置
        URL location= Main.class.getProtectionDomain().getCodeSource().getLocation();
        File home= new File(location.getFile());
        System.out.println("home: "+ home);

        // 首先尝试加载war包，如果war包不存在，就认为当前jar包是一个可执行的war包（自带webapp资源）
        File warfile= new File(home.getParent(), war);
        if(!warfile.exists())
            warfile= home;
        System.out.println("warfile: "+ warfile);

        // Jetty 9.2.x 和之前的版本允许使用下面3行设置context
        WebAppContext context= new WebAppContext();
        context.setContextPath("/");
        context.setWar(warfile.getAbsolutePath());

        /*
        // 适用于Jetty 9.3.x：为了支持jsp，需要额外设置container，instance manager等属性
        // 具体可以参考下面文章或者官方示例代码
        // https://segmentfault.com/q/1010000006126591
        // https://github.com/jetty-project/embedded-jetty-jsp
        File tmpfile= new File(home.getParent(), "temp");
        context.setParentLoaderPriority(true);
        context.setTempDirectory(tmpfile);
        context.setAttribute("javax.servlet.context.tempdir", tmpfile);
        context.setAttribute("org.eclipse.jetty.containerInitializers",
                Arrays.asList(new ContainerInitializer(new JettyJasperInitializer(), null)));
        context.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());
        */

        Server server= new Server(port);

        // 适用于Jetty 9.4.x：参考以下官方示例，主要还是为了支持jsp
        // https://github.com/puppetlabs/trapperkeeper-webserver-jetty9/issues/140
        // https://www.eclipse.org/jetty/documentation/current/embedded-examples.html#embedded-webapp-jsp
        Configuration.ClassList classList= Configuration.ClassList
                .setServerDefault(server);
        classList.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
                "org.eclipse.jetty.annotations.AnnotationConfiguration");
        context.setAttribute(
                "org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                ".*/[^/]*servlet-api-[^/]*\\.jar$|.*/javax.servlet.jsp.jstl-.*\\.jar$|.*/[^/]*taglibs.*\\.jar$");

        server.setHandler(context);
        server.start();
        server.join();
    }
}
