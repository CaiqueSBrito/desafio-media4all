import type { AgentStatus, SyncExecutionStatus } from "@/lib/api";
import { labelAgentStatus, labelSyncStatus } from "@/lib/format";
import { Badge, type BadgeProps } from "@/components/ui/badge";

const agentVariantByStatus: Record<AgentStatus, BadgeProps["variant"]> = {
  ONLINE: "success",
  PAUSED: "warning",
  SIGNAL_LOST: "destructive",
  OFFLINE: "muted",
};

const syncVariantByStatus: Record<SyncExecutionStatus, BadgeProps["variant"]> = {
  RUNNING: "warning",
  SUCCESS: "success",
  WARNING: "warning",
  FAILED: "destructive",
};

export function AgentStatusBadge({ status }: { status: AgentStatus }) {
  return <Badge variant={agentVariantByStatus[status]}>{labelAgentStatus(status)}</Badge>;
}

export function SyncStatusBadge({ status }: { status?: SyncExecutionStatus | null }) {
  return <Badge variant={status ? syncVariantByStatus[status] : "muted"}>{labelSyncStatus(status)}</Badge>;
}
