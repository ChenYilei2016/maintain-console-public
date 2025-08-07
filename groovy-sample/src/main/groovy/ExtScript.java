import groovy.lang.Script;

/**
 * @author chenyilei
 * 2022/05/09 17:17
 */
public abstract class ExtScript extends Script {
    public Object get(Class paramClass, Integer paramInteger) {
        paramInteger = Integer.valueOf((paramInteger == null) ? 10 : paramInteger.intValue());
//        VmTool vmTool = k.a();
//        return vmTool.getInstances(paramClass, paramInteger.intValue());
        return null;
    }

    public Object get(Class paramClass) {
        return get(paramClass, Integer.valueOf(10));
    }
}