package com.emergency.investigation.tools;

import com.emergency.gemini.GeminiDto;
import java.util.Map;

public interface InvestigationTool {
    String getName();
    GeminiDto.FunctionDeclaration getDeclaration();
    String execute(Map<String, Object> args);
}
