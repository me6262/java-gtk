package ch.bailu.gtk.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ch.bailu.gtk.Configuration;
import ch.bailu.gtk.converter.AliasTable;
import ch.bailu.gtk.converter.Filter;
import ch.bailu.gtk.converter.JavaNames;
import ch.bailu.gtk.converter.NamespaceType;
import ch.bailu.gtk.converter.RelativeNamespaceType;
import ch.bailu.gtk.tag.CallbackTag;
import ch.bailu.gtk.tag.EnumerationTag;
import ch.bailu.gtk.tag.MethodTag;
import ch.bailu.gtk.tag.NamespaceTag;
import ch.bailu.gtk.tag.ParameterTag;
import ch.bailu.gtk.tag.StructureTag;
import ch.bailu.gtk.writer.CodeWriter;

public class ClassModel extends Model {


    private final String name;
    private NamespaceModel nameSpace;

    private ClassModel parent;

    private final List<MethodModel> privateFactories = new ArrayList<>();
    private final List<MethodModel> factories = new ArrayList<>();
    private final List<MethodModel> constructors = new ArrayList<>();
    private final List<MethodModel> methods = new ArrayList<>();
    private final List<MethodModel> signals = new ArrayList<>();
    private final List<MethodModel> callbacks = new ArrayList<>();

    private final List<MethodModel> functions = new ArrayList<>();

    private final List<ParameterModel> fields = new ArrayList<>();
    private final List<Model>       unsupported = new ArrayList<>();

    private final List<ParameterModel> constants = new ArrayList<>();

    private String structureType;  // record, enum, class, interface, bitfield, callback
    private String cType;  // C type

    public ClassModel(StructureTag structure, NamespaceModel nameSpace) {
        cType = structure.getType();
        this.nameSpace = nameSpace;
        structureType = structure.getStructureType();
        name = convert(nameSpace.getNamespace(), structure.getName());
        parent = new ClassModel(nameSpace.getNamespace(), structure.getParentName(), structureType);

        for (MethodTag m: structure.getConstructors()) {
            addIfSupported(privateFactories, filterConstructor(new MethodModel(nameSpace.getNamespace(), m)));
        }

        for (MethodModel factory: privateFactories) {
            if (factory.isConstructorType()) {
                constructors.add(new MethodModel(factory));
            } else {
                factories.add(new MethodModel(factory));
            }
        }

        for (MethodTag method: structure.getMethods()) {
            addIfSupportedWithCallbacks(methods, filter(new MethodModel(nameSpace.getNamespace(), method)));
        }

        for (MethodTag signal: structure.getSignals()) {
            addIfSupported(signals, new MethodModel(nameSpace.getNamespace(), signal));
        }

        for (ParameterTag field: structure.getFields()) {
            var fieldModel = new ParameterModel(nameSpace.getNamespace(), field, false);
            addIfSupported(fields, filterField(fieldModel));

        }
    }


    private String convert(String namespace, String name) {
        NamespaceType from = new NamespaceType(namespace, name);
        var result = AliasTable.instance().convert(from).getName();
        return result;
    }


    private Model filterField(ParameterModel parameterModel) {
        parameterModel.setSupported("Callback", !parameterModel.isCallback());
        parameterModel.setSupported("Filter", Filter.field(this, parameterModel));
        return parameterModel;
    }

    private MethodModel filterConstructor(MethodModel methodModel) {
        methodModel.setSupported("Callback", !methodModel.hasCallback());
        return filter(methodModel);
    }
    private MethodModel filter(MethodModel methodModel) {
        methodModel.setSupported("Filter", Filter.method(this, methodModel));
        return methodModel;
    }

    /**
     * Gets called from builder when namespace ends
     * Create static class for package scoped functions
     * @param namespace
     */
    public ClassModel(NamespaceTag namespace) {
        this.nameSpace = new NamespaceModel(namespace);
        structureType = "package";
        name = JavaNames.toClassName(nameSpace.getNamespace());
        parent = new ClassModel(nameSpace.getNamespace(), null, structureType);

        for (MethodTag m : namespace.getFunctions()) {
            addIfSupportedWithCallbacks(functions, filter(new MethodModel(nameSpace.getNamespace(), m)));
        }
    }

    /**
     * This gets called from builder when namespace ends
     * Create interface with package scoped constants
     * @param namespace
     * @param members
     */
    public ClassModel(NamespaceModel namespace, List<ParameterTag> members) {
        this(namespace, JavaNames.toInterfaceName(namespace.getNamespace()), members, false);
    }

    /**
     * Gets called from ModelBuilder
     * Create interface with constants
     * @param namespace
     * @param enumeration
     */
    public ClassModel(NamespaceModel namespace, EnumerationTag enumeration) {
        this(namespace, enumeration.getName(), enumeration.getMembers(), true);
    }


    /**
     * Create interface with constants
     * @param namespace
     * @param name
     * @param members
     */
    private ClassModel(NamespaceModel namespace, String name, List<ParameterTag> members, boolean toUpper) {
        this.nameSpace = namespace;
        structureType = "enumeration";
        this.name = name;

        for (ParameterTag m : members) {

            addIfSupported(constants, new ParameterModel(namespace.getNamespace(), m, toUpper));
        }
    }


    private void addIfSupportedWithCallbacks(List models, MethodModel model) {
        addIfSupported(models, model);
        if (model.isSupported()) {
            for (MethodModel cb : model.getCallbackModel()) {
                if (!callbacks.contains(cb))
                    callbacks.add(cb);
            }
        }
    }

