package com.softwareag.wm.invoke.audit;

import com.wm.data.IData;
import java.util.*;
import org.apache.commons.jxpath.*;

public class XPathExpression
{
    static HashMap<Class<?>, Boolean> registeredImpl = new HashMap<Class<?>, Boolean>();
    String xpath;
    boolean lenient;
    FunctionSet functions;

	
    public static JXPathContext getContext(IData d)
    {
        Class<?> c = d.getClass();
        synchronized(registeredImpl)
        {
            if(registeredImpl.get(c) == null)
            {
                JXPathIntrospector.registerDynamicClass(c, IDataPropertyHandler.class);
                registeredImpl.put(c, Boolean.TRUE);
            }
        }
        return JXPathContext.newContext(d);
    }

    public static boolean canParse(String xpath)
    {
    	try{
    		JXPathContext.compile(xpath);
    		return true;
    	} catch (JXPathException x) {
    		return false;
    	}
    }

    public XPathExpression(String xpath)
    {
        this.xpath = xpath;
        lenient = true;
        functions = new FunctionSet();
    }

    public boolean isLenient()
    {
        return lenient;
    }

    public void setLenient(boolean lenient)
    {
        this.lenient = lenient;
    }

    public void setFunctions(String namespace, Class<?> clazz)
    {
        functions.put(namespace, clazz);
    }

    public Object getObject(IData pipe)
    {
        JXPathContext ctx = getContext(pipe);
        ctx.setLenient(lenient);
        ctx.setFunctions(functions);
        return ctx.getValue(xpath);
    }

    public void setValue(IData pipe, Object value, Map<String, String> arrayMap)
    {
        JXPathContext ctx = getContext(pipe);
        ctx.setLenient(lenient);
        ctx.setFunctions(functions);
        ctx.setFactory(new IDataXPathFactory());
        String key;
        for(Iterator<String> iter = arrayMap.keySet().iterator(); iter.hasNext(); ctx.getVariables().declareVariable(key, arrayMap.get(key)))
            key = (String)iter.next();

        ctx.createPathAndSetValue(xpath, value);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	public Object[] getObjectArray(IData pipe)
    {
        JXPathContext ctx = getContext(pipe);
        ctx.setLenient(lenient);
        ctx.setFunctions(functions);
        ArrayList results = new ArrayList();
        for(Iterator it = ctx.iterate(xpath); it.hasNext(); results.add(it.next()));
        return results.size() != 0 ? results.toArray() : null;
    }

    @SuppressWarnings("rawtypes")
    private class FunctionSet implements Functions
    {
		private HashMap f;
        
    	FunctionSet()
        {
            f = new HashMap();
        }

        @SuppressWarnings("unchecked")
		public void put(String namespace, Class clazz)
        {
            f.put(namespace, new ClassFunctions(clazz, namespace));
        }

        public Function getFunction(String namespace, String name, Object params[])
        {
            Functions func = (Functions)f.get(namespace);
            return func == null ? null : func.getFunction(namespace, name, params);
        }

        public Set getUsedNamespaces()
        {
            return f.keySet();
        }
    }
}