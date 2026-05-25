"use client";

import { useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Activity,
  AlertTriangle,
  Clock,
  Eye,
  FileClock,
  Gauge,
  History,
  Layers,
  Loader2,
  MapPin,
  MapPinned,
  Pencil,
  Plus,
  RefreshCcw,
  Route,
  Search,
  ServerCog,
  ShieldAlert,
  UserRound,
  UsersRound,
  Wifi,
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import type { Agent, AgentStatus, CheckIn } from "@/lib/api";
import { api, queryKeys } from "@/lib/api";
import {
  formatCoordinate,
  formatDateTime,
  formatMeters,
  isAgentDelayed,
  lastCheckIn,
  relativeTime,
  todayIsoDate,
} from "@/lib/format";
import { AgentDetailContent } from "@/components/dashboard/agent-detail-content";
import { AgentFormDialog } from "@/components/dashboard/agent-form-dialog";
import { CheckInDialog } from "@/components/dashboard/check-in-dialog";
import { AgentStatusBadge } from "@/components/dashboard/status-badge";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { toast } from "@/components/ui/use-toast";
import { cn } from "@/lib/utils";

const sidebarItems = [
  { id: "overview", label: "Visao geral", icon: Gauge },
  { id: "agents", label: "Agentes", icon: UsersRound },
  { id: "details", label: "Detalhes", icon: UserRound },
  { id: "routes", label: "Rotas", icon: Route },
  { id: "monitoring", label: "Monitoramento", icon: ServerCog },
];

const statusFilterOptions: Array<{ value: "ALL" | AgentStatus; label: string }> = [
  { value: "ALL", label: "Todos" },
  { value: "ONLINE", label: "Online" },
  { value: "PAUSED", label: "Pausado" },
  { value: "SIGNAL_LOST", label: "Sinal perdido" },
  { value: "OFFLINE", label: "Offline" },
];

function useDesktopBreakpoint() {
  const [isDesktop, setIsDesktop] = useState(false);

  useEffect(() => {
    const query = window.matchMedia("(min-width: 1280px)");
    const update = () => setIsDesktop(query.matches);
    update();
    query.addEventListener("change", update);
    return () => query.removeEventListener("change", update);
  }, []);

  return isDesktop;
}

function useAgentNotes() {
  const [notes, setNotes] = useState<Record<string, string>>({});

  useEffect(() => {
    const raw = window.localStorage.getItem("teams-tracking-agent-notes");
    if (raw) {
      try {
        setNotes(JSON.parse(raw) as Record<string, string>);
      } catch {
        setNotes({});
      }
    }
  }, []);

  const saveNote = (agentKey: string, value: string) => {
    setNotes((current) => {
      const next = { ...current, [agentKey]: value };
      window.localStorage.setItem("teams-tracking-agent-notes", JSON.stringify(next));
      return next;
    });
  };

  return { notes, saveNote };
}

function MetricCard({
  title,
  value,
  detail,
  icon: Icon,
  tone = "default",
}: {
  title: string;
  value: string | number;
  detail: string;
  icon: React.ElementType;
  tone?: "default" | "success" | "warning" | "danger" | "info";
}) {
  const toneClass = {
    default: "bg-slate-100 text-slate-700",
    success: "bg-emerald-50 text-emerald-700",
    warning: "bg-amber-50 text-amber-700",
    danger: "bg-red-50 text-red-700",
    info: "bg-sky-50 text-sky-700",
  }[tone];

  return (
    <Card className="min-h-[118px]">
      <CardContent className="flex h-full items-start justify-between gap-4 p-4">
        <div className="min-w-0">
          <p className="text-xs font-medium text-muted-foreground">{title}</p>
          <p className="mt-2 text-2xl font-semibold tracking-normal">{value}</p>
          <p className="mt-1 truncate text-xs text-muted-foreground">{detail}</p>
        </div>
        <div className={cn("rounded-md p-2", toneClass)}>
          <Icon className="h-4 w-4" />
        </div>
      </CardContent>
    </Card>
  );
}

function TableSkeleton() {
  return (
    <div className="space-y-2 p-4">
      {Array.from({ length: 7 }).map((_, index) => (
        <Skeleton key={index} className="h-12 w-full" />
      ))}
    </div>
  );
}

function EmptyAgents() {
  return (
    <div className="flex min-h-[280px] flex-col items-center justify-center rounded-md border border-dashed bg-muted/30 p-6 text-center">
      <UsersRound className="h-8 w-8 text-muted-foreground" />
      <p className="mt-3 text-sm font-medium">Nenhum agente encontrado</p>
      <p className="mt-1 max-w-sm text-sm text-muted-foreground">A listagem sera exibida assim que houver registros no backend.</p>
    </div>
  );
}

function getAgentNote(agent: Agent | null | undefined, notes: Record<string, string>) {
  if (!agent) {
    return "";
  }
  return notes[agent.id] ?? (agent.externalId ? notes[agent.externalId] : "") ?? "";
}

function MonitoringCard({
  title,
  icon: Icon,
  lastExecution,
  failures,
  retries,
  httpCodes,
  cursor,
}: {
  title: string;
  icon: React.ElementType;
  lastExecution?: string | null;
  failures?: number;
  retries?: number;
  httpCodes?: string;
  cursor?: string | null;
}) {
  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between space-y-0 pb-3">
        <CardTitle className="flex items-center gap-2 text-sm">
          <Icon className="h-4 w-4 text-muted-foreground" />
          {title}
        </CardTitle>
      </CardHeader>
      <CardContent className="grid gap-2 text-xs">
        <div className="flex justify-between gap-3">
          <span className="text-muted-foreground">Ultima execucao</span>
          <span className="text-right font-medium">{formatDateTime(lastExecution)}</span>
        </div>
        <div className="flex justify-between gap-3">
          <span className="text-muted-foreground">Proxima execucao</span>
          <span className="text-right font-medium">Nao exposta</span>
        </div>
        <div className="flex justify-between gap-3">
          <span className="text-muted-foreground">Falhas</span>
          <span className="font-medium">{failures ?? 0}</span>
        </div>
        <div className="flex justify-between gap-3">
          <span className="text-muted-foreground">Retries</span>
          <span className="font-medium">{retries ?? 0}</span>
        </div>
        <div className="flex justify-between gap-3">
          <span className="text-muted-foreground">HTTP</span>
          <span className="font-medium">{httpCodes ?? "429: 0 / 503: 0"}</span>
        </div>
        <div className="flex justify-between gap-3">
          <span className="text-muted-foreground">Cursor</span>
          <span className="max-w-[180px] truncate text-right font-medium">{cursor ?? "Sem cursor"}</span>
        </div>
      </CardContent>
    </Card>
  );
}

