package eu.jonahbauer.android.preference.annotations.util;

import com.google.testing.compile.Compilation;

import javax.tools.StandardLocation;
import java.io.IOException;

public final class CompilationClassLoader extends ClassLoader {
    private final Compilation compilation;

    public CompilationClassLoader(ClassLoader parent, Compilation compilation) {
        super(parent);
        this.compilation = compilation;
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        var file = compilation.generatedFile(StandardLocation.CLASS_OUTPUT, name.replace('.', '/') + ".class")
                .orElseThrow(() -> new ClassNotFoundException(name));

        try (var in = file.openInputStream()) {
            var bytes = in.readAllBytes();
            return defineClass(name, bytes, 0, bytes.length);
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
    }
}
