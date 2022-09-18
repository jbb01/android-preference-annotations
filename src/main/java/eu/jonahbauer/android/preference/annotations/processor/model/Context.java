package eu.jonahbauer.android.preference.annotations.processor.model;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import lombok.Data;
import lombok.experimental.Delegate;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

@Data
public final class Context {
    @Delegate
    private final ProcessingEnvironment env;
    private final Element element;

    private TypeName r;
    private ClassName root;
    private FieldSpec sharedPreferences;

    public void error(String message, Object...args) {
        env.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format(message, args), element);
    }
}
