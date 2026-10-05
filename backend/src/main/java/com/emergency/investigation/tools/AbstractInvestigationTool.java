package com.emergency.investigation.tools;

import com.emergency.gemini.GeminiDto;
import java.util.Map;
import java.util.Collections;

public abstract class AbstractInvestigationTool implements InvestigationTool {

    private final String name;
    private final String description;
    private final GeminiDto.Schema parameters;

    public AbstractInvestigationTool(String name, String description, GeminiDto.Schema parameters) {
        this.name = name;
        this.description = description;
        this.parameters = parameters != null ? parameters : GeminiDto.Schema.builder()
                .type("OBJECT")
                .properties(Collections.emptyMap())
                .build();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public GeminiDto.FunctionDeclaration getDeclaration() {
        return GeminiDto.FunctionDeclaration.builder()
                .name(name)
                .description(description)
                .parameters(parameters)
                .build();
    }

    @Override
    public abstract String execute(Map<String, Object> args);
}
