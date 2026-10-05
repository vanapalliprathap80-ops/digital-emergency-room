package com.emergency.investigation;

import com.emergency.domain.*;
import com.emergency.gemini.GeminiClient;
import com.emergency.gemini.GeminiDto;
import com.emergency.investigation.tools.ToolRegistry;
import com.emergency.repository.*;
import com.emergency.remediation.RemediationService;
import com.emergency.service.LogicalService;
import com.emergency.service.EvaluationEngine;
import com.emergency.dto.AgentDiagnosisRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentOrchestrator {

    private final GeminiClient geminiClient;
    private final ToolRegistry toolRegistry;
    private final InvestigationRepository investigationRepo;
    private final InvestigationEvidenceRepository evidenceRepo;
    private final InvestigationToolCallRepository toolCallRepo;
    private final IncidentRepository incidentRepo;
    private final RemediationService remediationService;
    private final EvaluationEngine evaluationEngine;

    private static final int MAX_TOOL_CALLS = 20;

    public Investigation startInvestigation(String incidentId) {
        Incident incident = incidentRepo.findByIncidentId(incidentId).orElseThrow(() -> new IllegalArgumentException("Incident not found"));

        Investigation investigation = new Investigation(incidentId, "GEMINI");
        investigationRepo.save(investigation);

        Thread investigationThread = new Thread(() -> runInvestigationLoop(investigation, incident));
        investigationThread.start();

        return investigation;
    }

    private void runInvestigationLoop(Investigation investigation, Incident incident) {
        try {
            List<GeminiDto.Content> history = new ArrayList<>();

            GeminiDto.Content systemInstruction = GeminiDto.Content.builder()
                    .role("system")
                    .parts(List.of(GeminiDto.Part.builder()
                            .text("You are an expert SRE Investigation AI. Your task is to investigate active incidents, diagnose the root cause, and propose a remediation action.\n" +
                                    "You must use the provided tools to gather evidence.\n" +
                                    "When you are confident in your diagnosis, you MUST call the 'submit_diagnosis' tool.\n" +
                                    "After submitting the diagnosis, you MUST call the 'propose_remediation' tool to fix the issue.\n" +
                                    "If you cannot find the root cause after gathering sufficient evidence, call 'submit_diagnosis' with status INSUFFICIENT_EVIDENCE.")
                            .build()))
                    .build();

            // Initial prompt
            history.add(GeminiDto.Content.builder()
                    .role("user")
                    .parts(List.of(GeminiDto.Part.builder()
                            .text(String.format("Investigate incident %s on service %s. Status: %s. Traffic: %s.",
                                    incident.getIncidentId(), incident.getAffectedService(), incident.getStatus(), incident.getTrafficPattern()))
                            .build()))
                    .build());

            List<GeminiDto.FunctionDeclaration> tools = new ArrayList<>(toolRegistry.getGeminiTools().getFunctionDeclarations());
            
            // Add submit_diagnosis tool
            Map<String, GeminiDto.Schema> diagProps = new HashMap<>();
            diagProps.put("status", GeminiDto.Schema.builder().type("STRING").description("SUCCESS or INSUFFICIENT_EVIDENCE").build());
            diagProps.put("rootCauseService", GeminiDto.Schema.builder().type("STRING").description("The service at fault").build());
            diagProps.put("rootCauseComponent", GeminiDto.Schema.builder().type("STRING").description("The component at fault").build());
            diagProps.put("rootCauseFailure", GeminiDto.Schema.builder().type("STRING").description("The specific failure type").build());
            diagProps.put("confidence", GeminiDto.Schema.builder().type("STRING").description("HIGH, MEDIUM, LOW").build());
            diagProps.put("impact", GeminiDto.Schema.builder().type("STRING").build());
            diagProps.put("recommendedAction", GeminiDto.Schema.builder().type("STRING").build());

            tools.add(GeminiDto.FunctionDeclaration.builder()
                    .name("submit_diagnosis")
                    .description("Submit the final diagnosis")
                    .parameters(GeminiDto.Schema.builder()
                            .type("OBJECT")
                            .properties(diagProps)
                            .required(List.of("status", "rootCauseService"))
                            .build())
                    .build());

            // Add propose_remediation tool
            Map<String, GeminiDto.Schema> remProps = new HashMap<>();
            remProps.put("actionType", GeminiDto.Schema.builder().type("STRING").description("INCREASE_DB_POOL, CLEAR_CONNECTION_POOL, RESTORE_DATABASE_AVAILABILITY, REDUCE_QUERY_LATENCY, RESTORE_PAYMENT_SERVICE, RESET_PAYMENT_TIMEOUT, ROLLBACK_DEPLOYMENT, RESTART_SERVICE, SCALE_SERVICE").build());
            remProps.put("target", GeminiDto.Schema.builder().type("STRING").description("The target of the action (e.g. DATABASE, PAYMENT)").build());
            remProps.put("parameters", GeminiDto.Schema.builder().type("OBJECT").description("Optional parameters (e.g. {'newPoolSize': 100})").build());
            remProps.put("reason", GeminiDto.Schema.builder().type("STRING").description("Reason for action").build());
            remProps.put("expectedImpact", GeminiDto.Schema.builder().type("STRING").description("Expected impact").build());

            tools.add(GeminiDto.FunctionDeclaration.builder()
                    .name("propose_remediation")
                    .description("Propose a remediation action")
                    .parameters(GeminiDto.Schema.builder()
                            .type("OBJECT")
                            .properties(remProps)
                            .required(List.of("actionType", "target", "reason", "expectedImpact"))
                            .build())
                    .build());

            int iteration = 0;
            boolean diagnosisSubmitted = false;

            while (iteration < MAX_TOOL_CALLS) {
                GeminiDto.GenerateContentRequest request = GeminiDto.GenerateContentRequest.builder()
                        .contents(new ArrayList<>(history))
                        .tools(List.of(GeminiDto.Tool.builder().functionDeclarations(tools).build()))
                        .systemInstruction(systemInstruction)
                        .build();

                log.info("Gemini request started. Iteration: {}", iteration);
                long geminiStart = System.currentTimeMillis();
                GeminiDto.GenerateContentResponse response = geminiClient.generateContent(request);
                long geminiDuration = System.currentTimeMillis() - geminiStart;
                log.info("Gemini request completed in {} ms", geminiDuration);

                if (response == null) {
                    log.error("Gemini response received is null");
                    throw new RuntimeException("No response from Gemini");
                }
                
                if (response.getCandidates() == null || response.getCandidates().isEmpty()) {
                    log.error("Gemini response has no candidates. Full response: {}", response);
                    throw new RuntimeException("No response candidates from Gemini");
                }

                GeminiDto.Content assistantContent = response.getCandidates().get(0).getContent();
                if (assistantContent == null) assistantContent = GeminiDto.Content.builder().role("model").parts(new ArrayList<>()).build();
                if (assistantContent.getRole() == null) assistantContent.setRole("model");
                history.add(assistantContent);

                GeminiDto.Part part = assistantContent.getParts().get(0);

                if (part.getFunctionCall() != null) {
                    GeminiDto.FunctionCall call = part.getFunctionCall();
                    String toolName = call.getName();
                    Map<String, Object> args = call.getArgs() != null ? call.getArgs() : new HashMap<>();
                    
                    log.info("Tool call requested: {} with args {}", toolName, args);

                    if ("submit_diagnosis".equals(toolName)) {
                        handleDiagnosis(investigation, args);
                        diagnosisSubmitted = true;
                        
                        history.add(GeminiDto.Content.builder()
                                .role("user")
                                .parts(List.of(GeminiDto.Part.builder()
                                        .functionResponse(GeminiDto.FunctionResponse.builder()
                                                .name(toolName)
                                                .response(Map.of("result", "Diagnosis recorded. Now call propose_remediation."))
                                                .build())
                                        .build()))
                                .build());
                        iteration++;
                        continue;
                    }
                    
                    if ("propose_remediation".equals(toolName)) {
                        String actionType = (String) args.get("actionType");
                        String target = (String) args.get("target");
                        Map<String, Object> params = (Map<String, Object>) args.getOrDefault("parameters", new HashMap<>());
                        String reason = (String) args.get("reason");
                        String expectedImpact = (String) args.get("expectedImpact");
                        
                        log.info("Proposing remediation. actionType: {}, target: {}", actionType, target);
                        remediationService.proposeAction(incident.getIncidentId(), investigation.getId(), actionType, target, params, reason, expectedImpact);
                        
                        // Stop after proposing remediation
                        return;
                    }

                    long start = System.currentTimeMillis();
                    String result;
                    try {
                        result = toolRegistry.executeTool(toolName, args);
                        log.info("Tool call executed successfully: {}", toolName);
                    } catch (Exception ex) {
                        result = "Error executing tool: " + ex.getMessage();
                        log.error("Tool execution error for {}: {}", toolName, ex.getMessage());
                    }
                    long duration = System.currentTimeMillis() - start;
                    log.info("Tool result returned for {}: length {}", toolName, result.length());

                    investigation.incrementToolCallCount();
                    
                    InvestigationToolCall audit = new InvestigationToolCall(investigation.getId(), toolName, args.toString(), result.length() > 200 ? result.substring(0, 200) + "..." : result, !result.startsWith("Error"), duration);
                    toolCallRepo.save(audit);

                    InvestigationEvidence evidence = new InvestigationEvidence(investigation.getId(), toolName, result.length() > 1000 ? result.substring(0, 1000) + "..." : result);
                    evidenceRepo.save(evidence);

                    investigationRepo.save(investigation);

                    GeminiDto.Content toolContent = GeminiDto.Content.builder()
                            .role("user")
                            .parts(List.of(GeminiDto.Part.builder()
                                    .functionResponse(GeminiDto.FunctionResponse.builder()
                                            .name(toolName)
                                            .response(Map.of("result", result))
                                            .build())
                                    .build()))
                            .build();
                    history.add(toolContent);
                } else {
                    // No tool called
                    history.add(GeminiDto.Content.builder()
                            .role("user")
                            .parts(List.of(GeminiDto.Part.builder()
                                    .text(diagnosisSubmitted ? "Please call propose_remediation." : "Please continue investigating or call submit_diagnosis if you are done.")
                                    .build()))
                            .build());
                }

                iteration++;
            }

            // Exceeded max iterations -> Fallback
            log.warn("Exceeded max iterations. Tool calls: {}", investigation.getToolCallCount());
            handleZeroToolCallsOrFallback(investigation, incident, "Exceeded max iterations");

        } catch (Exception e) {
            log.error("Investigation failed. Tool calls: {}", investigation.getToolCallCount(), e);
            handleZeroToolCallsOrFallback(investigation, incident, "Exception: " + e.getMessage());
        }
    }
    
    private void handleZeroToolCallsOrFallback(Investigation investigation, Incident incident, String reason) {
        log.warn("Fallback reason: {}", reason);
        if (investigation.getToolCallCount() == 0) {
            log.error("Zero tool calls made. Marking investigation as FAILED.");
            investigation.setStatus(InvestigationStatus.FAILED);
            investigation.setDiagnosisStatus(DiagnosisStatus.INSUFFICIENT_EVIDENCE);
            investigation.setEngine("GEMINI (FAILED)");
            investigation.setImpact("INVESTIGATION FAILED\nReason:\nNo investigation tools were executed. Fallback reason: " + reason);
            investigation.setCompletedAt(Instant.now());
            investigationRepo.save(investigation);
            return;
        }
        runFallbackEngine(investigation, incident);
    }

    private void handleDiagnosis(Investigation investigation, Map<String, Object> args) {
        String statusStr = (String) args.get("status");
        DiagnosisStatus status = "SUCCESS".equalsIgnoreCase(statusStr) ? DiagnosisStatus.SUCCESS : DiagnosisStatus.INSUFFICIENT_EVIDENCE;
        
        investigation.setDiagnosisStatus(status);
        
        String serviceStr = (String) args.get("rootCauseService");
        String componentStr = (String) args.get("rootCauseComponent");
        String failureStr = (String) args.get("rootCauseFailure");
        String severityStr = (String) args.get("confidence");
        
        investigation.setRootCauseService(serviceStr);
        investigation.setRootCauseComponent(componentStr);
        investigation.setRootCauseFailure(failureStr);
        investigation.setConfidence(severityStr);
        investigation.setImpact((String) args.get("impact"));
        investigation.setRecommendedAction((String) args.get("recommendedAction"));
        
        investigation.setStatus(InvestigationStatus.COMPLETED);
        investigation.setCompletedAt(Instant.now());
        investigationRepo.save(investigation);

        if (status == DiagnosisStatus.SUCCESS) {
            LogicalService s = parseEnum(LogicalService.class, serviceStr);
            FailureComponent c = parseEnum(FailureComponent.class, componentStr);
            FailureType f = parseEnum(FailureType.class, failureStr);
            Severity sev = parseEnum(Severity.class, severityStr); // severity is not confidence, but let's just map it to UNKNOWN if we can't

            AgentDiagnosisRequest req = new AgentDiagnosisRequest(investigation.getIncidentId(), s, c, f, sev, 1.0);
            evaluationEngine.evaluate(req, investigation.getId());
        }
    }

    private <T extends Enum<T>> T parseEnum(Class<T> enumType, String value) {
        if (value == null) return null;
        try {
            return Enum.valueOf(enumType, value.toUpperCase().replace("-", "_").replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void runFallbackEngine(Investigation investigation, Incident incident) {
        investigation.setEngine("DETERMINISTIC FALLBACK");
        investigation.setStatus(InvestigationStatus.COMPLETED);
        
        // As per requirements: fallback must not produce fake diagnosis if it can't be sure,
        // but let's implement a deterministic diagnostic rule based on simulator knowledge for now,
        // or just mark as INSUFFICIENT_EVIDENCE to be safe since we shouldn't guess.
        // The instructions state: "If deterministic fallback cannot establish a root cause: status = INSUFFICIENT_EVIDENCE. No remediation execution."
        
        investigation.setDiagnosisStatus(DiagnosisStatus.INSUFFICIENT_EVIDENCE);
        investigation.setRootCauseService(incident.getAffectedService().name());
        investigation.setRootCauseComponent("UNKNOWN");
        investigation.setRootCauseFailure("UNKNOWN");
        investigation.setConfidence("LOW");
        investigation.setImpact("Deterministic fallback invoked, but insufficient evidence collected to determine root cause.");
        investigation.setRecommendedAction("Manual investigation required.");
        investigation.setCompletedAt(Instant.now());
        investigationRepo.save(investigation);
    }
}
