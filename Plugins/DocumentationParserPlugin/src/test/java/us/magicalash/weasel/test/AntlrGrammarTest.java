package us.magicalash.weasel.test;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.Test;
import us.magicalash.weasel.plugin.DocumentationParserPlugin;

import java.io.InputStream;
import java.util.Scanner;

public class AntlrGrammarTest {
    @Test
    public void test() {
        DocumentationParserPlugin pl = new DocumentationParserPlugin();
        JsonObject obj = new JsonObject();
        JsonArray array = new JsonArray();

        InputStream file = this.getClass().getClassLoader().getResourceAsStream("Weird.java");
//        File file = new File("/home/wes/tmp/jdk/java.base/java/util/ArrayList.java");
        Scanner s;
        try {
            s = new Scanner(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        while (s.hasNext()) {
            array.add(s.nextLine());
        }

        obj.add("file_contents", array);

        pl.index(obj, o -> System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(o)));

    }
}
