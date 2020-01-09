package apiUsageConter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import entity.*;
import utils.DirExplorer;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class APIUsageCounter {

    Map<String, Type> typeMap = new HashMap<>();
    Map<String, Method> methodMap = new HashMap<>();
    Map<String, Constructor> constructorMap = new HashMap<>();
    Map<String, Field> fieldMap = new HashMap<>();

    Map<String, List<String>> inheritMap = new HashMap<>();
    Map<String, List<String>> extendedInheritMap = new HashMap<>();

    UsageCounter usageCounter = new UsageCounter();


    public void extendInheritMap(){
        for (String base : inheritMap.keySet()){
            Set<String> extendTypes = new HashSet<>();
            Queue<String> queue = new LinkedList<>();
            queue.add(base);
            while (!queue.isEmpty()){
                String curType = queue.poll();
                if (extendTypes.contains(curType)){
                    continue;
                }else {
                    extendTypes.add(curType);
                }
                List<String> exactExtendTypes = inheritMap.get(curType);
                if (exactExtendTypes!=null){
                    for (String type : exactExtendTypes){
                        queue.add(type);
                    }
                }
            }

            List<String> extendTypesList = new ArrayList<>();
            for (String type : extendTypes){
                extendTypesList.add(type);
            }
            extendedInheritMap.put(base,extendTypesList);
        }
    }

    public void load(){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            typeMap = (Map<String, Type>) objectMapper.readValue(new File("data/types"), objectMapper.getTypeFactory().constructParametricType(HashMap.class, String.class, Type.class));
            methodMap = (Map<String, Method>) objectMapper.readValue(new File("data/methods"), objectMapper.getTypeFactory().constructParametricType(HashMap.class, String.class, Method.class));
            constructorMap = (Map<String, Constructor>) objectMapper.readValue(new File("data/constructors"), objectMapper.getTypeFactory().constructParametricType(HashMap.class, String.class, Constructor.class));
            fieldMap = (Map<String, Field>) objectMapper.readValue(new File("data/fields"), objectMapper.getTypeFactory().constructParametricType(HashMap.class, String.class, Field.class));
            inheritMap = (Map<String, List<String>>) objectMapper.readValue(new File("data/inheritMap"),objectMapper.getTypeFactory().constructParametricType(HashMap.class,String.class,objectMapper.getTypeFactory().constructParametricType(ArrayList.class,String.class).getRawClass()));
        } catch (IOException e) {
            e.printStackTrace();
        }


        for (Type type : typeMap.values()){
            usageCounter.getExactCounter().put(type.getFullQualifiedName(),0);
        }
        for (Method method : methodMap.values()){
            method.setUsageCount(1);
            usageCounter.incrementByOne(method.getReturnType());
        }
        for (Constructor constructor : constructorMap.values()){
            constructor.setUsageCount(1);
            usageCounter.incrementByOne(constructor.getReturnType());
        }
        for (Field field : fieldMap.values()){
            field.setUsageCount(1);
            usageCounter.incrementByOne(field.getFieldType());
        }
        extendInheritMap();
    }

    public void parse(String rootDir){
        try {
            TypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
            TypeSolver poiSolver = new JarTypeSolver("/Users/apple/Downloads/poi-4.0.0-bin/poi-4.0.0.jar");
            TypeSolver combinedTypeSolver = new CombinedTypeSolver(reflectionTypeSolver, poiSolver);


            JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
            JavaParser.getStaticConfiguration().setSymbolResolver(symbolSolver);
        } catch (IOException e) {
            e.printStackTrace();
        }

        DirExplorer dirExplorer = new DirExplorer(
                (int level, String path, File file) -> (file.getName().endsWith(".java")),
                (int level, String path, File file) -> {
                    CompilationUnit cu = null;
                    try {
                        cu = JavaParser.parse(file);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (cu == null){
                        return;
                    }
                    cu.accept(new VoidVisitorAdapter<Object>() {

                        @Override
                        public void visit(MethodCallExpr n, Object arg) {
                            super.visit(n, arg);
                            try{
                                ResolvedMethodDeclaration rn = n.resolve();
                                Method method = methodMap.get(rn.getQualifiedSignature());
                                if (method!=null){
                                    method.setUsageCount(method.getUsageCount()+1);
                                    usageCounter.incrementByOne(method.getReturnType());
                                    //System.out.println(method.getQualifiedSignature());
                                }
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void visit(FieldAccessExpr n, Object arg) {
                            super.visit(n, arg);
                            try{
                                ResolvedValueDeclaration rn = n.resolve();
                                Field field = null;
                                if (rn.isField()){
                                    field = fieldMap.get(rn.asField().declaringType().getQualifiedName() + "." + rn.getName());
                                }else if (rn.isEnumConstant()){
                                    field = fieldMap.get(rn.asEnumConstant().getType().describe() + "." + rn.getName());
                                }
                                if (field!=null){
                                    field.setUsageCount(field.getUsageCount()+1);
                                    usageCounter.incrementByOne(field.getFieldType());
                                    //System.out.println(field.getFieldName());
                                }
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void visit(ObjectCreationExpr n, Object arg) {
                            super.visit(n, arg);
                            try{
                                ResolvedConstructorDeclaration rn = n.resolve();
                                Constructor constructor = constructorMap.get(rn.getQualifiedSignature());
                                if (constructor!=null){
                                    constructor.setUsageCount(constructor.getUsageCount()+1);
                                    usageCounter.incrementByOne(constructor.getReturnType());
                                    //System.out.println(constructor.getQualifiedSignature());
                                }
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    },null);
                });

        dirExplorer.explore(new File(rootDir));
        usageCounter.recomputeCounter(extendedInheritMap);


        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        File methodFile = new File("data/methods");
        File constructorFile = new File("data/constructors");
        File fieldFile = new File("data/fields");
        File counterFile = new File("data/counter");

        try {
            objectMapper.writeValue(methodFile, methodMap);
            objectMapper.writeValue(constructorFile, constructorMap);
            objectMapper.writeValue(fieldFile, fieldMap);
            objectMapper.writeValue(counterFile, usageCounter);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        APIUsageCounter apiUsageCounter = new APIUsageCounter();
        apiUsageCounter.load();
        apiUsageCounter.parse("/Users/apple/Downloads/apiCodes/all");
    }
}
