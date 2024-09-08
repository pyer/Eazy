//
// ========================================================================
// Copyright (c) 1995 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v. 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
// which is available at https://www.apache.org/licenses/LICENSE-2.0.
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package ab.eazy.util.component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import ab.eazy.util.Jetty;
import ab.eazy.util.StringUtil;
import ab.eazy.util.TypeUtil;
import ab.eazy.util.annotation.ManagedObject;
import ab.eazy.util.annotation.ManagedOperation;

@ManagedObject("Dumpable Object")
public interface Dumpable
{
    String KEY = "key: +- bean, += managed, +~ unmanaged, +? auto, +: iterable, +] array, +@ map, +> undefined\n";

    @ManagedOperation(value = "Dump the nested Object state as a String", impact = "INFO")
    default String dump()
    {
        return dump(this);
    }

    /**
     * Dump this object (and children) into an Appendable using the provided indent after any new lines.
     * The indent should not be applied to the first object dumped.
     *
     * @param out The appendable to dump to
     * @param indent The indent to apply after any new lines.
     * @throws IOException if unable to write to Appendable
     */
    void dump(Appendable out, String indent) throws IOException;

    /**
     * Utility method to dump to a {@link String}
     *
     * @param dumpable The dumpable to dump
     * @return The dumped string
     * @see #dump(Appendable, String)
     */
    static String dump(Dumpable dumpable)
    {
        StringBuilder b = new StringBuilder();
        dump(dumpable, b);
        return b.toString();
    }

