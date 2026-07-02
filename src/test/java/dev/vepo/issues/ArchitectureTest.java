package dev.vepo.issues;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

class ArchitectureTest {
    private static ArchCondition<JavaMethod> haveBodyClassEndingWithRequest() {
        return new ArchCondition<JavaMethod>("have body class ending with Request") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                method.getParameters()
                      .stream()
                      .filter(p -> !p.isAnnotatedWith(PathParam.class) && !p.isAnnotatedWith(QueryParam.class) && !p.isAnnotatedWith(Context.class))
                      .filter(p -> !p.getType().getName().endsWith("Request"))
                      .map(p -> SimpleConditionEvent.violated(method, "Method %s has body parameter %s that is not a Request".formatted(method.getFullName(),
                                                                                                                                        p.getType().getName())))
                      .forEach(events::add);

            }
        };
    }

    private static ArchCondition<JavaMethod> haveReturnTypeAsResponse() {
        return new ArchCondition<JavaMethod>("have return type as Response") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                if (method.getRawReturnType().getName().equals("void")) {
                    return;
                }
                if (method.getRawReturnType().isAssignableFrom(List.class) ||
                        method.getRawReturnType().isAssignableFrom(Set.class)) {
                    var types = method.getReturnType().getAllInvolvedRawTypes().toArray(JavaClass[]::new);
                    if (types.length == 1 || types[1].getSimpleName().equals("Object")) {
                        events.add(SimpleConditionEvent.violated(method, "Returned List/Set typet %s is not define!".formatted(method.getFullName())));
                    } else if (!(types[1].getSimpleName().endsWith("Response") || types[1].getSimpleName().endsWith("Event"))) {
                        events.add(SimpleConditionEvent.violated(method,
                                                                 "Returned List/Set type for %s is not a Response or an Event!".formatted(method.getFullName())));
                    }
                    return;
                }
                System.out.println("Checking method: " + method.getFullName());
                System.out.println("Return type: " + method.getRawReturnType().getName());
                if (!method.getRawReturnType().getClass().isInstance(Response.class) &&
                        !(method.getRawReturnType().getName().endsWith("Response") || method.getRawReturnType().getName().endsWith("Event"))) {
                    events.add(SimpleConditionEvent.violated(method, "Method %s does not return Response or Event".formatted(method.getFullName())));
                }

            }
        };
    }

    private final JavaClasses importedClasses = new ClassFileImporter().importPackages("dev.vepo.issues");

    @Test
    void verifyAllBodyClassIsNamedRequest() {
        methods().that()
                 .arePublic()
                 .and()
                 .areAnnotatedWith(POST.class)
                 .or().areAnnotatedWith(GET.class)
                 .or().areAnnotatedWith(PUT.class)
                 .or().areAnnotatedWith(PATCH.class)
                 .or().areAnnotatedWith(DELETE.class)
                 .should()
                 .beDeclaredInClassesThat()
                 .haveSimpleNameEndingWith("Endpoint")
                 .andShould(haveBodyClassEndingWithRequest())
                 .check(importedClasses);
    }

    @Test
    void verifyAllResponseClassIsNamedResponse() {
        methods().that()
                 .arePublic()
                 .and()
                 .areAnnotatedWith(POST.class)
                 .or().areAnnotatedWith(GET.class)
                 .or().areAnnotatedWith(PUT.class)
                 .or().areAnnotatedWith(PATCH.class)
                 .or().areAnnotatedWith(DELETE.class)
                 .should()
                 .beDeclaredInClassesThat()
                 .haveSimpleNameEndingWith("Endpoint")
                 .andShould(haveReturnTypeAsResponse())
                 .check(importedClasses);
    }

    @Test
    @DisplayName("Verify that Request and Response classes are records and accessed only by Resources or other Requests/Responses")
    void verifySuffixRequestAndResponse() {
        classes().that()
                 .haveSimpleNameEndingWith("Request")
                 .should()
                 .beRecords()
                 .andShould()
                 .onlyBeAccessed()
                 .byClassesThat()
                 .haveSimpleNameEndingWith("Resource")
                 .orShould()
                 .haveSimpleNameEndingWith("Request")
                 .check(importedClasses);

        classes().that()
                 .haveSimpleNameEndingWith("Response")
                 .should()
                 .beRecords()
                 .andShould()
                 .onlyBeAccessed()
                 .byClassesThat()
                 .haveSimpleNameEndingWith("Resource")
                 .orShould()
                 .haveSimpleNameEndingWith("Response")
                 .check(importedClasses);
    }
}
