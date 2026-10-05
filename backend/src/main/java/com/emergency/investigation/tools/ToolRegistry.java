package com.emergency.investigation.tools;

import com.emergency.gemini.GeminiDto;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ToolRegistry {
    private final Map<String, InvestigationTool> tools = new HashMap<>();

    public ToolRegistry(List<InvestigationTool> toolList) {
        for (InvestigationTool tool : toolList) {
            tools.put(tool.getName(), tool);
        }
    }

    public GeminiDto.Tool getGeminiTools() {
        List<GeminiDto.FunctionDeclaration> declarations = tools.values().stream()
                .map(InvestigationTool::getDeclaration)
                .collect(Collectors.toList());
        return GeminiDto.Tool.builder()
                .functionDeclarations(declarations)
                .build();
    }

    public String executeTool(String name, Map<String, Object> args) {
        InvestigationTool tool = tools.get(name);
        if (tool == null) {
            return "Error: Tool '" + name + "' not found.";
        }
        try {
            return tool.execute(args);
        } catch (Exception e) {
            return "Error executing tool: " + e.getMessage();
        }
    }
    
    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }
}
