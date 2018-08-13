package com.dada.injectmvp.compiler;

import com.dada.injectmvp.BinderWrapper;
import com.dada.injectmvp.PresenterType;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import java.io.IOException;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.util.Elements;

/**
 * AndroidBinder 实现类生成
 *
 * @author yueshaojun
 * @date 2018/8/9
 */

public class BinderCreator {
    public static void createFile(Elements elementUtil, Filer filer) {
        createFile(elementUtil, filer, PresenterType.ACTIVITY);
        createFile(elementUtil, filer, PresenterType.FRAGMENT);
    }

    private static void createFile(Elements elementUtil, Filer filer, PresenterType type) {

        ClassName hashMap = ClassName.get("java.util", "HashMap");
        ClassName activity = ClassName.get("android.app", "Activity");
        ClassName fragment = ClassName.get("android.support.v4.app", "Fragment");

        WildcardTypeName activityWildType = WildcardTypeName.subtypeOf(activity);
        WildcardTypeName fragmentWildType = WildcardTypeName.subtypeOf(fragment);

        ClassName className = ClassName.get("java.lang", "Class");
        ClassName bindWrapperClassName = ClassName.get(Constants.API_PACKAGE_NAME, "BinderWrapper");

        ParameterizedTypeName key = null;
        ParameterizedTypeName value = null;
        if (type == PresenterType.ACTIVITY) {
            key = ParameterizedTypeName.get(className, activityWildType);
            value = ParameterizedTypeName.get(bindWrapperClassName, activityWildType);
        }
        if (type == PresenterType.FRAGMENT) {
            key = ParameterizedTypeName.get(className, fragmentWildType);
            value = ParameterizedTypeName.get(bindWrapperClassName, fragmentWildType);
        }

        ParameterizedTypeName bindWrapperMap = ParameterizedTypeName.get(hashMap, key, value);
        ClassName interfaceClassName = ClassName.get(Constants.API_PACKAGE_NAME, "AndroidBinder");

        ParameterizedTypeName defaultInterfaceTypeName = null;
        if (type == PresenterType.ACTIVITY) {
            defaultInterfaceTypeName = ParameterizedTypeName.get(interfaceClassName, activity);
        }
        if (type == PresenterType.FRAGMENT) {
            defaultInterfaceTypeName = ParameterizedTypeName.get(interfaceClassName, fragment);
        }

        TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(type == PresenterType.ACTIVITY ? "ActivityBinder" : "FragmentBinder")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(defaultInterfaceTypeName)
                .addField(bindWrapperMap, "classBinderWrapperHashMap", Modifier.PRIVATE);

        MethodSpec.Builder constructorBuilder =
                MethodSpec.constructorBuilder().
                        addModifiers(Modifier.PRIVATE)
                        .addStatement("classBinderWrapperHashMap = new HashMap<>()");


        //activity
        if (type == PresenterType.ACTIVITY) {
            for (String classNameHasPresenter : Parser.currentActivityClassTypeMap.keySet()) {
                System.out.println("activity TypeElement :" + classNameHasPresenter + " start");
                TypeName typeNameHasPresenter = TypeName.get(Parser.currentActivityClassTypeMap.get(classNameHasPresenter).asType());
                ParameterizedTypeName bindWrapper = ParameterizedTypeName.get(ClassName.get(BinderWrapper.class), typeNameHasPresenter);

                typeSpecBuilder.addField(bindWrapper, "bindWrapper_" + classNameHasPresenter, Modifier.PRIVATE);

                //构造器里加语句
                constructorBuilder
                        .addStatement("bindWrapper_" + classNameHasPresenter + " = " + "new " +
                                Parser.currentActivityClassTypeMap.get(classNameHasPresenter).getQualifiedName() +
                                "_BinderWrapper()")
                        .addStatement("classBinderWrapperHashMap.put($T.class,bindWrapper_" + classNameHasPresenter + ")",
                                Parser.currentActivityClassTypeMap.get(classNameHasPresenter).asType());
            }
        }

        //fragment
        if (type == PresenterType.FRAGMENT) {
            for (String classNameHasPresenter : Parser.currentFragmentClassTypeMap.keySet()) {
                System.out.println("fragmentTypeElement :" + classNameHasPresenter);
                TypeName typeNameHasPresenter = TypeName.get(Parser.currentFragmentClassTypeMap.get(classNameHasPresenter).asType());
                ParameterizedTypeName bindWrapper = ParameterizedTypeName.get(ClassName.get(BinderWrapper.class), typeNameHasPresenter);

                typeSpecBuilder.addField(bindWrapper, "bindWrapper_" + classNameHasPresenter, Modifier.PRIVATE);

                //构造器里加语句
                constructorBuilder
                        .addStatement("bindWrapper_" + classNameHasPresenter + " = "
                                + "new " +
                                Parser.currentFragmentClassTypeMap.get(classNameHasPresenter).getQualifiedName() +
                                "_BinderWrapper()")
                        .addStatement("classBinderWrapperHashMap.put($T.class,bindWrapper_" + classNameHasPresenter + ")",
                                Parser.currentFragmentClassTypeMap.get(classNameHasPresenter).asType());

            }
        }

        MethodSpec.Builder createBuilder = MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get("com.dada.presenter", type == PresenterType.ACTIVITY ? "ActivityBinder" : "FragmentBinder"))
                .addStatement("return " + (type == PresenterType.ACTIVITY ? "new ActivityBinder()" : "new FragmentBinder()"));

        MethodSpec.Builder bindMethodBuilder = MethodSpec.methodBuilder("bind");
        ParameterSpec.Builder paramBuilder = ParameterSpec
                .builder(type == PresenterType.ACTIVITY ? activity : fragment, "instance");
        bindMethodBuilder.addParameter(paramBuilder.build())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addStatement("\nBinderWrapper<$T> wrapper = (BinderWrapper<$T>) classBinderWrapperHashMap.get(instance.getClass());\n" +
                                "if(wrapper == null){\n" +
                                "   return;\n" +
                                "}\n" +
                                "wrapper.bindMember($N);",
                        type == PresenterType.ACTIVITY ? activity.box() : fragment.box(),
                        type == PresenterType.ACTIVITY ? activity.box() : fragment.box(),
                        "instance");

        MethodSpec.Builder unbindMethodBuilder = MethodSpec.methodBuilder("unbind")
                .addParameter(paramBuilder.build())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addStatement("\nBinderWrapper<$T> wrapper = (BinderWrapper<$T>) classBinderWrapperHashMap.get(instance.getClass());\n" +
                                "if(wrapper == null){\n" +
                                "   return;\n" +
                                "}\n" +
                                "wrapper.unbind($N);",
                        type == PresenterType.ACTIVITY ? activity.box() : fragment.box(),
                        type == PresenterType.ACTIVITY ? activity.box() : fragment.box(),
                        "instance");
        typeSpecBuilder
                .addMethod(constructorBuilder.build())
                .addMethod(createBuilder.build())
                .addMethod(bindMethodBuilder.build())
                .addMethod(unbindMethodBuilder.build());

        String packageName = Constants.DEFAULT_PACKAGE_NAME;
        try {
            JavaFile.builder(packageName, typeSpecBuilder.build()).build().writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
