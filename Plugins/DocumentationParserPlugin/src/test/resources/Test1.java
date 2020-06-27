package test;

import foo.*;
import lombok.Getter;
import lombok.Setter;

@baz.Bar
public class Test1 {
    public Test1(Test1 foo) {

    }

    @Setter
    @Getter
    private Foo foo = new Foo();

    public strictfp Test1 bar() {

    }

    @Override
    private void baz(final Bar b1, @NotNull final Bar b2) {
        Bar bar = new Bar();
    }

    private <T> void baz2(Bar2<T> g) {




    }
}