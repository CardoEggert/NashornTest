<h1>This repository is meant for showing one issue that has come up with Nashorn</h1>
I have not been able to produce it locally but whenever Jenkins runs e2e tests and cause a lot of traffic, then there are situations where this script might return null instead of a numerical value.

<div>    
<pre>
var result = {
    type: 'boolean',
    value: getValue(),
    result: isTestAllowed()
};
function isTestAllowed() {
    return !(nameProvider.firstName.toUpperCase() == 'TEST')
};
function getValue() {
    if (!isTestAllowed()) {
        // This makes a request to the other server and the filters and counts the values (returns a primitive long)
        return someProvider.someRequest(2, 4);
    } else {
        return null;
    }
}
JSON.stringify(result);
</pre>
</div>
There are two providers which look like the following:
<pre>
public class NameProvider {

    public String firstName;

    public NameProvider(String firstName) {
        this.firstName = firstName;
    }
}
</pre>
<pre>
import java.util.List;

public class SomeProvider {

    public long someRequest(Long first, Long second) {
        // Imitate a request
        try {
            Thread.sleep(2_000);
        } catch (Exception ignored) {
        }
        // Try to use stream here
        final List<Long> list = List.of(first, second);
        return list.stream().filter(x -> x > 0).filter(x -> x < 10).count();
    }
}
</pre>

Everything is assembled in the test as following:
<pre>
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

class NashornTest {

    private final static String SCRIPT = """
            var result = {
                type: 'boolean',
                value: getValue(),
                result: isTestAllowed()
            };
            function isTestAllowed() {
                return !(nameProvider.firstName.toUpperCase() == 'TEST')
            };
            function getValue() {
                if (!isTestAllowed()) {
                    // This makes a request to the other server and the filters and counts the values (returns a primitive long)
                    return someProvider.someRequest(2, 4);
                } else {
                    return null;
                }
            }
            JSON.stringify(result);
            """;

    private static final ScriptEngine scriptEngine = new NashornScriptEngineFactory().getScriptEngine("--optimistic-types=true");

    @Test
    void test() throws ScriptException {
        final SimpleBindings ruleExecutionContext = new SimpleBindings();
        ruleExecutionContext.put("nameProvider", new NameProvider("TEST"));
        ruleExecutionContext.put("someProvider", new SomeProvider());
        final ScriptContext ruleContext = new SimpleScriptContext();
        ruleContext.getBindings(ScriptContext.ENGINE_SCOPE).putAll(scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE));
        ruleContext.getBindings(ScriptContext.ENGINE_SCOPE).putAll(ruleExecutionContext);
        final Object evaluationObject = scriptEngine.eval(SCRIPT, ruleContext);
        Assertions.assertTrue(evaluationObject.toString().contains("\"value\":2"));
    }

}
</pre>

Currently, it is asserting that value should always be 2. I have had cases where it does the same thing and the value is null. 
I saw instances where the workaround has been as simple as just returning a numerical value from the <code>getValue()</code> method (the place in the else statement).
In other cases not using the same method multiple times has also helped with this. In this script it is called before and after the declaration of the method.