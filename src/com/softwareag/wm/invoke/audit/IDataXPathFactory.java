package com.softwareag.wm.invoke.audit;


import com.wm.data.*;
import java.util.Date;
import org.apache.commons.jxpath.*;

public class IDataXPathFactory extends AbstractFactory
{

    public IDataXPathFactory()
    {
    }

    public boolean createObject(JXPathContext context, Pointer pointer, Object parent, String name, int index)
    {
        int type = 2;
        boolean hasvar = context.getVariables().isDeclaredVariable(name);
        if(hasvar)
            type = ((Integer)context.getVariables().getVariable(name)).intValue();
        if(parent != null && (parent instanceof IData))
        {
            if(ValuesEmulator.get((IData)parent, name) == null && index < 1 && !hasvar)
            {
                IData collection = IDataFactory.create();
                ValuesEmulator.put((IData)parent, name, collection);
            }
            if(hasvar)
                try
                {
                    int size = 1;
                    if(index > 0)
                        size = index;
                    boolean present = ValuesEmulator.get((IData)parent, name) != null;
                    switch(type)
                    {
                    default:
                        break;

                    case 102: // 'f'
                    case 104: // 'h'
                        IData array[] = null;
                        if(!present)
                        {
                            array = new IData[size];
                            ValuesEmulator.put((IData)parent, name, array);
                        }
                        if(index > 0)
                            array[index - 1] = IDataFactory.create();
                        else
                            array[0] = IDataFactory.create();
                        break;

                    case 101: // 'e'
                        if(!present)
                        {
                            String oarray[] = new String[size];
                            ValuesEmulator.put((IData)parent, name, oarray);
                        }
                        break;

                    case 10: // '\n'
                    case 103: // 'g'
                        if(!present)
                        {
                            Object oarray = ((Object) (new Object[size]));
                            ValuesEmulator.put((IData)parent, name, oarray);
                        }
                        break;

                    case 4: // '\004'
                        if(!present)
                        {
                            Double darray[] = new Double[size];
                            ValuesEmulator.put((IData)parent, name, darray);
                        }
                        break;

                    case 6: // '\006'
                        if(!present)
                        {
                            Integer iarray[] = new Integer[size];
                            ValuesEmulator.put((IData)parent, name, iarray);
                        }
                        break;

                    case 8: // '\b'
                        if(!present)
                        {
                            Short sarray[] = new Short[size];
                            ValuesEmulator.put((IData)parent, name, sarray);
                        }
                        break;

                    case 7: // '\007'
                        if(!present)
                        {
                            Long larray[] = new Long[size];
                            ValuesEmulator.put((IData)parent, name, larray);
                        }
                        break;

                    case 5: // '\005'
                        if(!present)
                        {
                            Float farray[] = new Float[size];
                            ValuesEmulator.put((IData)parent, name, farray);
                        }
                        break;

                    case 9: // '\t'
                        if(!present)
                        {
                            Date farray[] = new Date[size];
                            ValuesEmulator.put((IData)parent, name, farray);
                        }
                        break;

                    case 1: // '\001'
                        if(!present)
                        {
                            Boolean farray[] = new Boolean[size];
                            ValuesEmulator.put((IData)parent, name, farray);
                        }
                        break;

                    case 2: // '\002'
                        if(!present)
                        {
                            Byte farray[] = new Byte[size];
                            ValuesEmulator.put((IData)parent, name, farray);
                        }
                        break;

                    case 3: // '\003'
                        if(!present)
                        {
                            Character farray[] = new Character[size];
                            ValuesEmulator.put((IData)parent, name, farray);
                        }
                        break;
                    }
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                }
            return true;
        } else
        {
            return false;
        }
    }
}
