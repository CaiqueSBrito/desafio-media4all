"use client";

import { Download, ExternalLink, History, MapPin, MapPinned, Route, UserRound } from "lucide-react";
import { useMemo, useState } from "react";
import type { Agent, CheckIn, RouteHistory } from "@/lib/api";
import {
  formatCoordinate,
  formatDate,
  formatDateTime,
  formatMeters,
  lastCheckIn,
  relativeTime,
  todayIsoDate,
} from "@/lib/format";
import { AgentStatusBadge } from "@/components/dashboard/status-badge";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

type AgentDetailContentProps = {
  agent?: Agent | null;
  checkIns?: CheckIn[];
  route?: RouteHistory;
  notes?: string;
  loading?: boolean;
  checkInsLoading?: boolean;
  routeLoading?: boolean;
  onManualCheckIn: () => void;
};

function DataRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-4 border-b border-border/70 py-2 last:border-b-0">
      <span className="text-xs text-muted-foreground">{label}</span>
      <span className="text-right text-sm font-medium">{value}</span>
    </div>
  );
}

function EmptyLine({ children }: { children: React.ReactNode }) {
  return <div className="rounded-md border border-dashed bg-muted/30 px-3 py-4 text-sm text-muted-foreground">{children}</div>;
}

export function AgentDetailContent({
  agent,
  checkIns,
  route,
  notes,
  loading,
  checkInsLoading,
  routeLoading,
  onManualCheckIn,
}: AgentDetailContentProps) {
  const [historyDate, setHistoryDate] = useState(todayIsoDate());
  const orderedCheckIns = useMemo(
    () => [...(checkIns ?? [])].sort((a, b) => new Date(b.occurredAt).getTime() - new Date(a.occurredAt).getTime()),
    [checkIns],
  );
  const latestCheckIn = lastCheckIn(checkIns);
  const orderedRoutePoints = useMemo(
    () => [...(route?.points ?? [])].sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()),
    [route?.points],
  );
  const firstRoutePoint = orderedRoutePoints[0];
  const lastRoutePoint = orderedRoutePoints[orderedRoutePoints.length - 1];

  function openMap() {
    if (!agent?.currentLatitude || !agent.currentLongitude) {
      return;
    }
    const url = `https://www.google.com/maps?q=${agent.currentLatitude},${agent.currentLongitude}`;
    window.open(url, "_blank", "noopener,noreferrer");
  }

  function exportCsv() {
    const rows = [
      ["tipo", "origem", "data_hora", "latitude", "longitude", "observacao"],
      ...orderedCheckIns
        .filter((item) => item.occurredAt.startsWith(historyDate))
        .map((item) => [
          item.type,
          item.source,
          item.occurredAt,
          String(item.latitude ?? ""),
          String(item.longitude ?? ""),
          item.notes ?? "",
        ]),
    ];
    const csv = rows.map((row) => row.map((cell) => `"${String(cell).replaceAll('"', '""')}"`).join(",")).join("\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `historico-${agent?.externalId ?? agent?.id ?? "agente"}-${historyDate}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  }

  if (!agent && !loading) {
    return <EmptyLine>Selecione um agente para visualizar detalhes operacionais.</EmptyLine>;
  }

  if (loading || !agent) {
    return (
      <div className="space-y-3">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-40 w-full" />
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-base font-semibold leading-tight">{agent.name}</h2>
          <p className="mt-1 text-sm text-muted-foreground">{agent.externalId ?? agent.id}</p>
        </div>
        <AgentStatusBadge status={agent.status} />
      </div>

      <Tabs defaultValue="summary" className="w-full">
        <TabsList className="grid h-auto w-full grid-cols-2 gap-1 md:grid-cols-5">
          <TabsTrigger value="summary">
            <UserRound />
            Resumo
          </TabsTrigger>
          <TabsTrigger value="position">
            <MapPin />
            Posicao
          </TabsTrigger>
          <TabsTrigger value="checkins">
            <MapPinned />
            Check-ins
          </TabsTrigger>
          <TabsTrigger value="route">
            <Route />
            Rota
          </TabsTrigger>
          <TabsTrigger value="history">
            <History />
            Historico
          </TabsTrigger>
        </TabsList>

        <TabsContent value="summary" className="space-y-3">
          <div className="rounded-lg border bg-background px-3">
            <DataRow label="Nome" value={agent.name} />
            <DataRow label="Identificador" value={agent.externalId ?? "Sem dado"} />
            <DataRow label="Telefone" value={agent.phone ?? "Sem dado"} />
            <DataRow label="Equipe" value={agent.team ?? "Sem dado"} />
            <DataRow label="Status atual" value={<AgentStatusBadge status={agent.status} />} />
            <DataRow label="Ultima atualizacao" value={formatDateTime(agent.updatedAt)} />
            <DataRow label="Ultimo check-in" value={latestCheckIn ? formatDateTime(latestCheckIn.occurredAt) : "Sem dado"} />
            <DataRow label="Observacoes" value={notes || "Sem dado"} />
          </div>
        </TabsContent>

        <TabsContent value="position" className="space-y-3">
          <div className="grid gap-3 sm:grid-cols-2">
            <div className="rounded-lg border bg-background px-3">
              <DataRow label="Latitude" value={formatCoordinate(agent.currentLatitude)} />
              <DataRow label="Longitude" value={formatCoordinate(agent.currentLongitude)} />
              <DataRow label="Origem" value="GPS_SYNC" />
              <DataRow label="Horario do evento" value={formatDateTime(agent.lastSeen)} />
              <DataRow label="Tempo desde atualizacao" value={relativeTime(agent.lastSeen)} />
              <DataRow label="Status operacional" value={<AgentStatusBadge status={agent.status} />} />
            </div>
            <div className="min-h-[220px] rounded-lg border bg-[linear-gradient(135deg,#f8fafc_25%,#eef6f6_25%,#eef6f6_50%,#f8fafc_50%,#f8fafc_75%,#eef6f6_75%)] bg-[length:24px_24px] p-3">
              <div className="flex h-full min-h-[194px] flex-col justify-between rounded-md border bg-white/90 p-3">
                <div>
                  <p className="text-xs font-medium text-muted-foreground">Mini mapa</p>
                  <p className="mt-2 text-sm font-semibold">{agent.currentAddress ?? "Localizacao sem endereco"}</p>
                  <p className="mt-1 text-xs text-muted-foreground">
                    {formatCoordinate(agent.currentLatitude)}, {formatCoordinate(agent.currentLongitude)}
                  </p>
                </div>
                <Button type="button" variant="outline" size="sm" onClick={openMap} disabled={!agent.currentLatitude || !agent.currentLongitude}>
                  <ExternalLink />
                  Abrir mapa
                </Button>
              </div>
            </div>
          </div>
        </TabsContent>

        <TabsContent value="checkins" className="space-y-3">
          <div className="flex justify-end">
            <Button type="button" size="sm" onClick={onManualCheckIn}>
              <MapPin />
              Registrar check-in
            </Button>
          </div>
          {checkInsLoading ? (
            <div className="space-y-2">
              <Skeleton className="h-12 w-full" />
              <Skeleton className="h-12 w-full" />
              <Skeleton className="h-12 w-full" />
            </div>
          ) : orderedCheckIns.length ? (
            <div className="space-y-2">
              {orderedCheckIns.map((checkIn) => (
                <div key={checkIn.id} className="rounded-lg border bg-background p-3">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div className="text-sm font-medium">{formatDateTime(checkIn.occurredAt)}</div>
                    <Badge variant={checkIn.source === "MANUAL" ? "warning" : "secondary"}>{checkIn.source}</Badge>
                  </div>
                  <div className="mt-2 grid gap-1 text-xs text-muted-foreground sm:grid-cols-2">
                    <span>{checkIn.notes || "Sem observacao"}</span>
                    <span className="sm:text-right">Responsavel: {checkIn.source === "MANUAL" ? "Operador" : "Integracao"}</span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <EmptyLine>Nenhum check-in encontrado para este agente.</EmptyLine>
          )}
        </TabsContent>

        <TabsContent value="route" className="space-y-3">
          <div className="grid gap-2 sm:grid-cols-4">
            <div className="rounded-lg border bg-background p-3">
              <p className="text-xs text-muted-foreground">Distancia total</p>
              <p className="mt-1 text-sm font-semibold">{formatMeters(route?.totalDistanceMeters)}</p>
            </div>
            <div className="rounded-lg border bg-background p-3">
              <p className="text-xs text-muted-foreground">Pontos</p>
              <p className="mt-1 text-sm font-semibold">{orderedRoutePoints.length}</p>
            </div>
            <div className="rounded-lg border bg-background p-3">
              <p className="text-xs text-muted-foreground">Primeiro evento</p>
              <p className="mt-1 text-sm font-semibold">{formatDateTime(firstRoutePoint?.timestamp)}</p>
            </div>
            <div className="rounded-lg border bg-background p-3">
              <p className="text-xs text-muted-foreground">Ultimo evento</p>
              <p className="mt-1 text-sm font-semibold">{formatDateTime(lastRoutePoint?.timestamp)}</p>
            </div>
          </div>
          {routeLoading ? (
            <Skeleton className="h-48 w-full" />
          ) : orderedRoutePoints.length ? (
            <div className="space-y-2">
              {orderedRoutePoints.map((point, index) => (
                <div key={`${point.timestamp}-${index}`} className="grid grid-cols-[24px_1fr] gap-3 rounded-lg border bg-background p-3">
                  <div className="flex flex-col items-center">
                    <span className="mt-1 h-2.5 w-2.5 rounded-full bg-primary" />
                    <span className="mt-1 h-full w-px bg-border" />
                  </div>
                  <div>
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <span className="text-sm font-medium">{formatDateTime(point.timestamp)}</span>
                      <Badge variant={point.source === "MANUAL" ? "warning" : "secondary"}>{point.source}</Badge>
                    </div>
                    <p className="mt-1 text-xs text-muted-foreground">
                      {formatCoordinate(point.latitude)}, {formatCoordinate(point.longitude)}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <EmptyLine>Nenhum ponto de rota encontrado para hoje.</EmptyLine>
          )}
        </TabsContent>

        <TabsContent value="history" className="space-y-3">
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <Input type="date" value={historyDate} onChange={(event) => setHistoryDate(event.target.value)} className="sm:w-48" />
            <Button type="button" variant="outline" size="sm" onClick={exportCsv} disabled={!orderedCheckIns.length}>
              <Download />
              Exportar CSV
            </Button>
          </div>
          <Alert>
            <AlertDescription>
              Eventos em {formatDate(historyDate)}:{" "}
              {orderedCheckIns.filter((item) => item.occurredAt.startsWith(historyDate)).length}
            </AlertDescription>
          </Alert>
          <div className="space-y-2">
            {orderedCheckIns
              .filter((item) => item.occurredAt.startsWith(historyDate))
              .map((item) => (
                <div key={item.id} className="rounded-lg border bg-background p-3 text-sm">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <span className="font-medium">{item.type}</span>
                    <span className="text-xs text-muted-foreground">{formatDateTime(item.occurredAt)}</span>
                  </div>
                  <p className="mt-1 text-xs text-muted-foreground">{item.notes || item.address || "Sem detalhe adicional"}</p>
                </div>
              ))}
          </div>
        </TabsContent>
      </Tabs>
    </div>
  );
}
