package com.softwareag.wm.invoke.audit;

import com.wm.data.*;
import java.util.HashSet;
import org.apache.commons.jxpath.DynamicPropertyHandler;

public class IDataPropertyHandler implements DynamicPropertyHandler
{

    public static boolean DEBUG = false;
    
	public IDataPropertyHandler()
    {
    }

    public Object getProperty(Object tgt, String key)
    {
        Object val = null;
        if(DEBUG)
        {
            System.err.println("\t\t*** IDPH ***");
            System.err.println((new StringBuilder()).append("\t\ttgt = ").append(tgt).toString());
            System.err.println((new StringBuilder()).append("\t\tkey = ").append(key).toString());
        }
        if(tgt instanceof IData)
            val = ValuesEmulator.get((IData)tgt, key);
        if(DEBUG)
        {
            System.err.println((new StringBuilder()).append("\t\tval = ").append(val).toString());
            System.err.println("\t\t************");
        }
        return val;
    }

    public String[] getPropertyNames(Object tgt)
    {
        String keys[] = null;
        if(tgt instanceof IData)
        {
            HashSet<String> set = new HashSet<String>();
            IDataCursor c;
            for(c = ((IData)tgt).getCursor(); c.next(); set.add(c.getKey()));
            keys = (String[])(String[])set.toArray(new String[set.size()]);
            c.destroy();
        }
        return keys;
    }

    public void setProperty(Object tgt, String key, Object val)
    {
        if(tgt instanceof IData)
            ValuesEmulator.put((IData)tgt, key, val);
    }

}
