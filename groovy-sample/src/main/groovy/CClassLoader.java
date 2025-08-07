import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Enumeration;

/**
 * @author chenyilei
 * 2022/05/09 17:12
 */
public class CClassLoader extends URLClassLoader {
    private static final URL[] a = new URL[0];

    private final ClassLoader classLoader = CClassLoader.class.getClassLoader();

    private final String[] c = new String[]{"groovy.", "groovyjarjarantlr.", "groovyjarjarasm.asm.", "groovyjarjarcommonscli.", "org.apache.groovy.", "org.codehaus.groovy."};

    public CClassLoader(ClassLoader paramClassLoader) {
        super(a, paramClassLoader);
    }

    public URL getResource(String paramString) {
        return super.getResource(paramString);
    }

    public Enumeration findResources(String paramString) throws IOException {
        return super.findResources(paramString);
    }

    protected Class loadClass(String paramString, boolean paramBoolean) throws ClassNotFoundException {
        if (Arrays.<String>stream(this.c).anyMatch(paramString::startsWith))
            return this.classLoader.loadClass(paramString);
        try {
            return super.loadClass(paramString, paramBoolean);
        } catch (ClassNotFoundException classNotFoundException) {
            return this.classLoader.loadClass(paramString);
        }
    }
}