    /**
     * Utility method to dump to an {@link Appendable}
     *
     * @param dumpable The dumpable to dump
     * @param out The destination of the dump
     */
    static void dump(Dumpable dumpable, Appendable out)
    {
        try
        {
            dumpable.dump(out, "");

            out.append(KEY);
            Runtime runtime = Runtime.getRuntime();
            Instant now = Instant.now();
            String zone = System.getProperty("user.timezone");
            out.append("JVM: %s %s %s; OS: %s %s %s; Jetty: %s; CPUs: %d; mem(free/total/max): %,d/%,d/%,d MiB\nUTC: %s; %s: %s".formatted(
                System.getProperty("java.vm.vendor"),
                System.getProperty("java.vm.name"),
                System.getProperty("java.vm.version"),
                System.getProperty("os.name"),
                System.getProperty("os.arch"),
                System.getProperty("os.version"),
                Jetty.VERSION,
                runtime.availableProcessors(),
                runtime.freeMemory() / (1024 * 1024),
                runtime.totalMemory() / (1024 * 1024),
                runtime.maxMemory() / (1024 * 1024),
                DateTimeFormatter.ISO_DATE_TIME.format(now.atOffset(ZoneOffset.UTC)),
                zone,
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(now.atZone(ZoneId.of(zone)))));
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The description of this/self found in the dump.
     * Allows for alternative representation of Object other then .toString()
     * where the long form output of toString() is represented in a cleaner way
     * within the dump infrastructure.
     *
     * @return the representation of self
     */
    default String dumpSelf()
    {
        return toString();
    }

    /**
     * Dump just an Object (but not it's contained items) to an Appendable.
     *
     * @param out The Appendable to dump to
     * @param o The object to dump.
     * @throws IOException May be thrown by the Appendable
     */
    static void dumpObject(Appendable out, Object o) throws IOException
    {
        try
        {
            String s;
            if (o == null)
                s = "null";
            else if (o instanceof Dumpable)
            {
                s = ((Dumpable)o).dumpSelf();
                s = StringUtil.replace(s, "\r\n", "|");
                s = StringUtil.replace(s, '\n', '|');
            }
            else if (o instanceof Collection collection)
                s = String.format("%s@%x(size=%d)", TypeUtil.toShortName(o.getClass()), o.hashCode(), collection.size());
            else if (o.getClass().isArray())
                s = String.format("%s@%x[size=%d]", o.getClass().getComponentType(), o.hashCode(), Array.getLength(o));
            else if (o instanceof Map map)
                s = String.format("%s@%x{size=%d}", TypeUtil.toShortName(o.getClass()), o.hashCode(), map.size());
            else if (o instanceof Map.Entry<?, ?> entry)
                s = String.format("%s=%s", entry.getKey(), entry.getValue());
            else
            {
                s = String.valueOf(o);
                s = StringUtil.replace(s, "\r\n", "|");
                s = StringUtil.replace(s, '\n', '|');
            }

            if (o instanceof LifeCycle)
                out.append(s).append(" - ").append((AbstractLifeCycle.getState((LifeCycle)o))).append("\n");
            else
                out.append(s).append("\n");
        }
        catch (Throwable th)
        {
            out.append("=> ").append(th.toString()).append("\n");
        }
    }

    /**
     * Dump an Object, it's contained items and additional items to an {@link Appendable}.
     * If the object in an {@link Iterable} or an {@link Array}, then its contained items
     * are also dumped.
     *
     * @param out the Appendable to dump to
     * @param indent The indent to apply after any new lines
     * @param object The object to dump. If the object is an instance
     * of {@link Container}, {@link Stream}, {@link Iterable}, {@link Array} or {@link Map},
     * then children of the object a recursively dumped.
     * @param extraChildren Items to be dumped as children of the object, in addition to any discovered children of object
     * @throws IOException May be thrown by the Appendable
     */
    static void dumpObjects(Appendable out, String indent, Object object, Object... extraChildren) throws IOException
    {
        dumpObject(out, object);
        
        int extras = extraChildren == null ? 0 : extraChildren.length;
        
        if (object instanceof Stream)
            object = ((Stream<?>)object).toArray();
        if (object instanceof Array)
            object = Arrays.asList((Object[])object);

        if (object instanceof Container)
        {
            dumpContainer(out, indent, (Container)object, extras == 0);
        }
        // Dump an Iterable Path because it may contain itself.
        if (object instanceof Iterable && !(object instanceof Path))
        {
            dumpIterable(out, indent, (Iterable<?>)object, extras == 0);
        }
        else if (object instanceof Map)
        {
            dumpMapEntries(out, indent, (Map<?, ?>)object, extras == 0);
        }
        
        if (extras == 0)
            return;

        int i = 0;
        for (Object item : extraChildren)
        {
            i++;
            String nextIndent = indent + (i < extras ? "|  " : "   ");
            out.append(indent).append("+> ");
            if (item instanceof Dumpable)
                ((Dumpable)item).dump(out, nextIndent);
            else
                dumpObjects(out, nextIndent, item);
        }
    }
    
    static void dumpContainer(Appendable out, String indent, Container object, boolean last) throws IOException
    {
        Container container = object;
        ContainerLifeCycle containerLifeCycle = container instanceof ContainerLifeCycle ? (ContainerLifeCycle)container : null;
        for (Iterator<Object> i = container.getBeans().iterator(); i.hasNext(); )
        {
            Object bean = i.next();

            if (container instanceof DumpableContainer && !((DumpableContainer)container).isDumpable(bean))
                continue; //won't be dumped as a child bean

            String nextIndent = indent + ((i.hasNext() || !last) ? "|  " : "   ");
            if (bean instanceof LifeCycle)
            {
                if (container.isManaged(bean))
                {
                    out.append(indent).append("+= ");
                    if (bean instanceof Dumpable)
                        ((Dumpable)bean).dump(out, nextIndent);
                    else
                        dumpObjects(out, nextIndent, bean);
                }
                else if (containerLifeCycle != null && containerLifeCycle.isAuto(bean))
                {
                    out.append(indent).append("+? ");
                    if (bean instanceof Dumpable)
                        ((Dumpable)bean).dump(out, nextIndent);
                    else
                        dumpObjects(out, nextIndent, bean);
                }
                else
                {
                    out.append(indent).append("+~ ");
                    dumpObject(out, bean);
                }
            }
            else if (containerLifeCycle != null && containerLifeCycle.isUnmanaged(bean))
            {
                out.append(indent).append("+~ ");
                dumpObject(out, bean);
            }
            else
            {
                out.append(indent).append("+- ");
                if (bean instanceof Dumpable)
                    ((Dumpable)bean).dump(out, nextIndent);
                else
                    dumpObjects(out, nextIndent, bean);
            }
        }
    }

    static void dumpIterable(Appendable out, String indent, Iterable<?> iterable, boolean last) throws IOException
    {
        for (Iterator i = iterable.iterator(); i.hasNext(); )
        {
            Object item = i.next();
            // Safety net to stop iteration when an Iterable contains itself e.g. Path.
            if (Objects.equals(item, iterable))
                return;
            String nextIndent = indent + ((i.hasNext() || !last) ? "|  " : "   ");
            out.append(indent).append("+: ");
            if (item instanceof Dumpable)
                ((Dumpable)item).dump(out, nextIndent);
            else
                dumpObjects(out, nextIndent, item);
        }
    }

    static void dumpMapEntries(Appendable out, String indent, Map<?, ?> map, boolean last) throws IOException
    {
        for (Iterator<? extends Map.Entry<?, ?>> i = map.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry entry = i.next();
            String nextIndent = indent + ((i.hasNext() || !last) ? "|  " : "   ");
            out.append(indent).append("+@ ").append(String.valueOf(entry.getKey())).append(" = ");
            Object item = entry.getValue();
            if (item instanceof Dumpable)
                ((Dumpable)item).dump(out, nextIndent);
            else
                dumpObjects(out, nextIndent, item);
        }
    }

    static Dumpable named(String name, Object object)
    {
        if (object instanceof Dumpable dumpable)
        {
            return new Dumpable()
            {
                @Override
                public String dumpSelf()
                {
                    return name + ": " + dumpable.dumpSelf();
                }

                @Override
                public void dump(Appendable out, String indent) throws IOException
                {
                    out.append(name).append(": ");
                    dumpable.dump(out, indent);
                }
            };
        }
        return (out, indent) ->
        {
            out.append(name).append(": ");
            Dumpable.dumpObjects(out, indent, object);
        };
    }

    /**
     * DumpableContainer
     *
     * A Dumpable that is a container of beans can implement this
     * interface to allow it to refine which of its beans can be
     * dumped.
     */
    interface DumpableContainer extends Dumpable
    {
        default boolean isDumpable(Object o)
        {
            return true;
        }
    }
}
