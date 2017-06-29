package co.herovitamin.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import co.herovitamin.annotation.Elephant;

@SupportedAnnotationTypes("co.herovitamin.annotation.Elephant")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class ElephantProcessor extends AbstractProcessor{

    private Messager messager;
    private JavaFile fileCreator;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        messager = processingEnvironment.getMessager();
        filer = processingEnvironment.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        for (Element element : roundEnvironment.getElementsAnnotatedWith(Elephant.class)) {

            TypeSpec.Builder classBuilder = createClass(element);

            if (element.getKind() != ElementKind.CLASS) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Can be applied to class.");
                return true;
            }

            messager.printMessage(Diagnostic.Kind.NOTE, "annotation found on: " + element.getSimpleName());

            classBuilder.addField(createOriginalObject(element));
            classBuilder.addMethod(createConstructor(element));

            fileCreator = JavaFile.builder("co.herovitamin.generated", classBuilder.build()).build();
            try {
                fileCreator.writeTo(filer);
            } catch (IOException e) {
                e.printStackTrace();
                messager.printMessage(Diagnostic.Kind.ERROR, "Couldn't create Elephant version for class " + element.getSimpleName().toString());
            }
        }
        return true;
    }

    private TypeSpec.Builder createClass(Element element) {
        return TypeSpec
                .classBuilder("Elephant"+element.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
    }

    private MethodSpec createConstructor(Element element) {

        ClassName wrappedClassName = ClassName.get("", element.getSimpleName().toString());

        return MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(wrappedClassName, "object")
                .addStatement("this.$N = $N", "object", "object")
                .build();
    }

    private FieldSpec createOriginalObject(Element element){

        ClassName wrappedClassName = ClassName.get(element.getEnclosingElement().toString(), element.getSimpleName().toString());

        return FieldSpec.builder(wrappedClassName, "object")
                .addModifiers(Modifier.PUBLIC)
                .build();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_7;
    }
}
