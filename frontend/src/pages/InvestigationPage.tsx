
import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { ArrowLeft, BrainCircuit, Activity, FileText, CheckCircle2, AlertTriangle, AlertCircle, Clock } from 'lucide-react'
import { fetchInvestigation, startInvestigation, fetchIncident, fetchRemediations, approveRemediation, rejectRemediation } from '../api/client'
import LoadingSpinner from '../components/LoadingSpinner'
import ErrorState from '../components/ErrorState'
import type { IncidentResponse, RemediationAction } from '../types'

export default function InvestigationPage() {
  const { incidentId } = useParams<{ incidentId: string }>()
  const navigate = useNavigate()
  
  const [incident, setIncident] = useState<IncidentResponse | null>(null)
  const [investigationData, setInvestigationData] = useState<any>(null)
  const [remediations, setRemediations] = useState<RemediationAction[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [starting, setStarting] = useState(false)

  useEffect(() => {
    let interval: any;
    
    const load = async () => {
      if (!incidentId) return
      try {
        if (!incident) {
          const inc = await fetchIncident(incidentId)
          setIncident(inc)
        }
        
        const data = await fetchInvestigation(incidentId)
        setInvestigationData(data)
        
        const rems = await fetchRemediations(incidentId)
        setRemediations(rems)
        
        setLoading(false)
        
        const needsPolling = (data && data.investigation && data.investigation.status === 'RUNNING') ||
                             rems.some((r: RemediationAction) => r.status === 'PROPOSED' || r.status === 'EXECUTING')
                             
        if (needsPolling) {
          if (!interval) {
            interval = setInterval(load, 2000)
          }
        } else if (interval) {
          clearInterval(interval)
        }
      } catch (err: any) {
        setError(err.message || 'Failed to load investigation')
        setLoading(false)
        if (interval) clearInterval(interval)
      }
    }
    
    load()
    return () => { if (interval) clearInterval(interval) }
  }, [incidentId, incident])

  const handleStart = async () => {
    if (!incidentId) return
    setStarting(true)
    try {
      await startInvestigation(incidentId)
      const data = await fetchInvestigation(incidentId)
      setInvestigationData(data)
    } catch (err: any) {
      setError(err.message || 'Failed to start investigation')
    } finally {
      setStarting(false)
    }
  }

  if (loading) return <LoadingSpinner message="Loading investigation..." />
  if (error) return <ErrorState message={error} onRetry={() => window.location.reload()} />
  
  if (!investigationData) {
    return (
      <div className="flex flex-col items-center justify-center h-[70vh] space-y-6">
        <BrainCircuit size={64} className="text-primary/50" />
        <div className="text-center">
          <h2 className="text-2xl font-bold text-text-main mb-2">AI SRE Investigation</h2>
          <p className="text-text-muted max-w-md mx-auto">
            No investigation has been run for incident {incidentId} yet. Start an autonomous AI investigation to diagnose the root cause.
          </p>
        </div>
        <button 
          onClick={handleStart}
          disabled={starting}
          className="btn-primary py-3 px-6 text-sm flex items-center gap-2"
        >
          {starting ? <LoadingSpinner message="Initializing..." /> : (
            <>
              <BrainCircuit size={18} />
              Start Investigation
            </>
          )}
        </button>
      </div>
    )
  }

  const { investigation, evidence, toolCalls } = investigationData

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <button onClick={() => navigate('/chaos')} className="p-2 hover:bg-white/5 rounded-md text-text-muted transition-colors">
            <ArrowLeft size={20} />
          </button>
          <div>
            <h1 className="text-xl font-bold text-text-main flex items-center gap-2">
              <BrainCircuit size={20} className="text-primary" />
              Investigation Details
            </h1>
            <p className="text-sm text-text-muted mt-0.5">Incident {incidentId}</p>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <span className={`px-2.5 py-1 text-xs font-semibold uppercase tracking-wider rounded-full border ${
            investigation.status === 'RUNNING' ? 'bg-attention/10 text-attention border-attention/20 animate-pulse' :
            investigation.status === 'COMPLETED' ? 'bg-healthy/10 text-healthy border-healthy/20' :
            'bg-critical/10 text-critical border-critical/20'
          }`}>
            {investigation.status}
          </span>
          <span className="text-xs text-text-muted bg-surface border border-white/10 px-2 py-1 rounded">
            Engine: {investigation.engine}
          </span>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column: Trace & Evidence */}
        <div className="lg:col-span-2 space-y-6">
          <div className="card space-y-4">
            <h2 className="text-sm font-semibold text-text-main flex items-center gap-2">
              <Activity size={16} className="text-primary" />
              Investigation Trace ({toolCalls?.length || 0} Tools Called)
            </h2>
            <div className="space-y-3 max-h-[400px] overflow-y-auto pr-2 custom-scrollbar">
              {toolCalls?.map((call: any) => (
                <div key={call.id} className="bg-surface border border-white/5 p-3 rounded-md">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-xs font-mono font-semibold text-primary">{call.toolName}</span>
                    <span className="text-[10px] text-text-muted flex items-center gap-1">
                      <Clock size={10} /> {call.durationMs}ms
                    </span>
                  </div>
                  <div className="text-[10px] font-mono text-text-muted bg-black/20 p-2 rounded break-words">
                    {call.inputArgs}
                  </div>
                </div>
              ))}
              {investigation.status === 'RUNNING' && (
                <div className="flex items-center gap-2 text-xs text-text-muted p-2">
                  <LoadingSpinner message="" /> AI is thinking...
                </div>
              )}
            </div>
          </div>

          <div className="card space-y-4">
            <h2 className="text-sm font-semibold text-text-main flex items-center gap-2">
              <FileText size={16} className="text-secondary" />
              Collected Evidence
            </h2>
            <div className="space-y-4 max-h-[400px] overflow-y-auto pr-2 custom-scrollbar">
              {evidence?.map((ev: any) => (
                <div key={ev.id} className="border-l-2 border-secondary/50 pl-3">
                  <div className="text-xs font-semibold text-text-main mb-1">{ev.source}</div>
                  <div className="text-xs text-text-muted whitespace-pre-wrap font-mono bg-surface p-2 rounded">
                    {ev.observation}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Right Column: Diagnosis */}
        <div className="space-y-6">
          <div className="card border-l-4 border-l-primary space-y-4">
            <h2 className="text-sm font-semibold text-text-main">Root Cause Diagnosis</h2>
            
            {investigation.status !== 'COMPLETED' ? (
              <div className="text-sm text-text-muted text-center py-6 border border-dashed border-white/10 rounded">
                Diagnosis pending completion...
              </div>
            ) : investigation.diagnosisStatus === 'INSUFFICIENT_EVIDENCE' ? (
              <div className="bg-attention/10 text-attention p-4 rounded-md border border-attention/20 flex flex-col items-center text-center space-y-2">
                <AlertTriangle size={24} />
                <p className="text-sm font-medium">Insufficient Evidence</p>
                <p className="text-xs opacity-80">The AI could not conclusively determine the root cause based on available telemetry and logs.</p>
              </div>
            ) : (
              <div className="space-y-4">
                <div>
                  <div className="text-[10px] text-text-muted uppercase mb-1">Service at Fault</div>
                  <div className="text-sm font-medium text-critical flex items-center gap-1.5">
                    <AlertCircle size={14} />
                    {investigation.rootCauseService || 'Unknown'}
                  </div>
                </div>
                <div>
                  <div className="text-[10px] text-text-muted uppercase mb-1">Failing Component</div>
                  <div className="text-sm font-medium text-text-main bg-surface px-2 py-1 rounded inline-block">
                    {investigation.rootCauseComponent || 'Unknown'}
                  </div>
                </div>
                <div>
                  <div className="text-[10px] text-text-muted uppercase mb-1">Failure Signature</div>
                  <div className="text-sm font-medium text-text-main bg-surface px-2 py-1 rounded inline-block">
                    {investigation.rootCauseFailure || 'Unknown'}
                  </div>
                </div>
                <div>
                  <div className="text-[10px] text-text-muted uppercase mb-1">Confidence Score</div>
                  <div className="text-sm font-medium text-primary">
                    {investigation.confidence || 'N/A'}
                  </div>
                </div>
              </div>
            )}
            
            {investigationData.evaluation && (
              <div className="mt-4 pt-4 border-t border-white/10 space-y-3">
                <h3 className="text-sm font-semibold text-text-main flex items-center gap-2">
                  <CheckCircle2 size={16} className="text-healthy" />
                  Judge Evaluation
                </h3>
                
                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div className="bg-surface p-2 rounded flex justify-between items-center">
                    <span className="text-text-muted">Service Match:</span>
                    {investigationData.evaluation.serviceCorrect ? 
                      <span className="text-healthy font-bold">PASS</span> : 
                      <span className="text-critical font-bold">FAIL</span>}
                  </div>
                  <div className="bg-surface p-2 rounded flex justify-between items-center">
                    <span className="text-text-muted">Component Match:</span>
                    {investigationData.evaluation.componentCorrect ? 
                      <span className="text-healthy font-bold">PASS</span> : 
                      <span className="text-critical font-bold">FAIL</span>}
                  </div>
                  <div className="bg-surface p-2 rounded flex justify-between items-center">
                    <span className="text-text-muted">Failure Type Match:</span>
                    {investigationData.evaluation.failureCorrect ? 
                      <span className="text-healthy font-bold">PASS</span> : 
                      <span className="text-critical font-bold">FAIL</span>}
                  </div>
                  <div className="bg-surface p-2 rounded flex justify-between items-center">
                    <span className="text-text-muted">Severity Match:</span>
                    {investigationData.evaluation.severityCorrect ? 
                      <span className="text-healthy font-bold">PASS</span> : 
                      <span className="text-critical font-bold">FAIL</span>}
                  </div>
                </div>
              </div>
            )}
          </div>

          {(investigation.impact || investigation.recommendedAction) && (
            <div className="card space-y-4">
              <h2 className="text-sm font-semibold text-text-main">Analysis & Recommendation</h2>
              {investigation.impact && (
                <div>
                  <div className="text-[10px] text-text-muted uppercase mb-1">Estimated Impact</div>
                  <p className="text-sm text-text-main">{investigation.impact}</p>
                </div>
              )}
              {investigation.recommendedAction && (
                <div>
                  <div className="text-[10px] text-text-muted uppercase mb-1 mt-3">Recommended Remediation (Phase 4)</div>
                  <div className="bg-primary/10 text-primary p-3 rounded-md text-sm border border-primary/20">
                    {investigation.recommendedAction}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Remediations Panel */}
          {remediations.length > 0 && (
            <div className="card space-y-4">
              <h2 className="text-sm font-semibold text-text-main">Remediation Actions</h2>
              <div className="space-y-4">
                {remediations.map(rem => (
                  <div key={rem.actionId} className="bg-surface border border-white/10 p-4 rounded-md space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-sm font-semibold text-text-main">{rem.actionType}</span>
                      <span className={`px-2 py-1 text-[10px] font-bold rounded uppercase ${
                        rem.status === 'PROPOSED' ? 'bg-attention/20 text-attention' :
                        rem.status === 'APPROVED' ? 'bg-secondary/20 text-secondary' :
                        rem.status === 'SUCCESS' ? 'bg-healthy/20 text-healthy' :
                        rem.status === 'EXECUTING' ? 'bg-primary/20 text-primary animate-pulse' :
                        'bg-critical/20 text-critical'
                      }`}>
                        {rem.status}
                      </span>
                    </div>
                    
                    <div className="text-xs text-text-muted">
                      <span className="font-semibold text-text-main">Target:</span> {rem.targetService}
                    </div>
                    
                    <div className="text-xs text-text-muted">
                      <span className="font-semibold text-text-main">Reason:</span> {rem.reason}
                    </div>
                    
                    <div className="text-[10px] font-mono bg-black/20 p-2 rounded text-text-muted break-all">
                      {rem.parameters}
                    </div>
                    
                    {rem.status === 'PROPOSED' && (
                      <div className="flex gap-2 pt-2 border-t border-white/5">
                        <button 
                          onClick={async () => {
                            await approveRemediation(rem.actionId)
                            const updated = await fetchRemediations(incidentId!)
                            setRemediations(updated)
                          }}
                          className="flex-1 bg-healthy/20 text-healthy hover:bg-healthy/30 py-1.5 rounded text-xs font-semibold transition-colors"
                        >
                          Approve
                        </button>
                        <button 
                          onClick={async () => {
                            await rejectRemediation(rem.actionId)
                            const updated = await fetchRemediations(incidentId!)
                            setRemediations(updated)
                          }}
                          className="flex-1 bg-critical/20 text-critical hover:bg-critical/30 py-1.5 rounded text-xs font-semibold transition-colors"
                        >
                          Reject
                        </button>
                      </div>
                    )}
                    
                    {rem.status === 'SUCCESS' && rem.verificationResult && (
                      <div className="mt-2 text-xs text-healthy bg-healthy/10 p-2 rounded border border-healthy/20">
                        {rem.verificationResult}
                      </div>
                    )}
                    {(rem.status === 'FAILED' || rem.status === 'VERIFICATION_FAILED' || rem.status === 'VALIDATION_FAILED') && (
                      <div className="mt-2 text-xs text-critical bg-critical/10 p-2 rounded border border-critical/20">
                        {rem.failureReason || rem.validationResult || rem.verificationResult}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
