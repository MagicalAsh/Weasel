package test;

import foo.Bar;

public class OuterClass {
    public InnerClass outerMethod() {

    }

    public InnerClass.InnerInnerClass outerMethod2() {

    }

    public static class InnerClass {
        public static class InnerInnerClass {
            int i = 0;
        }
        public Bar innerMethod() {

        }
    }
}