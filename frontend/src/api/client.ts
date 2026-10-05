import axios from 'axios'
import type {
  Order, TelemetryRecord, ApplicationEvent, ServiceStatus,
  SystemMetrics, ServiceMetrics, TopologyResponse,
  TrafficGenerateResponse, Page, ResetResponse, HealthResponse,
  RemediationAction
} from '../types'

const getBaseUrl = () => {
  const envUrl = import.meta.env.VITE_API_BASE_URL
  if (!envUrl) return '/api'
  return envUrl.endsWith('/api') ? envUrl : `${envUrl.replace(/\/+$/, '')}/api`
}

const api = axios.create({
  baseURL: getBaseUrl(),
  headers: { 'Content-Type': 'application/json' },
  timeout: 30000,
})

// ── Health ──────────────────────────────────────────────────
export const fetchHealth = () =>
  api.get<HealthResponse>('/health').then(r => r.data)

export const fetchServices = () =>
  api.get<ServiceStatus[]>('/services').then(r => r.data)

export const fetchService = (service: string) =>
  api.get<ServiceStatus>(`/services/${service}`).then(r => r.data)

// ── Orders ──────────────────────────────────────────────────
export const fetchOrders = (page = 0, size = 20) =>
  api.get<Page<Order>>('/orders', { params: { page, size } }).then(r => r.data)

export const fetchOrder = (id: string) =>
  api.get<Order>(`/orders/${id}`).then(r => r.data)

export const createOrder = (userId: string, amount: number) =>
  api.post<Order>('/orders', { userId, amount }).then(r => r.data)

// ── Telemetry / Requests ────────────────────────────────────
export const fetchRequests = (page = 0, size = 50, service?: string) =>
  api.get<Page<TelemetryRecord>>('/requests', { params: { page, size, service } }).then(r => r.data)

// ── Events ──────────────────────────────────────────────────
export const fetchEvents = (page = 0, size = 50) =>
  api.get<Page<ApplicationEvent>>('/events', { params: { page, size } }).then(r => r.data)

// ── Metrics ─────────────────────────────────────────────────
export const fetchMetrics = () =>
  api.get<SystemMetrics>('/metrics').then(r => r.data)

export const fetchServiceMetrics = () =>
  api.get<ServiceMetrics[]>('/metrics/services').then(r => r.data)

// ── Topology ────────────────────────────────────────────────
export const fetchTopology = () =>
  api.get<TopologyResponse>('/topology').then(r => r.data)

// ── Traffic ─────────────────────────────────────────────────
export const generateTraffic = (count: number) =>
  api.post<TrafficGenerateResponse>('/traffic/generate', { count }).then(r => r.data)

// ── Simulator ───────────────────────────────────────────────
export const resetSimulator = () =>
  api.post<ResetResponse>('/simulator/reset').then(r => r.data)

// ── Incidents & Chaos (Phase 2) ─────────────────────────────

import type { IncidentResponse, ChaosSuiteResponse, SimulatedSystemStateDto, TimelineEventResponse, IncidentMode, TrafficPattern, LogicalService } from '../types'

export const fetchSystemState = () =>
  api.get<SimulatedSystemStateDto>('/incidents/system-state').then(r => r.data)

export const fetchIncidents = (page = 0, size = 20) =>
  api.get<Page<IncidentResponse>>('/incidents', { params: { page, size } }).then(r => r.data)

export const fetchIncident = (id: string) =>
  api.get<IncidentResponse>(`/incidents/${id}`).then(r => r.data)

export const fetchTimeline = (id: string) =>
  api.get<TimelineEventResponse[]>(`/incidents/${id}/timeline`).then(r => r.data)

export const resetIncident = (id: string) =>
  api.post<IncidentResponse>(`/incidents/${id}/reset`).then(r => r.data)

export const createManualIncident = (service: LogicalService, mode: IncidentMode) =>
  api.post<IncidentResponse>('/incidents/manual', { service, mode }).then(r => r.data)

export const createChaosIncident = (sessionId?: string, seed?: number, trafficPattern?: TrafficPattern) =>
  api.post<IncidentResponse>('/incidents/chaos', { sessionId, seed, trafficPattern }).then(r => r.data)

export const runChaosSuite = (count = 5, sessionId = 'default-session') =>
  api.post<ChaosSuiteResponse>('/chaos/suite', null, { params: { count, sessionId } }).then(r => r.data)

// "?"? Investigations (Phase 3) "?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?"?

export const startInvestigation = (incidentId: string) =>
  api.post(`/investigations/${incidentId}/start`).then(r => r.data)

export const fetchInvestigation = (incidentId: string) =>
  api.get(`/investigations/${incidentId}`).then(r => r.data).catch(e => {
    if (e.response && e.response.status === 404) return null
    throw e
  })

// ============================================================
// Phase 4: Remediation
// ============================================================

export const fetchRemediations = (incidentId: string) =>
  api.get<RemediationAction[]>(`/remediations/incident/${incidentId}`).then(r => r.data)

export const approveRemediation = (actionId: string, approvedBy: string = 'Human Operator') =>
  api.post<RemediationAction>(`/remediations/${actionId}/approve`, { approvedBy }).then(r => r.data)

export const rejectRemediation = (actionId: string, rejectedBy: string = 'Human Operator', reason: string = 'No reason provided') =>
  api.post<RemediationAction>(`/remediations/${actionId}/reject`, { rejectedBy, reason }).then(r => r.data)

