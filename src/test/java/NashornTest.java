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
