module eu.jonahbauer.android.preference.annotation {
    exports eu.jonahbauer.android.preference.annotations;
    exports eu.jonahbauer.android.preference.annotations.serializer;

    requires com.squareup.javapoet;
    requires java.compiler;
    requires static lombok;

    provides javax.annotation.processing.Processor with eu.jonahbauer.android.preference.annotations.processor.PreferenceProcessor;
}