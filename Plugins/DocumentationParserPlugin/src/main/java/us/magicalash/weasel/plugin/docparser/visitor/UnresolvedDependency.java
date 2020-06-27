package us.magicalash.weasel.plugin.docparser.visitor;

import lombok.Data;
import lombok.EqualsAndHashCode;
import us.magicalash.weasel.plugin.PackageHierarchy;

import java.util.List;

@Data
@EqualsAndHashCode
public class UnresolvedDependency {
    private String name;
    private List<String> validPackages;

    public boolean resolvedBy(String name) {
        if (this.name.equals(name)) {
            return true;
        }

        for (String prefix : validPackages) {
            if (name.equals(prefix + '/' +  this.name)){
                return true;
            }
        }

        return false;
    }

    public boolean resolvesIn(PackageHierarchy hierarchy) {
        boolean out = false;
        for (int i = 0; i < validPackages.size() && !out; i++) {
            String name = validPackages.get(i) + '/' + this.name;
            out = hierarchy.containsType(name);
        }

        return out;
    }
}
