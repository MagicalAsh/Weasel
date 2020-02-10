package us.magicalash.weasel.plugin.representation;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class DummyContainer extends JavaCodeUnit {
    private List<? extends JavaCodeUnit> dummyContainer;
}
