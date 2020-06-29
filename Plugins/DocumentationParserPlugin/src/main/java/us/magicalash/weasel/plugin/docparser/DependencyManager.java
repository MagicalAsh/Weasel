package us.magicalash.weasel.plugin.docparser;

import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import us.magicalash.weasel.index.plugin.representations.ParsedCodeUnit;
import us.magicalash.weasel.plugin.PackageHierarchy;
import us.magicalash.weasel.plugin.docparser.visitor.UnresolvedDependency;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DependencyManager implements Runnable {

    private final Semaphore newTypesWaiting;
    private final ConcurrentLinkedQueue<String> newTypes;

    private final ConcurrentHashMap<String, List<CodeUnit>> dependencyRequirements;

    private final ExecutorService executorService;
    private final BiFunction<JsonObject, Consumer<ParsedCodeUnit>, Runnable> createCodeVisitorRunnable;

    public DependencyManager(ExecutorService executorService, BiFunction<JsonObject,
            Consumer<ParsedCodeUnit>, Runnable> createCodeVisitorRunnable) {
        this.newTypesWaiting = new Semaphore(0);
        this.dependencyRequirements = new ConcurrentHashMap<>();
        this.executorService = executorService;
        this.createCodeVisitorRunnable = createCodeVisitorRunnable;
        this.newTypes = new ConcurrentLinkedQueue<>();
    }

    public void addType(String type) {
        newTypes.add(type);
        newTypesWaiting.release();
    }

    public void addUnit(List<UnresolvedDependency> dependencies, JsonObject obj, Consumer<ParsedCodeUnit> onCompletion) {
        if (dependencies.size() > 0) {
            CodeUnit unit = new CodeUnit(dependencies, obj, onCompletion);
            for (UnresolvedDependency dependency : dependencies) {
                this.dependencyRequirements.computeIfAbsent(dependency.getName(), k -> new ArrayList<>())
                                           .add(unit);
                for (String prefix : dependency.getValidPackages()) {
                    String fqn = prefix + '/' + dependency.getName();
                    this.dependencyRequirements.computeIfAbsent(fqn, k -> new ArrayList<>())
                                               .add(unit);
                }
            }
        } else {
            executorService.submit(this.createCodeVisitorRunnable.apply(obj, onCompletion));
        }
    }

    @Override
    public void run() {
        while(!Thread.interrupted()) {
            try {
                newTypesWaiting.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            String newType = newTypes.remove();

            List<CodeUnit> units = this.dependencyRequirements.remove(newType);
            if (units == null) {
                units = Collections.emptyList(); // this is effectively a continue.
            }
            for (CodeUnit unit : units) {
                List<UnresolvedDependency> resolvedDeps =
                        unit.dependencies.stream()
                                .filter(dep -> dep.resolvedBy(newType))
                                .collect(Collectors.toList());

                unit.dependencies.removeIf(resolvedDeps::contains);

                // go through and remove all of the other possible dependencies this could be from
                // them map. This prevents codeunits hanging around because they're technically accessible
                // through a dependency that may never get filled, like java/lang/Foo.
                for (UnresolvedDependency dependency : resolvedDeps) {
                    removeFromDependencyRequirements(unit, dependency.getName());
                    for (String prefix : dependency.getValidPackages()) {
                        String fqn = prefix + '/' + dependency.getName();
                        removeFromDependencyRequirements(unit, fqn);
                    }
                }

                if (unit.dependencies.size() == 0) {
                    executorService.submit(this.createCodeVisitorRunnable.apply(unit.obj, unit.onCompletion));
                }
            }
        }
    }

    private void removeFromDependencyRequirements(CodeUnit unit, String fqn) {
        List<CodeUnit> dependenciesForFqn = this.dependencyRequirements.get(fqn);
        // because we explicitly removed the actual type, we shouldn't get
        // a concurrent modification exception.
        if (dependenciesForFqn != null) {
            dependenciesForFqn.remove(unit);
            if (dependenciesForFqn.size() == 0) {
                this.dependencyRequirements.remove(fqn);
            }
        }
    }

    @AllArgsConstructor
    private static class CodeUnit {
        private List<UnresolvedDependency> dependencies;
        private JsonObject obj;
        private Consumer<ParsedCodeUnit> onCompletion;
    }

}
