package co.herovitamin.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

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
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import co.herovitamin.annotation.Elephant;
import co.herovitamin.annotation.Memoize;
import co.herovitamin.annotation.Memoizer.Memoizer;

@SupportedAnnotationTypes("co.herovitamin.annotation.Elephant")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
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
            CodeBlock.Builder staticCodeBlockBuilder = CodeBlock.builder();
            if (element.getKind() != ElementKind.CLASS) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Only classes can be annotated with @Elephant");
                return true;
            }

            messager.printMessage(Diagnostic.Kind.NOTE, "annotation found on: " + element.getSimpleName());

            for (Element enclosedElement : element.getEnclosedElements()) {
                if (enclosedElement.getKind() == ElementKind.METHOD
                        && enclosedElement.getModifiers().contains(Modifier.PUBLIC)
                        && enclosedElement.getModifiers().contains(Modifier.STATIC)){
                        if(enclosedElement.getAnnotation(Memoize.class) != null){
                                messager.printMessage(Diagnostic.Kind.NOTE, "Annotated element found: " + enclosedElement.getSimpleName());
                                classBuilder.addField(createFunctionField(enclosedElement));
                                createFieldInStaticBlock(staticCodeBlockBuilder, (ExecutableElement) enclosedElement);
                                classBuilder.addMethod(createMethod((ExecutableElement) enclosedElement));
                        }
                        else {
                            messager.printMessage(Diagnostic.Kind.NOTE, "Not annotated element found: " + enclosedElement.getSimpleName());
                            classBuilder.addMethod(duplicateMethod((ExecutableElement) enclosedElement));
                        }
                }
            }

            classBuilder.addStaticBlock(staticCodeBlockBuilder.build());

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

    private FieldSpec createFunctionField(Element element) {

        ClassName functionClassName = ClassName.get(Function.class);
        TypeName returnTypeClassName = TypeName.get(((ExecutableElement) element).getReturnType());
        TypeName paramTypeClassName = TypeName.get(String.class);

        List<? extends VariableElement> params = ((ExecutableElement) element).getParameters();
        for (VariableElement param : params) {
            paramTypeClassName = TypeName.get(param.asType());
        }

        ParameterizedTypeName.get(functionClassName, returnTypeClassName);

        FieldSpec functionField = FieldSpec
                .builder(ParameterizedTypeName.get(functionClassName, paramTypeClassName, returnTypeClassName), element.getSimpleName().toString())
                .addModifiers(Modifier.PRIVATE)
                .addModifiers(Modifier.STATIC)
                .build();
        return functionField;
    }

    private CodeBlock.Builder createFieldInStaticBlock(CodeBlock.Builder staticBlock, ExecutableElement methodElement) {

        String[] packageName = methodElement.getEnclosingElement().getEnclosingElement().toString().split(" ");

        ClassName memoizerClassName = ClassName.get(Memoizer.class);
        ClassName originalClassName = ClassName.get(
                packageName[packageName.length - 1],
                methodElement.getEnclosingElement().getSimpleName().toString()
        );
        staticBlock.addStatement(
                    methodElement.getSimpleName().toString() + " = " + "$T.memoize(" + "$T::" + methodElement.getSimpleName().toString() + ");", memoizerClassName, originalClassName
                );

        return staticBlock;
    }

    private MethodSpec duplicateMethod(ExecutableElement enclosedElement) {

        String originalClassName = enclosedElement.getEnclosingElement().toString();
        String methodName = enclosedElement.getSimpleName().toString();

        ClassName className = ClassName.get(
                enclosedElement.getEnclosingElement().getEnclosingElement().toString(),
                originalClassName
            );

        MethodSpec.Builder methodBuilder = MethodSpec
                .methodBuilder(methodName)
                .addModifiers(enclosedElement.getModifiers())
                .returns(TypeName.get(enclosedElement.getReturnType()));

        StringBuilder paramsInString = new StringBuilder();

        for (VariableElement variableElement : enclosedElement.getParameters()) {
            ParameterSpec parameterSpec = ParameterSpec.builder(TypeName.get(variableElement.asType()), variableElement.getSimpleName().toString()).build();
            methodBuilder.addParameter(parameterSpec);
            paramsInString.append(variableElement.getSimpleName().toString() + ",");
        }

        methodBuilder.addStatement(
                "return "
                        + originalClassName
                        + "."
                        + enclosedElement.getSimpleName().toString()
                        + "("
                        + paramsInString.toString().substring(0, paramsInString.toString().length() - 1)
                        + ")");

        return methodBuilder.build();
    }

    private MethodSpec createMethod(ExecutableElement enclosedElement) {

        List<? extends VariableElement> params = enclosedElement.getParameters();
        MethodSpec.Builder methodBuilder = MethodSpec
                .methodBuilder(enclosedElement.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.STATIC)
                .returns(TypeName.get(enclosedElement.getReturnType()));

        StringBuilder applyStatement = new StringBuilder();

        for (VariableElement param : params) {
            ParameterSpec parameterSpec = ParameterSpec.builder(
                    TypeName.get(param.asType()), param.getSimpleName().toString())
                    .build();

            methodBuilder.addParameter(parameterSpec);
            applyStatement.append(".apply(").append(param.getSimpleName().toString()).append(")");
        }

        methodBuilder.addStatement("return "+enclosedElement.getSimpleName().toString()+applyStatement.toString());

        return methodBuilder.build();
    }

    private TypeSpec.Builder createClass(Element element) {
        return TypeSpec
                .classBuilder("Elephant"+element.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_8;
    }
}
