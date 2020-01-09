package extractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import entity.Constructor;
import entity.Field;
import entity.Method;
import entity.Type;
import utils.DirExplorer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public class Counter {
        public int apiCounter;
        public int publicApiCounter;
        public int unsolvedApiCounter;
        public int unsolvedPublicApiCounter;
        public List<String> unsolvedApi = new ArrayList<>();
        public Map<String, String> fullName2SimpleName = new HashMap<>();
        public Map<String, String> returnType2FullName = new HashMap<>();
    }

    Map<String, Method> methodMap = new HashMap<>();
    Map<String, Constructor> constructorMap = new HashMap<>();
    Map<String, Type> typeMap = new HashMap<>();
    Map<String, Field> fieldMap = new HashMap<>();
    Map<String, List<String>> basicTypeToFieldMap = new HashMap<>();

    Map<String, List<String>> inheritMap = new HashMap<>();

    public Main(){
        basicTypeToFieldMap.put("int", new ArrayList<>());
        basicTypeToFieldMap.put("long", new ArrayList<>());
        basicTypeToFieldMap.put("short", new ArrayList<>());
        basicTypeToFieldMap.put("byte", new ArrayList<>());
        basicTypeToFieldMap.put("char", new ArrayList<>());
        basicTypeToFieldMap.put("float", new ArrayList<>());
        basicTypeToFieldMap.put("double", new ArrayList<>());
        basicTypeToFieldMap.put("boolean", new ArrayList<>());
    }

    public void parse() {
        try {
            TypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
            TypeSolver poiSolver = new JarTypeSolver("/Users/apple/Downloads/poi-4.0.0-bin/poi-4.0.0.jar");
            TypeSolver combinedTypeSolver = new CombinedTypeSolver(reflectionTypeSolver, poiSolver);


            JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
            JavaParser.getStaticConfiguration().setSymbolResolver(symbolSolver);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Counter counter = new Counter();
        DirExplorer dirExplorer = new DirExplorer(
                (int level, String path, File file) -> (file.getName().endsWith(".java")),
                (int level, String path, File file) -> {
                    CompilationUnit cu = null;
                    try {
                        cu = JavaParser.parse(file);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    cu.accept(new VoidVisitorAdapter<Object>() {
                        private boolean inInterface = false;

                        @Override
                        public void visit(ClassOrInterfaceDeclaration n, Object arg) {
                            if (n.getAccessSpecifier() == Modifier.Keyword.PUBLIC && !n.isStatic()) {
                                try {
                                    ResolvedReferenceTypeDeclaration r = n.resolve();
                                    String classFullName = r.getQualifiedName();
                                    typeMap.put(classFullName, new Type(classFullName));

                                    for (ResolvedFieldDeclaration rf : r.getDeclaredFields()) {
                                        if (rf.accessSpecifier() == Modifier.Keyword.PUBLIC || r.isInterface()) {
                                            fieldMap.put(classFullName + "." + rf.getName(), new Field(rf));
                                        }
                                    }

                                    for (ResolvedReferenceType rr : r.getAncestors()) {
                                        inheritMap.putIfAbsent(rr.getQualifiedName(), new ArrayList<>());
                                        inheritMap.get(rr.getQualifiedName()).add(classFullName);
                                    }

                                    boolean originalInInerface = inInterface;
                                    inInterface = n.isInterface();
                                    super.visit(n, arg);
                                    inInterface = originalInInerface;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    System.exit(1);
                                }
                            } else {
                                /*System.out.println(n.getName());
                                System.out.println(n.getAccessSpecifier());
                                System.out.println(n.isStatic());*/
                            }
                        }

                        @Override
                        public void visit(EnumDeclaration n, Object arg) {
                            super.visit(n, arg);
                            if (n.getAccessSpecifier() == Modifier.Keyword.PUBLIC) {
                                try {
                                    ResolvedEnumDeclaration r = n.resolve();
                                    String classFullName = r.getQualifiedName();
                                    typeMap.put(classFullName, new Type(classFullName));

                                    for (ResolvedEnumConstantDeclaration re : r.getEnumConstants()) {
                                        fieldMap.put(classFullName + "." + re.getName(), new Field(re));
                                    }

                                    for (ResolvedReferenceType rr : r.getAncestors()) {
                                        inheritMap.putIfAbsent(rr.getQualifiedName(), new ArrayList<>());
                                        inheritMap.get(rr.getQualifiedName()).add(classFullName);
                                    }

                                    super.visit(n, arg);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    System.exit(1);
                                }
                            } else {
                                /*System.out.println(n.getName());
                                System.out.println(n.getAccessSpecifier());
                                System.out.println(n.isStatic());*/
                            }
                        }

                        @Override
                        public void visit(ConstructorDeclaration n, Object arg) {
                            super.visit(n, arg);
                            if (n.getAccessSpecifier() == Modifier.Keyword.PUBLIC) {
                                try {
                                    ResolvedConstructorDeclaration r = n.resolve();
                                    constructorMap.put(r.getQualifiedSignature(), new Constructor(r));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void visit(MethodDeclaration n, Object arg) {
                            super.visit(n, arg);
                            if (n.getAccessSpecifier() == Modifier.Keyword.PUBLIC || inInterface) {
                                try {
                                    ResolvedMethodDeclaration r = n.resolve();
                                    methodMap.put(r.getQualifiedSignature(), new Method(r));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                    }, counter);
                });

        dirExplorer.explore(new File("/Users/apple/Downloads/poi-4.0.0/src/java/org/apache/poi"));

        for (String baseClass : inheritMap.keySet()) {
            List<String> subClasses = inheritMap.get(baseClass);
            try {
                Type baseType = typeMap.get(baseClass);
                if (baseType == null) {
                    throw new Exception("type not exist : " + baseClass);
                }
                /*for (String subClass : subClasses){
                    if (!typeMap.containsKey(subClass)){
                        System.exit(9);
                    }
                }*/
                baseType.setSubClasses(subClasses);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (String fieldName : fieldMap.keySet()){
            Field field = fieldMap.get(fieldName);
            for (String basicTypeName : basicTypeToFieldMap.keySet()){
                if (field.getFieldType().equals(basicTypeName) && field.isStatic()){
                    basicTypeToFieldMap.get(basicTypeName).add(fieldName);
                }
            }
        }

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        File typeFile = new File("data/types");
        File methodFile = new File("data/methods");
        File constructorFile = new File("data/constructors");
        File fieldFile = new File("data/fields");
        File inheritMapFile = new File("data/inheritMap");
        File basicTypeToFieldMapFile = new File("data/basicTypeToFieldMap");

        try {
            objectMapper.writeValue(typeFile, typeMap);
            objectMapper.writeValue(methodFile, methodMap);
            objectMapper.writeValue(constructorFile, constructorMap);
            objectMapper.writeValue(fieldFile, fieldMap);
            objectMapper.writeValue(inheritMapFile, inheritMap);
            objectMapper.writeValue(basicTypeToFieldMapFile, basicTypeToFieldMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Main().parse();
    }
}
