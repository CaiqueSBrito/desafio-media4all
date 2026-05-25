"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Save } from "lucide-react";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import type { Agent, AgentPayload, AgentStatus } from "@/lib/api";
import { api, queryKeys } from "@/lib/api";
import { agentFormSchema, type AgentFormValues } from "@/lib/forms";
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

const statusOptions: Array<{ value: AgentStatus; label: string }> = [
  { value: "ONLINE", label: "Online" },
  { value: "PAUSED", label: "Pausado" },
  { value: "SIGNAL_LOST", label: "Sinal perdido" },
  { value: "OFFLINE", label: "Offline" },
];

type AgentFormDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  agent?: Agent | null;
  notes?: string;
  onNotesChange: (agentKey: string, notes: string) => void;
};

export function AgentFormDialog({ open, onOpenChange, agent, notes = "", onNotesChange }: AgentFormDialogProps) {
  const queryClient = useQueryClient();
  const isEditing = Boolean(agent);

  const form = useForm<AgentFormValues>({
    resolver: zodResolver(agentFormSchema),
    defaultValues: {
      name: "",
      externalId: "",
      phone: "",
      status: "ONLINE",
      notes: "",
    },
  });

  useEffect(() => {
    if (!open) {
      return;
    }
    form.reset({
      name: agent?.name ?? "",
      externalId: agent?.externalId ?? "",
      phone: agent?.phone ?? "",
      status: agent?.status ?? "ONLINE",
      notes,
    });
  }, [agent, form, notes, open]);

  const mutation = useMutation({
    mutationFn: async (values: AgentFormValues) => {
      const payload: AgentPayload = {
        name: values.name,
        phone: values.phone || null,
        status: values.status,
        active: values.status !== "OFFLINE",
      };

      if (isEditing && agent) {
        return api.updateAgent(agent.id, {
          ...payload,
          role: agent.role ?? "TECHNICIAN",
        });
      }

      return api.createAgent({
        ...payload,
        externalId: values.externalId,
        role: "TECHNICIAN",
      });
    },
    onSuccess: async (savedAgent, values) => {
      onNotesChange(savedAgent.id || values.externalId, values.notes ?? "");
      await queryClient.invalidateQueries({ queryKey: queryKeys.agents });
      await queryClient.invalidateQueries({ queryKey: queryKeys.agent(savedAgent.id) });
      toast({
        title: isEditing ? "Agente atualizado" : "Agente criado",
        description: savedAgent.name,
        variant: "success",
      });
      onOpenChange(false);
    },
    onError: (error) => {
      toast({
        title: "Falha ao salvar agente",
        description: error instanceof Error ? error.message : "Erro inesperado.",
        variant: "destructive",
      });
    },
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[92vh] overflow-y-auto dashboard-scrollbar sm:max-w-xl">
        <DialogHeader>
          <DialogTitle>{isEditing ? "Editar agente" : "Criar agente"}</DialogTitle>
          <DialogDescription>Dados operacionais usados na gestao da equipe externa.</DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit((values) => mutation.mutate(values))} className="grid gap-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <FormField
                control={form.control}
                name="name"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Nome</FormLabel>
                    <FormControl>
                      <Input placeholder="Nome do agente" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="externalId"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Identificador</FormLabel>
                    <FormControl>
                      <Input placeholder="ID externo" disabled={isEditing} {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <FormField
                control={form.control}
                name="phone"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Telefone</FormLabel>
                    <FormControl>
                      <Input placeholder="(00) 00000-0000" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="status"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Status</FormLabel>
                    <Select onValueChange={field.onChange} value={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Selecione" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {statusOptions.map((option) => (
                          <SelectItem key={option.value} value={option.value}>
                            {option.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
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
                  <FormLabel>Observacoes</FormLabel>
                  <FormControl>
                    <Textarea placeholder="Observacoes operacionais" {...field} />
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
                <Save />
                {mutation.isPending ? "Salvando" : "Salvar"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
