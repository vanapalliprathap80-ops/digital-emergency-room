package com.emergency.investigation.tools;

import com.emergency.gemini.GeminiDto;
import com.emergency.repository.SimulatorLogRepository;
import com.emergency.repository.TelemetryRepository;
import com.emergency.repository.IncidentTimelineRepository;
import com.emergency.service.LogicalService;
import com.emergency.domain.SimulatorLog;
import com.emergency.domain.TelemetryRecord;
import com.emergency.domain.IncidentTimelineEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.stream.Collectors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class ToolsConfig {

    @Bean
    public InvestigationTool getServiceLogsTool(SimulatorLogRepository logRepo) {
        Map<String, GeminiDto.Schema> props = new HashMap<>();
        props.put("service", GeminiDto.Schema.builder().type("STRING").description("The service name (e.g. USER_SERVICE)").build());
        props.put("limit", GeminiDto.Schema.builder().type("INTEGER").description("Max logs to return (max 50)").build());

        GeminiDto.Schema schema = GeminiDto.Schema.builder()
                .type("OBJECT")
                .properties(props)
                .required(List.of("service"))
                .build();

        return new AbstractInvestigationTool("get_service_logs", "Get recent logs for a specific service", schema) {
            @Override
            public String execute(Map<String, Object> args) {
                String serviceStr = (String) args.get("service");
                LogicalService service;
                try {
                    service = LogicalService.valueOf(serviceStr);
                } catch (Exception e) {
                    return "Invalid service: " + serviceStr;
                }
                
                int limit = 20;
                if (args.containsKey("limit")) {
                    try { limit = ((Number) args.get("limit")).intValue(); } catch (Exception e) {}
                }
                if (limit > 50) limit = 50;
                
                Pageable page = PageRequest.of(0, limit);
                List<SimulatorLog> logs = logRepo.findByServiceOrderByTimestampDesc(service, page).getContent();
                
                if (logs.isEmpty()) return "No logs found for " + service;
                
                StringBuilder sb = new StringBuilder("Logs for " + service + ":\n");
                for (SimulatorLog l : logs) {
                    sb.append(String.format("[%s] %s: %s %s\n", l.getTimestamp(), l.getLevel(), l.getMessage(), l.getMetadata() != null ? l.getMetadata() : ""));
                }
                return sb.toString();
            }
        };
    }

    @Bean
    public InvestigationTool getServiceMetricsTool(TelemetryRepository telemetryRepo) {
        Map<String, GeminiDto.Schema> props = new HashMap<>();
        props.put("service", GeminiDto.Schema.builder().type("STRING").description("The service name").build());
        props.put("limit", GeminiDto.Schema.builder().type("INTEGER").description("Max records to return (max 50)").build());

        GeminiDto.Schema schema = GeminiDto.Schema.builder()
                .type("OBJECT")
                .properties(props)
                .required(List.of("service"))
                .build();

        return new AbstractInvestigationTool("get_service_metrics", "Get recent telemetry (latencies, errors) for a specific service", schema) {
            @Override
            public String execute(Map<String, Object> args) {
                String service = (String) args.get("service");
                int limit = 20;
                if (args.containsKey("limit")) {
                    try { limit = ((Number) args.get("limit")).intValue(); } catch (Exception e) {}
                }
                if (limit > 50) limit = 50;

                Pageable page = PageRequest.of(0, limit);
                List<TelemetryRecord> records = telemetryRepo.findByServiceOrderByTimestampDesc(service, page).getContent();

                if (records.isEmpty()) return "No telemetry found for " + service;

                StringBuilder sb = new StringBuilder("Telemetry for " + service + ":\n");
                for (TelemetryRecord r : records) {
                    sb.append(String.format("[%s] Req: %s | Success: %b | Latency: %dms\n",
                            r.getTimestamp(), r.getRequestId(), r.isSuccess(), r.getLatencyMs()));
                }
                return sb.toString();
            }
        };
    }

    @Bean
    public InvestigationTool getDependencyGraphTool() {
        return new AbstractInvestigationTool("get_dependency_graph", "Returns the static architecture and dependencies between services", null) {
            @Override
            public String execute(Map<String, Object> args) {
                return "API_GATEWAY -> USER_SERVICE, INVENTORY_SERVICE, ORDER_SERVICE\n" +
                       "ORDER_SERVICE -> USER_SERVICE, INVENTORY_SERVICE, PAYMENT_SERVICE, NOTIFICATION_SERVICE\n" +
                       "USER_SERVICE -> (Database)\n" +
                       "INVENTORY_SERVICE -> (Database)\n" +
                       "PAYMENT_SERVICE -> (External API)\n" +
                       "NOTIFICATION_SERVICE -> (External API)";
            }
        };
    }

    @Bean
    public InvestigationTool getRecentEventsTool(IncidentTimelineRepository timelineRepo) {
        Map<String, GeminiDto.Schema> props = new HashMap<>();
        props.put("incidentId", GeminiDto.Schema.builder().type("STRING").description("The active incident ID").build());

        GeminiDto.Schema schema = GeminiDto.Schema.builder()
                .type("OBJECT")
                .properties(props)
                .required(List.of("incidentId"))
                .build();

        return new AbstractInvestigationTool("get_recent_events", "Get recent system events (e.g. deployments, config changes)", schema) {
            @Override
            public String execute(Map<String, Object> args) {
                String incidentId = (String) args.get("incidentId");
                List<IncidentTimelineEvent> events = timelineRepo.findByIncidentIdOrderByTimestampAsc(incidentId);
                
                if (events.isEmpty()) return "No events found.";
                
                StringBuilder sb = new StringBuilder("Recent Events:\n");
                for (IncidentTimelineEvent e : events) {
                    sb.append(String.format("[%s] %s | %s: %s\n", e.getTimestamp(), e.getEventType(), e.getService() != null ? e.getService() : "SYSTEM", e.getMessage()));
                }
                return sb.toString();
            }
        };
    }
}
