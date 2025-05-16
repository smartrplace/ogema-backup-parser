/**
 * Copyright 2011-2018 Fraunhofer-Gesellschaft zur Förderung der angewandten Wissenschaften e.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartrplace.analysis.backuploader.resdb;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.array.BooleanArrayResource;
import org.ogema.core.model.array.ByteArrayResource;
import org.ogema.core.model.array.FloatArrayResource;
import org.ogema.core.model.array.IntegerArrayResource;
import org.ogema.core.model.array.StringArrayResource;
import org.ogema.core.model.array.TimeArrayResource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.persistence.DBConstants;
import org.ogema.resourcetree.SimpleResourceData;
import org.ogema.resourcetree.TreeElement;

/**
 *
 * @author jlapp
 */
public class TransientTreeElement implements TreeElement {
	
    protected static final AtomicInteger RESOURCE_IDS = new AtomicInteger(0);
    
    private final boolean decorating;
    private final boolean array;
    private Class<? extends Resource> type;
    private volatile TreeElement parent;
    private final String name;
    private final int resId;
    private final Map<String, TransientTreeElement> children = new HashMap<>();
	private final TransientResourceDB db;

    private volatile String appID;
    private volatile Object resRef;
    private volatile boolean active = false;
    private volatile Class<? extends Resource> listType;
    private volatile SimpleResourceData data;
    
    public TransientTreeElement(TransientTreeElement original, TreeElement newParent, TransientResourceDB db) {
        this(original.name, original.type, newParent, original.decorating, db);
        this.data = original.data;
        this.active = original.active;
        this.appID = original.appID;
        this.resRef = original.resRef;
        this.listType = original.listType;
        this.children.putAll(original.children);
    }
    
    public TransientTreeElement(String name, Class<? extends Resource> type, TreeElement parent, TransientResourceDB db) {
        this(name, type, parent, false, db);
    }
    
    public TransientTreeElement(String name, Class<? extends Resource> type, TreeElement parent, boolean decorating, SimpleResourceData data, TransientResourceDB db) {
        this(name,type,parent,decorating, db);
        this.data = data;
    }

    public TransientTreeElement(String name, Class<? extends Resource> type, TreeElement parent, boolean decorating, TransientResourceDB db) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(name, "name must not be null");
        this.parent = parent == db.ROOT ? null : parent;
        this.resId = RESOURCE_IDS.incrementAndGet();
		db.resourcesById.put(resId, this);
		this.db = db;
        this.name = name;
        this.decorating = decorating;
		
		if (parent == null && !name.equals("/")) {
			throw new IllegalArgumentException("null parent on new element " + name);
		}

