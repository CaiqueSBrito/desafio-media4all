import { z } from "zod";

export const agentFormSchema = z.object({
  name: z.string().trim().min(2, "Informe ao menos 2 caracteres.").max(150, "Use ate 150 caracteres."),
  externalId: z.string().trim().min(1, "Informe o identificador.").max(100, "Use ate 100 caracteres."),
  phone: z.string().trim().max(30, "Use ate 30 caracteres.").optional().or(z.literal("")),
  status: z.enum(["ONLINE", "PAUSED", "SIGNAL_LOST", "OFFLINE"]),
  notes: z.string().trim().max(1000, "Use ate 1000 caracteres.").optional().or(z.literal("")),
});

export type AgentFormValues = z.infer<typeof agentFormSchema>;

export const manualCheckInSchema = z.object({
  agentId: z.string().trim().min(1, "Selecione um agente."),
  latitude: z.coerce.number().min(-90, "Latitude minima: -90.").max(90, "Latitude maxima: 90."),
  longitude: z.coerce.number().min(-180, "Longitude minima: -180.").max(180, "Longitude maxima: 180."),
  notes: z.string().trim().max(1000, "Use ate 1000 caracteres.").optional().or(z.literal("")),
  origin: z.enum(["MANUAL"]),
  occurredAt: z.string().trim().min(1, "Informe o horario."),
});

export type ManualCheckInFormValues = z.infer<typeof manualCheckInSchema>;