    private void addIfSupported(List models, Model model) {
        if (model.isSupported()) {
            models.add(model);
        } else {
            unsupported.add(model);
        }
    }


    // parent initializer
    private ClassModel(String defaultNamespace, String className, String structType) {
	    if (className == null) {
            nameSpace = new NamespaceModel();

            if ("record".equalsIgnoreCase(structType)) {
                name = nameSpace.getFullNamespace() + ".Record";
            } else if ("package".equalsIgnoreCase(structType)) {
                name = nameSpace.getFullNamespace() + ".Package";
            } else if ("callback".equalsIgnoreCase(structType)) {
                name = nameSpace.getFullNamespace() + ".Callback";
            } else {
                name = nameSpace.getFullNamespace() + ".Pointer";
            }

        } else {
            RelativeNamespaceType type = new RelativeNamespaceType(defaultNamespace, className);
            nameSpace = new NamespaceModel(type);

            if (nameSpace.isSupported()) {
                if (type.hasCurrentNamespace()) {
                    name = type.getName();
                } else {
                    name = nameSpace.getFullNamespace() + "."  + type.getName();
                }

            } else {
                nameSpace = new NamespaceModel();
                name = nameSpace.getFullNamespace() + ".Outsider";
            }
        }
    }


    public boolean hasNativeCalls() {
        if (isNameSpaceSupported()) {
            if (isRecord() && Filter.createMalloc(this)) {
                return true;
            } else if (isPackage() || isClassType()) {
                return (   methods.size() >0
                        || privateFactories.size() > 0
                        || signals.size()>0
                        || fields.size()>0
                        || functions.size()>0);
            }
        }
        return false;
    }

    private boolean isClassType() {
        return "class".equals(structureType) || "record".equals(structureType) || "interface".equals(structureType);
    }

    private boolean isNameSpaceSupported() {
        return nameSpace.isSupported();
    }

    public void write(CodeWriter writer) throws IOException {
        writer.writeStart(this, nameSpace);
        writer.next();

        if (isClassType()) {
            writer.writeClass(this);

            writer.next();
            for (MethodModel cb :callbacks) {
                writer.writeCallback(this, cb);
            }

            writer.next();
            for (MethodModel m : privateFactories) {
                writer.writePrivateFactory(this, m);
            }

            writer.next();
            for (MethodModel m : factories) {
                writer.writeFactory(this, m);
            }

            writer.next();
            writer.writeInternalConstructor(this);

            if (isRecord() && Filter.createMalloc(this)) {
                writer.writeMallocConstructor(this);
            }

            writer.next();
            for (MethodModel m : constructors) {
                writer.writeConstructor(this, m);
            }

            writer.next();
            for (ParameterModel p: fields) {
                writer.writeField(this, p);
            }

            writer.next();
            for (MethodModel m : methods) {
                writer.writeNativeMethod(this, m);
            }

            writer.next();
            for (MethodModel s : signals) {
                writer.writeSignal(this, s);
            }


            writer.next();
            for(MethodModel m : functions) {
                writer.writeFunction(this, m);
            }

        } else if (isPackage()) {
            writer.writeClass(this);

            writer.next();
            for (MethodModel cb :callbacks) {
                writer.writeCallback(this, cb);
            }

            writer.next();
            for (MethodModel m : functions) {
                writer.writeFunction(this, m);
            }


        } else if (isCallback()) {
            writer.writeInterface(this);
            writer.next();

            for (MethodModel m : functions) {
                writer.writeInterfaceMethod(this, m);
            }

        } else {
            writer.writeInterface(this);

            writer.next();
            for (ParameterModel p: constants) {
                writer.writeConstant(p);
            }
        }

        writer.next();
        for (Model m : unsupported) {
            writer.writeUnsupported(m);
        }

        writer.writeEnd();
    }

    private boolean isCallback() {
        return "callback".equals(structureType);
    }

    private boolean isPackage() {
        return "package".equals(structureType);
    }

    public String getImpName() {
        return "Imp" + name;
    }

    public String getApiName() {
        return name;
    }

    public String getApiParentName() {
        return parent.getApiName();
    }


    public String getHeaderFileName() {
        return nameSpace.getHeaderFileBase() + getImpName() + ".h";
    }

    public String getJniMethodName(MethodModel m) {
            return getJniMethodName(m.getApiName());
    }

    public String getJniMethodName(String apiName) {
        return Configuration.JNI_METHOD_NAME_BASE + nameSpace.getNamespace()  +"_" + getImpName() + "_" + apiName;
    }

    public String getJniSignalConnectMethodName(MethodModel m) {
        return Configuration.JNI_METHOD_NAME_BASE + nameSpace.getNamespace()  +"_" + getImpName() + "_" + m.getSignalMethodName();
    }

    public String getCSignalCallbackName(MethodModel m) {
        return nameSpace.getNamespace()  +"_" + getImpName() + "_" + m.getSignalMethodName();
    }

    public String getGlobalName(String name) {
        return nameSpace.getNamespace() + "_" + getImpName() + "_" + name;
    }

    public boolean isRecord() {
        return "record".equals(structureType);
    }

    public boolean hasDefaultConstructor() {
        for (MethodModel m : constructors) {
            if (m.getParameters().size()==0) {
                return true;
            }
        }
        return false;
    }

    public String getCType() {
        return cType;
    }
}
