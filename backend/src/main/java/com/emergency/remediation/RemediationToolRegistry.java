package com.emergency.remediation;

import com.emergency.domain.RemediationActionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Dispatches execution to the correct typed RemediationTool.
 * No arbitrary tool can be invoked — only pre-registered implementations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RemediationToolRegistry {

    private final List<RemediationTool> tools;

    public Optional<RemediationTool> find(RemediationActionType type) {
        return tools.stream()
                .filter(t -> t.getType() == type)
                .findFirst();
    }

    public String execute(RemediationActionType type, Map<String, Object> parameters) {
        RemediationTool tool = find(type)
                .orElseThrow(() -> new IllegalArgumentException("No tool registered for: " + type));
        log.info("Executing remediation tool: {}", type);
        return tool.execute(parameters);
    }
}
