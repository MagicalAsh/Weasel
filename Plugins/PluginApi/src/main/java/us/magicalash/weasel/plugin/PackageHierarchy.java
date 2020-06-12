package us.magicalash.weasel.plugin;

import lombok.Getter;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The PackageHierarchy class is used to represent package structures like those in Java and Go, where types
 * have a location specified relative to a root. Empty packages are not allowed and are automatically removed
 * when all types in a package are removed.
 */
public class PackageHierarchy {
    /**
     * Internal map of the hierarchy. Every leaf should should have isType set to true, as well as possibly some
     * vertices. These internal PackageHierarchy objects should never be leaked out of the root hierarchy.
     */
    private Map<String, PackageHierarchy> hierarchyMap;

    /**
     * A reference to the parent of this package. Used to make deleting a package faster.
     */
    private PackageHierarchy parent;

    /**
     * True if this is a type and not merely an intermediary. The hierarchyMap variable can still have entries
     * if subtypes are allowed in the language.
     */
    private boolean isType;

    /**
     * The name of this vertex. Only null for the root, all others must be nonnull and nonempty..
     */
    private String name;

    public PackageHierarchy() {
        this.hierarchyMap = new HashMap<>();
        this.isType = false;
        this.parent = null;
    }

    private PackageHierarchy(boolean isType, PackageHierarchy parent, String name) {
        this.hierarchyMap = new HashMap<>();
        this.name = name;
        this.isType = isType;
        this.parent = parent;
    }

    /**
     * Adds a type to the package hierarchy. Individual package levels should be delimited using
     * a forward-slash character (/). The last segment of the path MUST be a type, and not a subpackage.
     * @param name the name of the type to add
     */
    public void addType(String name)  {
        String[] packageName = name.split("/");
        PackageHierarchy hierarchy = this;
        for (int i = 0; i < packageName.length; i++) {
            if (packageName[i].isEmpty()) {
                throw new IllegalArgumentException("Packages must not have an empty string as a subpackage.");
            }
            if (hierarchy.hierarchyMap.get(packageName[i]) == null) {
                // if this subpackage isn't already in the hierarchy, add it to it.
                // if it's not the last one, it probably isn't a type. isType is not final in case we get it wrong.
                hierarchy.hierarchyMap.put(packageName[i],
                        new PackageHierarchy(packageName.length - 1 == i, hierarchy, packageName[i]));
            }

            hierarchy = hierarchy.hierarchyMap.get(packageName[i]);
        }
    }

    /**
     * Checks to see if this package hierarchy contains a type with the given qualified name.
     * @param name the qualified name to check for
     * @return     true if contained, false otherwise.
     */
    public boolean containsType(String name) {
        PackageHierarchy hierarchy = traverseTo(name);

        return hierarchy != null && hierarchy.isType;
    }

    /**
     * Gets a list of the names of immediate children of the given qualified name.
     * @param name the qualified name the get children of
     * @return     a list of immediate children
     */
    public Set<String> getImmediateChildren(String name) {
        PackageHierarchy subpackage = traverseTo(name);

        if (subpackage == null) {
            return Collections.emptySet();
        }

        Set<String> children = new HashSet<>();

        for (PackageHierarchy child : subpackage.hierarchyMap.values()) {
            children.add(child.fullName());
        }

        return children;
    }

    public Set<String> getAllChildren(String name) {
        PackageHierarchy subpackage = traverseTo(name);

        if (subpackage == null) {
            return Collections.emptySet();
        }

        Set<String> children = new HashSet<>();

        if (this.name != null) { // don't add the root
            children.add(this.fullName());
        }

        for (PackageHierarchy child : subpackage.hierarchyMap.values()) {
            children.addAll(child.getAllChildren(null));
        }

        return children;
    }

    public Set<String> getPackageTypes(String name) {
        Set<String> out = getAllChildren(name);
        out.removeIf(Predicate.not(this::containsType));

        return out;
    }

    /**
     * Removes a type or subpackage from the hierarchy.
     * @param name the name to remove
     * @return     true if something was removed, false otherwise.
     */
    public boolean remove(String name) {
        PackageHierarchy type = traverseTo(name);
        if (type == null) {
            return false;
        }

        // the type existed, so now we need to delete it
        PackageHierarchy parent = type.parent;
        if (parent == null) {
            throw new IllegalStateException("Parent of package should never be null");
        }

        parent.hierarchyMap.remove(type.name);

        // if we removed the last entry, delete the parent as well
        if (parent.hierarchyMap.size() == 0) {
            remove(parent.fullName());
        }

        return true;
    }

    private PackageHierarchy traverseTo(String name) {
        if (name == null) {
            return this;
        }

        String[] packageName = name.split("/");
        PackageHierarchy hierarchy = this;
        for (String s : packageName) {
            if (s.isEmpty()) {
                throw new IllegalArgumentException("Packages must not have an empty string as a subpackage.");
            }
            if (hierarchy.hierarchyMap.get(s) == null) {
                // if there isn't an entry at some point in the name, there's nothing there.
                return null;
            }

            hierarchy = hierarchy.hierarchyMap.get(s);
        }

        return hierarchy;
    }

    private String fullName() {
        StringBuilder out = new StringBuilder(this.name);
        PackageHierarchy hierarchy = this;
        while (hierarchy.parent != null) {
            hierarchy = hierarchy.parent;

            // we check to see if the name isn't "" or else we would end up with a leading /
            if (hierarchy.name != null && !hierarchy.name.isEmpty()) {
                out.insert(0, "/");
                out.insert(0, hierarchy.name);
            }
        }

        return out.toString();
    }

    // Package Root
    // |-Value1
    // | |-InnerValue
    // | |-Value2
    // |- Value3
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("Package Root\n");
        for (Map.Entry<String, PackageHierarchy> entry : this.hierarchyMap.entrySet()) {
            out.append("|-");
            out.append(entry.getKey());
            out.append("\n");
            List<String> inner = new ArrayList<>(Arrays.asList(entry.getValue().toString().split("\\r?\\n")));
            if (inner.size() > 0) {
                inner.remove(0); // remove the package root bit
            }

            for (String line : inner) {
                out.append("| ");
                out.append(line);
                out.append("\n");
            }
        }

        return out.toString();
    }
}