        if (ResourceList.class.isAssignableFrom(type)){
            listType = findElementTypeOnParent();
            array = true;
        } else {
            array = false;
        }
        this.type = type;
		if (ValueResource.class.isAssignableFrom(type)) {
			this.data = new DefaultSimpleResourceData();
		}
		db.addOrUpdateResourceType(type);
		//System.out.printf("new %s:%s (%s)%n", name, type.getSimpleName(), data);
    }
    
    @SuppressWarnings("unchecked")
	protected final Class<? extends Resource> findElementTypeOnParent() {
        if (parent == null) {
            return null;
        }
		Class<? extends Resource> pType = parent.getType();
		for (Method m : pType.getMethods()) {
			if (m.getName().equals(getName()) && !m.isBridge() &&!m.isSynthetic() && Resource.class.isAssignableFrom(m.getReturnType())) {
				Type returnType = m.getGenericReturnType();
				if (returnType instanceof ParameterizedType) {
					Type[] actualTypes = ((ParameterizedType) returnType).getActualTypeArguments();
					if (actualTypes.length > 0) {
						return (Class<? extends Resource>) actualTypes[0];
					}
				}
			}
		}
		return null;
	}
    
    @Override
    public String toString() {
        return String.format("%s:%s (%d)", getName(), getType().getSimpleName(), getResID());
    }

    @Override
    public String getAppID() {
        return appID;
    }

    @Override
    public void setAppID(String appID) {
        Objects.requireNonNull(appID);
        this.appID = appID;
    }

    @Override
    public Object getResRef() {
        return resRef;
    }

    @Override
    public void setResRef(Object resRef) {
        Objects.requireNonNull(resRef);
        this.resRef = resRef;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public TreeElement getParent() {
        return parent;
    }

    public void setParent(TreeElement parent) {
    	this.parent = parent;
    }
    
    @Override
    public int getResID() {
        return resId;
    }

    @Override
    public int getTypeKey() {
		Class<? extends Resource> t = getType();
		if (t == null) {
			return DBConstants.TYPE_KEY_INVALID;
		} else if (BooleanResource.class.isAssignableFrom(t)) {
			return DBConstants.TYPE_KEY_BOOLEAN;
		} else if (FloatResource.class.isAssignableFrom(t)) {
			return DBConstants.TYPE_KEY_FLOAT;
		} else if (IntegerResource.class.isAssignableFrom(t)) {
			return DBConstants.TYPE_KEY_INT;
		} else if (TimeResource.class.isAssignableFrom(t)) {
			return DBConstants.TYPE_KEY_LONG;
		} else if (StringResource.class.isAssignableFrom(t)) {
			return DBConstants.TYPE_KEY_STRING;
		} else if (FloatArrayResource.class.isAssignableFrom(t)) {
			return DBConstants.TYPE_KEY_FLOAT_ARR;
		} else if (BooleanArrayResource.class.isAssignableFrom(t)) {
			return DBConstants.TYPE_KEY_BOOLEAN_ARR;
		} else if (IntegerArrayResource.class.isAssignableFrom(t)) {
			return DBConstants.TYPE_KEY_INT_ARR;
		} else if (TimeArrayResource.class.isAssignableFrom(t)) {
			return DBConstants.TYPE_KEY_LONG_ARR;
		} else if (StringArrayResource.class.isAssignableFrom(t)) {
			return DBConstants.TYPE_KEY_STRING_ARR;
		} else if (ByteArrayResource.class.isAssignableFrom(t)) {
			return DBConstants.TYPE_KEY_OPAQUE;
		} else if (ResourceList.class.isAssignableFrom(t)) {
			return DBConstants.TYPE_KEY_COMPLEX_ARR;
		}
        return DBConstants.TYPE_KEY_COMPLEX;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<? extends Resource> getType() {
        return type;
    }

    @Override
    public boolean isNonpersistent() {
		return false;
        //throw new UnsupportedOperationException(getClass().getSimpleName()+"#isNonpersistent");
    }

    @Override
    public boolean isDecorator() {
        return decorating;
    }

    @Override
    public boolean isToplevel() {
        return parent == null;
    }

    @Override
    public boolean isReference() {
        return false;
    }

    @Override
    public boolean isComplexArray() {
        return array;
    }

    @Override
    public synchronized TreeElement addChild(String name, Class<? extends Resource> type, boolean isDecorating) {
		//System.out.printf("%s addChild %s:%s%n", this.name, name, type.getSimpleName());
        if (!isDecorating) {
            Class<?> optionalType = getOptionalElementType(name);
            if (optionalType == null) {
                throw new IllegalArgumentException("not an optional element");
            }
            if (!optionalType.isAssignableFrom(type)) {
                throw new IllegalArgumentException("type does not match definition of optional subresource");
            }
            TransientTreeElement el = new TransientTreeElement(name, type, this, db);
//            el.optional = true;
            children.put(name, el);
            return el;
        } else {
            TransientTreeElement decorator = new TransientTreeElement(name, type, this, true, db);
            children.put(name, decorator);
            return decorator;
        }
    }

    /* return the type of an optional element or null if there is no such element */
    protected Class<?> getOptionalElementType(String optionalName) {
        try {
            Method m = type.getMethod(optionalName);
            return m.getReturnType();
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    @Override
    public synchronized TreeElement addReference(TreeElement ref, String name, boolean isDecorating) {
        TransientTreeElement existingChild = children.get(name);
        if (existingChild != null && existingChild.isReference()){
            ((ReferenceElement)existingChild).setReference(ref);
            return existingChild;
        } else {
            ReferenceElement refElement = new ReferenceElement(ref, name, this, isDecorating, db);
            children.put(name, refElement);
            return refElement;
        }
    }

    @Override
    public synchronized SimpleResourceData getData() {
		//System.out.println("getData " + this);
        if (data == null) {
            data = new DefaultSimpleResourceData();
        }
        return data;
    }

    @Override
    public synchronized List<TreeElement> getChildren() {
        List<TreeElement> rval = new ArrayList<>(children.size());
        rval.addAll(children.values());
		//System.out.printf("%s getChildren = %s%n", this, rval);
        return rval;
    }

    @Override
    public synchronized TreeElement getChild(String childName) {
        TransientTreeElement el = children.get(childName);
		//System.out.printf("%s getChild %s = %s%n", this, childName, el);
		return el;
    }

    @Override
    public TreeElement getReference() {
        throw new UnsupportedOperationException(getClass().getSimpleName()+"#getReference");
    }

    @Override
    public void fireChangeEvent() {
    }

	@Override
	public String getPath() {
		StringBuilder sb = new StringBuilder(getName());
        for (TreeElement e = getParent(); e != null; e = e.getParent()){
            sb.insert(0, "/").insert(0, e.getName());
        }
		//System.out.printf("%s getPath = %s%n", this, sb);
        return sb.toString();
	}

	@Override
	public Class<? extends Resource> getResourceListType() {
		return listType;
	}

	@Override
	public void setResourceListType(Class<? extends Resource> cls) {
		listType = cls;		
	}

	@Override
	public void setLastModified(long time) {
	}

	@Override
	public long getLastModified() {
		return -1;
	}

	@Override
	public String getLocation() {
		// TODO Auto-generated method stub
		return getPath();
	}
    
    //XXX TransientTreeElement should be a VirtualTreeElement (use constrainType)
    public void setType(Class<? extends Resource> type) {
        this.type = type;
    }

}
