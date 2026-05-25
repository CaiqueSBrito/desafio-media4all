import type { Agent, AgentStatus, CheckIn, SyncExecutionStatus } from "@/lib/api";

const statusLabels: Record<AgentStatus, string> = {
  ONLINE: "Online",
  PAUSED: "Pausado",
  SIGNAL_LOST: "Sinal perdido",
  OFFLINE: "Offline",
};

const syncStatusLabels: Record<SyncExecutionStatus, string> = {
  RUNNING: "Executando",
  SUCCESS: "Sucesso",
  FAILED: "Falha",
  WARNING: "Alerta",
};

export function labelAgentStatus(status: AgentStatus) {
  return statusLabels[status] ?? status;
}

export function labelSyncStatus(status?: SyncExecutionStatus | null) {
  return status ? syncStatusLabels[status] ?? status : "Nao informado";
}

export function toNumber(value: number | string | null | undefined) {
  if (value === null || value === undefined || value === "") {
    return null;
  }
  const numeric = Number(value);
  return Number.isFinite(numeric) ? numeric : null;
}

export function formatCoordinate(value: number | string | null | undefined) {
  const numeric = toNumber(value);
  return numeric === null ? "Sem dado" : numeric.toFixed(6);
}

export function formatMeters(value: number | string | null | undefined) {
  const numeric = toNumber(value);
  if (numeric === null) {
    return "0 m";
  }
  if (numeric >= 1000) {
    return `${(numeric / 1000).toFixed(2)} km`;
  }
  return `${Math.round(numeric)} m`;
}

export function formatDateTime(value?: string | null) {
  if (!value) {
    return "Sem dado";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "Sem dado";
  }
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(date);
}

export function formatDate(value?: string | null) {
  if (!value) {
    return "Sem dado";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "Sem dado";
  }
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
  }).format(date);
}

export function relativeTime(value?: string | null) {
  if (!value) {
    return "Sem atualizacao";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "Sem atualizacao";
  }
  const diffMs = Date.now() - date.getTime();
  if (diffMs < 60_000) {
    return "agora";
  }
  const minutes = Math.floor(diffMs / 60_000);
  if (minutes < 60) {
    return `${minutes} min`;
  }
  const hours = Math.floor(minutes / 60);
  if (hours < 24) {
    return `${hours} h`;
  }
  const days = Math.floor(hours / 24);
  return `${days} d`;
}

export function isAgentDelayed(agent: Agent) {
  if (!agent.lastSeen || !agent.currentLatitude || !agent.currentLongitude) {
    return true;
  }
  const date = new Date(agent.lastSeen);
  if (Number.isNaN(date.getTime())) {
    return true;
  }
  const minutes = (Date.now() - date.getTime()) / 60_000;
  return minutes > 30 || agent.status === "SIGNAL_LOST" || agent.status === "OFFLINE";
}

export function lastCheckIn(checkIns?: CheckIn[]) {
  if (!checkIns?.length) {
    return null;
  }
  return [...checkIns].sort((a, b) => new Date(b.occurredAt).getTime() - new Date(a.occurredAt).getTime())[0];
}

export function todayIsoDate() {
  return new Date().toISOString().slice(0, 10);
}

export function toDateTimeLocalValue(date = new Date()) {
  const offset = date.getTimezoneOffset();
  const local = new Date(date.getTime() - offset * 60_000);
  return local.toISOString().slice(0, 16);
}