export function OperationalDashboard() {
  const queryClient = useQueryClient();
  const isDesktop = useDesktopBreakpoint();
  const { notes, saveNote } = useAgentNotes();
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<"ALL" | AgentStatus>("ALL");
  const [selectedAgentId, setSelectedAgentId] = useState<string | null>(null);
  const [detailsOpen, setDetailsOpen] = useState(false);
  const [agentDialogOpen, setAgentDialogOpen] = useState(false);
  const [agentDialogMode, setAgentDialogMode] = useState<"create" | "edit">("create");
  const [checkInDialogOpen, setCheckInDialogOpen] = useState(false);
  const [refreshing, setRefreshing] = useState(false);

  const today = todayIsoDate();
  const agentsQuery = useQuery({
    queryKey: queryKeys.agents,
    queryFn: api.listAgents,
  });

  const monitoringQuery = useQuery({
    queryKey: queryKeys.monitoring,
    queryFn: api.getMonitoring,
    refetchInterval: 15_000,
  });

  const agents = agentsQuery.data ?? [];

  const checkInQueries = useQueries({
    queries: agents.map((agent) => ({
      queryKey: queryKeys.checkIns(agent.id),
      queryFn: () => api.listCheckIns(agent.id),
      staleTime: 60_000,
    })),
  });

  const selectedFromList = agents.find((agent) => agent.id === selectedAgentId) ?? null;

  const selectedAgentQuery = useQuery({
    queryKey: selectedAgentId ? queryKeys.agent(selectedAgentId) : ["agent", "none"],
    queryFn: () => api.getAgent(selectedAgentId as string),
    enabled: Boolean(selectedAgentId),
    refetchInterval: 30_000,
  });

  const selectedCheckInsQuery = useQuery({
    queryKey: selectedAgentId ? queryKeys.checkIns(selectedAgentId) : ["agent", "none", "check-ins"],
    queryFn: () => api.listCheckIns(selectedAgentId as string),
    enabled: Boolean(selectedAgentId),
  });

  const selectedRouteQuery = useQuery({
    queryKey: selectedAgentId ? queryKeys.route(selectedAgentId, today) : ["agent", "none", "route", today],
    queryFn: () => api.getRoute(selectedAgentId as string, today),
    enabled: Boolean(selectedAgentId),
  });

  const selectedAgent = selectedAgentQuery.data ?? selectedFromList;

  const checkInsByAgent = useMemo(() => {
    const entries = new Map<string, CheckIn[]>();
    agents.forEach((agent, index) => {
      entries.set(agent.id, checkInQueries[index]?.data ?? []);
    });
    return entries;
  }, [agents, checkInQueries]);

  const allCheckIns = useMemo(
    () =>
      [...checkInsByAgent.values()]
        .flat()
        .sort((a, b) => new Date(b.occurredAt).getTime() - new Date(a.occurredAt).getTime()),
    [checkInsByAgent],
  );

  const filteredAgents = useMemo(() => {
    const normalizedSearch = search.trim().toLowerCase();
    return agents.filter((agent) => {
      const matchesSearch =
        !normalizedSearch ||
        agent.name.toLowerCase().includes(normalizedSearch) ||
        agent.id.toLowerCase().includes(normalizedSearch) ||
        (agent.externalId ?? "").toLowerCase().includes(normalizedSearch);
      const matchesStatus = statusFilter === "ALL" || agent.status === statusFilter;
      return matchesSearch && matchesStatus;
    });
  }, [agents, search, statusFilter]);

  const monitoring = monitoringQuery.data;
  const lastUpdated = monitoring?.generatedAt ?? new Date(Math.max(agentsQuery.dataUpdatedAt, monitoringQuery.dataUpdatedAt || 0)).toISOString();

  const metrics = {
    activeAgents: agents.filter((agent) => agent.active && agent.status !== "OFFLINE").length,
    delayedAgents: agents.filter(isAgentDelayed).length,
    syncFailures: monitoring?.totals.failures ?? 0,
    retries: monitoring?.totals.retryAttempts ?? 0,
    httpErrors: (monitoring?.totals.rateLimitErrors ?? 0) + (monitoring?.totals.serviceUnavailableErrors ?? 0),
    recentCheckIns: allCheckIns.filter((checkIn) => Date.now() - new Date(checkIn.occurredAt).getTime() < 24 * 60 * 60 * 1000).length,
  };

  const monitoringCards: Array<{
    title: string;
    icon: React.ElementType;
    lastExecution?: string | null;
    failures?: number;
    retries?: number;
    httpCodes?: string;
    cursor?: string | null;
  }> = [
    {
      title: "Scheduler",
      icon: Clock,
      lastExecution: monitoring?.schedulers[0]?.lastFinishedAt,
      failures: monitoring?.totals.failures,
      retries: monitoring?.totals.retryAttempts,
      httpCodes: `429: ${monitoring?.totals.rateLimitErrors ?? 0} / 503: ${monitoring?.totals.serviceUnavailableErrors ?? 0}`,
      cursor: monitoring?.schedulers[0]?.lastCursorValue,
    },
    {
      title: "Sincronizacao",
      icon: RefreshCcw,
      lastExecution: monitoring?.latestExecutions[0]?.finishedAt ?? monitoring?.latestExecutions[0]?.startedAt,
      failures: monitoring?.latestExecutions[0]?.recordsFailed,
      retries: monitoring?.latestExecutions[0]?.retryAttempts,
      httpCodes: `429: ${monitoring?.latestExecutions[0]?.rateLimitErrors ?? 0} / 503: ${monitoring?.latestExecutions[0]?.serviceUnavailableErrors ?? 0}`,
      cursor: monitoring?.latestExecutions[0]?.cursorValueAfter,
    },
    {
      title: "Fila/Retentativas",
      icon: Layers,
      lastExecution: monitoring?.generatedAt,
      failures: monitoring?.totals.recordsFailed,
      retries: metrics.retries,
      httpCodes: `429: ${monitoring?.totals.rateLimitErrors ?? 0} / 503: ${monitoring?.totals.serviceUnavailableErrors ?? 0}`,
      cursor: monitoring?.cursors[0]?.lastCursorValue,
    },
    {
      title: "Erros recentes",
      icon: ShieldAlert,
      lastExecution: monitoring?.recentFailures[0]?.createdAt,
      failures: monitoring?.recentFailures.length,
      retries: metrics.retries,
      httpCodes: `429: ${monitoring?.totals.rateLimitErrors ?? 0} / 503: ${monitoring?.totals.serviceUnavailableErrors ?? 0}`,
      cursor: monitoring?.recentFailures[0]?.syncType,
    },
    {
      title: "API externa",
      icon: Wifi,
      lastExecution: monitoring?.generatedAt,
      failures: metrics.httpErrors,
      retries: metrics.retries,
      httpCodes: `429: ${monitoring?.totals.rateLimitErrors ?? 0} / 503: ${monitoring?.totals.serviceUnavailableErrors ?? 0}`,
      cursor: monitoring?.cursors[0]?.lastCursorValue,
    },
    {
      title: "Cursor atual",
      icon: FileClock,
      lastExecution: monitoring?.cursors[0]?.updatedAt,
      failures: monitoring?.totals.failures,
      retries: metrics.retries,
      httpCodes: `429: ${monitoring?.totals.rateLimitErrors ?? 0} / 503: ${monitoring?.totals.serviceUnavailableErrors ?? 0}`,
      cursor: monitoring?.cursors[0]?.lastCursorValue,
    },
  ];

  const handleSelectAgent = (agentId: string) => {
    setSelectedAgentId(agentId);
    if (!isDesktop) {
      setDetailsOpen(true);
    }
  };

  const openCreateAgent = () => {
    setAgentDialogMode("create");
    setAgentDialogOpen(true);
  };

  const openEditAgent = (agent: Agent) => {
    setSelectedAgentId(agent.id);
    setAgentDialogMode("edit");
    setAgentDialogOpen(true);
  };

  const handleRefresh = async () => {
    setRefreshing(true);
    try {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.agents }),
        queryClient.invalidateQueries({ queryKey: queryKeys.monitoring }),
        selectedAgentId ? queryClient.invalidateQueries({ queryKey: queryKeys.agent(selectedAgentId) }) : Promise.resolve(),
        selectedAgentId ? queryClient.invalidateQueries({ queryKey: queryKeys.checkIns(selectedAgentId) }) : Promise.resolve(),
        selectedAgentId ? queryClient.invalidateQueries({ queryKey: queryKeys.route(selectedAgentId, today) }) : Promise.resolve(),
      ]);
      toast({
        title: "Dados atualizados",
        description: formatDateTime(new Date().toISOString()),
        variant: "success",
      });
    } finally {
      setRefreshing(false);
    }
  };

  return (
    <div className="min-h-screen bg-background">
      <aside className="fixed inset-y-0 left-0 z-40 flex w-20 flex-col border-r bg-card">
        <div className="flex h-16 items-center justify-center border-b">
          <Activity className="h-6 w-6 text-primary" />
        </div>
        <nav className="flex flex-1 flex-col items-center gap-2 py-4">
          {sidebarItems.map((item) => (
            <a
              key={item.id}
              href={`#${item.id}`}
              className="group flex w-full flex-col items-center gap-1 px-2 py-2 text-center text-[11px] font-medium text-muted-foreground transition-colors hover:text-primary"
              title={item.label}
            >
              <item.icon className="h-5 w-5" />
              <span className="leading-tight">{item.label}</span>
            </a>
          ))}
        </nav>
      </aside>

      <header className="fixed left-20 right-0 top-0 z-30 flex min-h-16 items-center justify-between gap-3 border-b bg-background/95 px-4 backdrop-blur">
        <div className="min-w-0">
          <h1 className="truncate text-base font-semibold">Operacao de agentes</h1>
          <p className="truncate text-xs text-muted-foreground">Gestao, rastreabilidade e sincronizacao</p>
        </div>
        <div className="flex flex-wrap items-center justify-end gap-2">
          <span className="hidden text-xs text-muted-foreground md:inline">Atualizado {formatDateTime(lastUpdated)}</span>
          <Button type="button" size="sm" variant="outline" onClick={handleRefresh} disabled={refreshing}>
            {refreshing ? <Loader2 className="animate-spin" /> : <RefreshCcw />}
            Atualizar
          </Button>
        </div>
      </header>

      <main className="ml-20 pt-16">
        <div className="space-y-6 p-4 lg:p-6">
          {(agentsQuery.isError || monitoringQuery.isError) && (
            <Alert variant="destructive">
              <AlertTriangle className="h-4 w-4" />
              <AlertTitle>Falha ao buscar dados operacionais</AlertTitle>
              <AlertDescription>
                {agentsQuery.error instanceof Error
                  ? agentsQuery.error.message
                  : monitoringQuery.error instanceof Error
                    ? monitoringQuery.error.message
                    : "Verifique se o backend esta disponivel."}
              </AlertDescription>
            </Alert>
          )}

          <section id="overview" className="grid gap-3 sm:grid-cols-2 xl:grid-cols-6">
            <MetricCard title="Agentes ativos" value={metrics.activeAgents} detail={`${agents.length} cadastrados`} icon={UsersRound} tone="success" />
            <MetricCard title="Sem posicao recente" value={metrics.delayedAgents} detail="> 30 min, offline ou sem dado" icon={MapPin} tone="warning" />
            <MetricCard title="Falhas de sync" value={metrics.syncFailures} detail="historico persistido" icon={ShieldAlert} tone="danger" />
            <MetricCard title="Retries pendentes" value={metrics.retries} detail="tentativas registradas" icon={RefreshCcw} tone="info" />
            <MetricCard title="Erros 429/503" value={metrics.httpErrors} detail="rate limit e indisponibilidade" icon={AlertTriangle} tone="warning" />
            <MetricCard title="Ultimos check-ins" value={metrics.recentCheckIns} detail="registrados nas ultimas 24h" icon={MapPinned} tone="default" />
          </section>

          <section id="agents" className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_430px]">
            <Card className="min-w-0">
              <CardHeader className="gap-3 md:flex-row md:items-center md:justify-between">
                <div>
                  <CardTitle>Agentes</CardTitle>
                  <p className="mt-1 text-sm text-muted-foreground">Tabela operacional com filtros e acoes rapidas.</p>
                </div>
                <div className="flex flex-col gap-2 sm:flex-row">
                  <div className="relative">
                    <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                    <Input
                      value={search}
                      onChange={(event) => setSearch(event.target.value)}
                      placeholder="Buscar agente"
                      className="pl-9 sm:w-64"
                    />
                  </div>
                  <Select value={statusFilter} onValueChange={(value) => setStatusFilter(value as "ALL" | AgentStatus)}>
                    <SelectTrigger className="sm:w-44">
                      <SelectValue placeholder="Status" />
                    </SelectTrigger>
                    <SelectContent>
                      {statusFilterOptions.map((option) => (
                        <SelectItem key={option.value} value={option.value}>
                          {option.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <Button type="button" onClick={openCreateAgent}>
                    <Plus />
                    Novo
                  </Button>
                </div>
              </CardHeader>
              <CardContent className="p-0">
                {agentsQuery.isLoading ? (
                  <TableSkeleton />
                ) : !filteredAgents.length ? (
                  <div className="p-4">
                    <EmptyAgents />
                  </div>
                ) : (
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Nome</TableHead>
                        <TableHead>Status</TableHead>
                        <TableHead>Ultima posicao</TableHead>
                        <TableHead>Ultimo check-in</TableHead>
                        <TableHead>Origem</TableHead>
                        <TableHead>Atualizacao</TableHead>
                        <TableHead className="text-right">Acoes</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {filteredAgents.map((agent) => {
                        const agentCheckIns = checkInsByAgent.get(agent.id);
                        const latestCheckIn = lastCheckIn(agentCheckIns);
                        const delayed = isAgentDelayed(agent);
                        return (
                          <TableRow key={agent.id} data-state={selectedAgentId === agent.id ? "selected" : undefined}>
                            <TableCell>
                              <div className="font-medium">{agent.name}</div>
                              <div className="text-xs text-muted-foreground">{agent.externalId ?? agent.id}</div>
                            </TableCell>
                            <TableCell>
                              <div className="flex flex-col gap-1">
                                <AgentStatusBadge status={agent.status} />
                                {delayed ? <span className="text-xs text-amber-700">atrasado ou sem dado</span> : null}
                              </div>
                            </TableCell>
                            <TableCell className="min-w-44">
                              <div className="text-sm">{agent.currentAddress ?? "Sem endereco"}</div>
                              <div className="text-xs text-muted-foreground">
                                {formatCoordinate(agent.currentLatitude)}, {formatCoordinate(agent.currentLongitude)}
                              </div>
                            </TableCell>
                            <TableCell>{latestCheckIn ? formatDateTime(latestCheckIn.occurredAt) : "Sem dado"}</TableCell>
                            <TableCell>
                              <Badge variant={latestCheckIn?.source === "MANUAL" ? "warning" : "secondary"}>
                                {latestCheckIn?.source ?? "GPS_SYNC"}
                              </Badge>
                            </TableCell>
                            <TableCell>{relativeTime(agent.lastSeen ?? agent.updatedAt)}</TableCell>
                            <TableCell>
                              <div className="flex justify-end gap-1">
                                <Button type="button" variant="ghost" size="icon" title="Selecionar agente" onClick={() => handleSelectAgent(agent.id)}>
                                  <Eye />
                                  <span className="sr-only">Selecionar</span>
                                </Button>
                                <Button type="button" variant="ghost" size="icon" title="Editar agente" onClick={() => openEditAgent(agent)}>
                                  <Pencil />
                                  <span className="sr-only">Editar</span>
                                </Button>
                                <Button
                                  type="button"
                                  variant="ghost"
                                  size="icon"
                                  title="Registrar check-in"
                                  onClick={() => {
                                    setSelectedAgentId(agent.id);
                                    setCheckInDialogOpen(true);
                                  }}
                                >
                                  <MapPin />
                                  <span className="sr-only">Check-in</span>
                                </Button>
                              </div>
                            </TableCell>
                          </TableRow>
                        );
                      })}
                    </TableBody>
                  </Table>
                )}
              </CardContent>
            </Card>

            <Card id="details" className="hidden min-w-0 xl:block">
              <CardHeader>
                <CardTitle>Detalhes</CardTitle>
              </CardHeader>
              <CardContent>
                <AgentDetailContent
                  agent={selectedAgent}
                  loading={selectedAgentQuery.isLoading}
                  checkIns={selectedCheckInsQuery.data}
                  checkInsLoading={selectedCheckInsQuery.isLoading}
                  route={selectedRouteQuery.data}
                  routeLoading={selectedRouteQuery.isLoading}
                  notes={getAgentNote(selectedAgent, notes)}
                  onManualCheckIn={() => setCheckInDialogOpen(true)}
                />
              </CardContent>
            </Card>
          </section>

          <section id="routes" className="grid gap-3 md:grid-cols-3">
            <Card>
              <CardHeader>
                <CardTitle>Rota do dia</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-2xl font-semibold">{formatMeters(selectedRouteQuery.data?.totalDistanceMeters)}</p>
                <p className="mt-1 text-sm text-muted-foreground">{selectedRouteQuery.data?.points.length ?? 0} pontos registrados</p>
              </CardContent>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle>Primeiro evento</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-sm font-medium">{formatDateTime(selectedRouteQuery.data?.points[0]?.timestamp)}</p>
                <p className="mt-1 text-sm text-muted-foreground">{selectedRouteQuery.data?.points[0]?.source ?? "Sem origem"}</p>
              </CardContent>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle>Ultimo evento</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-sm font-medium">
                  {formatDateTime(selectedRouteQuery.data?.points[selectedRouteQuery.data.points.length - 1]?.timestamp)}
                </p>
                <p className="mt-1 text-sm text-muted-foreground">
                  {selectedRouteQuery.data?.points[selectedRouteQuery.data.points.length - 1]?.source ?? "Sem origem"}
                </p>
              </CardContent>
            </Card>
          </section>

          <section id="monitoring" className="space-y-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <h2 className="text-base font-semibold">Monitoramento tecnico</h2>
                <p className="text-sm text-muted-foreground">Schedulers, sincronizacao, retries, erros externos e cursor incremental.</p>
              </div>
              <Badge variant="outline">Gerado {formatDateTime(monitoring?.generatedAt)}</Badge>
            </div>
            {monitoringQuery.isLoading ? (
              <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
                {Array.from({ length: 6 }).map((_, index) => (
                  <Skeleton key={index} className="h-48 w-full" />
                ))}
              </div>
            ) : (
              <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
                {monitoringCards.map((card) => (
                  <MonitoringCard key={card.title} {...card} />
                ))}
              </div>
            )}
          </section>
        </div>
      </main>

      <Sheet open={!isDesktop && detailsOpen && Boolean(selectedAgent)} onOpenChange={setDetailsOpen}>
        <SheetContent className="overflow-y-auto dashboard-scrollbar">
          <SheetHeader>
            <SheetTitle>Detalhes do agente</SheetTitle>
            <SheetDescription>{selectedAgent?.name ?? "Agente selecionado"}</SheetDescription>
          </SheetHeader>
          <div className="mt-4">
            <AgentDetailContent
              agent={selectedAgent}
              loading={selectedAgentQuery.isLoading}
              checkIns={selectedCheckInsQuery.data}
              checkInsLoading={selectedCheckInsQuery.isLoading}
              route={selectedRouteQuery.data}
              routeLoading={selectedRouteQuery.isLoading}
              notes={getAgentNote(selectedAgent, notes)}
              onManualCheckIn={() => setCheckInDialogOpen(true)}
            />
          </div>
        </SheetContent>
      </Sheet>

      <AgentFormDialog
        open={agentDialogOpen}
        onOpenChange={setAgentDialogOpen}
        agent={agentDialogMode === "edit" ? selectedAgent : null}
        notes={agentDialogMode === "edit" ? getAgentNote(selectedAgent, notes) : ""}
        onNotesChange={saveNote}
      />

      <CheckInDialog
        open={checkInDialogOpen}
        onOpenChange={setCheckInDialogOpen}
        agents={agents}
        selectedAgentId={selectedAgentId}
      />
    </div>
  );
}
