package com.emergency.topology;

import com.emergency.dto.TopologyEdge;
import com.emergency.dto.TopologyNode;
import com.emergency.dto.TopologyResponse;
import com.emergency.service.LogicalService;
import com.emergency.service.ServiceRegistry;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Returns the authoritative dependency topology.
 * Backend is the single source of truth — frontend only renders what we return.
 *
 * Topology:
 *   API_GATEWAY → AUTH
 *   API_GATEWAY → ORDERS
 *   ORDERS → DATABASE
 *   ORDERS → PAYMENT
 *   PAYMENT → DATABASE
 *   PAYMENT → NOTIFICATION
 */
@Service
public class TopologyService {

    private final ServiceRegistry serviceRegistry;

    public TopologyService(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    public TopologyResponse getTopology() {
        List<TopologyNode> nodes = List.of(
                node(LogicalService.API_GATEWAY, "API Gateway", "gateway", List.of()),
                node(LogicalService.AUTH, "Auth", "auth", List.of("API_GATEWAY")),
                node(LogicalService.ORDERS, "Orders", "orders", List.of("API_GATEWAY")),
                node(LogicalService.DATABASE, "Database", "database", List.of("ORDERS", "PAYMENT")),
                node(LogicalService.PAYMENT, "Payment", "payment", List.of("ORDERS")),
                node(LogicalService.NOTIFICATION, "Notification", "notification", List.of("PAYMENT"))
        );

        List<TopologyEdge> edges = List.of(
                new TopologyEdge("API_GATEWAY", "AUTH", "authenticates"),
                new TopologyEdge("API_GATEWAY", "ORDERS", "routes"),
                new TopologyEdge("ORDERS", "DATABASE", "persists order"),
                new TopologyEdge("ORDERS", "PAYMENT", "triggers payment"),
                new TopologyEdge("PAYMENT", "DATABASE", "persists payment"),
                new TopologyEdge("PAYMENT", "NOTIFICATION", "triggers notification")
        );

        return new TopologyResponse(nodes, edges);
    }

    private TopologyNode node(LogicalService service, String label, String type,
                               List<String> dependsOn) {
        return new TopologyNode(
                service.name(),
                label,
                type,
                serviceRegistry.getStatus(service).name(),
                dependsOn
        );
    }
}
