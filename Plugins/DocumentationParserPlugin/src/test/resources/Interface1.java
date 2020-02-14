package test;

import bar.Bar;
import foo.oof.Foo;

public interface Interface1 extends Bar {
    public final int NUM = 1;

    public Foo foo();

    @Override
    public void bar();

    /**
     * AAAAaaa. AAAAAAAAAAA.
     *
     * @dummy foo bar foobar
     *        fart bartholomew
     */
    public default void bar2() {
        // dummy
    }
}