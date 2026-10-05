// ============================================================
// Domain types mirroring the backend DTOs
// ============================================================

export type OrderStatus = 'CREATED' | 'PAYMENT_PENDING' | 'PAID' | 'FAILED'
export type HealthStatus = 'HEALTHY' | 'DEGRADED' | 'UNAVAILABLE'

export interface Order {
  id: string
  userId: string
  amount: number
  status: OrderStatus
  createdAt: string
}

export interface TelemetryRecord {
  id: number
  requestId: string
  timestamp: string
  service: string
  operation: string
  endpoint: string
  httpMethod: string
  statusCode: number
  success: boolean
  latencyMs: number
  errorType: string | null
  message: string | null
}

export interface ApplicationEvent {
  id: number
  eventId: string
  timestamp: string
  requestId: string | null
  eventType: string
  service: string
  message: string | null
  metadata: string | null
}

export interface ServiceStatus {
  service: string
  status: HealthStatus
  description: string
}

export interface ServiceMetrics {
  service: string
  healthStatus: HealthStatus
  requestCount: number
  errorCount: number
  errorRate: number
  averageLatencyMs: number
  p95LatencyMs: number
  requestsPerMinute: number
}

export interface SystemMetrics {
  totalRequests: number
  totalErrors: number
  errorRate: number
  averageLatencyMs: number
  p95LatencyMs: number
  requestsPerMinute: number
  services: ServiceMetrics[]
}

export interface TopologyNode {
  id: string
  label: string
  type: string
  healthStatus: HealthStatus
  dependsOn: string[]
}

export interface TopologyEdge {
  source: string
  target: string
  label: string
}

export interface TopologyResponse {
  nodes: TopologyNode[]
  edges: TopologyEdge[]
}

export interface TrafficGenerateResponse {
  requested: number
  completed: number
  succeeded: number
  failed: number
  durationMs: number
}

// Spring Data Page
export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  last: boolean
}

export interface ResetResponse {
  status: string
  telemetryCleared: number
  eventsCleared: number
  ordersCleared: number
  servicesRestored: string
}

export interface HealthResponse {
  status: string
  services: ServiceStatus[]
}

// ── Phase 2: Chaos & Incidents ──────────────────────────────

export type IncidentStatus = 'CREATED' | 'INJECTED' | 'ACTIVE' | 'RECOVERING' | 'RESOLVED' | 'MITIGATED' | 'RESET'
export type IncidentMode = 'MANUAL' | 'CHAOS' | 'AI_AGENT'
export type Severity = 'SEV_1' | 'SEV_2' | 'SEV_3' | 'SEV_4' | 'SEV_5'
export type LogicalService = 'API_GATEWAY' | 'AUTH' | 'ORDERS' | 'PAYMENT' | 'DATABASE' | 'NOTIFICATION'
export type TrafficPattern = 'NORMAL' | 'SPIKE' | 'NONE'

export interface IncidentResponse {
  incidentId: string
  sessionId: string
  status: IncidentStatus
  mode: IncidentMode
  severity: Severity
  affectedService: LogicalService
  trafficPattern: TrafficPattern
  createdAt: string
  startedAt: string
  endedAt: string | null
}

export interface ChaosSuiteResponse {
  totalRequested: number
  incidentsInjected: number
  incidentsCompleted: number
  injectionFailures: number
  resetSuccessRate: number
  telemetryObserved: number
  expectedDegradationObserved: number
  incidentSummaries: string[]
}

export interface SimulatedSystemStateDto {
  dbPoolSize: number
  dbActiveConnections: number
  dbAvailableConnections: number
  dbAcquisitionFailures: number
  dbAvailable: boolean
  cpuUtilizationPercent: number
  memoryUtilizationPercent: number
  deploymentVersion: string
  deploymentStatus: string
  paymentAvailable: boolean
  hasActiveIncident: boolean
  activeIncidentId: string | null
}

export interface TimelineEventResponse {
  id: number
  incidentId: string
  timestamp: string
  eventType: string
  description: string
  source: string
}

// ============================================================
// Phase 4: Remediation
// ============================================================
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
export type RemediationStatus = 'PROPOSED' | 'VALIDATION_FAILED' | 'APPROVED' | 'REJECTED' | 'EXECUTING' | 'SUCCESS' | 'FAILED' | 'VERIFICATION_FAILED'

export interface RemediationAction {
  actionId: string
  incidentId: string
  investigationId: string
  actionType: string
  targetService: string
  parameters: string
  riskLevel: RiskLevel
  status: RemediationStatus
  proposedAt: string
  approvedAt: string | null
  executedAt: string | null
  completedAt: string | null
  proposedBy: string
  approvedBy: string | null
  validationResult: string | null
  executionResult: string | null
  failureReason: string | null
  reason: string
  expectedImpact: string
  verificationResult: string | null
}

