"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { MapPinCheck } from "lucide-react";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import type { Agent } from "@/lib/api";
import { api, queryKeys } from "@/lib/api";
import { manualCheckInSchema, type ManualCheckInFormValues } from "@/lib/forms";
import { toDateTimeLocalValue, todayIsoDate } from "@/lib/format";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { toast } from "@/components/ui/use-toast";

type CheckInDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  agents: Agent[];
  selectedAgentId?: string | null;
};

export function CheckInDialog({ open, onOpenChange, agents, selectedAgentId }: CheckInDialogProps) {
  const queryClient = useQueryClient();
  const form = useForm<ManualCheckInFormValues>({
    resolver: zodResolver(manualCheckInSchema),
    defaultValues: {
      agentId: "",
      latitude: 0,
      longitude: 0,
      notes: "",
      origin: "MANUAL",
      occurredAt: toDateTimeLocalValue(),
    },
  });

  useEffect(() => {
    if (!open) {
      return;
    }
    const agent = agents.find((item) => item.id === selectedAgentId);
    form.reset({
      agentId: selectedAgentId ?? "",
      latitude: Number(agent?.currentLatitude ?? 0),
      longitude: Number(agent?.currentLongitude ?? 0),
      notes: "",
      origin: "MANUAL",
      occurredAt: toDateTimeLocalValue(),
    });
  }, [agents, form, open, selectedAgentId]);

  const mutation = useMutation({
    mutationFn: async (values: ManualCheckInFormValues) => {
      const occurredAt = new Date(values.occurredAt);
      const isoOccurredAt = Number.isNaN(occurredAt.getTime()) ? new Date().toISOString() : occurredAt.toISOString();
      const idempotencyKey = `manual-${values.agentId}-${isoOccurredAt}-${values.latitude}-${values.longitude}`;

      return api.createCheckIn(values.agentId, {
        type: "CHECKIN",
        latitude: values.latitude,
        longitude: values.longitude,
        notes: values.notes || null,
        occurredAt: isoOccurredAt,
        idempotencyKey: idempotencyKey.slice(0, 100),
      });
    },
    onSuccess: async (checkIn, values) => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.checkIns(values.agentId) });
      await queryClient.invalidateQueries({ queryKey: queryKeys.route(values.agentId, todayIsoDate()) });
      await queryClient.invalidateQueries({ queryKey: queryKeys.agent(values.agentId) });
      await queryClient.invalidateQueries({ queryKey: queryKeys.agents });
      toast({
        title: "Check-in registrado",
        description: checkIn.occurredAt,
        variant: "success",
      });
      onOpenChange(false);
    },
    onError: (error) => {
      toast({
        title: "Falha ao registrar check-in",
        description: error instanceof Error ? error.message : "Erro inesperado.",
        variant: "destructive",
      });
    },
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[92vh] overflow-y-auto dashboard-scrollbar sm:max-w-xl">
        <DialogHeader>
          <DialogTitle>Registrar check-in manual</DialogTitle>
          <DialogDescription>Evento manual associado ao agente selecionado.</DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit((values) => mutation.mutate(values))} className="grid gap-4">
            <FormField
              control={form.control}
              name="agentId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Agente</FormLabel>
                  <Select onValueChange={field.onChange} value={field.value}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Selecione o agente" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {agents.map((agent) => (
                        <SelectItem key={agent.id} value={agent.id}>
                          {agent.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="grid gap-4 sm:grid-cols-2">
              <FormField
                control={form.control}
                name="latitude"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Latitude</FormLabel>
                    <FormControl>
                      <Input type="number" step="0.000001" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="longitude"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Longitude</FormLabel>
                    <FormControl>
                      <Input type="number" step="0.000001" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <FormField
                control={form.control}
                name="origin"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Origem</FormLabel>
                    <Select onValueChange={field.onChange} value={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Origem" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value="MANUAL">Manual</SelectItem>
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="occurredAt"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Horario</FormLabel>
                    <FormControl>
                      <Input type="datetime-local" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <FormField
              control={form.control}
              name="notes"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Observacao</FormLabel>
                  <FormControl>
                    <Textarea placeholder="Observacao do check-in" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                Cancelar
              </Button>
              <Button type="submit" disabled={mutation.isPending}>
                <MapPinCheck />
                {mutation.isPending ? "Registrando" : "Registrar"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
