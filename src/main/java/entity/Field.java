package entity;

import com.github.javaparser.resolution.declarations.ResolvedEnumConstantDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import lombok.Data;

@Data
public class Field {
    private boolean isStatic;
    private String packageName;
    private String className;
    private String fieldType;
    private String fieldName;
    private int usageCount;


    public Field(ResolvedFieldDeclaration r) {
        isStatic = r.isStatic();
        fieldType = r.getType().describe();
        fieldName = r.getName();

        String classFullName = r.declaringType().getQualifiedName();
        int lastDotIndex = classFullName.lastIndexOf(".");
        packageName = classFullName.substring(0, lastDotIndex);
        className = classFullName.substring(lastDotIndex + 1);
    }

    public Field(ResolvedEnumConstantDeclaration r) {
        isStatic = true;
        fieldType = r.getType().describe();
        fieldName = r.getName();

        String classFullName = r.getType().describe();
        int lastDotIndex = classFullName.lastIndexOf(".");
        packageName = classFullName.substring(0, lastDotIndex);
        className = classFullName.substring(lastDotIndex + 1);
    }

    public Field(){}
}
