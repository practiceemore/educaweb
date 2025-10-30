import { Request } from 'express';
import { User } from '@prisma/client';

// Extensão do Request para incluir usuário autenticado, preservando body/params/query/header
export type AuthRequest<P = any, ResBody = any, ReqBody = any, ReqQuery = any> =
  Request<P, ResBody, ReqBody, ReqQuery> & { user?: User };

// Tipos para autenticação
export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  name: string;
  role?: string;
}

export interface AuthResponse {
  token: string;
  user: {
    id: number;
    email: string;
    name: string;
    role: string;
  };
}

// Tipos para disciplinas
export interface CreateDisciplinaRequest {
  nome: string;
  descricao?: string;
  userId: number;
}

export interface UpdateDisciplinaRequest {
  nome?: string;
  descricao?: string;
}

// Tipos para professores
export interface CreateProfessorRequest {
  nome: string;
  email?: string;
  telefone?: string;
  especialidade: string;
  aulasContratadas?: number;
  salario?: number;
  dataAdmissao?: string;
  disciplinas?: number[];
  userId: number;
}

export interface UpdateProfessorRequest {
  nome?: string;
  email?: string;
  telefone?: string;
  especialidade?: string;
  aulasContratadas?: number;
  salario?: number;
  dataAdmissao?: string;
  ativo?: boolean;
  disciplinas?: number[];
}

// Tipos para turmas
export interface CreateTurmaRequest {
  nome: string;
  serie: string;
  turno: string;
  capacidade: number;
  alunosMatriculados?: number;
  anoLetivo: string;
  userId: number;
}

export interface UpdateTurmaRequest {
  nome?: string;
  serie?: string;
  alunosMatriculados?: number;
  anoLetivo?: string;
  ativa?: boolean;
  turno?: string;
  capacidade?: number;
}

// Tipos para salas
export interface CreateSalaRequest {
  nome: string;
  tipo: string;
  capacidade: number;
  recursos?: string[];
  status?: string;
  userId: number;
}

export interface UpdateSalaRequest {
  nome?: string;
  tipo?: string;
  capacidade?: number;
  recursos?: string[];
}

// Tipos para turma-disciplina
export interface CreateTurmaDisciplinaRequest {
  turmaId: number;
  disciplinaId: number;
  aulasPorSemana: number;
  userId: number;
}

export interface UpdateTurmaDisciplinaRequest {
  aulasPorSemana?: number;
}

export interface TurmaDisciplinaResponse {
  id: number;
  turmaId: number;
  disciplinaId: number;
  aulasPorSemana: number;
  disciplina: {
    id: number;
    nome: string;
    descricao?: string;
  };
  turma: {
    id: number;
    nome: string;
    serie: string;
  };
  createdAt: string;
  updatedAt: string;
}

// Tipos para grade horária
export interface CreateGradeHorariaRequest {
  turmaId: number;
  disciplinaId: number;
  professorId: number;
  salaId: number;
  diaSemana: string;
  horarioInicio: string;
  horarioFim: string;
  userId: number;
}

export interface UpdateGradeHorariaRequest {
  disciplinaId?: number;
  professorId?: number;
  salaId?: number;
  diaSemana?: string;
  horarioInicio?: string;
  horarioFim?: string;
  ativa?: boolean;
}

export interface GradeHorariaResponse {
  id: number;
  turmaId: number;
  disciplinaId: number;
  professorId: number;
  salaId: number;
  diaSemana: string;
  horarioInicio: string;
  horarioFim: string;
  ativa: boolean;
  turma: {
    id: number;
    nome: string;
    serie: string;
  };
  disciplina: {
    id: number;
    nome: string;
  };
  professor: {
    id: number;
    nome: string;
    especialidade: string;
  };
  sala: {
    id: number;
    nome: string;
    tipo: string;
  };
  createdAt: string;
  updatedAt: string;
}

// Tipos para geração automática de grade pela IA
export interface GradeGenerationRequest {
  turmaId?: number; // Se não especificado, gera para todas as turmas
  horariosDisponiveis?: {
    inicio: string;
    fim: string;
  }[];
  diasSemana?: string[];
}

export interface GradeGenerationResponse {
  success: boolean;
  data: {
    turmaId: number;
    turmaNome: string;
    grade: Array<{
      diaSemana: string;
      horarioInicio: string;
      horarioFim: string;
      disciplina: string;
      professor: string;
      sala: string;
    }>;
  }[];
  conflitos?: Array<{
    tipo: 'professor' | 'sala';
    professorId?: number;
    salaId?: number;
    diaSemana: string;
    horario: string;
    turmas: string[];
  }>;
}

// Tipos para aulas
export interface CreateAulaRequest {
  disciplinaId: number;
  professorId: number;
  turmaId: number;
  salaId: number;
  diaSemana: string;
  horarioInicio: string;
  horarioFim: string;
  userId: number;
}

export interface UpdateAulaRequest {
  disciplinaId?: number;
  professorId?: number;
  turmaId?: number;
  salaId?: number;
  diaSemana?: string;
  horarioInicio?: string;
  horarioFim?: string;
}

// Tipos para chat
export interface CreateMensagemRequest {
  conteudo: string;
  userId: number;
}

// Tipos para grade
export interface GradeHorariaRequest {
  nome: string;
  horariosInicio: string;
  horariosFim: string;
  diasSemana: string;
  configuracoes: string;
}

export interface CreateGradeConfigRequest {
  nome: string;
  configuracao: any;
  userId: number;
}

export interface UpdateGradeConfigRequest {
  nome?: string;
  configuracao?: any;
}

// Tipos para chat
export interface ChatMessageRequest {
  mensagem: string;
  metadata?: any;
}

export interface ChatMessageResponse {
  id: number;
  mensagem: string;
  resposta: string;
  timestamp: string;
}
