export type AgentStatus = "ONLINE" | "PAUSED" | "SIGNAL_LOST" | "OFFLINE";
export type AgentRole = "TECHNICIAN" | "VENDOR" | "INSTALLER" | "MAINTENANCE";
export type CheckInType =
  | "CHECKIN"
  | "CHECKOUT"
  | "VISIT_COMPLETED"
  | "STOP_DETECTED"
  | "STOP_ENDED"
  | "SIGNAL_LOST"
  | "SIGNAL_RESTORED"
  | "LOW_BATTERY"
  | "GEOFENCE_ENTER"
  | "GEOFENCE_EXIT";
export type SyncSource = "MANUAL" | "GPS_SYNC" | "EVENT_SYNC";
export type SyncType =
  | "AGENTS"
  | "LOCATIONS"
  | "CHECK_INS"
  | "GEOFENCES"
  | "ROUTES"
  | CheckInType;
export type SyncExecutionStatus = "RUNNING" | "SUCCESS" | "WARNING" | "FAILED";

type Numeric = number | string | null;

export type Agent = {
  id: string;
  externalId: string | null;
  name: string;
  role: AgentRole;
  team: string | null;
  phone: string | null;
  email: string | null;
  active: boolean;
  status: AgentStatus;
  battery: number | null;
  lastSeen: string | null;
  currentLatitude: Numeric;
  currentLongitude: Numeric;
  currentAddress: string | null;
  currentAccuracy: Numeric;
  currentSpeed: Numeric;
  createdAt: string;
  updatedAt: string;
};

export type CheckIn = {
  id: string;
  agentId: string;
  type: CheckInType;
  source: SyncSource;
  latitude: Numeric;
  longitude: Numeric;
  address: string | null;
  accuracy: Numeric;
  speed: Numeric;
  notes: string | null;
  distanceFromPrevious: Numeric;
  externalEventId: string | null;
  manualIdempotencyKey: string | null;
  occurredAt: string;
  syncedAt: string | null;
};

export type RoutePoint = {
  latitude: Numeric;
  longitude: Numeric;
  accuracy: Numeric;
  speed: Numeric;
  address: string | null;
  timestamp: string;
  source: SyncSource;
};

export type RouteHistory = {
  agentId: string;
  agentName: string;
  date: string;
  totalDistanceMeters: Numeric;
  points: RoutePoint[];
};

export type SyncMonitoringTotals = {
  executions: number;
  failures: number;
  recordsRead: number;
  recordsCreated: number;
  recordsUpdated: number;
  recordsIgnored: number;
  recordsFailed: number;
  retryAttempts: number;
  rateLimitErrors: number;
  serviceUnavailableErrors: number;
};

export type SyncSchedulerStatus = {
  syncType: SyncType;
  status: SyncExecutionStatus;
  lastStartedAt: string | null;
  lastFinishedAt: string | null;
  lastCursorValue: string | null;
  lastSuccessfulSyncAt: string | null;
  retryAttempts: number;
  rateLimitErrors: number;
  serviceUnavailableErrors: number;
  errorMessage: string | null;
};

export type SyncExecutionMonitoring = {
  id: number;
  syncType: SyncType;
  status: SyncExecutionStatus;
  startedAt: string;
  finishedAt: string | null;
  durationMillis: number | null;
  recordsRead: number;
  recordsCreated: number;
  recordsUpdated: number;
  recordsIgnored: number;
  recordsFailed: number;
  retryAttempts: number;
  rateLimitErrors: number;
  serviceUnavailableErrors: number;
  cursorValueBefore: string | null;
  cursorValueAfter: string | null;
  errorMessage: string | null;
};

export type SyncFailureMonitoring = {
  id: number;
  syncExecutionId: number;
  syncType: SyncType;
  entityType: string;
  reason: string;
  createdAt: string;
};

export type SyncCursorMonitoring = {
  syncType: SyncType;
  lastCursorValue: string | null;
  lastPage: number | null;
  lastOccurredAt: string | null;
  lastSyncedAt: string | null;
  lastSuccessfulSyncAt: string | null;
  updatedAt: string | null;
};

export type SyncMonitoring = {
  generatedAt: string;
  totals: SyncMonitoringTotals;
  schedulers: SyncSchedulerStatus[];
  latestExecutions: SyncExecutionMonitoring[];
  recentFailures: SyncFailureMonitoring[];
  cursors: SyncCursorMonitoring[];
};

export type AgentPayload = {
  externalId?: string;
  name?: string;
  role?: AgentRole;
  team?: string | null;
  phone?: string | null;
  email?: string | null;
  active?: boolean;
  status?: AgentStatus;
  battery?: number | null;
  lastSeen?: string | null;
};

export type ManualCheckInPayload = {
  type: CheckInType;
  latitude: number;
  longitude: number;
  address?: string | null;
  accuracy?: number | null;
  speed?: number | null;
  notes?: string | null;
  occurredAt?: string | null;
  idempotencyKey?: string | null;
};

export class ApiError extends Error {
  status: number;
  details: unknown;

  constructor(message: string, status: number, details: unknown) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details;
  }
}

const basePath = process.env.NEXT_PUBLIC_API_BASE_PATH ?? "/api/backend";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${basePath}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
    ...init,
  });

  if (!response.ok) {
    let details: unknown = null;
    try {
      details = await response.json();
    } catch {
      details = await response.text();
    }

    const message =
      typeof details === "object" && details && "message" in details
        ? String((details as { message?: unknown }).message)
        : `Falha HTTP ${response.status}`;
    throw new ApiError(message, response.status, details);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

export const api = {
  listAgents: async () => {
    const response = await request<{ data: Agent[] }>("/api/v1/agents");
    return response.data;
  },
  getAgent: (id: string) => request<Agent>(`/api/v1/agents/${id}`),
  createAgent: (payload: AgentPayload) =>
    request<Agent>("/api/v1/agents", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  updateAgent: (id: string, payload: AgentPayload) =>
    request<Agent>(`/api/v1/agents/${id}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    }),
  listCheckIns: async (agentId: string) => {
    const response = await request<{ data: CheckIn[] }>(`/api/v1/agents/${agentId}/check-ins`);
    return response.data;
  },
  createCheckIn: (agentId: string, payload: ManualCheckInPayload) =>
    request<CheckIn>(`/api/v1/agents/${agentId}/check-ins`, {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  getRoute: (agentId: string, date: string) => request<RouteHistory>(`/api/v1/agents/${agentId}/route?date=${date}`),
  getMonitoring: () => request<SyncMonitoring>("/api/v1/monitoring/sync?executionLimit=8&failureLimit=8"),
};

export const queryKeys = {
  agents: ["agents"] as const,
  agent: (id: string) => ["agent", id] as const,
  checkIns: (agentId: string) => ["agent", agentId, "check-ins"] as const,
  route: (agentId: string, date: string) => ["agent", agentId, "route", date] as const,
  monitoring: ["monitoring", "sync"] as const,
};
